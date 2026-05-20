package com.cryptolens.app.features.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.data.worker.AlertScheduler
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.usecase.CreateAlertUseCase
import com.cryptolens.app.features.alerts.domain.usecase.DeleteAlertUseCase
import com.cryptolens.app.features.alerts.domain.usecase.ObserveAlertsUseCase
import com.cryptolens.app.features.alerts.domain.usecase.ToggleAlertUseCase
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<PriceAlert> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateSheet: Boolean = false,
    val coinId: String = "",
    val coinSymbol: String = "",
    val coinName: String = "",
    val coinImageUrl: String = "",
    val coinCurrentPrice: Double = 0.0,
    val selectedCondition: AlertCondition = AlertCondition.ABOVE,
    val targetPriceInput: String = "",
    val noteInput: String = "",
    val targetPriceError: String? = null,
    val isSaving: Boolean = false,
)

sealed class AlertsEvent {
    data object ShowCreateSheet : AlertsEvent()
    data object HideCreateSheet : AlertsEvent()
    data class PreFillCoin(
        val coinId: String,
        val symbol: String,
        val name: String,
        val imageUrl: String,
        val currentPrice: Double,
    ) : AlertsEvent()
    data class ConditionChanged(val condition: AlertCondition) : AlertsEvent()
    data class TargetPriceChanged(val value: String) : AlertsEvent()
    data class NoteChanged(val value: String) : AlertsEvent()
    data class DeleteAlert(val id: String) : AlertsEvent()
    data class ToggleAlert(val alert: PriceAlert) : AlertsEvent()
    data object SaveAlert : AlertsEvent()
}

sealed class AlertsEffect {
    data class ShowMessage(val message: String) : AlertsEffect()
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val observeAlerts: ObserveAlertsUseCase,
    private val createAlert: CreateAlertUseCase,
    private val deleteAlert: DeleteAlertUseCase,
    private val toggleAlert: ToggleAlertUseCase,
    private val getTopCoins: GetTopCoinsUseCase,
    private val alertScheduler: AlertScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AlertsEffect>()
    val effect: SharedFlow<AlertsEffect> = _effect.asSharedFlow()

    private val currentPrices = MutableStateFlow<Map<String, Double>>(emptyMap())

    init {
        observeMarketPrices()
        observeAlertList()
        alertScheduler.schedulePeriodicCheck()
    }

    fun onEvent(event: AlertsEvent) {
        when (event) {
            AlertsEvent.ShowCreateSheet -> {
                if (_uiState.value.coinId.isBlank()) {
                    viewModelScope.launch {
                        _effect.emit(AlertsEffect.ShowMessage("Open a coin detail page to create a price alert."))
                    }
                } else {
                    _uiState.update { it.copy(showCreateSheet = true) }
                }
            }
            AlertsEvent.HideCreateSheet -> resetCreateSheet()
            is AlertsEvent.PreFillCoin -> prefillCoin(event)
            is AlertsEvent.ConditionChanged ->
                _uiState.update { it.copy(selectedCondition = event.condition, targetPriceError = null) }
            is AlertsEvent.TargetPriceChanged ->
                _uiState.update { it.copy(targetPriceInput = event.value, targetPriceError = null) }
            is AlertsEvent.NoteChanged ->
                _uiState.update { it.copy(noteInput = event.value) }
            AlertsEvent.SaveAlert -> saveAlert()
            is AlertsEvent.DeleteAlert -> delete(event.id)
            is AlertsEvent.ToggleAlert -> toggle(event.alert)
        }
    }

    private fun observeMarketPrices() {
        viewModelScope.launch {
            getTopCoins(perPage = 250).collect { resource ->
                if (resource is Resource.Success) {
                    currentPrices.value = resource.data.associate { it.id to it.currentPrice }
                }
            }
        }
    }

    private fun observeAlertList() {
        viewModelScope.launch {
            combine(observeAlerts(), currentPrices) { alerts, prices ->
                alerts.map { alert ->
                    alert.copy(currentPrice = prices[alert.coinId] ?: alert.currentPrice)
                }
            }.collect { alerts ->
                _uiState.update { it.copy(alerts = alerts, isLoading = false) }
            }
        }
    }

    private fun prefillCoin(event: AlertsEvent.PreFillCoin) {
        val priceText = if (event.currentPrice > 0.0) {
            event.currentPrice.toBigDecimal().stripTrailingZeros().toPlainString()
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                showCreateSheet = true,
                coinId = event.coinId,
                coinSymbol = event.symbol.uppercase(),
                coinName = event.name,
                coinImageUrl = event.imageUrl,
                coinCurrentPrice = event.currentPrice,
                targetPriceInput = priceText,
                targetPriceError = null,
            )
        }
    }

    private fun saveAlert() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = createAlert(
                coinId = state.coinId,
                symbol = state.coinSymbol,
                name = state.coinName,
                imageUrl = state.coinImageUrl,
                condition = state.selectedCondition,
                targetPriceText = state.targetPriceInput,
                note = state.noteInput.trim(),
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false, showCreateSheet = false) }
                    resetCreateSheet()
                    _effect.emit(AlertsEffect.ShowMessage("Price alert created."))
                    alertScheduler.runImmediateCheck()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, targetPriceError = result.message)
                    }
                }
                Resource.Loading -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            when (val result = deleteAlert(id)) {
                is Resource.Success -> _effect.emit(AlertsEffect.ShowMessage("Price alert deleted."))
                is Resource.Error -> _effect.emit(AlertsEffect.ShowMessage(result.message))
                Resource.Loading -> Unit
            }
        }
    }

    private fun toggle(alert: PriceAlert) {
        viewModelScope.launch {
            when (val result = toggleAlert(alert)) {
                is Resource.Success -> Unit
                is Resource.Error -> _effect.emit(AlertsEffect.ShowMessage(result.message))
                Resource.Loading -> Unit
            }
        }
    }

    private fun resetCreateSheet() {
        _uiState.update {
            it.copy(
                showCreateSheet = false,
                coinId = "",
                coinSymbol = "",
                coinName = "",
                coinImageUrl = "",
                coinCurrentPrice = 0.0,
                selectedCondition = AlertCondition.ABOVE,
                targetPriceInput = "",
                noteInput = "",
                targetPriceError = null,
                isSaving = false,
            )
        }
    }
}
