package com.cryptolens.app.features.auth.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.ui.theme.*

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Home -> onNavigateToHome()
            is SplashDestination.Auth -> onNavigateToAuth()
            else -> Unit
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val logoScale by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow,
        ),
        label = "logoScale",
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val textAlpha by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = tween(800, delayMillis = 400),
        label = "textAlpha",
    )

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        // Glow ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(BinanceYellow.copy(alpha = pulseAlpha * 0.15f)),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo circle
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BinanceYellow),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "CL",
                    color      = Color(0xFF1A1400),
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text       = "CryptoLens",
                color      = TextPrimary,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.alpha(textAlpha),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = "Smart Crypto Analytics",
                color     = TextSecondary,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(textAlpha),
            )
        }

        // Bottom tagline
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text     = "Powered by Binance • CoinGecko • Gemini AI",
                color    = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.alpha(textAlpha),
            )
        }
    }
}
