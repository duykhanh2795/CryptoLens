package com.cryptolens.app.features.wallet.domain.model

data class WalletWatchItem(
    val id: String,
    val userId: String,
    val label: String,
    val address: String,
    val chain: WalletChain,
    val nativeBalance: Double,
    val txCount: Long,
    val lastSyncedAt: Long?,
    val syncError: String?,
    val createdAt: Long,
)

data class WalletWatchListItem(
    val wallet: WalletWatchItem,
    val nativePriceUsd: Double?,
) {
    val nativeValueUsd: Double? =
        nativePriceUsd?.let { price -> wallet.nativeBalance * price }

    val isNativeWhale: Boolean =
        (nativeValueUsd ?: 0.0) >= NATIVE_WHALE_VALUE_USD

    companion object {
        const val NATIVE_WHALE_VALUE_USD = 1_000_000.0
    }
}

data class WalletBalance(
    val nativeBalance: Double,
    val txCount: Long,
)
