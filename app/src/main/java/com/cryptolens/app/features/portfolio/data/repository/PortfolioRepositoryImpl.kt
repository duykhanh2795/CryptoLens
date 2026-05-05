package com.cryptolens.app.features.portfolio.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val assetDao: PortfolioAssetDao,
    private val txDao: TransactionDao,
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
        return try {
            // 1. Save transaction
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

            // 2. Recalculate and upsert the portfolio asset
            val existingAsset = assetDao.getAsset(userId, coinId)
            val updatedAsset  = recalculateAsset(
                existing = existingAsset,
                type     = type,
                quantity = quantity,
                price    = price,
                userId   = userId,
                coinId   = coinId,
                symbol   = symbol,
                name     = name,
                imageUrl = imageUrl,
            )
            if (updatedAsset != null) {
                assetDao.upsertAsset(updatedAsset)
            } else if (existingAsset != null) {
                assetDao.deleteAsset(existingAsset.id)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save transaction: ${e.message}", e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String, userId: String): Resource<Unit> {
        return try {
            val transaction = txDao.getTransactionById(transactionId)
            txDao.deleteTransaction(transactionId)
            if (transaction != null && transaction.userId == userId) {
                rebuildAssetFromTransactions(userId, transaction.coinId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete transaction: ${e.message}", e)
        }
    }

    override suspend fun deleteAsset(assetId: String, userId: String): Resource<Unit> {
        return try {
            assetDao.deleteAsset(assetId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete asset: ${e.message}", e)
        }
    }

    override suspend fun clearAll(userId: String): Resource<Unit> {
        return try {
            assetDao.deleteAllAssets(userId)
            txDao.deleteAllTransactions(userId)
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
                val newAvg    = if (prevQty == 0.0) price
                                else (prevQty * prevAvg + quantity * price) / newQty
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
            existing?.let { assetDao.deleteAsset(it.id) }
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
                        averageBuyPrice = if (quantity == 0.0) tx.price
                        else ((quantity * averageBuyPrice) + (tx.quantity * tx.price)) / newQty
                    }
                    quantity = newQty
                }
                TransactionType.SELL -> {
                    quantity = (quantity - tx.quantity).coerceAtLeast(0.0)
                }
            }
        }

        if (quantity <= 0.0) {
            existing?.let { assetDao.deleteAsset(it.id) }
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
}
