package com.cryptolens.app.features.exchange.presentation.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.exchange.domain.model.ApiKeyValidation
import com.cryptolens.app.features.exchange.domain.model.ExchangeType
import com.cryptolens.app.features.exchange.domain.usecase.SaveExchangeConnectionUseCase
import com.cryptolens.app.features.exchange.domain.usecase.ValidateApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectExchangeUiState(
    val selectedExchange: ExchangeType  = ExchangeType.BINANCE,
    val apiKey: String                  = "",
    val apiSecret: String               = "",
    val label: String                   = "",
    val isApiKeyVisible: Boolean        = false,
    val isApiSecretVisible: Boolean     = false,
    val isValidating: Boolean           = false,
    val isSaving: Boolean               = false,
    val validation: ApiKeyValidation?   = null,
    val apiKeyError: String?            = null,
    val apiSecretError: String?         = null,
    // Step 1 = select exchange, Step 2 = enter keys, Step 3 = validated/success
    val step: ConnectStep               = ConnectStep.SELECT_EXCHANGE,
)

enum class ConnectStep { SELECT_EXCHANGE, ENTER_KEYS, VALIDATED }

sealed class ConnectExchangeEvent {
    data class ExchangeSelected(val type: ExchangeType)  : ConnectExchangeEvent()
    data class ApiKeyChanged(val value: String)           : ConnectExchangeEvent()
    data class ApiSecretChanged(val value: String)        : ConnectExchangeEvent()
    data class LabelChanged(val value: String)            : ConnectExchangeEvent()
    data object ToggleApiKeyVisibility                    : ConnectExchangeEvent()
    data object ToggleApiSecretVisibility                 : ConnectExchangeEvent()
    data object ValidateAndConnect                        : ConnectExchangeEvent()
    data object BackToEnterKeys                           : ConnectExchangeEvent()
    data object SaveConnection                            : ConnectExchangeEvent()
}

sealed class ConnectExchangeEffect {
    data class ShowError(val message: String)   : ConnectExchangeEffect()
    data class ShowSuccess(val message: String) : ConnectExchangeEffect()
    data object NavigateBack                    : ConnectExchangeEffect()
}

@HiltViewModel
class ConnectExchangeViewModel @Inject constructor(
    private val validateApiKey: ValidateApiKeyUseCase,
    private val saveConnection: SaveExchangeConnectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectExchangeUiState())
    val uiState: StateFlow<ConnectExchangeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ConnectExchangeEffect>()
    val effect: SharedFlow<ConnectExchangeEffect> = _effect.asSharedFlow()

    fun onEvent(event: ConnectExchangeEvent) {
        when (event) {
            is ConnectExchangeEvent.ExchangeSelected ->
                _uiState.update { it.copy(selectedExchange = event.type, step = ConnectStep.ENTER_KEYS) }

            is ConnectExchangeEvent.ApiKeyChanged ->
                _uiState.update { it.copy(apiKey = event.value, apiKeyError = null, validation = null) }

            is ConnectExchangeEvent.ApiSecretChanged ->
                _uiState.update { it.copy(apiSecret = event.value, apiSecretError = null, validation = null) }

            is ConnectExchangeEvent.LabelChanged ->
                _uiState.update { it.copy(label = event.value) }

            is ConnectExchangeEvent.ToggleApiKeyVisibility ->
                _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }

            is ConnectExchangeEvent.ToggleApiSecretVisibility ->
                _uiState.update { it.copy(isApiSecretVisible = !it.isApiSecretVisible) }

            is ConnectExchangeEvent.ValidateAndConnect -> handleValidate()
            is ConnectExchangeEvent.BackToEnterKeys ->
                _uiState.update { it.copy(step = ConnectStep.ENTER_KEYS, validation = null) }
            is ConnectExchangeEvent.SaveConnection -> handleSave()
        }
    }

    private fun handleValidate() {
        val state = _uiState.value
        var hasError = false

        if (state.apiKey.isBlank()) {
            _uiState.update { it.copy(apiKeyError = "API Key is required") }
            hasError = true
        }
        if (state.apiSecret.isBlank()) {
            _uiState.update { it.copy(apiSecretError = "API Secret is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true) }
            val result = validateApiKey(state.selectedExchange, state.apiKey, state.apiSecret)
            _uiState.update { it.copy(isValidating = false) }

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            validation = result.data,
                            step       = ConnectStep.VALIDATED,
                        )
                    }
                }
                is Resource.Error -> {
                    _effect.emit(ConnectExchangeEffect.ShowError(result.message))
                }
                else -> Unit
            }
        }
    }

    private fun handleSave() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveConnection(
                exchangeType = state.selectedExchange,
                label        = state.label,
                apiKey       = state.apiKey,
                apiSecret    = state.apiSecret,
            )
            _uiState.update { it.copy(isSaving = false) }

            when (result) {
                is Resource.Success -> {
                    _effect.emit(ConnectExchangeEffect.ShowSuccess(
                        "${state.selectedExchange.displayName} connected successfully!"
                    ))
                    _effect.emit(ConnectExchangeEffect.NavigateBack)
                }
                is Resource.Error -> {
                    _effect.emit(ConnectExchangeEffect.ShowError(result.message))
                }
                else -> Unit
            }
        }
    }
}
