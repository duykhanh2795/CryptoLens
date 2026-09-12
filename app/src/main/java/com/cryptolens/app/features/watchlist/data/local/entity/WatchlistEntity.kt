package com.cryptolens.app.features.watchlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val addedAt: Long = System.currentTimeMillis(),
    val groupIds: String = WatchlistDefaults.DEFAULT_GROUP_ID,
    val isPinned: Boolean = false,
    val sortIndex: Int = 0,
)

object WatchlistDefaults {
    const val DEFAULT_GROUP_ID = "spot"
    const val DEFAULT_GROUP_NAME = "Spot"
}
