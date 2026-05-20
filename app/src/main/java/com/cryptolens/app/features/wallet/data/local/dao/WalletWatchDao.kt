package com.cryptolens.app.features.wallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptolens.app.features.wallet.data.local.entity.WalletWatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletWatchDao {
    @Query("SELECT * FROM wallet_watchlist WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeWallets(userId: String): Flow<List<WalletWatchEntity>>

    @Query("SELECT * FROM wallet_watchlist WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getWallets(userId: String): List<WalletWatchEntity>

    @Query("SELECT * FROM wallet_watchlist WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getWallet(id: String, userId: String): WalletWatchEntity?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM wallet_watchlist
            WHERE userId = :userId AND chain = :chain AND address = :address
        )
        """,
    )
    suspend fun exists(userId: String, chain: String, address: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: WalletWatchEntity)

    @Query("DELETE FROM wallet_watchlist WHERE id = :id AND userId = :userId")
    suspend fun delete(id: String, userId: String)

    @Query(
        """
        UPDATE wallet_watchlist
        SET nativeBalance = :nativeBalance,
            txCount = :txCount,
            lastSyncedAt = :lastSyncedAt,
            syncError = NULL
        WHERE id = :id AND userId = :userId
        """,
    )
    suspend fun updateSyncSuccess(
        id: String,
        userId: String,
        nativeBalance: Double,
        txCount: Long,
        lastSyncedAt: Long,
    )

    @Query(
        """
        UPDATE wallet_watchlist
        SET lastSyncedAt = :lastSyncedAt,
            syncError = :syncError
        WHERE id = :id AND userId = :userId
        """,
    )
    suspend fun updateSyncError(
        id: String,
        userId: String,
        lastSyncedAt: Long,
        syncError: String,
    )
}
