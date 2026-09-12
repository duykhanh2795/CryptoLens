package com.cryptolens.app.features.watchlist.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.domain.model.WatchlistComparePreset
import com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<WatchlistItem>>
    fun observeGroups(): Flow<List<WatchlistGroup>>
    fun observeComparePresets(): Flow<List<WatchlistComparePreset>>
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
    suspend fun createGroup(name: String): Resource<WatchlistGroup>
    suspend fun renameGroup(groupId: String, name: String): Resource<Unit>
    suspend fun deleteGroup(groupId: String): Resource<Unit>
    suspend fun addCoinsToGroup(coinIds: Set<String>, groupId: String): Resource<Unit>
    suspend fun removeCoinsFromGroup(coinIds: Set<String>, groupId: String): Resource<Unit>
    suspend fun setPinned(coinId: String, pinned: Boolean): Resource<Unit>
    suspend fun moveToTop(coinId: String): Resource<Unit>
    suspend fun saveComparePreset(coinIds: List<String>): Resource<WatchlistComparePreset>
    suspend fun deleteComparePreset(presetId: String): Resource<Unit>
}
