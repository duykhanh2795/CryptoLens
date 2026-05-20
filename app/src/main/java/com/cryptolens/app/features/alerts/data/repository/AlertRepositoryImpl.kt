package com.cryptolens.app.features.alerts.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.data.local.dao.PriceAlertDao
import com.cryptolens.app.features.alerts.data.local.entity.PriceAlertEntity
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val dao: PriceAlertDao,
) : AlertRepository {
    override fun observeAlerts(): Flow<List<PriceAlert>> =
        dao.observeAll().map { alerts -> alerts.map { it.toDomain() } }

    override fun observeActiveAlerts(): Flow<List<PriceAlert>> =
        dao.observeActive().map { alerts -> alerts.map { it.toDomain() } }

    override suspend fun getActiveAlerts(): List<PriceAlert> =
        dao.getActive().map { it.toDomain() }

    override suspend fun createAlert(
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
    ): Resource<PriceAlert> = runCatching {
        val alert = PriceAlert(
            id = UUID.randomUUID().toString(),
            coinId = coinId,
            symbol = symbol.uppercase(),
            name = name,
            imageUrl = imageUrl,
            type = type,
            valueType = valueType,
            frequency = frequency,
            condition = condition,
            targetPrice = targetValue,
            baselineValue = baselineValue,
            status = AlertStatus.ACTIVE,
            note = note,
            createdAt = System.currentTimeMillis(),
        )
        dao.upsert(PriceAlertEntity.fromDomain(alert))
        Resource.Success(alert)
    }.getOrElse { Resource.Error("Could not create alert.", it) }

    override suspend fun deleteAlert(id: String): Resource<Unit> = runCatching {
        dao.delete(id)
        Resource.Success(Unit)
    }.getOrElse { Resource.Error("Could not delete alert.", it) }

    override suspend fun setActive(id: String, active: Boolean): Resource<Unit> = runCatching {
        dao.updateStatus(id, if (active) AlertStatus.ACTIVE.name else AlertStatus.PAUSED.name)
        Resource.Success(Unit)
    }.getOrElse { Resource.Error("Could not update alert.", it) }

    override suspend fun markTriggered(alert: PriceAlert, currentValue: Double): Resource<Unit> = runCatching {
        val now = System.currentTimeMillis()
        if (alert.frequency == AlertFrequency.PERSISTENT) {
            dao.markPersistentTriggered(alert.id, now, currentValue)
        } else {
            dao.markTriggered(alert.id, now)
        }
        Resource.Success(Unit)
    }.getOrElse { Resource.Error("Could not mark alert as triggered.", it) }
}
