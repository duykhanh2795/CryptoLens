package com.cryptolens.app.features.watchlist.domain.model

import com.cryptolens.app.features.market.domain.model.Coin

data class WatchlistItem(
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val addedAt: Long,
    // Enriched with live market data after fetch
    val coin: Coin? = null,
) {
    val currentPrice: Double get() = coin?.currentPrice ?: 0.0
    val priceChangePercent24h: Double get() = coin?.priceChangePercent24h ?: 0.0
    val isPositive: Boolean get() = priceChangePercent24h >= 0
    val hasMarketData: Boolean get() = coin != null
}
