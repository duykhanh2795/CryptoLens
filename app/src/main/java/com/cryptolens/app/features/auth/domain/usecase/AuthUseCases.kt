package com.cryptolens.app.features.auth.domain.usecase

import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.features.auth.domain.repository.AuthRepository
import com.cryptolens.app.core.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Resource<AuthUser> {
        if (email.isBlank()) return Resource.Error("Email cannot be empty")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Resource.Error("Invalid email address")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        return repo.login(email.trim(), password)
    }
}

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
    ): Resource<AuthUser> {
        if (displayName.isBlank()) return Resource.Error("Name cannot be empty")
        if (email.isBlank()) return Resource.Error("Email cannot be empty")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Resource.Error("Invalid email address")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        if (password != confirmPassword) return Resource.Error("Passwords do not match")
        return repo.register(email.trim(), password, displayName.trim())
    }
}

class LogoutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(): Resource<Unit> = repo.logout()
}

class ResetPasswordUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        if (email.isBlank()) return Resource.Error("Email cannot be empty")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Resource.Error("Invalid email address")
        return repo.resetPassword(email.trim())
    }
}

class UpdatePasswordUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): Resource<Unit> {
        if (currentPassword.isBlank()) return Resource.Error("Current password cannot be empty")
        if (newPassword.length < 6) return Resource.Error("New password must be at least 6 characters")
        if (newPassword != confirmPassword) return Resource.Error("Passwords do not match")
        if (currentPassword == newPassword) return Resource.Error("New password must be different")
        return repo.updatePassword(currentPassword, newPassword)
    }
}

class ObserveAuthStateUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Flow<AuthUser?> = repo.observeAuthState()
}

class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): AuthUser? = repo.currentUser
}
