package com.cryptolens.app.features.alerts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptolens.app.features.alerts.data.local.entity.PriceAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE status = 'ACTIVE'")
    suspend fun getActive(): List<PriceAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: PriceAlertEntity)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE price_alerts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE price_alerts SET status = 'TRIGGERED', triggeredAt = :triggeredAt WHERE id = :id")
    suspend fun markTriggered(id: String, triggeredAt: Long)

    @Query("UPDATE price_alerts SET triggeredAt = :triggeredAt, baselineValue = :baselineValue WHERE id = :id")
    suspend fun markPersistentTriggered(id: String, triggeredAt: Long, baselineValue: Double)
}
