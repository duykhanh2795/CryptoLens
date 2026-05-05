package com.cryptolens.app.features.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_assets")
data class PortfolioAssetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val quantity: Double,
    val averageBuyPrice: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val type: String,           // "BUY" | "SELL"
    val quantity: Double,
    val price: Double,
    val fee: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
