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

val BinanceYellow: Color get() = Color(0xFFF6C84C)
val BinanceYellowDark: Color get() = Color(0xFFD8A90B)
val BinanceYellowLight: Color get() = Color(0xFFFFDF7A)

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
)

private val DarkPalette = CryptoPalette(
    background = Color(0xFF07090D),
    surface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFF151B23),
    surfaceElevated = Color(0xFF0A0F14),
    textPrimary = Color(0xFFF7F9FC),
    textSecondary = Color(0xFF9AA6B5),
    textTertiary = Color(0xFF667386),
    greenSurface = Color(0xFF062117),
    redSurface = Color(0xFF271014),
    divider = Color(0xFF1A2230),
    border = Color(0xFF202A38),
)

private val LightPalette = CryptoPalette(
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F4F8),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0B1220),
    textSecondary = Color(0xFF526071),
    textTertiary = Color(0xFF8793A4),
    greenSurface = Color(0xFFE7F8EF),
    redSurface = Color(0xFFFDECEF),
    divider = Color(0xFFE7ECF3),
    border = Color(0xFFDDE5EE),
)

private var activePalette by mutableStateOf(DarkPalette)

val BackgroundDark: Color get() = activePalette.background
val SurfaceDark: Color get() = activePalette.surface
val SurfaceVariant: Color get() = activePalette.surfaceVariant
val SurfaceElevated: Color get() = activePalette.surfaceElevated

val TextPrimary: Color get() = activePalette.textPrimary
val TextSecondary: Color get() = activePalette.textSecondary
val TextTertiary: Color get() = activePalette.textTertiary

val GreenUp: Color get() = Color(0xFF0ECB81)
val RedDown: Color get() = Color(0xFFF6465D)
val GreenUpSurface: Color get() = activePalette.greenSurface
val RedDownSurface: Color get() = activePalette.redSurface

val DividerColor: Color get() = activePalette.divider
val BorderColor: Color get() = activePalette.border

private fun darkScheme(palette: CryptoPalette) = darkColorScheme(
    primary = BinanceYellow,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = BinanceYellowDark,
    secondary = GreenUp,
    onSecondary = Color(0xFF001A0F),
    tertiary = RedDown,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
    error = RedDown,
)

private fun lightScheme(palette: CryptoPalette) = lightColorScheme(
    primary = BinanceYellowDark,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = BinanceYellowLight,
    secondary = GreenUp,
    onSecondary = Color(0xFF001A0F),
    tertiary = RedDown,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
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
