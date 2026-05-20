package com.cryptolens.app.features.alerts.domain.model

enum class AlertCondition(val label: String, val symbol: String) {
    ABOVE("Above", ">="),
    BELOW("Below", "<=");
}

enum class AlertStatus {
    ACTIVE,
    TRIGGERED,
    PAUSED
}

data class PriceAlert(
    val id: String,
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val condition: AlertCondition,
    val targetPrice: Double,
    val currentPrice: Double = 0.0,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val note: String = "",
    val createdAt: Long,
    val triggeredAt: Long? = null,
) {
    val isActive: Boolean get() = status == AlertStatus.ACTIVE

    fun isTriggered(price: Double): Boolean = when (condition) {
        AlertCondition.ABOVE -> price >= targetPrice
        AlertCondition.BELOW -> price <= targetPrice
    }
}
