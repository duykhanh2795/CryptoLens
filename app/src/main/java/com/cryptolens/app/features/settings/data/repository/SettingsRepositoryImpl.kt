package com.cryptolens.app.features.settings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppLanguage
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import com.cryptolens.app.features.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val CURRENCY             = stringPreferencesKey("currency")
        val THEME                = stringPreferencesKey("theme")
        val LANGUAGE             = stringPreferencesKey("language")
        val PRICE_FORMAT         = stringPreferencesKey("price_format")
        val SHOW_PORTFOLIO_VALUE = booleanPreferencesKey("show_portfolio_value")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
        val BIOMETRIC_LOCK       = booleanPreferencesKey("biometric_lock")
    }

    override fun observeSettings(): Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                currency = prefs[Keys.CURRENCY]?.let { name ->
                    AppCurrency.entries.firstOrNull { it.name == name }
                } ?: AppCurrency.USD,
                theme = prefs[Keys.THEME]?.let { name ->
                    AppTheme.entries.firstOrNull { it.name == name }
                } ?: AppTheme.DARK,
                language = prefs[Keys.LANGUAGE]?.let { name ->
                    AppLanguage.entries.firstOrNull { it.name == name }
                } ?: AppLanguage.ENGLISH,
                priceFormat = prefs[Keys.PRICE_FORMAT]?.let { name ->
                    PriceDisplayFormat.entries.firstOrNull { it.name == name }
                } ?: PriceDisplayFormat.FULL,
                showPortfolioValue  = prefs[Keys.SHOW_PORTFOLIO_VALUE] ?: true,
                enableNotifications = prefs[Keys.ENABLE_NOTIFICATIONS] ?: true,
                biometricLock       = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            )
        }

    override suspend fun setCurrency(currency: AppCurrency) {
        context.dataStore.edit { it[Keys.CURRENCY] = currency.name }
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setPriceFormat(format: PriceDisplayFormat) {
        context.dataStore.edit { it[Keys.PRICE_FORMAT] = format.name }
    }

    override suspend fun setShowPortfolioValue(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PORTFOLIO_VALUE] = show }
    }

    override suspend fun setEnableNotifications(enable: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_NOTIFICATIONS] = enable }
    }

    override suspend fun setBiometricLock(enable: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enable }
    }
}
