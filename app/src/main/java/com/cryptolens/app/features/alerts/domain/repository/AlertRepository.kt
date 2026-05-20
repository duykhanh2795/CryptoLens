package com.cryptolens.app.features.alerts.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
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
        type: AlertType,
        valueType: AlertValueType,
        frequency: AlertFrequency,
        condition: AlertCondition,
        targetValue: Double,
        baselineValue: Double,
        note: String,
    ): Resource<PriceAlert>
    suspend fun deleteAlert(id: String): Resource<Unit>
    suspend fun setActive(id: String, active: Boolean): Resource<Unit>
    suspend fun markTriggered(alert: PriceAlert, currentValue: Double): Resource<Unit>
}
