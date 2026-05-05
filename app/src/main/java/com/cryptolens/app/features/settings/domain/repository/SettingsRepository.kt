package com.cryptolens.app.features.settings.domain.repository

import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppLanguage
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setCurrency(currency: AppCurrency)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setPriceFormat(format: PriceDisplayFormat)
    suspend fun setShowPortfolioValue(show: Boolean)
    suspend fun setEnableNotifications(enable: Boolean)
    suspend fun setBiometricLock(enable: Boolean)
}
