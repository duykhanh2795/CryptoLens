package com.cryptolens.app.features.portfolio.data.local.dao

import androidx.room.*
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioSnapshotEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioAssetDao {

    @Query("SELECT * FROM portfolio_assets WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAssets(userId: String): Flow<List<PortfolioAssetEntity>>

    @Query("SELECT * FROM portfolio_assets WHERE userId = :userId AND coinId = :coinId LIMIT 1")
    suspend fun getAsset(userId: String, coinId: String): PortfolioAssetEntity?

    @Query("SELECT * FROM portfolio_assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: String): PortfolioAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: PortfolioAssetEntity)

    @Query("DELETE FROM portfolio_assets WHERE id = :id AND userId = :userId")
    suspend fun deleteAsset(id: String, userId: String)

    @Query("DELETE FROM portfolio_assets WHERE userId = :userId")
    suspend fun deleteAllAssets(userId: String)
}

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND coinId = :coinId ORDER BY timestamp DESC")
    fun observeTransactionsByCoin(userId: String, coinId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND coinId = :coinId ORDER BY timestamp ASC")
    suspend fun getTransactionsByCoin(userId: String, coinId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE sourceConnectionId = :connectionId")
    suspend fun getTransactionsByConnection(connectionId: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND coinId = :coinId")
    suspend fun deleteTransactionsByCoin(userId: String, coinId: String)

    @Query("DELETE FROM transactions WHERE sourceConnectionId = :connectionId")
    suspend fun deleteTransactionsByConnection(connectionId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactions(userId: String)
}

@Dao
interface PortfolioSnapshotDao {
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM portfolio_snapshots
            WHERE userId = :userId
            ORDER BY dayStart DESC
            LIMIT :limit
        )
        ORDER BY dayStart ASC
        """
    )
    fun observeSnapshots(userId: String, limit: Int = 90): Flow<List<PortfolioSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: PortfolioSnapshotEntity)

    @Query("DELETE FROM portfolio_snapshots WHERE userId = :userId")
    suspend fun deleteAllSnapshots(userId: String)
}
