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
            marketRepository.getTopCoins(perPage = 250),
        ) { assets, marketResource ->
            val marketById = when (marketResource) {
                is Resource.Success -> marketResource.data.associateBy { it.id }
                else -> emptyMap()
            }
            val enrichedAssets = assets.map { asset ->
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

            val totalInvested   = enrichedAssets.sumOf { it.totalInvested }
            val totalValue      = enrichedAssets.sumOf { it.currentValue }
            val totalPnl        = totalValue - totalInvested
            val totalPnlPercent = if (totalInvested == 0.0) 0.0 else (totalPnl / totalInvested) * 100
            PortfolioSummary(
                totalValue          = totalValue,
                totalInvested       = totalInvested,
                totalProfitLoss     = totalPnl,
                totalProfitLossPercent = totalPnlPercent,
                assets              = enrichedAssets,
            )
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
        val quantity = quantityStr.toDoubleOrNull()
            ?: return Resource.Error("Invalid quantity")
        if (quantity <= 0) return Resource.Error("Quantity must be greater than 0")

        val price = priceStr.toDoubleOrNull()
            ?: return Resource.Error("Invalid price")
        if (price <= 0) return Resource.Error("Price must be greater than 0")

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
