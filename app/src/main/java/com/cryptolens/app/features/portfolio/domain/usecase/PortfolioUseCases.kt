package com.cryptolens.app.features.portfolio.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObservePortfolioSummaryUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    operator fun invoke(userId: String): Flow<PortfolioSummary> =
        repo.observeAssets(userId).map { assets ->
            val totalInvested   = assets.sumOf { it.totalInvested }
            val totalValue      = assets.sumOf { it.currentValue }
            val totalPnl        = totalValue - totalInvested
            val totalPnlPercent = if (totalInvested == 0.0) 0.0 else (totalPnl / totalInvested) * 100
            PortfolioSummary(
                totalValue          = totalValue,
                totalInvested       = totalInvested,
                totalProfitLoss     = totalPnl,
                totalProfitLossPercent = totalPnlPercent,
                assets              = assets,
            )
        }
}

class ObserveTransactionsUseCase @Inject constructor(
    private val repo: PortfolioRepository,
) {
    operator fun invoke(userId: String): Flow<List<Transaction>> =
        repo.observeTransactions(userId)
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
