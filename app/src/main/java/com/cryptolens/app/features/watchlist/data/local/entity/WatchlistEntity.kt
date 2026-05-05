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
)
