package com.cryptolens.app.features.auth.domain.repository

import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.core.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Resource<AuthUser>
    suspend fun register(email: String, password: String, displayName: String): Resource<AuthUser>
    suspend fun logout(): Resource<Unit>
    suspend fun resetPassword(email: String): Resource<Unit>
    fun observeAuthState(): Flow<AuthUser?>
}
