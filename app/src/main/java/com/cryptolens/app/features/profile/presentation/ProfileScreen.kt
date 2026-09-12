package com.cryptolens.app.features.profile.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.R
import com.cryptolens.app.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToExchanges: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToWalletWatchlist: () -> Unit,
    onNavigateToConverter: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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
        BinanceProfileContent(
            uiState = uiState,
            modifier = Modifier.padding(padding),
            onEditProfile = { viewModel.onEvent(ProfileEvent.ComingSoon("Edit profile")) },
            onVerify = { viewModel.onEvent(ProfileEvent.ComingSoon("Identity verification")) },
            onSecurity = { viewModel.onEvent(ProfileEvent.ShowChangePasswordDialog) },
            onTwitter = { viewModel.onEvent(ProfileEvent.ComingSoon("Twitter link")) },
            onNavigateToExchanges = onNavigateToExchanges,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAlerts = onNavigateToAlerts,
            onNavigateToWalletWatchlist = onNavigateToWalletWatchlist,
            onNavigateToConverter = onNavigateToConverter,
            onExportData = { viewModel.onEvent(ProfileEvent.ComingSoon("Export data")) },
            onClearPortfolio = { viewModel.onEvent(ProfileEvent.ShowClearPortfolioDialog) },
            onLogout = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
            onBack = { backDispatcher?.onBackPressed() },
        )
    }

    // â”€â”€ Logout Confirmation Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest  = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
            containerColor    = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            icon = {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                    tint = RedDown, modifier = Modifier.size(32.dp))
            },
            title = { Text(stringResource(R.string.profile_logout_title), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text  = {
                Text(
                    stringResource(R.string.profile_logout_message),
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
                    Text(stringResource(R.string.action_logout), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
                    shape   = RoundedCornerShape(8.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (uiState.showClearPortfolioDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissClearPortfolioDialog) },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null,
                    tint = RedDown, modifier = Modifier.size(32.dp))
            },
            title = { Text(stringResource(R.string.profile_clear_title), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Text(
                    stringResource(R.string.profile_clear_message),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.ConfirmClearPortfolio) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDown,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_clear), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ProfileEvent.DismissClearPortfolioDialog) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            state = uiState,
            onEvent = viewModel::onEvent,
        )
    }
}

// â”€â”€ Shared Components â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun BinanceProfileContent(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit,
    onVerify: () -> Unit,
    onSecurity: () -> Unit,
    onTwitter: () -> Unit,
    onNavigateToExchanges: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToWalletWatchlist: () -> Unit,
    onNavigateToConverter: () -> Unit,
    onExportData: () -> Unit,
    onClearPortfolio: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        BinanceProfileTopBar(isLoading = uiState.isLoading, onBack = onBack)
        AccountInfoCard(state = uiState, onEditProfile = onEditProfile)

        Spacer(Modifier.height(22.dp))

        BinanceProfileRow(Icons.Outlined.Person, stringResource(R.string.profile_verification), stringResource(R.string.profile_not_verified), onVerify)
        BinanceProfileRow(Icons.Outlined.Security, stringResource(R.string.profile_security), onClick = onSecurity)
        BinanceProfileRow(Icons.Outlined.AccountBalance, stringResource(R.string.profile_connected_exchanges), stringResource(R.string.profile_binance), onNavigateToExchanges)
        BinanceProfileRow(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.profile_wallet_watchlist), stringResource(R.string.profile_on_chain), onNavigateToWalletWatchlist)
        BinanceProfileRow(Icons.Outlined.NotificationsNone, stringResource(R.string.profile_alerts), stringResource(R.string.profile_price_rules), onNavigateToAlerts)
        BinanceProfileRow(Icons.Outlined.SwapHoriz, stringResource(R.string.profile_converter), stringResource(R.string.profile_live_rates), onNavigateToConverter)
        BinanceProfileRow(Icons.Outlined.Settings, stringResource(R.string.profile_settings), stringResource(R.string.profile_theme_currency), onNavigateToSettings)
        BinanceProfileRow(Icons.Default.Close, stringResource(R.string.profile_twitter), stringResource(R.string.profile_unlinked), onTwitter)

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactProfileAction(
                label = stringResource(R.string.profile_export),
                icon = Icons.Outlined.FileDownload,
                onClick = onExportData,
                modifier = Modifier.weight(1f),
            )
            CompactProfileAction(
                label = stringResource(R.string.profile_clear_portfolio),
                icon = Icons.Outlined.DeleteForever,
                contentColor = RedDown,
                onClick = onClearPortfolio,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(220.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceVariant,
                contentColor = TextPrimary,
            ),
            enabled = !uiState.isLoading,
        ) {
            Text(stringResource(R.string.action_logout), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }

        Text(
            text = "CryptoLens v${uiState.appVersion}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )

        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun BinanceProfileTopBar(isLoading: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_account_info),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BinanceYellow,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ManageAccounts,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(27.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountInfoCard(
    state: ProfileUiState,
    onEditProfile: () -> Unit,
) {
    val user = state.user
    val displayName = user?.displayName?.ifBlank { null }
        ?: user?.email?.substringBefore("@")?.ifBlank { null }
        ?: "CryptoLens User"
    val email = user?.email.orEmpty()
    val uid = user?.id.orEmpty().profileUid()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceDark),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(bottomStart = 18.dp))
                .background(SurfaceVariant)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Standard",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(name = displayName, onClick = onEditProfile)
                Spacer(Modifier.width(20.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(26.dp))

            AccountInfoLine(
                label = "CryptoLens UID",
                value = uid,
                trailingIcon = Icons.Outlined.ContentCopy,
            )
            Spacer(Modifier.height(18.dp))
            AccountInfoLine(
                label = "Registration info",
                value = email.ifBlank { "Not available" },
                trailingIcon = Icons.Outlined.Visibility,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                color = DividerColor,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Upgrade to Pro 1",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Sync exchanges and wallet activity to level up",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(SurfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.08f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(BinanceYellow),
                        )
                    }
                }
                TextButton(onClick = onEditProfile, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = "Benefits",
                        color = BinanceYellow,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BinanceYellow,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, onClick: () -> Unit) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "C"

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(BinanceYellow),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color(0xFF111214),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(SurfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit profile",
                tint = TextPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AccountInfoLine(
    label: String,
    value: String,
    trailingIcon: ImageVector,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(142.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun BinanceProfileRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CompactProfileAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentColor: Color = TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun String.profileUid(): String {
    val compact = filter { it.isLetterOrDigit() }
    return if (compact.length >= 9) compact.takeLast(9) else compact.ifBlank { "000000000" }
}

@Composable
private fun ChangePasswordDialog(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
) {
    val canSubmit = state.currentPassword.isNotBlank() &&
        state.newPassword.length >= 6 &&
        state.confirmPassword.isNotBlank() &&
        !state.isChangingPassword

    AlertDialog(
        onDismissRequest = {
            if (!state.isChangingPassword) {
                onEvent(ProfileEvent.DismissChangePasswordDialog)
            }
        },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = BinanceYellow,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                "Change Password",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordTextField(
                    value = state.currentPassword,
                    onValueChange = { onEvent(ProfileEvent.CurrentPasswordChanged(it)) },
                    label = "Current password",
                    enabled = !state.isChangingPassword,
                )
                PasswordTextField(
                    value = state.newPassword,
                    onValueChange = { onEvent(ProfileEvent.NewPasswordChanged(it)) },
                    label = "New password",
                    enabled = !state.isChangingPassword,
                )
                PasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = { onEvent(ProfileEvent.ConfirmPasswordChanged(it)) },
                    label = "Confirm new password",
                    enabled = !state.isChangingPassword,
                )

                Text(
                    text = state.changePasswordError
                        ?: "Use at least 6 characters. Your current password is checked before saving.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.changePasswordError != null) RedDown else TextTertiary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(ProfileEvent.SubmitChangePassword) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor = Color(0xFF1A1400),
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor = TextTertiary,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (state.isChangingPassword) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF1A1400),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onEvent(ProfileEvent.DismissChangePasswordDialog) },
                enabled = !state.isChangingPassword,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = TextSecondary,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = BinanceYellow,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = BinanceYellow,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant,
            disabledContainerColor = SurfaceVariant,
            disabledTextColor = TextTertiary,
        ),
    )
}

