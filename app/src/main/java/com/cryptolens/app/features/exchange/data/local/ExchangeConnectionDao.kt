package com.cryptolens.app.features.exchange.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeConnectionDao {

    @Query("SELECT * FROM exchange_connections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExchangeConnectionEntity>>

    @Query("SELECT * FROM exchange_connections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExchangeConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExchangeConnectionEntity)

    @Query("DELETE FROM exchange_connections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE exchange_connections SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("UPDATE exchange_connections SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastSync(id: String, timestamp: Long)
}
