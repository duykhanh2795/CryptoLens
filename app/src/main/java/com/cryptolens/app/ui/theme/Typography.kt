package com.cryptolens.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system default — swap out for a custom font if desired
val CryptoTypography = Typography(
    displayLarge  = TextStyle(color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold,   letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge    = TextStyle(color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall    = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(color = TextPrimary,    fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium    = TextStyle(color = TextSecondary,  fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(color = TextSecondary,  fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(color = TextPrimary,    fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium   = TextStyle(color = TextSecondary,  fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(color = TextTertiary,   fontSize = 11.sp, fontWeight = FontWeight.Normal),
)
