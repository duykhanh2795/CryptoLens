package com.cryptolens.app.features.wallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem

@Entity(
    tableName = "wallet_watchlist",
    indices = [
        Index(value = ["userId", "chain", "address"], unique = true),
    ],
)
data class WalletWatchEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val label: String,
    val address: String,
    val chain: String,
    val nativeBalance: Double,
    val txCount: Long,
    val lastSyncedAt: Long?,
    val syncError: String?,
    val createdAt: Long,
) {
    fun toDomain(): WalletWatchItem? {
        val walletChain = WalletChain.fromStored(chain) ?: return null
        return WalletWatchItem(
            id = id,
            userId = userId,
            label = label,
            address = address,
            chain = walletChain,
            nativeBalance = nativeBalance,
            txCount = txCount,
            lastSyncedAt = lastSyncedAt,
            syncError = syncError,
            createdAt = createdAt,
        )
    }
}

fun WalletWatchItem.toEntity(): WalletWatchEntity =
    WalletWatchEntity(
        id = id,
        userId = userId,
        label = label,
        address = address,
        chain = chain.name,
        nativeBalance = nativeBalance,
        txCount = txCount,
        lastSyncedAt = lastSyncedAt,
        syncError = syncError,
        createdAt = createdAt,
    )
