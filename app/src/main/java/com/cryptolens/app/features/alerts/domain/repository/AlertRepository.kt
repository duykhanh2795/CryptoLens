package com.cryptolens.app.features.alerts.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeAlerts(): Flow<List<PriceAlert>>
    fun observeActiveAlerts(): Flow<List<PriceAlert>>
    suspend fun getActiveAlerts(): List<PriceAlert>
    suspend fun createAlert(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
        condition: AlertCondition,
        targetPrice: Double,
        note: String,
    ): Resource<PriceAlert>
    suspend fun deleteAlert(id: String): Resource<Unit>
    suspend fun setActive(id: String, active: Boolean): Resource<Unit>
    suspend fun markTriggered(id: String): Resource<Unit>
}
