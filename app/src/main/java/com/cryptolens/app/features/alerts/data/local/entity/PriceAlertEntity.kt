package com.cryptolens.app.features.alerts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.PriceAlert

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val condition: String,
    val targetPrice: Double,
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
        condition = runCatching { AlertCondition.valueOf(condition) }.getOrDefault(AlertCondition.ABOVE),
        targetPrice = targetPrice,
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
            condition = alert.condition.name,
            targetPrice = alert.targetPrice,
            status = alert.status.name,
            note = alert.note,
            createdAt = alert.createdAt,
            triggeredAt = alert.triggeredAt,
        )
    }
}
