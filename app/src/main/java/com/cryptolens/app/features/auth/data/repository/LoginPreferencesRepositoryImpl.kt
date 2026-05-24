package com.cryptolens.app.features.auth.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cryptolens.app.features.auth.domain.model.LoginPreferences
import com.cryptolens.app.features.auth.domain.repository.LoginPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.loginPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "login_preferences",
)

@Singleton
class LoginPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LoginPreferencesRepository {

    private object Keys {
        val REMEMBER_LOGIN = booleanPreferencesKey("remember_login")
        val REMEMBERED_EMAIL = stringPreferencesKey("remembered_email")
    }

    override fun observePreferences(): Flow<LoginPreferences> =
        context.loginPreferencesDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs ->
                LoginPreferences(
                    rememberLogin = prefs[Keys.REMEMBER_LOGIN] ?: false,
                    rememberedEmail = prefs[Keys.REMEMBERED_EMAIL].orEmpty(),
                )
            }

    override suspend fun savePreferences(rememberLogin: Boolean, email: String) {
        context.loginPreferencesDataStore.edit { prefs ->
            val normalizedEmail = email.trim()
            if (rememberLogin && normalizedEmail.isNotBlank()) {
                prefs[Keys.REMEMBER_LOGIN] = true
                prefs[Keys.REMEMBERED_EMAIL] = normalizedEmail
            } else {
                prefs[Keys.REMEMBER_LOGIN] = false
                prefs.remove(Keys.REMEMBERED_EMAIL)
            }
        }
    }
}
