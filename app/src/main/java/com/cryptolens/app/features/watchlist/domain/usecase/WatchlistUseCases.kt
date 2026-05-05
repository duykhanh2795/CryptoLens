package com.cryptolens.app.features.watchlist.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWatchlistUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<WatchlistItem>> = repo.observeWatchlist()
}

class ObserveIsWatchlistedUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    operator fun invoke(coinId: String): Flow<Boolean> = repo.observeIsWatchlisted(coinId)
}

class ToggleWatchlistUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
    ): Resource<Boolean> = repo.toggleWatchlist(coinId, symbol, name, imageUrl)
}

class RemoveFromWatchlistUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinId: String): Resource<Unit> =
        repo.removeFromWatchlist(coinId)
}
