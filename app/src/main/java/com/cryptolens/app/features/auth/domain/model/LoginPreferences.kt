package com.cryptolens.app.features.auth.domain.model

data class LoginPreferences(
    val rememberLogin: Boolean = false,
    val rememberedEmail: String = "",
)
