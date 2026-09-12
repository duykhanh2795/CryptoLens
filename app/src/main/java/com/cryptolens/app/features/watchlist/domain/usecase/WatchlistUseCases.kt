package com.cryptolens.app.features.watchlist.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.domain.model.WatchlistComparePreset
import com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWatchlistUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<WatchlistItem>> = repo.observeWatchlist()
}

class ObserveWatchlistGroupsUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<WatchlistGroup>> = repo.observeGroups()
}

class ObserveWatchlistComparePresetsUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<WatchlistComparePreset>> =
        repo.observeComparePresets()
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

class CreateWatchlistGroupUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(name: String): Resource<WatchlistGroup> =
        repo.createGroup(name)
}

class DeleteWatchlistGroupUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(groupId: String): Resource<Unit> =
        repo.deleteGroup(groupId)
}

class RenameWatchlistGroupUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(groupId: String, name: String): Resource<Unit> =
        repo.renameGroup(groupId, name)
}

class AddCoinsToWatchlistGroupUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinIds: Set<String>, groupId: String): Resource<Unit> =
        repo.addCoinsToGroup(coinIds, groupId)
}

class RemoveCoinsFromWatchlistGroupUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinIds: Set<String>, groupId: String): Resource<Unit> =
        repo.removeCoinsFromGroup(coinIds, groupId)
}

class SetWatchlistPinnedUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinId: String, pinned: Boolean): Resource<Unit> =
        repo.setPinned(coinId, pinned)
}

class MoveWatchlistCoinToTopUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinId: String): Resource<Unit> =
        repo.moveToTop(coinId)
}

class SaveWatchlistComparePresetUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(coinIds: List<String>): Resource<WatchlistComparePreset> =
        repo.saveComparePreset(coinIds)
}

class DeleteWatchlistComparePresetUseCase @Inject constructor(
    private val repo: WatchlistRepository,
) {
    suspend operator fun invoke(presetId: String): Resource<Unit> =
        repo.deleteComparePreset(presetId)
}
