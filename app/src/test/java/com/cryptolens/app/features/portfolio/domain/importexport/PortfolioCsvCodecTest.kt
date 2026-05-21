package com.cryptolens.app.features.portfolio.domain.importexport

import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioCsvCodecTest {
    private val codec = PortfolioCsvCodec()

    @Test
    fun `round trips quoted transaction fields`() {
        val transactions = listOf(
            Transaction(
                id = "tx-1",
                coinId = "bitcoin",
                symbol = "BTC",
                name = "Bitcoin",
                imageUrl = "https://example.com/btc.png",
                type = TransactionType.BUY,
                quantity = 0.25,
                price = 65000.0,
                fee = 1.5,
                note = "DCA, weekly \"buy\"",
                timestamp = 1_700_000_000_000,
            )
        )

        val decoded = codec.decode(codec.encode(transactions))

        assertEquals(transactions, decoded)
    }

    @Test
    fun `rejects missing required column`() {
        val result = runCatching {
            codec.decode("id,coin_id,symbol\n1,bitcoin,BTC")
        }

        assertTrue(result.exceptionOrNull() is PortfolioCsvCodec.CsvParseException)
        assertEquals("Missing required column: name", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rejects invalid sell quantity value`() {
        val csv = """
            id,coin_id,symbol,name,image_url,type,quantity,price_usd,fee_usd,note,timestamp_ms
            tx-1,bitcoin,BTC,Bitcoin,,SELL,0,65000,0,,1700000000000
        """.trimIndent()

        val result = runCatching { codec.decode(csv) }

        assertTrue(result.exceptionOrNull() is PortfolioCsvCodec.CsvParseException)
        assertEquals("Line 2: quantity must be greater than 0", result.exceptionOrNull()?.message)
    }
}
