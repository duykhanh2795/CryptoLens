package com.cryptolens.app.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppLanguage
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetBiometricLockUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetCurrencyUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetLanguageUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetNotificationsUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetPriceFormatUseCase
import com.cryptolens.app.features.settings.domain.usecase.SetShowPortfolioValueUseCase
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
    data class LanguageChanged(val language: AppLanguage)        : SettingsEvent()
    data class PriceFormatChanged(val format: PriceDisplayFormat): SettingsEvent()
    data class ShowPortfolioValueChanged(val show: Boolean)      : SettingsEvent()
    data class NotificationsChanged(val enable: Boolean)         : SettingsEvent()
    data class BiometricLockChanged(val enable: Boolean)         : SettingsEvent()
}

sealed class SettingsEffect {
    data class ShowMessage(val message: String) : SettingsEffect()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettings: ObserveSettingsUseCase,
    private val setCurrency: SetCurrencyUseCase,
    private val setTheme: SetThemeUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val setPriceFormat: SetPriceFormatUseCase,
    private val setShowPortfolioValue: SetShowPortfolioValueUseCase,
    private val setNotifications: SetNotificationsUseCase,
    private val setBiometricLock: SetBiometricLockUseCase,
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
                is SettingsEvent.LanguageChanged -> {
                    setLanguage(event.language)
                    _effect.emit(SettingsEffect.ShowMessage("Language set to ${event.language.displayName}"))
                }
                is SettingsEvent.PriceFormatChanged -> {
                    setPriceFormat(event.format)
                }
                is SettingsEvent.ShowPortfolioValueChanged -> {
                    setShowPortfolioValue(event.show)
                }
                is SettingsEvent.NotificationsChanged -> {
                    setNotifications(event.enable)
                }
                is SettingsEvent.BiometricLockChanged -> {
                    setBiometricLock(event.enable)
                    val msg = if (event.enable) "Biometric lock enabled" else "Biometric lock disabled"
                    _effect.emit(SettingsEffect.ShowMessage(msg))
                }
            }
        }
    }
}
