package com.cryptolens.app.features.portfolio.data.repository

import androidx.room.withTransaction
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.core.data.local.CryptoDatabase
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioSnapshotDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioSnapshotEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.features.portfolio.domain.model.PortfolioImportMode
import com.cryptolens.app.features.portfolio.domain.model.PortfolioSnapshot
import com.cryptolens.app.features.portfolio.domain.model.PortfolioSummary
import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val database: CryptoDatabase,
    private val assetDao: PortfolioAssetDao,
    private val txDao: TransactionDao,
    private val snapshotDao: PortfolioSnapshotDao,
) : PortfolioRepository {

    override fun observeAssets(userId: String): Flow<List<PortfolioAsset>> =
        assetDao.observeAssets(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTransactions(userId: String): Flow<List<Transaction>> =
        txDao.observeTransactions(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTransactionsByCoin(userId: String, coinId: String): Flow<List<Transaction>> =
        txDao.observeTransactionsByCoin(userId, coinId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeSnapshots(userId: String, limit: Int): Flow<List<PortfolioSnapshot>> =
        snapshotDao.observeSnapshots(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addTransaction(
        userId: String,
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
        type: TransactionType,
        quantity: Double,
        price: Double,
        fee: Double,
        note: String,
    ): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        return try {
            database.withTransaction {
                val existingAsset = assetDao.getAsset(userId, coinId)
                if (type == TransactionType.SELL && (existingAsset == null || existingAsset.quantity < quantity)) {
                    throw PortfolioValidationException("Sell quantity exceeds current holdings.")
                }

                val tx = TransactionEntity(
                    id       = UUID.randomUUID().toString(),
                    userId   = userId,
                    coinId   = coinId,
                    symbol   = symbol,
                    name     = name,
                    imageUrl = imageUrl,
                    type     = type.name,
                    quantity = quantity,
                    price    = price,
                    fee      = fee,
                    note     = note,
                )
                txDao.insertTransaction(tx)

                val updatedAsset = recalculateAsset(
                    existing = existingAsset,
                    type     = type,
                    quantity = quantity,
                    price    = price,
                    fee      = fee,
                    userId   = userId,
                    coinId   = coinId,
                    symbol   = symbol,
                    name     = name,
                    imageUrl = imageUrl,
                )
                if (updatedAsset != null) {
                    assetDao.upsertAsset(updatedAsset)
                } else if (existingAsset != null) {
                    assetDao.deleteAsset(existingAsset.id, userId)
                }
            }
            Resource.Success(Unit)
        } catch (e: PortfolioValidationException) {
            Resource.Error(e.message ?: "Invalid portfolio transaction")
        } catch (e: Exception) {
            Resource.Error("Failed to save transaction: ${e.message}", e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String, userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        return try {
            database.withTransaction {
                val transaction = txDao.getTransactionById(transactionId)
                    ?: throw PortfolioValidationException("Transaction not found")
                if (transaction.userId != userId) {
                    throw PortfolioValidationException("Transaction not found")
                }
                txDao.deleteTransaction(transactionId)
                rebuildAssetFromTransactions(userId, transaction.coinId)
            }
            Resource.Success(Unit)
        } catch (e: PortfolioValidationException) {
            Resource.Error(e.message ?: "Invalid portfolio transaction")
        } catch (e: Exception) {
            Resource.Error("Failed to delete transaction: ${e.message}", e)
        }
    }

    override suspend fun deleteAsset(assetId: String, userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        return try {
            database.withTransaction {
                val asset = assetDao.getAssetById(assetId)
                    ?: throw PortfolioValidationException("Asset not found")
                if (asset.userId != userId) {
                    throw PortfolioValidationException("Asset not found")
                }
                txDao.deleteTransactionsByCoin(userId, asset.coinId)
                assetDao.deleteAsset(assetId, userId)
            }
            Resource.Success(Unit)
        } catch (e: PortfolioValidationException) {
            Resource.Error(e.message ?: "Invalid portfolio asset")
        } catch (e: Exception) {
            Resource.Error("Failed to delete asset: ${e.message}", e)
        }
    }

    override suspend fun importTransactions(
        userId: String,
        transactions: List<Transaction>,
        mode: PortfolioImportMode,
    ): Resource<Int> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        if (transactions.isEmpty()) return Resource.Error("No transactions to import")

        return try {
            val importedCount = transactions.distinctBy { it.id }.size
            database.withTransaction {
                val normalizedImports = transactions
                    .distinctBy { it.id }
                    .map { it.copy(symbol = it.symbol.uppercase()) }
                val importedIds = normalizedImports.map { it.id }.toSet()
                val existingTransactions = when (mode) {
                    PortfolioImportMode.APPEND -> txDao.getTransactions(userId)
                    PortfolioImportMode.REPLACE -> emptyList()
                }
                val completeHistory = when (mode) {
                    PortfolioImportMode.REPLACE -> normalizedImports
                    PortfolioImportMode.APPEND ->
                        existingTransactions.filterNot { it.id in importedIds }.map { it.toDomain() } +
                            normalizedImports
                }

                validateHoldings(completeHistory)

                if (mode == PortfolioImportMode.REPLACE) {
                    assetDao.deleteAllAssets(userId)
                    txDao.deleteAllTransactions(userId)
                    snapshotDao.deleteAllSnapshots(userId)
                }

                txDao.upsertTransactions(normalizedImports.map { it.toEntity(userId) })
                (normalizedImports.map { it.coinId } +
                    existingTransactions.filter { it.id in importedIds }.map { it.coinId })
                    .distinct()
                    .forEach { coinId ->
                        rebuildAssetFromTransactions(userId, coinId)
                    }
            }
            Resource.Success(importedCount)
        } catch (e: PortfolioValidationException) {
            Resource.Error(e.message ?: "Invalid imported portfolio")
        } catch (e: Exception) {
            Resource.Error("Failed to import portfolio: ${e.message}", e)
        }
    }

    override suspend fun saveSnapshot(
        userId: String,
        summary: PortfolioSummary,
        timestamp: Long,
    ): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        return try {
            val dayStart = timestamp.startOfDayMillis()
            snapshotDao.upsertSnapshot(
                PortfolioSnapshotEntity(
                    userId = userId,
                    dayStart = dayStart,
                    totalValue = summary.totalValue,
                    totalInvested = summary.totalInvested,
                    totalProfitLoss = summary.totalProfitLoss,
                    totalProfitLossPercent = summary.totalProfitLossPercent,
                    assetCount = summary.assets.size,
                    createdAt = timestamp,
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save portfolio snapshot: ${e.message}", e)
        }
    }

    override suspend fun clearAll(userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")
        return try {
            database.withTransaction {
                assetDao.deleteAllAssets(userId)
                txDao.deleteAllTransactions(userId)
                snapshotDao.deleteAllSnapshots(userId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to clear portfolio: ${e.message}", e)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun recalculateAsset(
        existing: PortfolioAssetEntity?,
        type: TransactionType,
        quantity: Double,
        price: Double,
        fee: Double,
        userId: String,
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): PortfolioAssetEntity? {
        return when (type) {
            TransactionType.BUY -> {
                val prevQty   = existing?.quantity ?: 0.0
                val prevAvg   = existing?.averageBuyPrice ?: 0.0
                val newQty    = prevQty + quantity
                val newCost   = (prevQty * prevAvg) + (quantity * price) + fee
                val newAvg    = newCost / newQty
                PortfolioAssetEntity(
                    id             = existing?.id ?: UUID.randomUUID().toString(),
                    userId         = userId,
                    coinId         = coinId,
                    symbol         = symbol,
                    name           = name,
                    imageUrl       = imageUrl,
                    quantity       = newQty,
                    averageBuyPrice = newAvg,
                    createdAt      = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt      = System.currentTimeMillis(),
                )
            }
            TransactionType.SELL -> {
                val prevQty = existing?.quantity ?: 0.0
                val newQty  = (prevQty - quantity).coerceAtLeast(0.0)
                if (newQty <= 0.0) {
                    null
                } else {
                    existing?.copy(quantity = newQty, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    private suspend fun rebuildAssetFromTransactions(userId: String, coinId: String) {
        val history = txDao.getTransactionsByCoin(userId, coinId)
        val existing = assetDao.getAsset(userId, coinId)
        if (history.isEmpty()) {
            existing?.let { assetDao.deleteAsset(it.id, userId) }
            return
        }

        var quantity = 0.0
        var averageBuyPrice = 0.0
        var lastMetadata: TransactionEntity? = null

        history.forEach { tx ->
            lastMetadata = tx
            when (TransactionType.valueOf(tx.type)) {
                TransactionType.BUY -> {
                    val newQty = quantity + tx.quantity
                    if (newQty > 0.0) {
                        val newCost = (quantity * averageBuyPrice) + (tx.quantity * tx.price) + tx.fee
                        averageBuyPrice = newCost / newQty
                    }
                    quantity = newQty
                }
                TransactionType.SELL -> {
                    quantity = (quantity - tx.quantity).coerceAtLeast(0.0)
                }
            }
        }

        if (quantity <= 0.0) {
            existing?.let { assetDao.deleteAsset(it.id, userId) }
            return
        }

        val metadata = lastMetadata ?: return
        assetDao.upsertAsset(
            PortfolioAssetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = userId,
                coinId = coinId,
                symbol = metadata.symbol,
                name = metadata.name,
                imageUrl = metadata.imageUrl,
                quantity = quantity,
                averageBuyPrice = averageBuyPrice,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun validateHoldings(transactions: List<Transaction>) {
        val quantities = mutableMapOf<String, Double>()
        transactions
            .sortedWith(compareBy<Transaction> { it.timestamp }.thenBy { it.type.ordinal })
            .forEach { tx ->
                val current = quantities[tx.coinId] ?: 0.0
                val next = when (tx.type) {
                    TransactionType.BUY -> current + tx.quantity
                    TransactionType.SELL -> current - tx.quantity
                }
                if (next < -0.00000001) {
                    throw PortfolioValidationException(
                        "Imported ${tx.symbol} sells more than current holdings"
                    )
                }
                quantities[tx.coinId] = next.coerceAtLeast(0.0)
            }
    }

    private fun PortfolioAssetEntity.toDomain() = PortfolioAsset(
        id              = id,
        coinId          = coinId,
        symbol          = symbol,
        name            = name,
        imageUrl        = imageUrl,
        quantity        = quantity,
        averageBuyPrice = averageBuyPrice,
    )

    private fun TransactionEntity.toDomain() = Transaction(
        id        = id,
        coinId    = coinId,
        symbol    = symbol,
        name      = name,
        imageUrl  = imageUrl,
        type      = TransactionType.valueOf(type),
        quantity  = quantity,
        price     = price,
        fee       = fee,
        note      = note,
        timestamp = timestamp,
    )

    private fun Transaction.toEntity(userId: String) = TransactionEntity(
        id = id,
        userId = userId,
        coinId = coinId,
        symbol = symbol,
        name = name,
        imageUrl = imageUrl,
        type = type.name,
        quantity = quantity,
        price = price,
        fee = fee,
        note = note,
        timestamp = timestamp,
    )

    private fun PortfolioSnapshotEntity.toDomain() = PortfolioSnapshot(
        userId = userId,
        dayStart = dayStart,
        totalValue = totalValue,
        totalInvested = totalInvested,
        totalProfitLoss = totalProfitLoss,
        totalProfitLossPercent = totalProfitLossPercent,
        assetCount = assetCount,
        createdAt = createdAt,
    )

    private fun Long.startOfDayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private class PortfolioValidationException(message: String) : IllegalArgumentException(message)
}
