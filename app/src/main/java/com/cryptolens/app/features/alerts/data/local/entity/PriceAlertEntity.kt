package com.cryptolens.app.features.alerts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val alertType: String = AlertType.PRICE_LIMIT.name,
    val valueType: String = AlertValueType.NUMBER.name,
    val frequency: String = AlertFrequency.ONE_TIME.name,
    val condition: String,
    val targetPrice: Double,
    val baselineValue: Double = 0.0,
    val status: String,
    val note: String,
    val createdAt: Long,
    val triggeredAt: Long?,
) {
    fun toDomain(currentPrice: Double = 0.0): PriceAlert = PriceAlert(
        id = id,
        coinId = coinId,
        symbol = symbol,
        name = name,
        imageUrl = imageUrl,
        type = runCatching { AlertType.valueOf(alertType) }.getOrDefault(AlertType.PRICE_LIMIT),
        valueType = runCatching { AlertValueType.valueOf(valueType) }.getOrDefault(AlertValueType.NUMBER),
        frequency = runCatching { AlertFrequency.valueOf(frequency) }.getOrDefault(AlertFrequency.ONE_TIME),
        condition = runCatching { AlertCondition.valueOf(condition) }.getOrDefault(AlertCondition.ABOVE),
        targetPrice = targetPrice,
        baselineValue = baselineValue,
        currentPrice = currentPrice,
        status = runCatching { AlertStatus.valueOf(status) }.getOrDefault(AlertStatus.ACTIVE),
        note = note,
        createdAt = createdAt,
        triggeredAt = triggeredAt,
    )

    companion object {
        fun fromDomain(alert: PriceAlert): PriceAlertEntity = PriceAlertEntity(
            id = alert.id,
            coinId = alert.coinId,
            symbol = alert.symbol.uppercase(),
            name = alert.name,
            imageUrl = alert.imageUrl,
            alertType = alert.type.name,
            valueType = alert.valueType.name,
            frequency = alert.frequency.name,
            condition = alert.condition.name,
            targetPrice = alert.targetPrice,
            baselineValue = alert.baselineValue,
            status = alert.status.name,
            note = alert.note,
            createdAt = alert.createdAt,
            triggeredAt = alert.triggeredAt,
        )
    }
}
