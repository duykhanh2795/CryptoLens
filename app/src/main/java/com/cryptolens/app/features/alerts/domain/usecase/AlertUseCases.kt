package com.cryptolens.app.features.alerts.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
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
        condition: AlertCondition,
        targetPriceText: String,
        note: String,
    ): Resource<PriceAlert> {
        if (coinId.isBlank() || symbol.isBlank() || name.isBlank()) {
            return Resource.Error("Open a coin detail screen before creating an alert.")
        }

        val targetPrice = targetPriceText.replace(",", "").trim().toDoubleOrNull()
            ?: return Resource.Error("Enter a valid target price.")

        if (targetPrice <= 0.0) {
            return Resource.Error("Target price must be greater than zero.")
        }

        return repository.createAlert(
            coinId = coinId,
            symbol = symbol.uppercase(),
            name = name,
            imageUrl = imageUrl,
            condition = condition,
            targetPrice = targetPrice,
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
    suspend operator fun invoke(id: String): Resource<Unit> = repository.markTriggered(id)
}
