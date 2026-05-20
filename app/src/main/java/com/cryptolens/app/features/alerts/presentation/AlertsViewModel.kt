package com.cryptolens.app.features.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.data.worker.AlertScheduler
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.usecase.CreateAlertUseCase
import com.cryptolens.app.features.alerts.domain.usecase.DeleteAlertUseCase
import com.cryptolens.app.features.alerts.domain.usecase.ObserveAlertsUseCase
import com.cryptolens.app.features.alerts.domain.usecase.ToggleAlertUseCase
import com.cryptolens.app.features.market.domain.model.Coin
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
    val marketCoins: List<Coin> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateSheet: Boolean = false,
    val coinId: String = "",
    val coinSymbol: String = "",
    val coinName: String = "",
    val coinImageUrl: String = "",
    val coinCurrentPrice: Double = 0.0,
    val coinCurrentVolume: Double = 0.0,
    val coinCurrentMarketCap: Double = 0.0,
    val selectedAlertType: AlertType = AlertType.PRICE_LIMIT,
    val selectedValueType: AlertValueType = AlertValueType.NUMBER,
    val selectedFrequency: AlertFrequency = AlertFrequency.ONE_TIME,
    val selectedCondition: AlertCondition = AlertCondition.ABOVE,
    val targetValueInput: String = "",
    val noteInput: String = "",
    val targetValueError: String? = null,
    val isSaving: Boolean = false,
)

sealed class AlertsEvent {
    data class ShowCreateSheet(val type: AlertType = AlertType.PRICE_LIMIT) : AlertsEvent()
    data object HideCreateSheet : AlertsEvent()
    data class PreFillCoin(
        val coinId: String,
        val symbol: String,
        val name: String,
        val imageUrl: String,
        val currentPrice: Double,
        val currentVolume: Double = 0.0,
        val currentMarketCap: Double = 0.0,
        val alertType: AlertType = AlertType.PRICE_LIMIT,
    ) : AlertsEvent()
    data class CoinSelected(val coinId: String) : AlertsEvent()
    data class AlertTypeChanged(val type: AlertType) : AlertsEvent()
    data class ValueTypeChanged(val valueType: AlertValueType) : AlertsEvent()
    data class FrequencyChanged(val frequency: AlertFrequency) : AlertsEvent()
    data class ConditionChanged(val condition: AlertCondition) : AlertsEvent()
    data class TargetValueChanged(val value: String) : AlertsEvent()
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

    private val marketCoins = MutableStateFlow<List<Coin>>(emptyList())

    init {
        observeMarketPrices()
        observeAlertList()
        alertScheduler.schedulePeriodicCheck()
    }

    fun onEvent(event: AlertsEvent) {
        when (event) {
            is AlertsEvent.ShowCreateSheet -> showCreateSheet(event.type)
            AlertsEvent.HideCreateSheet -> resetCreateSheet()
            is AlertsEvent.PreFillCoin -> prefillCoin(event)
            is AlertsEvent.CoinSelected -> selectCoin(event.coinId)
            is AlertsEvent.AlertTypeChanged ->
                _uiState.update {
                    it.copy(
                        selectedAlertType = event.type,
                        targetValueInput = if (it.selectedValueType == AlertValueType.NUMBER) {
                            defaultInputFor(it, event.type)
                        } else {
                            ""
                        },
                        targetValueError = null,
                    )
                }
            is AlertsEvent.ValueTypeChanged ->
                _uiState.update { it.copy(selectedValueType = event.valueType, targetValueInput = "", targetValueError = null) }
            is AlertsEvent.FrequencyChanged ->
                _uiState.update { it.copy(selectedFrequency = event.frequency) }
            is AlertsEvent.ConditionChanged ->
                _uiState.update { it.copy(selectedCondition = event.condition, targetValueError = null) }
            is AlertsEvent.TargetValueChanged ->
                _uiState.update { it.copy(targetValueInput = event.value, targetValueError = null) }
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
                    marketCoins.value = resource.data
                    _uiState.update { state ->
                        val selected = resource.data.firstOrNull { it.id == state.coinId }
                        state.copy(
                            marketCoins = resource.data,
                            coinCurrentPrice = selected?.currentPrice ?: state.coinCurrentPrice,
                            coinCurrentVolume = selected?.volume24h ?: state.coinCurrentVolume,
                            coinCurrentMarketCap = selected?.marketCap ?: state.coinCurrentMarketCap,
                        )
                    }
                }
            }
        }
    }

    private fun observeAlertList() {
        viewModelScope.launch {
            combine(observeAlerts(), marketCoins) { alerts, coins ->
                val coinById = coins.associateBy { it.id }
                alerts.map { alert ->
                    alert.copy(currentPrice = coinById[alert.coinId]?.metricValue(alert.type) ?: alert.currentPrice)
                }
            }.collect { alerts ->
                _uiState.update { it.copy(alerts = alerts, isLoading = false) }
            }
        }
    }

    private fun showCreateSheet(type: AlertType) {
        val state = _uiState.value
        val selectedCoin = state.marketCoins.firstOrNull { it.id == state.coinId }
            ?: state.marketCoins.firstOrNull()
        _uiState.update {
            it.copy(
                showCreateSheet = true,
                selectedAlertType = type,
                coinId = selectedCoin?.id ?: it.coinId,
                coinSymbol = selectedCoin?.symbol ?: it.coinSymbol,
                coinName = selectedCoin?.name ?: it.coinName,
                coinImageUrl = selectedCoin?.imageUrl ?: it.coinImageUrl,
                coinCurrentPrice = selectedCoin?.currentPrice ?: it.coinCurrentPrice,
                coinCurrentVolume = selectedCoin?.volume24h ?: it.coinCurrentVolume,
                coinCurrentMarketCap = selectedCoin?.marketCap ?: it.coinCurrentMarketCap,
                selectedValueType = AlertValueType.NUMBER,
                selectedFrequency = AlertFrequency.ONE_TIME,
                selectedCondition = AlertCondition.ABOVE,
                targetValueInput = defaultInputFor(
                    currentPrice = selectedCoin?.currentPrice ?: it.coinCurrentPrice,
                    currentVolume = selectedCoin?.volume24h ?: it.coinCurrentVolume,
                    currentMarketCap = selectedCoin?.marketCap ?: it.coinCurrentMarketCap,
                    type = type,
                ),
                targetValueError = null,
            )
        }
    }

    private fun prefillCoin(event: AlertsEvent.PreFillCoin) {
        _uiState.update {
            it.copy(
                showCreateSheet = true,
                coinId = event.coinId,
                coinSymbol = event.symbol.uppercase(),
                coinName = event.name,
                coinImageUrl = event.imageUrl,
                coinCurrentPrice = event.currentPrice,
                coinCurrentVolume = event.currentVolume,
                coinCurrentMarketCap = event.currentMarketCap,
                selectedAlertType = event.alertType,
                selectedValueType = AlertValueType.NUMBER,
                selectedFrequency = AlertFrequency.ONE_TIME,
                selectedCondition = AlertCondition.ABOVE,
                targetValueInput = defaultInputFor(
                    currentPrice = event.currentPrice,
                    currentVolume = event.currentVolume,
                    currentMarketCap = event.currentMarketCap,
                    type = event.alertType,
                ),
                targetValueError = null,
            )
        }
    }

    private fun selectCoin(coinId: String) {
        val coin = _uiState.value.marketCoins.firstOrNull { it.id == coinId } ?: return
        _uiState.update {
            it.copy(
                coinId = coin.id,
                coinSymbol = coin.symbol,
                coinName = coin.name,
                coinImageUrl = coin.imageUrl,
                coinCurrentPrice = coin.currentPrice,
                coinCurrentVolume = coin.volume24h,
                coinCurrentMarketCap = coin.marketCap,
                targetValueInput = defaultInputFor(
                    currentPrice = coin.currentPrice,
                    currentVolume = coin.volume24h,
                    currentMarketCap = coin.marketCap,
                    type = it.selectedAlertType,
                ),
                targetValueError = null,
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
                type = state.selectedAlertType,
                valueType = state.selectedValueType,
                frequency = state.selectedFrequency,
                condition = state.selectedCondition,
                targetValueText = state.targetValueInput,
                baselineValue = state.currentMetricValue(),
                note = state.noteInput.trim(),
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false, showCreateSheet = false) }
                    resetCreateSheet()
                    _effect.emit(AlertsEffect.ShowMessage("${state.selectedAlertType.label} alert created."))
                    alertScheduler.runImmediateCheck()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, targetValueError = result.message)
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
                coinCurrentVolume = 0.0,
                coinCurrentMarketCap = 0.0,
                selectedAlertType = AlertType.PRICE_LIMIT,
                selectedValueType = AlertValueType.NUMBER,
                selectedFrequency = AlertFrequency.ONE_TIME,
                targetValueInput = "",
                noteInput = "",
                targetValueError = null,
                isSaving = false,
            )
        }
    }

    private fun defaultInputFor(state: AlertsUiState, type: AlertType): String =
        defaultInputFor(
            currentPrice = state.coinCurrentPrice,
            currentVolume = state.coinCurrentVolume,
            currentMarketCap = state.coinCurrentMarketCap,
            type = type,
        )

    private fun defaultInputFor(
        currentPrice: Double,
        currentVolume: Double,
        currentMarketCap: Double,
        type: AlertType,
    ): String {
        val value = when (type) {
            AlertType.PRICE_LIMIT -> currentPrice
            AlertType.VOLUME -> currentVolume
            AlertType.MARKET_CAP -> currentMarketCap
        }
        return if (value > 0.0) value.toBigDecimal().stripTrailingZeros().toPlainString() else ""
    }

    private fun AlertsUiState.currentMetricValue(): Double = when (selectedAlertType) {
        AlertType.PRICE_LIMIT -> coinCurrentPrice
        AlertType.VOLUME -> coinCurrentVolume
        AlertType.MARKET_CAP -> coinCurrentMarketCap
    }

    private fun Coin.metricValue(type: AlertType): Double = when (type) {
        AlertType.PRICE_LIMIT -> currentPrice
        AlertType.VOLUME -> volume24h
        AlertType.MARKET_CAP -> marketCap
    }
}
