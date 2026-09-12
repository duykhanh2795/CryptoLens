package com.cryptolens.app.features.watchlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_groups")
data class WatchlistGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortIndex: Int = 0,
)
