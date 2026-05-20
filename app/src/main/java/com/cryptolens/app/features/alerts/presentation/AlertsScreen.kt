package com.cryptolens.app.features.alerts.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.ui.theme.BackgroundDark
import com.cryptolens.app.ui.theme.BinanceYellow
import com.cryptolens.app.ui.theme.BorderColor
import com.cryptolens.app.ui.theme.DividerColor
import com.cryptolens.app.ui.theme.GreenUp
import com.cryptolens.app.ui.theme.GreenUpSurface
import com.cryptolens.app.ui.theme.RedDown
import com.cryptolens.app.ui.theme.RedDownSurface
import com.cryptolens.app.ui.theme.SurfaceDark
import com.cryptolens.app.ui.theme.SurfaceElevated
import com.cryptolens.app.ui.theme.SurfaceVariant
import com.cryptolens.app.ui.theme.TextPrimary
import com.cryptolens.app.ui.theme.TextSecondary
import com.cryptolens.app.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit,
    prefill: AlertCoinPrefill? = null,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(prefill) {
        prefill?.let {
            viewModel.onEvent(
                AlertsEvent.PreFillCoin(
                    coinId = it.coinId,
                    symbol = it.symbol,
                    name = it.name,
                    imageUrl = it.imageUrl,
                    currentPrice = it.currentPrice,
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AlertsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = SurfaceVariant, contentColor = TextPrimary, actionColor = BinanceYellow)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(AlertsEvent.ShowCreateSheet()) },
                containerColor = BinanceYellow,
                contentColor = Color(0xFF1A1400),
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = "Create alert")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(BackgroundDark)) {
            AlertsTopBar(onNavigateBack = onNavigateBack)

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        AlertTypeChooser(
                            onSelect = { viewModel.onEvent(AlertsEvent.ShowCreateSheet(it)) },
                        )
                    }
                    if (uiState.alerts.isEmpty()) {
                        item { EmptyAlerts() }
                    } else {
                        items(uiState.alerts, key = { it.id }) { alert ->
                            AlertCard(
                                alert = alert,
                                onToggle = { viewModel.onEvent(AlertsEvent.ToggleAlert(it)) },
                                onDelete = { viewModel.onEvent(AlertsEvent.DeleteAlert(it)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(AlertsEvent.HideCreateSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
        ) {
            CreateAlertSheet(
                state = uiState,
                onEvent = viewModel::onEvent,
            )
        }
    }
}

data class AlertCoinPrefill(
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
)

@Composable
private fun AlertsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = TextPrimary)
        }
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun EmptyAlerts() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text("No alerts yet", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose Price Limit, Volume, or Market Cap to create your first custom alert.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AlertTypeChooser(
    onSelect: (AlertType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Custom Alerts",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        AlertType.entries.forEach { type ->
            val icon = when (type) {
                AlertType.PRICE_LIMIT -> Icons.Default.AttachMoney
                AlertType.VOLUME -> Icons.Default.BarChart
                AlertType.MARKET_CAP -> Icons.Default.PieChart
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor),
                onClick = { onSelect(type) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = BinanceYellow, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            type.label,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            type.description,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AlertList(
    alerts: List<PriceAlert>,
    onToggle: (PriceAlert) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert, onToggle = onToggle, onDelete = onDelete)
        }
    }
}

@Composable
private fun AlertCard(
    alert: PriceAlert,
    onToggle: (PriceAlert) -> Unit,
    onDelete: (String) -> Unit,
) {
    val conditionColor = if (alert.condition == AlertCondition.ABOVE) GreenUp else RedDown
    val conditionSurface = if (alert.condition == AlertCondition.ABOVE) GreenUpSurface else RedDownSurface
    val conditionText = if (alert.valueType == AlertValueType.PERCENT) {
        "${alert.condition.movementLabel} ${alert.targetPrice.formatPrice()}%"
    } else {
        "${alert.condition.label} ${formatAlertMetric(alert.type, alert.targetPrice)}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = alert.imageUrl,
                    contentDescription = alert.name,
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.symbol, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${alert.name} - ${alert.type.label}", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = alert.status == AlertStatus.ACTIVE,
                    onCheckedChange = { onToggle(alert) },
                    enabled = alert.status != AlertStatus.TRIGGERED,
                )
                IconButton(onClick = { onDelete(alert.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(conditionSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        conditionText,
                        color = conditionColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${alert.frequency.label} - ${alert.status.label()}",
                    color = if (alert.status == AlertStatus.TRIGGERED) BinanceYellow else TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            if (alert.currentPrice > 0.0 || alert.note.isNotBlank()) {
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                if (alert.currentPrice > 0.0) {
                    Text(
                        "Current ${alert.type.label.lowercase()}: ${formatAlertMetric(alert.type, alert.currentPrice)}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (alert.valueType == AlertValueType.PERCENT && alert.baselineValue > 0.0) {
                    Text(
                        "Baseline: ${formatAlertMetric(alert.type, alert.baselineValue)}",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (alert.note.isNotBlank()) {
                    Text(alert.note, color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun AlertStatus.label(): String = when (this) {
    AlertStatus.ACTIVE -> "Active"
    AlertStatus.PAUSED -> "Paused"
    AlertStatus.TRIGGERED -> "Triggered"
}

private fun formatAlertMetric(type: AlertType, value: Double): String = when (type) {
    AlertType.PRICE_LIMIT -> "$${value.formatPrice()}"
    AlertType.VOLUME -> "$${value.formatCompactUsd()}"
    AlertType.MARKET_CAP -> "$${value.formatCompactUsd()}"
}

private fun Double.formatCompactUsd(): String = when {
    this >= 1_000_000_000 -> "%.2fB".format(this / 1_000_000_000)
    this >= 1_000_000 -> "%.2fM".format(this / 1_000_000)
    this >= 1_000 -> "%.2fK".format(this / 1_000)
    else -> "%.2f".format(this)
}

@Composable
private fun CreateAlertSheet(
    state: AlertsUiState,
    onEvent: (AlertsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Text("Create ${state.selectedAlertType.label} Alert", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        AlertTypeTabs(
            selected = state.selectedAlertType,
            onSelect = { onEvent(AlertsEvent.AlertTypeChanged(it)) },
        )
        Spacer(Modifier.height(14.dp))

        CoinPicker(
            coins = state.marketCoins,
            selectedCoinId = state.coinId,
            onSelect = { onEvent(AlertsEvent.CoinSelected(it)) },
        )
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = state.coinImageUrl,
                contentDescription = state.coinName,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(state.coinSymbol, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Current ${state.selectedAlertType.label.lowercase()}: ${formatAlertMetric(state.selectedAlertType, state.currentMetricValue())}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlertValueType.entries.forEach { valueType ->
                FilterChip(
                    selected = state.selectedValueType == valueType,
                    onClick = { onEvent(AlertsEvent.ValueTypeChanged(valueType)) },
                    label = { Text("Alert by ${valueType.label}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        selectedLabelColor = BinanceYellow,
                        labelColor = TextSecondary,
                    ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertCondition.entries.forEach { condition ->
                FilterChip(
                    selected = state.selectedCondition == condition,
                    onClick = { onEvent(AlertsEvent.ConditionChanged(condition)) },
                    label = {
                        Text(if (state.selectedValueType == AlertValueType.PERCENT) condition.movementLabel else condition.label)
                    },
                    leadingIcon = {
                        Icon(
                            if (condition == AlertCondition.ABOVE) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        selectedLabelColor = BinanceYellow,
                        labelColor = TextSecondary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val targetValueError = state.targetValueError
        OutlinedTextField(
            value = state.targetValueInput,
            onValueChange = { onEvent(AlertsEvent.TargetValueChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (state.selectedValueType == AlertValueType.PERCENT) {
                        "Percent change"
                    } else {
                        "Target ${state.selectedAlertType.label.lowercase()}"
                    },
                )
            },
            prefix = if (state.selectedValueType == AlertValueType.NUMBER) {
                { Text("$") }
            } else {
                null
            },
            suffix = if (state.selectedValueType == AlertValueType.PERCENT) {
                { Text("%") }
            } else {
                null
            },
            singleLine = true,
            isError = targetValueError != null,
            supportingText = if (targetValueError != null) {
                { Text(targetValueError) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Spacer(Modifier.height(10.dp))
        FrequencySelector(
            selected = state.selectedFrequency,
            onSelect = { onEvent(AlertsEvent.FrequencyChanged(it)) },
        )

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.noteInput,
            onValueChange = { onEvent(AlertsEvent.NoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note") },
            maxLines = 2,
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onEvent(AlertsEvent.HideCreateSheet) },
                modifier = Modifier.weight(1f).height(48.dp),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Cancel", color = TextSecondary)
            }
            Button(
                onClick = { onEvent(AlertsEvent.SaveAlert) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color(0xFF1A1400)),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1A1400))
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onEvent(AlertsEvent.HideCreateSheet) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Close", color = TextTertiary)
        }
    }
}

@Composable
private fun AlertTypeTabs(
    selected: AlertType,
    onSelect: (AlertType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AlertType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                    selectedLabelColor = BinanceYellow,
                    labelColor = TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun CoinPicker(
    coins: List<Coin>,
    selectedCoinId: String,
    onSelect: (String) -> Unit,
) {
    if (coins.isEmpty()) return
    Text("Coin", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(coins.take(30), key = { it.id }) { coin ->
            val selected = selectedCoinId == coin.id
            FilterChip(
                selected = selected,
                onClick = { onSelect(coin.id) },
                label = { Text(coin.symbol, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    AsyncImage(
                        model = coin.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).clip(CircleShape),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                    selectedLabelColor = BinanceYellow,
                    labelColor = TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun FrequencySelector(
    selected: AlertFrequency,
    onSelect: (AlertFrequency) -> Unit,
) {
    Column {
        Text("Frequency", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertFrequency.entries.forEach { frequency ->
                FilterChip(
                    selected = selected == frequency,
                    onClick = { onSelect(frequency) },
                    label = { Text(frequency.label) },
                    leadingIcon = {
                        Icon(
                            if (frequency == AlertFrequency.PERSISTENT) Icons.Default.Repeat else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        selectedLabelColor = BinanceYellow,
                        labelColor = TextSecondary,
                    ),
                )
            }
        }
    }
}

private fun AlertsUiState.currentMetricValue(): Double = when (selectedAlertType) {
    AlertType.PRICE_LIMIT -> coinCurrentPrice
    AlertType.VOLUME -> coinCurrentVolume
    AlertType.MARKET_CAP -> coinCurrentMarketCap
}
