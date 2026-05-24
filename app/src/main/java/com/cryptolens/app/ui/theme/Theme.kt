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

val BinanceYellow: Color get() = Color(0xFFE8ECEF)
val BinanceYellowDark: Color get() = Color(0xFFB7BDC5)
val BinanceYellowLight: Color get() = Color(0xFFF7F8FA)

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
    background = Color(0xFF050607),
    surface = Color(0xFF111112),
    surfaceVariant = Color(0xFF1B1C1E),
    surfaceElevated = Color(0xFF171719),
    textPrimary = Color(0xFFF4F5F6),
    textSecondary = Color(0xFFA7ABB0),
    textTertiary = Color(0xFF6E737A),
    greenSurface = Color(0xFF13202A),
    redSurface = Color(0xFF28161B),
    divider = Color(0xFF222326),
    border = Color(0xFF303136),
)

private val LightPalette = CryptoPalette(
    background = Color(0xFFF5F5F3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEDEA),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF101113),
    textSecondary = Color(0xFF5D6269),
    textTertiary = Color(0xFF8B8F95),
    greenSurface = Color(0xFFE6F2F7),
    redSurface = Color(0xFFFFECEF),
    divider = Color(0xFFE1E1DE),
    border = Color(0xFFD8D8D4),
)

private var activePalette by mutableStateOf(DarkPalette)

val BackgroundDark: Color get() = activePalette.background
val SurfaceDark: Color get() = activePalette.surface
val SurfaceVariant: Color get() = activePalette.surfaceVariant
val SurfaceElevated: Color get() = activePalette.surfaceElevated

val TextPrimary: Color get() = activePalette.textPrimary
val TextSecondary: Color get() = activePalette.textSecondary
val TextTertiary: Color get() = activePalette.textTertiary

val GreenUp: Color get() = Color(0xFF8EC8F6)
val RedDown: Color get() = Color(0xFFFF7182)
val GreenUpSurface: Color get() = activePalette.greenSurface
val RedDownSurface: Color get() = activePalette.redSurface

val DividerColor: Color get() = activePalette.divider
val BorderColor: Color get() = activePalette.border

private fun darkScheme(palette: CryptoPalette) = darkColorScheme(
    primary = BinanceYellow,
    onPrimary = Color(0xFF050607),
    primaryContainer = BinanceYellowDark,
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
    primary = BinanceYellowDark,
    onPrimary = Color(0xFF050607),
    primaryContainer = BinanceYellowLight,
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
