package com.cryptolens.app.features.exchange.presentation.manage

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.features.exchange.domain.model.ExchangeConnection
import com.cryptolens.app.features.exchange.domain.model.SyncResult
import com.cryptolens.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageExchangeScreen(
    onNavigateBack: () -> Unit,
    onConnectNew: () -> Unit,
    viewModel: ManageExchangeViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ManageExchangeEffect.ShowError   -> snackbar.showSnackbar(effect.message)
                is ManageExchangeEffect.ShowSuccess -> snackbar.showSnackbar(effect.message)
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint     = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text       = "Connected Exchanges",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f),
                )
                IconButton(onClick = onConnectNew) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = BinanceYellow)
                }
            }

            if (uiState.connections.isEmpty()) {
                // ── Empty State ────────────────────────────────────────────
                EmptyExchangeState(onConnectNew = onConnectNew)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Info header
                    item {
                        InfoBanner()
                    }

                    items(uiState.connections, key = { it.id }) { connection ->
                        ExchangeConnectionCard(
                            connection = connection,
                            isSyncing  = uiState.syncingId == connection.id,
                            onSync     = { viewModel.onEvent(ManageExchangeEvent.SyncConnection(connection.id)) },
                            onDelete   = { viewModel.onEvent(ManageExchangeEvent.DeleteConnection(connection.id)) },
                            onToggleActive = { isActive ->
                                viewModel.onEvent(ManageExchangeEvent.ToggleActive(connection.id, isActive))
                            },
                        )
                    }

                    item {
                        // Add new button
                        OutlinedButton(
                            onClick  = onConnectNew,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, BinanceYellow),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BinanceYellow),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Connect Another Exchange", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // ── Sync Result Dialog ─────────────────────────────────────────────────
    uiState.lastSyncResult?.let { result ->
        SyncResultDialog(
            result    = result,
            onDismiss = { viewModel.onEvent(ManageExchangeEvent.DismissSyncResult) },
        )
    }
}

// ── Exchange Connection Card ───────────────────────────────────────────────────

@Composable
private fun ExchangeConnectionCard(
    connection: ExchangeConnection,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = if (connection.isActive) BinanceYellow.copy(alpha = 0.3f) else DividerColor,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Exchange avatar
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (connection.isActive) BinanceYellow.copy(alpha = 0.15f)
                        else SurfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = connection.exchangeType.displayName.take(2).uppercase(),
                    color      = if (connection.isActive) BinanceYellow else TextTertiary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text       = connection.label,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
                Text(
                    text  = connection.exchangeType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            // Active toggle
            Switch(
                checked         = connection.isActive,
                onCheckedChange = onToggleActive,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Color(0xFF1A1400),
                    checkedTrackColor   = BinanceYellow,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceVariant,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))

        // API Key masked
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VpnKey, contentDescription = null,
                tint = TextTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text  = connection.apiKey,   // already masked in domain layer
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(6.dp))

        // Last sync time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null,
                tint = TextTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text  = if (connection.lastSyncAt != null)
                    "Last sync: ${fmt.format(Date(connection.lastSyncAt))}"
                else "Never synced",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(14.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Sync button
            Button(
                onClick  = onSync,
                enabled  = !isSyncing && connection.isActive,
                modifier = Modifier.weight(1f).height(40.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = BinanceYellow,
                    contentColor           = Color(0xFF1A1400),
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor   = TextTertiary,
                ),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = Color(0xFF1A1400),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Syncing...", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Trades", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }

            // Delete button
            OutlinedButton(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.height(40.dp),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, RedDown.copy(alpha = 0.5f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedDown),
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete",
                    modifier = Modifier.size(18.dp))
            }
        }
    }

    // ── Delete Confirmation ────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest  = { showDeleteDialog = false },
            containerColor    = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null,
                    tint = RedDown, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Remove ${connection.label}?", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
            },
            text = {
                Text(
                    "This will remove the connection and all imported trades from " +
                    "${connection.exchangeType.displayName}. Your manual transactions won't be affected.",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = RedDown,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
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

// ── Sync Result Dialog ────────────────────────────────────────────────────────

@Composable
private fun SyncResultDialog(result: SyncResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor  = TextSecondary,
        icon = {
            Icon(Icons.Default.CheckCircle, contentDescription = null,
                tint = GreenUp, modifier = Modifier.size(36.dp))
        },
        title = { Text("Sync Complete!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(result.exchangeType.displayName, color = BinanceYellow,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    SyncStatItem(
                        value = "${result.tradesImported}",
                        label = "Imported",
                        color = GreenUp,
                    )
                    SyncStatItem(
                        value = "${result.tradesSkipped}",
                        label = "Skipped",
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = "Check your Portfolio tab to see the updated assets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor   = Color(0xFF1A1400),
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun SyncStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ── Empty & Info States ───────────────────────────────────────────────────────

@Composable
private fun EmptyExchangeState(onConnectNew: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.AccountBalance, contentDescription = null,
            tint = TextTertiary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("No Exchanges Connected", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Connect your exchange to automatically import your trade history and portfolio.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick  = onConnectNew,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = BinanceYellow,
                contentColor   = Color(0xFF1A1400),
            ),
        ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Connect Exchange", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Default.Info, contentDescription = null,
            tint = BinanceYellow, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text  = "Tap Sync to import latest trades. Toggle the switch to pause auto-sync for a connection.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}
