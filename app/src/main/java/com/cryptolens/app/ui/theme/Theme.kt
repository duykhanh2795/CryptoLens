package com.cryptolens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Binance-inspired Color Palette ──────────────────────────────────────────
val BinanceYellow       = Color(0xFFF0B90B)
val BinanceYellowDark   = Color(0xFFD4A200)
val BinanceYellowLight  = Color(0xFFF5CC3D)

val BackgroundDark      = Color(0xFF0B0E11)   // Binance main bg
val SurfaceDark         = Color(0xFF1E2329)   // Card/surface bg
val SurfaceVariant      = Color(0xFF2B3139)   // Input, secondary surface
val SurfaceElevated     = Color(0xFF151A1F)   // Bottom nav, top bar

val TextPrimary         = Color(0xFFEAECEF)
val TextSecondary       = Color(0xFF848E9C)
val TextTertiary        = Color(0xFF5E6673)

val GreenUp             = Color(0xFF0ECB81)   // Price up
val RedDown             = Color(0xFFF6465D)   // Price down
val GreenUpSurface      = Color(0xFF0D2621)
val RedDownSurface      = Color(0xFF2C1215)

val DividerColor        = Color(0xFF2B3139)
val BorderColor         = Color(0xFF2B3139)

// ── Dark Color Scheme ────────────────────────────────────────────────────────
private val CryptoColorScheme = darkColorScheme(
    primary          = BinanceYellow,
    onPrimary        = Color(0xFF1A1400),
    primaryContainer = BinanceYellowDark,
    secondary        = GreenUp,
    onSecondary      = Color(0xFF001A0F),
    tertiary         = RedDown,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline          = BorderColor,
    error            = RedDown,
)

@Composable
fun CryptoLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CryptoColorScheme,
        typography  = CryptoTypography,
        content     = content
    )
}
