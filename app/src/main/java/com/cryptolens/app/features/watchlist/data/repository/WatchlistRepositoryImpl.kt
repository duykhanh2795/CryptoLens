package com.cryptolens.app.features.watchlist.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.watchlist.data.local.dao.WatchlistDao
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistComparePresetEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistDefaults
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistGroupEntity
import com.cryptolens.app.features.watchlist.domain.model.WatchlistComparePreset
import com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<WatchlistItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeGroups(): Flow<List<WatchlistGroup>> =
        dao.observeGroups().map { entities ->
            val groups = entities.map { it.toDomain() }
            listOf(WatchlistGroup.default) +
                groups.filterNot { it.id == WatchlistGroup.DEFAULT_ID }
        }

    override fun observeComparePresets(): Flow<List<WatchlistComparePreset>> =
        dao.observeComparePresets().map { entities -> entities.map { it.toDomain() } }

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

    override suspend fun createGroup(name: String): Resource<WatchlistGroup> = try {
        val cleanName = name.trim().take(20)
        if (cleanName.isBlank()) {
            Resource.Error("Group name is required")
        } else {
            val group = WatchlistGroupEntity(
                id = "group_${UUID.randomUUID()}",
                name = cleanName,
                createdAt = System.currentTimeMillis(),
                sortIndex = Int.MAX_VALUE / 2,
            )
            dao.upsertGroup(group)
            Resource.Success(group.toDomain())
        }
    } catch (e: Exception) {
        Resource.Error("Failed to create group: ${e.message}")
    }

    override suspend fun deleteGroup(groupId: String): Resource<Unit> = try {
        if (groupId == WatchlistGroup.DEFAULT_ID) {
            Resource.Error("Default group cannot be deleted")
        } else {
            dao.getAllOnce().forEach { entity ->
                val updated = entity.groupIdSet().filterNot { it == groupId }.toSet()
                dao.updateGroups(entity.coinId, updated.normalizedGroupIds())
            }
            dao.deleteGroup(groupId)
            Resource.Success(Unit)
        }
    } catch (e: Exception) {
        Resource.Error("Failed to delete group: ${e.message}")
    }

    override suspend fun renameGroup(groupId: String, name: String): Resource<Unit> = try {
        val cleanName = name.trim().take(20)
        if (groupId == WatchlistGroup.DEFAULT_ID) {
            Resource.Error("Default group cannot be renamed")
        } else if (cleanName.isBlank()) {
            Resource.Error("Group name is required")
        } else {
            dao.renameGroup(groupId, cleanName)
            Resource.Success(Unit)
        }
    } catch (e: Exception) {
        Resource.Error("Failed to rename group: ${e.message}")
    }

    override suspend fun addCoinsToGroup(
        coinIds: Set<String>,
        groupId: String,
    ): Resource<Unit> = try {
        val coinIdSet = coinIds.filter { it.isNotBlank() }.toSet()
        if (coinIdSet.isEmpty()) {
            Resource.Error("Select at least one coin")
        } else {
            dao.getAllOnce()
                .filter { it.coinId in coinIdSet }
                .forEach { entity ->
                    dao.updateGroups(
                        entity.coinId,
                        (entity.groupIdSet() + groupId).normalizedGroupIds(),
                    )
                }
            Resource.Success(Unit)
        }
    } catch (e: Exception) {
        Resource.Error("Failed to add coins to group: ${e.message}")
    }

    override suspend fun removeCoinsFromGroup(
        coinIds: Set<String>,
        groupId: String,
    ): Resource<Unit> = try {
        val coinIdSet = coinIds.filter { it.isNotBlank() }.toSet()
        if (coinIdSet.isEmpty()) {
            Resource.Error("Select at least one coin")
        } else {
            if (groupId == WatchlistGroup.DEFAULT_ID) {
                coinIdSet.forEach { dao.delete(it) }
            } else {
                dao.getAllOnce()
                    .filter { it.coinId in coinIdSet }
                    .forEach { entity ->
                        dao.updateGroups(
                            entity.coinId,
                            entity.groupIdSet()
                                .filterNot { it == groupId }
                                .toSet()
                                .normalizedGroupIds(),
                        )
                    }
            }
            Resource.Success(Unit)
        }
    } catch (e: Exception) {
        Resource.Error("Failed to remove coins from group: ${e.message}")
    }

    override suspend fun setPinned(coinId: String, pinned: Boolean): Resource<Unit> = try {
        dao.updatePinned(coinId, pinned)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to update pin: ${e.message}")
    }

    override suspend fun moveToTop(coinId: String): Resource<Unit> = try {
        val minIndex = dao.getAllOnce().minOfOrNull { it.sortIndex } ?: 0
        dao.updateSortIndex(coinId, minIndex - 1)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to move coin: ${e.message}")
    }

    override suspend fun saveComparePreset(
        coinIds: List<String>,
    ): Resource<WatchlistComparePreset> = try {
        val uniqueIds = coinIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (uniqueIds.size < 2) {
            Resource.Error("Select at least two coins")
        } else {
            val existing = dao.observeComparePresets()
                .map { entities ->
                    entities.firstOrNull { entity ->
                        entity.coinIdList().sorted() == uniqueIds.sorted()
                    }
                }
                .first()
            if (existing != null) {
                Resource.Success(existing.toDomain())
            } else {
                val preset = WatchlistComparePresetEntity(
                    id = "compare_${UUID.randomUUID()}",
                    name = uniqueIds.take(3).joinToString(" / ") { it.toDisplayLabel() },
                    coinIds = uniqueIds.joinToString(","),
                    createdAt = System.currentTimeMillis(),
                )
                dao.upsertComparePreset(preset)
                Resource.Success(preset.toDomain())
            }
        }
    } catch (e: Exception) {
        Resource.Error("Failed to save compare: ${e.message}")
    }

    override suspend fun deleteComparePreset(presetId: String): Resource<Unit> = try {
        dao.deleteComparePreset(presetId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to delete compare: ${e.message}")
    }

    private fun WatchlistEntity.toDomain() = WatchlistItem(
        coinId   = coinId,
        symbol   = symbol,
        name     = name,
        imageUrl = imageUrl,
        addedAt  = addedAt,
        groupIds = groupIdSet(),
        isPinned = isPinned,
        sortIndex = sortIndex,
    )

    private fun WatchlistGroupEntity.toDomain() = WatchlistGroup(
        id = id,
        name = name,
        createdAt = createdAt,
        sortIndex = sortIndex,
    )

    private fun WatchlistComparePresetEntity.toDomain() = WatchlistComparePreset(
        id = id,
        name = name,
        coinIds = coinIdList(),
        createdAt = createdAt,
    )

    private fun WatchlistComparePresetEntity.coinIdList(): List<String> =
        coinIds.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun WatchlistEntity.groupIdSet(): Set<String> =
        groupIds.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(WatchlistDefaults.DEFAULT_GROUP_ID) }
            .toSet()

    private fun Set<String>.normalizedGroupIds(): String {
        val normalized = filter { it.isNotBlank() }.ifEmpty {
            listOf(WatchlistDefaults.DEFAULT_GROUP_ID)
        }
        return normalized.sorted().joinToString(",")
    }

    private fun String.toDisplayLabel(): String =
        split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char -> char.uppercaseChar() }
            }
}
