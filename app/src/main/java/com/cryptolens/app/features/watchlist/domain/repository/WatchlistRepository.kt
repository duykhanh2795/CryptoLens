package com.cryptolens.app.features.watchlist.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<WatchlistItem>>
    fun observeIsWatchlisted(coinId: String): Flow<Boolean>
    suspend fun addToWatchlist(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): Resource<Unit>
    suspend fun removeFromWatchlist(coinId: String): Resource<Unit>
    suspend fun toggleWatchlist(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): Resource<Boolean>   // returns new state: true = added, false = removed
}
