package com.cryptolens.app.features.portfolio.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.repository.MarketRepository
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObservePortfolioSummaryUseCase @Inject constructor(
    private val repo: PortfolioRepository,
    private val marketRepository: MarketRepository,
) {
    operator fun invoke(userId: String): Flow<PortfolioSummary> =
        combine(
            repo.observeAssets(userId),
            repo.observeTransactions(userId),
            marketRepository.getTopCoins(perPage = 250),
        ) { assets, transactions, marketResource ->
            val marketById = when (marketResource) {
                is Resource.Success -> marketResource.data.associateBy { it.id }
                else -> emptyMap()
            }
            val pricedAssets = assets.map { asset ->
                val coin = marketById[asset.coinId]
                if (coin != null) {
                    asset.copy(
                        imageUrl = asset.imageUrl.ifBlank { coin.imageUrl },
                        currentPrice = coin.currentPrice,
                        priceChangePercent24h = coin.priceChangePercent24h,
                    )
                } else {
                    asset.copy(currentPrice = asset.averageBuyPrice)
                }
            }

            val totalValue = pricedAssets.sumOf { it.currentValue }
            val enrichedAssets = pricedAssets
                .map { asset ->
                    asset.copy(
                        allocationPercent = if (totalValue == 0.0) 0.0
                        else (asset.currentValue / totalValue) * 100.0,
                    )
                }
                .sortedByDescending { it.currentValue }

            val totalInvested   = enrichedAssets.sumOf { it.totalInvested }
            val unrealizedPnl   = totalValue - totalInvested
            val realizedPnl     = transactions.realizedProfitLoss()
            val feesPaid        = transactions.sumOf { it.fee }
            val totalPnl        = unrealizedPnl + realizedPnl
            val totalPnlPercent = if (totalInvested == 0.0) 0.0 else (totalPnl / totalInvested) * 100
            val totalChange24h  = enrichedAssets.sumOf { it.change24h }
            val previousValue24h = totalValue - totalChange24h
            val totalChange24hPercent = if (previousValue24h == 0.0) 0.0
                else (totalChange24h / previousValue24h) * 100.0
            PortfolioSummary(
                totalValue          = totalValue,
                totalInvested       = totalInvested,
                totalProfitLoss     = totalPnl,
                totalProfitLossPercent = totalPnlPercent,
                unrealizedProfitLoss = unrealizedPnl,
                unrealizedProfitLossPercent = if (totalInvested == 0.0) 0.0
                    else (unrealizedPnl / totalInvested) * 100.0,
                realizedProfitLoss = realizedPnl,
                totalFeesPaid = feesPaid,
                totalChange24h = totalChange24h,
                totalChange24hPercent = totalChange24hPercent,
                assets              = enrichedAssets,
            )
        }

    private fun List<Transaction>.realizedProfitLoss(): Double {
        data class Position(var quantity: Double = 0.0, var averageCost: Double = 0.0)

        val positions = mutableMapOf<String, Position>()
        var realized = 0.0

        sortedBy { it.timestamp }.forEach { tx ->
            val position = positions.getOrPut(tx.coinId) { Position() }
            when (tx.type) {
                TransactionType.BUY -> {
                    val newQuantity = position.quantity + tx.quantity
                    if (newQuantity > 0.0) {
                        val newCost = (position.quantity * position.averageCost) +
                            (tx.quantity * tx.price) + tx.fee
                        position.averageCost = newCost / newQuantity
                    }
                    position.quantity = newQuantity
                }
                TransactionType.SELL -> {
                    val matchedQuantity = minOf(tx.quantity, position.quantity)
                    val proceeds = matchedQuantity * tx.price
                    val cost = matchedQuantity * position.averageCost
                    realized += proceeds - cost - tx.fee
                    position.quantity = (position.quantity - matchedQuantity).coerceAtLeast(0.0)
                    if (position.quantity == 0.0) {
                        position.averageCost = 0.0
                    }
                }
            }
        }

        return realized
    }
}

class ObserveTransactionsUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    operator fun invoke(userId: String): Flow<List<Transaction>> =
        repo.observeTransactions(userId)
}

class ObservePortfolioSnapshotsUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    operator fun invoke(userId: String, limit: Int = 90): Flow<List<PortfolioSnapshot>> =
        repo.observeSnapshots(userId, limit)
}

class AddTransactionUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    suspend operator fun invoke(
        userId: String,
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
        type: TransactionType,
        quantityStr: String,
        priceStr: String,
        fee: Double = 0.0,
        note: String = "",
    ): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Missing user")

        val quantity = quantityStr.toDoubleOrNull()
            ?: return Resource.Error("Invalid quantity")
        if (quantity <= 0) return Resource.Error("Quantity must be greater than 0")

        val price = priceStr.toDoubleOrNull()
            ?: return Resource.Error("Invalid price")
        if (price <= 0) return Resource.Error("Price must be greater than 0")

        if (fee < 0) return Resource.Error("Fee cannot be negative")

        return repo.addTransaction(userId, coinId, symbol, name, imageUrl, type, quantity, price, fee, note)
    }
}

class DeleteTransactionUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    suspend operator fun invoke(transactionId: String, userId: String): Resource<Unit> =
        repo.deleteTransaction(transactionId, userId)
}

class DeleteAssetUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    suspend operator fun invoke(assetId: String, userId: String): Resource<Unit> =
        repo.deleteAsset(assetId, userId)
}

class SavePortfolioSnapshotUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    suspend operator fun invoke(userId: String, summary: PortfolioSummary): Resource<Unit> =
        repo.saveSnapshot(userId, summary, System.currentTimeMillis())
}

class ClearPortfolioUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    suspend operator fun invoke(userId: String): Resource<Unit> =
        repo.clearAll(userId)
}
