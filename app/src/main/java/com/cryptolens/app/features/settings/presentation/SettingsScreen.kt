package com.cryptolens.app.features.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.R
import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppLanguage
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.AppTheme
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import com.cryptolens.app.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceVariant,
                    contentColor   = TextPrimary,
                    actionColor    = BinanceYellow,
                )
            }
        },
        containerColor = BackgroundDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.action_back),
                        tint     = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text       = stringResource(R.string.settings_title),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
            } else {
                SettingsContent(
                    settings = uiState.settings,
                    onEvent  = viewModel::onEvent,
                )
            }
        }
    }
}

// ── Settings Content ──────────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Display ────────────────────────────────────────────────────────
        SettingsSectionHeader(stringResource(R.string.settings_general))

        CurrencySettingItem(
            current  = settings.currency,
            onSelect = { onEvent(SettingsEvent.CurrencyChanged(it)) },
        )

        ThemeSettingItem(
            current  = settings.theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )

        LanguageSettingItem(
            current  = settings.language,
            onSelect = { onEvent(SettingsEvent.LanguageChanged(it)) },
        )

        PriceFormatSettingItem(
            current  = settings.priceFormat,
            onSelect = { onEvent(SettingsEvent.PriceFormatChanged(it)) },
        )

        // ── Privacy ────────────────────────────────────────────────────────
        SettingsDivider()
        SettingsSectionHeader(stringResource(R.string.settings_privacy))

        ToggleSettingItem(
            icon    = Icons.Default.Visibility,
            title   = stringResource(R.string.settings_show_portfolio_value),
            subtitle = "Hide/show balance on Home screen",
            checked  = settings.showPortfolioValue,
            onCheckedChange = { onEvent(SettingsEvent.ShowPortfolioValueChanged(it)) },
        )

        ToggleSettingItem(
            icon    = Icons.Default.Fingerprint,
            title   = stringResource(R.string.settings_biometric_lock),
            subtitle = "Require fingerprint to open app",
            checked  = settings.biometricLock,
            onCheckedChange = { onEvent(SettingsEvent.BiometricLockChanged(it)) },
        )

        // ── Notifications ──────────────────────────────────────────────────
        SettingsDivider()
        SettingsSectionHeader(stringResource(R.string.settings_notifications))

        ToggleSettingItem(
            icon    = Icons.Default.NotificationsNone,
            title   = stringResource(R.string.settings_push_notifications),
            subtitle = "Price alerts and market updates",
            checked  = settings.enableNotifications,
            onCheckedChange = { onEvent(SettingsEvent.NotificationsChanged(it)) },
        )

        // ── Data ───────────────────────────────────────────────────────────
        SettingsDivider()
        SettingsSectionHeader(stringResource(R.string.settings_data))

        InfoSettingItem(
            icon    = Icons.Default.CloudSync,
            title   = stringResource(R.string.settings_data_source),
            value   = "Binance · CoinGecko",
        )

        InfoSettingItem(
            icon    = Icons.Default.Timer,
            title   = stringResource(R.string.settings_price_refresh),
            value   = stringResource(R.string.settings_realtime_websocket),
        )

        // ── About ──────────────────────────────────────────────────────────
        SettingsDivider()
        SettingsSectionHeader(stringResource(R.string.settings_about))

        InfoSettingItem(
            icon  = Icons.Default.Info,
            title = stringResource(R.string.settings_version),
            value = "1.0.0",
        )

        InfoSettingItem(
            icon  = Icons.Default.Code,
            title = stringResource(R.string.settings_built_with),
            value = "Kotlin · Compose · Supabase",
        )

        Spacer(Modifier.height(80.dp))
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text      = title,
        style     = MaterialTheme.typography.titleSmall,
        color     = TextSecondary,
        fontWeight = FontWeight.Medium,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 14.dp),
        color = DividerColor.copy(alpha = 0.55f),
        thickness = 0.6.dp,
    )
}

// ── Currency Setting ──────────────────────────────────────────────────────────

@Composable
private fun CurrencySettingItem(
    current: AppCurrency,
    onSelect: (AppCurrency) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon     = Icons.Default.AttachMoney,
        title    = stringResource(R.string.settings_currency),
        subtitle = current.displayName,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "${current.symbol} ${current.code}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        },
        onClick  = { showDialog = true },
    )

    if (showDialog) {
        SelectionDialog(
            title     = stringResource(R.string.settings_select_currency),
            items     = AppCurrency.entries.map { it to "${it.symbol} ${it.displayName}" },
            selected  = current,
            onSelect  = { onSelect(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

// ── Theme Setting ─────────────────────────────────────────────────────────────

@Composable
private fun ThemeSettingItem(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    val icon = when (current) {
        AppTheme.DARK   -> Icons.Default.DarkMode
        AppTheme.LIGHT  -> Icons.Default.LightMode
        AppTheme.SYSTEM -> Icons.Default.Brightness4
    }

    SettingsRow(
        icon     = icon,
        title    = stringResource(R.string.settings_theme),
        subtitle = current.localizedDisplayName(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = current.localizedDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        },
        onClick  = { showDialog = true },
    )

    if (showDialog) {
        SelectionDialog(
            title     = stringResource(R.string.settings_select_theme),
            items     = AppTheme.entries.map { it to it.localizedDisplayName() },
            selected  = current,
            onSelect  = { onSelect(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

// ── Language Setting ──────────────────────────────────────────────────────────

@Composable
private fun LanguageSettingItem(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon     = Icons.Default.Language,
        title    = stringResource(R.string.settings_language),
        subtitle = current.localizedDisplayName(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = current.localizedDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        },
        onClick  = { showDialog = true },
    )

    if (showDialog) {
        SelectionDialog(
            title     = stringResource(R.string.settings_select_language),
            items     = AppLanguage.entries.map { it to it.localizedDisplayName() },
            selected  = current,
            onSelect  = { onSelect(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

// ── Price Format Setting ──────────────────────────────────────────────────────

@Composable
private fun PriceFormatSettingItem(
    current: PriceDisplayFormat,
    onSelect: (PriceDisplayFormat) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon     = Icons.Default.Numbers,
        title    = stringResource(R.string.settings_price_format),
        subtitle = current.localizedDisplayName(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = current.localizedDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        },
        onClick  = { showDialog = true },
    )

    if (showDialog) {
        SelectionDialog(
            title     = stringResource(R.string.settings_select_price_format),
            items     = PriceDisplayFormat.entries.map { it to it.localizedDisplayName() },
            selected  = current,
            onSelect  = { onSelect(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

// ── Toggle Setting ────────────────────────────────────────────────────────────

@Composable
private fun ToggleSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = BinanceYellow,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceVariant,
            ),
        )
    }
}

// ── Info Setting ──────────────────────────────────────────────────────────────

@Composable
private fun InfoSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color      = TextPrimary,
            modifier   = Modifier.weight(1f),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

// ── Settings Row (clickable with trailing) ────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal, color = TextPrimary, modifier = Modifier.weight(1f))
        trailing()
    }
}

// ── Generic Selection Dialog ──────────────────────────────────────────────────

@Composable
private fun <T> SelectionDialog(
    title: String,
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            HorizontalDivider(color = DividerColor)

            items.forEach { (item, label) ->
                val isSelected = item == selected
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = if (isSelected) BinanceYellow else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier   = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = BinanceYellow,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (items.last().first != item) {
                    HorizontalDivider(
                        color    = DividerColor,
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(start = 20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick  = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, bottom = 4.dp),
            ) {
                Text(stringResource(R.string.action_cancel), color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AppTheme.localizedDisplayName(): String = when (this) {
    AppTheme.DARK -> stringResource(R.string.theme_dark)
    AppTheme.LIGHT -> stringResource(R.string.theme_light)
    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
}

@Composable
private fun AppLanguage.localizedDisplayName(): String = when (this) {
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.VIETNAMESE -> stringResource(R.string.language_vietnamese)
}

@Composable
private fun PriceDisplayFormat.localizedDisplayName(): String = when (this) {
    PriceDisplayFormat.FULL -> stringResource(R.string.price_format_full)
    PriceDisplayFormat.COMPACT -> stringResource(R.string.price_format_compact)
}
