package com.cryptolens.app.features.auth.domain.repository

import com.cryptolens.app.features.auth.domain.model.LoginPreferences
import kotlinx.coroutines.flow.Flow

interface LoginPreferencesRepository {
    fun observePreferences(): Flow<LoginPreferences>

    suspend fun savePreferences(rememberLogin: Boolean, email: String)
}
