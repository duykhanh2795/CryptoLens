package com.cryptolens.app.features.portfolio.domain.model

data class PortfolioAsset(
    val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val quantity: Double,
    val averageBuyPrice: Double,
    val currentPrice: Double = 0.0,
    val priceChangePercent24h: Double = 0.0,
) {
    val totalInvested: Double get() = quantity * averageBuyPrice
    val currentValue: Double get() = quantity * currentPrice
    val profitLoss: Double get() = currentValue - totalInvested
    val profitLossPercent: Double get() =
        if (totalInvested == 0.0) 0.0 else (profitLoss / totalInvested) * 100
    val isProfit: Boolean get() = profitLoss >= 0
}

data class Transaction(
    val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val type: TransactionType,
    val quantity: Double,
    val price: Double,
    val fee: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
) {
    val total: Double get() = quantity * price
}

enum class TransactionType {
    BUY, SELL
}

data class PortfolioSummary(
    val totalValue: Double,
    val totalInvested: Double,
    val totalProfitLoss: Double,
    val totalProfitLossPercent: Double,
    val assets: List<PortfolioAsset>,
) {
    val isProfit: Boolean get() = totalProfitLoss >= 0
}
