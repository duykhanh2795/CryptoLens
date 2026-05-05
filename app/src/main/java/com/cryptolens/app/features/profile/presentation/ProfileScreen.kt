package com.cryptolens.app.features.profile.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToExchanges: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateToLogin  -> onNavigateToLogin()
                is ProfileEffect.ShowError        -> snackbar.showSnackbar(effect.message)
                is ProfileEffect.ShowSuccess      -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = SurfaceVariant, contentColor = TextPrimary, actionColor = BinanceYellow)
            }
        },
        containerColor = BackgroundDark,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Profile", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color    = BinanceYellow,
                        strokeWidth = 2.dp,
                    )
                }
            }

            // ── Avatar & Account Info ──────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().background(SurfaceDark)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BinanceYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = uiState.user?.displayName
                        ?.split(" ")
                        ?.take(2)
                        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        ?.joinToString("")
                        ?.ifBlank { uiState.user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
                        ?: "?"
                    Text(
                        text       = initials,
                        color      = Color(0xFF1A1400),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text       = uiState.user?.displayName?.ifBlank { "CryptoLens User" } ?: "CryptoLens User",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = uiState.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))

                // Verified badge
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(99.dp))
                        .background(GreenUpSurface)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null,
                        tint = GreenUp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Verified Account", color = GreenUp,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Account Section ────────────────────────────────────────────
            ProfileSectionHeader("Account")
            ProfileMenuItem(
                icon        = Icons.Outlined.Person,
                label       = "Edit Profile",
                subtitle    = "Name, avatar",
                onClick     = { /* TODO */ },
            )
            ProfileMenuItem(
                icon        = Icons.Outlined.Lock,
                label       = "Change Password",
                subtitle    = "Update your password",
                onClick     = { /* TODO */ },
            )
            ProfileMenuItem(
                icon        = Icons.Outlined.NotificationsNone,
                label       = "Notifications",
                subtitle    = "Price alerts, news",
                onClick     = { /* TODO */ },
            )

            Spacer(Modifier.height(8.dp))

            // ── Portfolio Section ──────────────────────────────────────────
            ProfileSectionHeader("Portfolio")
            ProfileMenuItem(
                icon     = Icons.Outlined.AccountBalance,
                label    = "Connected Exchanges",
                subtitle = "Import trades from Binance & more",
                onClick  = onNavigateToExchanges,
                trailing = {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .background(BinanceYellow.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("NEW", color = BinanceYellow,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                },
            )
            ProfileMenuItem(
                icon     = Icons.Outlined.FileDownload,
                label    = "Export Data",
                subtitle = "Download your transaction history",
                onClick  = { /* TODO */ },
            )
            ProfileMenuItem(
                icon     = Icons.Outlined.DeleteForever,
                label    = "Clear Portfolio",
                subtitle = "Remove all assets and transactions",
                iconTint = RedDown,
                onClick  = { /* TODO */ },
            )

            Spacer(Modifier.height(8.dp))

            // ── App Section ────────────────────────────────────────────────
            ProfileSectionHeader("App")
            ProfileMenuItem(
                icon     = Icons.Outlined.Settings,
                label    = "Settings",
                subtitle = "Currency, theme, language, notifications",
                onClick  = onNavigateToSettings,
            )

            Spacer(Modifier.height(8.dp))

            // ── About Section ──────────────────────────────────────────────
            ProfileSectionHeader("About")
            ProfileMenuItem(
                icon     = Icons.Outlined.Info,
                label    = "About CryptoLens",
                subtitle = "Version ${uiState.appVersion}",
                onClick  = { /* TODO */ },
            )
            ProfileMenuItem(
                icon     = Icons.Outlined.Shield,
                label    = "Privacy Policy",
                onClick  = { /* TODO */ },
            )
            ProfileMenuItem(
                icon     = Icons.Outlined.Description,
                label    = "Terms of Service",
                onClick  = { /* TODO */ },
            )

            Spacer(Modifier.height(16.dp))

            // ── Logout Button ──────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick  = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(8.dp),
                    enabled  = !uiState.isLoading,
                    border   = BorderStroke(1.dp, RedDown),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDown),
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // App info footer
            Text(
                text      = "CryptoLens v${uiState.appVersion}\nPowered by Binance • CoinGecko • Gemini AI",
                style     = MaterialTheme.typography.labelSmall,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Logout Confirmation Dialog ─────────────────────────────────────────
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest  = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
            containerColor    = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            icon = {
                Icon(Icons.Default.Logout, contentDescription = null,
                    tint = RedDown, modifier = Modifier.size(32.dp))
            },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text  = {
                Text(
                    "Are you sure you want to log out? Your portfolio data will remain saved locally.",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.ConfirmLogout) },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = RedDown,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
                    border  = BorderStroke(1.dp, BorderColor),
                    shape   = RoundedCornerShape(8.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ── Shared Components ──────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = TextTertiary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        letterSpacing = 1.sp,
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    iconTint: Color = TextSecondary,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp,
        modifier = Modifier.padding(start = 66.dp))
}
