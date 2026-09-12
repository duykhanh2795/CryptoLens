package com.cryptolens.app.features.watchlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_compare_presets")
data class WatchlistComparePresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coinIds: String,
    val createdAt: Long = System.currentTimeMillis(),
)
