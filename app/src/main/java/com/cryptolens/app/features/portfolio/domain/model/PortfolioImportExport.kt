package com.cryptolens.app.features.portfolio.domain.model

enum class PortfolioImportMode {
    APPEND,
    REPLACE,
}

data class PortfolioImportPreview(
    val transactions: List<Transaction>,
) {
    val transactionCount: Int get() = transactions.size
    val buyCount: Int get() = transactions.count { it.type == TransactionType.BUY }
    val sellCount: Int get() = transactions.count { it.type == TransactionType.SELL }
    val coinCount: Int get() = transactions.map { it.coinId }.distinct().size
    val firstTimestamp: Long? get() = transactions.minOfOrNull { it.timestamp }
    val lastTimestamp: Long? get() = transactions.maxOfOrNull { it.timestamp }
}
