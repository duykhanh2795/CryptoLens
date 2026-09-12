package com.cryptolens.app.features.watchlist.domain.model

import com.cryptolens.app.features.market.domain.model.Coin

data class WatchlistItem(
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val addedAt: Long,
    val groupIds: Set<String> = setOf(WatchlistGroup.DEFAULT_ID),
    val isPinned: Boolean = false,
    val sortIndex: Int = 0,
    // Enriched with live market data after fetch
    val coin: Coin? = null,
) {
    val currentPrice: Double get() = coin?.currentPrice ?: 0.0
    val priceChangePercent24h: Double get() = coin?.priceChangePercent24h ?: 0.0
    val isPositive: Boolean get() = priceChangePercent24h >= 0
    val hasMarketData: Boolean get() = coin != null
}

data class WatchlistGroup(
    val id: String,
    val name: String,
    val createdAt: Long,
    val sortIndex: Int = 0,
) {
    companion object {
        const val DEFAULT_ID = "spot"
        const val DEFAULT_NAME = "Spot"

        val default = WatchlistGroup(
            id = DEFAULT_ID,
            name = DEFAULT_NAME,
            createdAt = 0L,
            sortIndex = 0,
        )
    }
}

data class WatchlistComparePreset(
    val id: String,
    val name: String,
    val coinIds: List<String>,
    val createdAt: Long,
)
