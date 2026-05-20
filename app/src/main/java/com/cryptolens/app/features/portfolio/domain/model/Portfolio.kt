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
    val allocationPercent: Double = 0.0,
) {
    val totalInvested: Double get() = quantity * averageBuyPrice
    val currentValue: Double get() = quantity * currentPrice
    val profitLoss: Double get() = currentValue - totalInvested
    val profitLossPercent: Double get() =
        if (totalInvested == 0.0) 0.0 else (profitLoss / totalInvested) * 100
    val previousValue24h: Double get() {
        val divisor = 1.0 + (priceChangePercent24h / 100.0)
        return if (divisor <= 0.0) currentValue else currentValue / divisor
    }
    val change24h: Double get() = currentValue - previousValue24h
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
    val unrealizedProfitLoss: Double,
    val unrealizedProfitLossPercent: Double,
    val realizedProfitLoss: Double,
    val totalFeesPaid: Double,
    val totalChange24h: Double,
    val totalChange24hPercent: Double,
    val assets: List<PortfolioAsset>,
) {
    val isProfit: Boolean get() = totalProfitLoss >= 0
    val isUp24h: Boolean get() = totalChange24h >= 0
}

data class PortfolioSnapshot(
    val userId: String,
    val dayStart: Long,
    val totalValue: Double,
    val totalInvested: Double,
    val totalProfitLoss: Double,
    val totalProfitLossPercent: Double,
    val assetCount: Int,
    val createdAt: Long,
) {
    val isProfit: Boolean get() = totalProfitLoss >= 0
}
