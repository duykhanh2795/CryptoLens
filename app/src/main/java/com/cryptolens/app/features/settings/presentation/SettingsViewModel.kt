package com.cryptolens.app.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetCurrencyUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetNotificationsUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetPriceFormatUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean    = true,
)

sealed class SettingsEvent {
    data class CurrencyChanged(val currency: AppCurrency)        : SettingsEvent()
    data class ThemeChanged(val theme: AppTheme)                 : SettingsEvent()
    data class PriceFormatChanged(val format: PriceDisplayFormat): SettingsEvent()
    data class NotificationsChanged(val enable: Boolean)         : SettingsEvent()
}

sealed class SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase,
    private val setCurrency: SetCurrencyUseCase,
    private val setTheme: SetThemeUseCase,
    private val setPriceFormat: SetPriceFormatUseCase,
    private val setNotifications: SetNotificationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.CurrencyChanged -> {
                    setCurrency(event.currency)
                    _effect.emit(SettingsEffect.ShowMessage("Currency set to ${event.currency.displayName}"))
                }
                is SettingsEvent.ThemeChanged -> {
                    setTheme(event.theme)
                    _effect.emit(SettingsEffect.ShowMessage("Theme set to ${event.theme.displayName}"))
                }
                is SettingsEvent.PriceFormatChanged -> {
                    setPriceFormat(event.format)
                }
                is SettingsEvent.NotificationsChanged -> {
                    setNotifications(event.enable)
                }
            }
        }
    }
}
