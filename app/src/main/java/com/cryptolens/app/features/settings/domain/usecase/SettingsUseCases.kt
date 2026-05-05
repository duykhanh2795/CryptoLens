package com.cryptolens.app.features.settings.domain.usecase

import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppLanguage
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import com.cryptolens.app.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repo.observeSettings()
}

class SetCurrencyUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(currency: AppCurrency) = repo.setCurrency(currency)
}

class SetThemeUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(theme: AppTheme) = repo.setTheme(theme)
}

class SetLanguageUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(language: AppLanguage) = repo.setLanguage(language)
}

class SetPriceFormatUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(format: PriceDisplayFormat) = repo.setPriceFormat(format)
}

class SetShowPortfolioValueUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(show: Boolean) = repo.setShowPortfolioValue(show)
}

class SetNotificationsUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(enable: Boolean) = repo.setEnableNotifications(enable)
}

class SetBiometricLockUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(enable: Boolean) = repo.setBiometricLock(enable)
}
