package com.cryptolens.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cryptolens.app.features.settings.domain.model.AppTheme

val BinanceYellow: Color get() = activePalette.accent
val BinanceYellowDark: Color get() = activePalette.accent
val BinanceYellowLight: Color get() = activePalette.accentContainer

private data class CryptoPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val greenSurface: Color,
    val redSurface: Color,
    val divider: Color,
    val border: Color,
    val accent: Color,
    val accentContainer: Color,
)

private val DarkPalette = CryptoPalette(
    background = Color(0xFF050607),
    surface = Color(0xFF111112),
    surfaceVariant = Color(0xFF1B1C1E),
    surfaceElevated = Color(0xFF171719),
    textPrimary = Color(0xFFF4F5F6),
    textSecondary = Color(0xFFA7ABB0),
    textTertiary = Color(0xFF6E737A),
    greenSurface = Color(0xFF0F2A22),
    redSurface = Color(0xFF28161B),
    divider = Color(0xFF222326),
    border = Color(0xFF303136),
    accent = Color(0xFFE8ECEF),
    accentContainer = Color(0xFF25272B),
)

private val LightPalette = CryptoPalette(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F6),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textTertiary = Color(0xFF8A94A3),
    greenSurface = Color(0xFFE8FAF3),
    redSurface = Color(0xFFFFEEF2),
    divider = Color(0xFFE9EEF3),
    border = Color.Transparent,
    accent = Color(0xFF111827),
    accentContainer = Color(0xFFE8EDF3),
)

private var activePalette by mutableStateOf(DarkPalette)

val BackgroundDark: Color get() = activePalette.background
val SurfaceDark: Color get() = activePalette.surface
val SurfaceVariant: Color get() = activePalette.surfaceVariant
val SurfaceElevated: Color get() = activePalette.surfaceElevated

val TextPrimary: Color get() = activePalette.textPrimary
val TextSecondary: Color get() = activePalette.textSecondary
val TextTertiary: Color get() = activePalette.textTertiary

val GreenUp: Color get() = Color(0xFF00C087)
val RedDown: Color get() = Color(0xFFFF7182)
val GreenUpSurface: Color get() = activePalette.greenSurface
val RedDownSurface: Color get() = activePalette.redSurface

val DividerColor: Color get() = activePalette.divider
val BorderColor: Color get() = activePalette.border

val AppAccent: Color get() = activePalette.accent
val AppAccentContainer: Color get() = activePalette.accentContainer

private fun darkScheme(palette: CryptoPalette) = darkColorScheme(
    primary = palette.accent,
    onPrimary = Color(0xFF050607),
    primaryContainer = palette.accentContainer,
    secondary = GreenUp,
    onSecondary = Color(0xFF071016),
    tertiary = RedDown,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.textSecondary,
    outline = Color.Transparent,
    error = RedDown,
)

private fun lightScheme(palette: CryptoPalette) = lightColorScheme(
    primary = palette.accent,
    onPrimary = Color(0xFF050607),
    primaryContainer = palette.accentContainer,
    secondary = GreenUp,
    onSecondary = Color(0xFF071016),
    tertiary = RedDown,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.textSecondary,
    outline = Color.Transparent,
    error = RedDown,
)

@Composable
fun CryptoLensTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (useDarkTheme) DarkPalette else LightPalette

    SideEffect {
        activePalette = palette
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) darkScheme(palette) else lightScheme(palette),
        typography = CryptoTypography,
        content = content,
    )
}
