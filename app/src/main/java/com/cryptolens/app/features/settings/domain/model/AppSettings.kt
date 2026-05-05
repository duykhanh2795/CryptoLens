package com.cryptolens.app.features.settings.domain.model

enum class AppCurrency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$",  "US Dollar"),
    VND("VND", "₫",  "Vietnamese Dong"),
    EUR("EUR", "€",  "Euro"),
    GBP("GBP", "£",  "British Pound"),
    JPY("JPY", "¥",  "Japanese Yen"),
    KRW("KRW", "₩",  "Korean Won"),
    SGD("SGD", "S$", "Singapore Dollar"),
    BTC("BTC", "₿",  "Bitcoin"),
}

enum class AppTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("Follow System"),
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Tiếng Việt"),
}

enum class PriceDisplayFormat(val displayName: String) {
    FULL("Full precision"),
    COMPACT("Compact (1.2K)"),
}

data class AppSettings(
    val currency: AppCurrency          = AppCurrency.USD,
    val theme: AppTheme                = AppTheme.DARK,
    val language: AppLanguage          = AppLanguage.ENGLISH,
    val priceFormat: PriceDisplayFormat = PriceDisplayFormat.FULL,
    val showPortfolioValue: Boolean    = true,
    val enableNotifications: Boolean   = true,
    val biometricLock: Boolean         = false,
)
