package com.cryptolens.app.features.watchlist.data.local.dao

import androidx.room.*
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistGroupEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistComparePresetEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY isPinned DESC, sortIndex ASC, addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist_groups ORDER BY sortIndex ASC, createdAt ASC")
    fun observeGroups(): Flow<List<WatchlistGroupEntity>>

    @Query("SELECT * FROM watchlist_compare_presets ORDER BY createdAt DESC")
    fun observeComparePresets(): Flow<List<WatchlistComparePresetEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE coinId = :coinId)")
    fun observeIsWatchlisted(coinId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE coinId = :coinId)")
    suspend fun isWatchlisted(coinId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(entity: WatchlistGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComparePreset(entity: WatchlistComparePresetEntity)

    @Query("UPDATE watchlist_groups SET name = :name WHERE id = :groupId")
    suspend fun renameGroup(groupId: String, name: String)

    @Query("SELECT * FROM watchlist")
    suspend fun getAllOnce(): List<WatchlistEntity>

    @Query("UPDATE watchlist SET groupIds = :groupIds WHERE coinId = :coinId")
    suspend fun updateGroups(coinId: String, groupIds: String)

    @Query("UPDATE watchlist SET isPinned = :isPinned WHERE coinId = :coinId")
    suspend fun updatePinned(coinId: String, isPinned: Boolean)

    @Query("UPDATE watchlist SET sortIndex = :sortIndex WHERE coinId = :coinId")
    suspend fun updateSortIndex(coinId: String, sortIndex: Int)

    @Query("DELETE FROM watchlist WHERE coinId = :coinId")
    suspend fun delete(coinId: String)

    @Query("DELETE FROM watchlist_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM watchlist_compare_presets WHERE id = :presetId")
    suspend fun deleteComparePreset(presetId: String)

    @Query("DELETE FROM watchlist")
    suspend fun deleteAll()
}
