package com.cryptolens.app.features.auth.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String = "",
    val avatarUrl: String = "",
    val createdAt: String = "",
)

data class LoginCredentials(
    val email: String,
    val password: String,
)

data class RegisterCredentials(
    val email: String,
    val password: String,
    val displayName: String,
)
