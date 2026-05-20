package com.cryptolens.app.features.alerts.domain.model

enum class AlertCondition(val label: String, val symbol: String) {
    ABOVE("Above", ">="),
    BELOW("Below", "<=");

    val movementLabel: String get() = when (this) {
        ABOVE -> "Increases"
        BELOW -> "Decreases"
    }
}

enum class AlertType(val label: String, val description: String) {
    PRICE_LIMIT("Price Limit", "Track coin price changes"),
    VOLUME("Volume", "Track 24h trading volume changes"),
    MARKET_CAP("Market Cap", "Track total market cap changes");
}

enum class AlertValueType(val label: String) {
    NUMBER("Number"),
    PERCENT("Percent");
}

enum class AlertFrequency(val label: String) {
    ONE_TIME("One Time"),
    PERSISTENT("Persistent");
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
    val type: AlertType = AlertType.PRICE_LIMIT,
    val valueType: AlertValueType = AlertValueType.NUMBER,
    val frequency: AlertFrequency = AlertFrequency.ONE_TIME,
    val condition: AlertCondition,
    val targetPrice: Double,
    val baselineValue: Double = 0.0,
    val currentPrice: Double = 0.0,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val note: String = "",
    val createdAt: Long,
    val triggeredAt: Long? = null,
) {
    val isActive: Boolean get() = status == AlertStatus.ACTIVE
    val targetValue: Double get() = targetPrice

    fun thresholdValue(): Double = if (valueType == AlertValueType.PERCENT) {
        when (condition) {
            AlertCondition.ABOVE -> baselineValue * (1.0 + targetPrice / 100.0)
            AlertCondition.BELOW -> baselineValue * (1.0 - targetPrice / 100.0)
        }
    } else {
        targetPrice
    }

    fun isTriggered(currentValue: Double): Boolean {
        if (currentValue <= 0.0) return false
        if (valueType == AlertValueType.PERCENT && baselineValue <= 0.0) return false
        val threshold = thresholdValue()
        return when (condition) {
            AlertCondition.ABOVE -> currentValue >= threshold
            AlertCondition.BELOW -> currentValue <= threshold
        }
    }
}
