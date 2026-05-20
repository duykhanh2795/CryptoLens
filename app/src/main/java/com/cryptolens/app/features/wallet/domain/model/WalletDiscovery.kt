package com.cryptolens.app.features.wallet.domain.model

data class TrendingWallet(
    val label: String,
    val address: String,
    val chain: WalletChain,
    val nativeBalance: Double,
    val txCount: Long,
    val valueUsd: Double?,
    val changePercent24h: Double,
    val avatarSeed: Int,
) {
    val displayName: String get() = address.shortWalletAddress()
    val isPositive: Boolean get() = changePercent24h >= 0.0
}

data class WalletAsset(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val priceUsd: Double?,
    val valueUsd: Double?,
    val changePercent24h: Double,
    val chain: WalletChain,
    val networkLabel: String = chain.label,
    val contractAddress: String? = null,
)

data class WalletTransaction(
    val id: String,
    val type: WalletTransactionType,
    val symbol: String,
    val amount: Double,
    val valueUsd: Double?,
    val counterparty: String?,
    val timestamp: Long,
    val networkLabel: String,
)

enum class WalletTransactionType {
    RECEIVED,
    SENT,
    EXECUTED
}

data class TrendingWalletDetail(
    val wallet: TrendingWallet,
    val assets: List<WalletAsset>,
    val history: List<WalletTransaction>,
    val historyNote: String?,
) {
    val totalValueUsd: Double? =
        assets.mapNotNull { it.valueUsd }.takeIf { it.isNotEmpty() }?.sum()
}

fun String.shortWalletAddress(): String =
    if (length <= 14) this else "${take(6)}...${takeLast(6)}"
