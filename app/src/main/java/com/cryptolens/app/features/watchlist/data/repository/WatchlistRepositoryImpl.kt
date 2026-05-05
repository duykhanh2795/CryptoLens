package com.cryptolens.app.features.watchlist.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.data.local.dao.WatchlistDao
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistEntity
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<WatchlistItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeIsWatchlisted(coinId: String): Flow<Boolean> =
        dao.observeIsWatchlisted(coinId)

    override suspend fun addToWatchlist(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): Resource<Unit> = try {
        dao.insert(WatchlistEntity(coinId, symbol, name, imageUrl))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to add to watchlist: ${e.message}")
    }

    override suspend fun removeFromWatchlist(coinId: String): Resource<Unit> = try {
        dao.delete(coinId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to remove from watchlist: ${e.message}")
    }

    override suspend fun toggleWatchlist(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): Resource<Boolean> = try {
        val isCurrently = dao.isWatchlisted(coinId)
        if (isCurrently) {
            dao.delete(coinId)
            Resource.Success(false)
        } else {
            dao.insert(WatchlistEntity(coinId, symbol, name, imageUrl))
            Resource.Success(true)
        }
    } catch (e: Exception) {
        Resource.Error("Failed to toggle watchlist: ${e.message}")
    }

    private fun WatchlistEntity.toDomain() = WatchlistItem(
        coinId   = coinId,
        symbol   = symbol,
        name     = name,
        imageUrl = imageUrl,
        addedAt  = addedAt,
    )
}
