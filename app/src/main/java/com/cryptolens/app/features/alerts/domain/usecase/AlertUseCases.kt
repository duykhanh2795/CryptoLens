package com.cryptolens.app.features.alerts.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAlertsUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    operator fun invoke(): Flow<List<PriceAlert>> = repository.observeAlerts()
}

class GetActiveAlertsUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(): List<PriceAlert> = repository.getActiveAlerts()
}

class CreateAlertUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
        type: AlertType,
        valueType: AlertValueType,
        frequency: AlertFrequency,
        condition: AlertCondition,
        targetValueText: String,
        baselineValue: Double,
        note: String,
    ): Resource<PriceAlert> {
        if (coinId.isBlank() || symbol.isBlank() || name.isBlank()) {
            return Resource.Error("Open a coin detail screen before creating an alert.")
        }

        val targetValue = targetValueText.replace(",", "").trim().toDoubleOrNull()
            ?: return Resource.Error("Enter a valid ${valueType.label.lowercase()} value.")

        if (targetValue <= 0.0) {
            return Resource.Error("Target value must be greater than zero.")
        }
        if (valueType == AlertValueType.PERCENT && targetValue > 500.0) {
            return Resource.Error("Percent alerts cannot exceed 500%.")
        }
        if (valueType == AlertValueType.PERCENT && condition == AlertCondition.BELOW && targetValue > 100.0) {
            return Resource.Error("Decrease percent alerts cannot exceed 100%.")
        }
        if (valueType == AlertValueType.PERCENT && baselineValue <= 0.0) {
            return Resource.Error("Current ${type.label.lowercase()} is unavailable for percent alerts.")
        }

        return repository.createAlert(
            coinId = coinId,
            symbol = symbol.uppercase(),
            name = name,
            imageUrl = imageUrl,
            type = type,
            valueType = valueType,
            frequency = frequency,
            condition = condition,
            targetValue = targetValue,
            baselineValue = baselineValue,
            note = note,
        )
    }
}

class DeleteAlertUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(id: String): Resource<Unit> = repository.deleteAlert(id)
}

class ToggleAlertUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(alert: PriceAlert): Resource<Unit> =
        repository.setActive(alert.id, alert.status != AlertStatus.ACTIVE)
}

class MarkAlertTriggeredUseCase @Inject constructor(
    private val repository: AlertRepository,
) {
    suspend operator fun invoke(alert: PriceAlert, currentValue: Double): Resource<Unit> =
        repository.markTriggered(alert, currentValue)
}
