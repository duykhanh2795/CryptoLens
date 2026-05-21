package com.cryptolens.app.features.portfolio.domain.importexport

import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType

class PortfolioCsvCodec {
    fun encode(transactions: List<Transaction>): String {
        val builder = StringBuilder()
        builder.appendLine(HEADER.joinToString(","))
        transactions
            .sortedBy { it.timestamp }
            .forEach { tx ->
                builder.appendLine(
                    listOf(
                        tx.id,
                        tx.coinId,
                        tx.symbol,
                        tx.name,
                        tx.imageUrl,
                        tx.type.name,
                        tx.quantity.toString(),
                        tx.price.toString(),
                        tx.fee.toString(),
                        tx.note,
                        tx.timestamp.toString(),
                    ).joinToString(",") { it.csvEscape() }
                )
            }
        return builder.toString()
    }

    fun decode(content: String): List<Transaction> {
        val rows = parseRows(content)
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) {
            throw CsvParseException("CSV file is empty")
        }

        val header = rows.first().mapIndexed { index, value ->
            if (index == 0) value.removePrefix("\uFEFF") else value
        }.map { it.trim().lowercase() }
        val indexByName = header.withIndex().associate { it.value to it.index }
        REQUIRED_COLUMNS.forEach { column ->
            if (column !in indexByName) {
                throw CsvParseException("Missing required column: $column")
            }
        }

        return rows.drop(1).mapIndexed { rowIndex, row ->
            val lineNumber = rowIndex + 2
            fun value(name: String): String =
                row.getOrNull(indexByName.getValue(name)).orEmpty().trim()

            val id = value("id")
            val coinId = value("coin_id")
            val symbol = value("symbol").uppercase()
            val type = runCatching { TransactionType.valueOf(value("type").uppercase()) }.getOrNull()
                ?: throw CsvParseException("Line $lineNumber: type must be BUY or SELL")
            val quantity = value("quantity").toDoubleOrNull()
                ?: throw CsvParseException("Line $lineNumber: invalid quantity")
            val price = value("price_usd").toDoubleOrNull()
                ?: throw CsvParseException("Line $lineNumber: invalid price_usd")
            val fee = value("fee_usd").ifBlank { "0" }.toDoubleOrNull()
                ?: throw CsvParseException("Line $lineNumber: invalid fee_usd")
            val timestamp = value("timestamp_ms").toLongOrNull()
                ?: throw CsvParseException("Line $lineNumber: invalid timestamp_ms")

            when {
                id.isBlank() -> throw CsvParseException("Line $lineNumber: id cannot be empty")
                coinId.isBlank() -> throw CsvParseException("Line $lineNumber: coin_id cannot be empty")
                symbol.isBlank() -> throw CsvParseException("Line $lineNumber: symbol cannot be empty")
                quantity <= 0.0 -> throw CsvParseException("Line $lineNumber: quantity must be greater than 0")
                price <= 0.0 -> throw CsvParseException("Line $lineNumber: price_usd must be greater than 0")
                fee < 0.0 -> throw CsvParseException("Line $lineNumber: fee_usd cannot be negative")
                timestamp <= 0L -> throw CsvParseException("Line $lineNumber: timestamp_ms must be positive")
            }

            Transaction(
                id = id,
                coinId = coinId,
                symbol = symbol,
                name = value("name").ifBlank { symbol },
                imageUrl = value("image_url"),
                type = type,
                quantity = quantity,
                price = price,
                fee = fee,
                note = value("note"),
                timestamp = timestamp,
            )
        }
    }

    private fun parseRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && inQuotes && content.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    row += field.toString()
                    field.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && content.getOrNull(index + 1) == '\n') {
                        index++
                    }
                    row += field.toString()
                    field.clear()
                    rows += row.toList()
                    row.clear()
                }
                else -> field.append(char)
            }
            index++
        }

        if (inQuotes) {
            throw CsvParseException("CSV contains an unclosed quoted field")
        }
        row += field.toString()
        if (row.any { it.isNotBlank() }) {
            rows += row.toList()
        }
        return rows
    }

    private fun String.csvEscape(): String {
        val escaped = replace("\"", "\"\"")
        return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    class CsvParseException(message: String) : IllegalArgumentException(message)

    private companion object {
        val HEADER = listOf(
            "id",
            "coin_id",
            "symbol",
            "name",
            "image_url",
            "type",
            "quantity",
            "price_usd",
            "fee_usd",
            "note",
            "timestamp_ms",
        )
        val REQUIRED_COLUMNS = HEADER.toSet()
    }
}
