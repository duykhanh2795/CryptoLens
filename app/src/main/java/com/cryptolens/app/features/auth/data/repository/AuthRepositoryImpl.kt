package com.cryptolens.app.features.auth.data.repository

import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.features.auth.domain.repository.AuthRepository
import com.cryptolens.app.core.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = supabase.auth.currentUserOrNull()?.let { user ->
            AuthUser(
                id          = user.id,
                email       = user.email ?: "",
                displayName = user.userMetadata
                    ?.get("display_name")?.toString()?.trim('"') ?: "",
                avatarUrl   = user.userMetadata
                    ?.get("avatar_url")?.toString()?.trim('"') ?: "",
                createdAt   = user.createdAt.toString(),
            )
        }

    override val isLoggedIn: Boolean
        get() = supabase.auth.currentUserOrNull() != null

    override suspend fun login(email: String, password: String): Resource<AuthUser> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: return Resource.Error("Login failed. Please try again.")
            Resource.Success(
                AuthUser(
                    id          = user.id,
                    email       = user.email ?: "",
                    displayName = user.userMetadata
                        ?.get("display_name")?.toString()?.trim('"') ?: "",
                )
            )
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("Invalid login credentials") == true ->
                    "Incorrect email or password"
                e.message?.contains("Email not confirmed") == true ->
                    "Please verify your email first"
                e.message?.contains("network") == true ->
                    "Network error. Check your connection."
                else -> e.message ?: "Login failed"
            }
            Resource.Error(msg, e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): Resource<AuthUser> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
                this.data     = buildJsonObject { put("display_name", displayName) }
            }
            val user = supabase.auth.currentUserOrNull()
            Resource.Success(
                AuthUser(
                    id          = user?.id ?: "",
                    email       = email,
                    displayName = displayName,
                )
            )
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("already registered") == true ->
                    "Email is already registered"
                e.message?.contains("network") == true ->
                    "Network error. Check your connection."
                else -> e.message ?: "Registration failed"
            }
            Resource.Error(msg, e)
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            supabase.auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed", e)
        }
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send reset email", e)
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Resource<Unit> {
        return try {
            val email = currentUser?.email?.takeIf { it.isNotBlank() }
                ?: return Resource.Error("Please log in again before changing password")

            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
            supabase.auth.updateUser {
                password = newPassword
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("Invalid login credentials") == true ->
                    "Current password is incorrect"
                e.message?.contains("weak", ignoreCase = true) == true ->
                    "New password is too weak"
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Check your connection."
                else -> e.message ?: "Failed to update password"
            }
            Resource.Error(msg, e)
        }
    }

    override fun observeAuthState(): Flow<AuthUser?> =
        supabase.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> currentUser
                else -> null
            }
        }
}
