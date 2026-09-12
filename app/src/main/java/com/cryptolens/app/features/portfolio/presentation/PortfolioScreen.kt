package com.cryptolens.app.features.portfolio.presentation

import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatMoney
import com.cryptolens.app.core.domain.util.formatSignedMoney
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onCoinClick: (String) -> Unit,
    onConnectExchange: () -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingExportContent
        pendingExportContent = null
        val success = uri != null && content != null && runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
            } ?: error("Could not open output file")
        }.isSuccess
        viewModel.onEvent(PortfolioEvent.ExportCsvCompleted(success))
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Could not open CSV file")
        }.onSuccess { content ->
            viewModel.onEvent(PortfolioEvent.ImportCsvLoaded(content))
        }.onFailure {
            scope.launch { snackbar.showSnackbar("Failed to read CSV file") }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PortfolioEffect.ShowError   -> snackbar.showSnackbar(effect.message)
                is PortfolioEffect.ShowSuccess -> snackbar.showSnackbar(effect.message)
                is PortfolioEffect.LaunchCsvExport -> {
                    pendingExportContent = effect.content
                    exportLauncher.launch(effect.fileName)
                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundDark)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Portfolio", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                if (uiState.isImportExportBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BinanceYellow,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) },
                    enabled = !uiState.isImportExportBusy,
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = "Import CSV",
                        tint = TextSecondary)
                }
                IconButton(
                    onClick = { viewModel.onEvent(PortfolioEvent.ExportCsv) },
                    enabled = !uiState.isImportExportBusy,
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = "Export CSV",
                        tint = TextSecondary)
                }
                IconButton(onClick = onConnectExchange) {
                    Icon(Icons.Outlined.AccountBalance, contentDescription = "Connect Exchange",
                        tint = TextSecondary)
                }
                IconButton(onClick = { viewModel.onEvent(PortfolioEvent.ShowAddSheet) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = BinanceYellow)
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
                else -> {
                    PortfolioHeroSection(
                        summary = uiState.summary,
                        snapshots = uiState.snapshots,
                        settings = uiState.settings,
                    )

                    TabRow(
                        selectedTabIndex = PortfolioTab.entries.indexOf(uiState.selectedTab),
                        containerColor   = SurfaceDark,
                        contentColor     = BinanceYellow,
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            ,
                        indicator = { tabs ->
                            val idx = PortfolioTab.entries.indexOf(uiState.selectedTab)
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabs[idx]),
                                height = 2.dp,
                                color = BinanceYellow,
                            )
                        },
                        divider = {},
                    ) {
                        PortfolioTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            Tab(
                                selected = selected,
                                onClick  = { viewModel.onEvent(PortfolioEvent.TabSelected(tab)) },
                                text = {
                                    Text(
                                        tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (selected) TextPrimary else TextSecondary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                }
                            )
                        }
                    }

                    when (uiState.selectedTab) {
                        PortfolioTab.ASSETS -> AssetsTab(
                            assets = uiState.summary?.assets ?: emptyList(),
                            settings = uiState.settings,
                            onCoinClick = onCoinClick,
                            onDelete = { viewModel.onEvent(PortfolioEvent.DeleteAsset(it)) },
                        )
                        PortfolioTab.TRANSACTIONS -> TransactionsTab(
                            transactions = uiState.transactions,
                            settings = uiState.settings,
                            onDelete = { viewModel.onEvent(PortfolioEvent.DeleteTransaction(it)) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddSheet) {
        AddTransactionSheet(
            settings = uiState.settings,
            onDismiss = { viewModel.onEvent(PortfolioEvent.HideAddSheet) },
            onConfirm = { coinId, sym, nm, img, type, qty, price, fee, note ->
                viewModel.onEvent(PortfolioEvent.AddTransaction(
                    coinId = coinId, symbol = sym, name = nm, imageUrl = img,
                    type = type, quantityStr = qty, priceStr = price,
                    fee = fee.toDoubleOrNull() ?: 0.0, note = note,
                ))
            },
        )
    }

    uiState.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            isBusy = uiState.isImportExportBusy,
            onDismiss = { viewModel.onEvent(PortfolioEvent.DismissImportPreview) },
            onConfirm = { mode -> viewModel.onEvent(PortfolioEvent.ConfirmImportCsv(mode)) },
        )
    }
}
@Composable
private fun ImportPreviewDialog(
    preview: PortfolioImportPreview,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PortfolioImportMode) -> Unit,
) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateRange = remember(preview) {
        val first = preview.firstTimestamp
        val last = preview.lastTimestamp
        if (first != null && last != null) {
            "${fmt.format(Date(first))} - ${fmt.format(Date(last))}"
        } else {
            "Unknown range"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(Icons.Outlined.UploadFile, contentDescription = null,
                tint = BinanceYellow, modifier = Modifier.size(32.dp))
        },
        title = { Text("Import Portfolio CSV", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Review the file before applying it to your portfolio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                ImportPreviewRow("Transactions", "${preview.transactionCount}")
                ImportPreviewRow("Coins", "${preview.coinCount}")
                ImportPreviewRow("Buys / Sells", "${preview.buyCount} / ${preview.sellCount}")
                ImportPreviewRow("Date range", dateRange)
                Text(
                    "Append keeps current transactions and updates matching exported IDs. Replace clears current portfolio transactions and snapshots first.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(PortfolioImportMode.APPEND) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor = Color(0xFF1A1400),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Append", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, enabled = !isBusy) {
                    Text("Cancel", color = TextSecondary)
                }
                OutlinedButton(
                    onClick = { onConfirm(PortfolioImportMode.REPLACE) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDown),
                ) {
                    Text("Replace")
                }
            }
        },
    )
}

@Composable
private fun ImportPreviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium,
            color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PortfolioHeroSection(
    summary: PortfolioSummary?,
    snapshots: List<PortfolioSnapshot>,
    settings: AppSettings,
) {
    var selectedRange by remember { mutableStateOf("24H") }
    var showAllocation by remember { mutableStateOf(false) }
    val totalValue = summary?.totalValue ?: 0.0
    val totalPnl = summary?.totalProfitLoss ?: 0.0
    val totalPnlPercent = summary?.totalProfitLossPercent ?: 0.0
    val dayChange = summary?.totalChange24h ?: 0.0
    val dayChangePercent = summary?.totalChange24hPercent ?: 0.0
    val isProfit = totalPnl >= 0.0
    val isDayUp = dayChange >= 0.0
    val chartColor = if (isDayUp) GreenUp else RedDown
    val chartValues = remember(snapshots, summary) {
        if (snapshots.size >= 2) {
            snapshots.map { it.totalValue }
        } else {
            portfolioFallbackSeries(totalValue, dayChangePercent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "USD",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceVariant.copy(alpha = 0.55f))
                    .clickable { showAllocation = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.PieChart,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Allocation",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            settings.formatMoney(totalValue),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                settings.formatSignedMoney(totalPnl),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isProfit) GreenUp else RedDown,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isProfit) GreenUpSurface else RedDownSurface)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    formatSignedPercent(totalPnlPercent),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isProfit) GreenUp else RedDown,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        PortfolioHeroChart(
            values = chartValues,
            lineColor = chartColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp)
                .padding(top = 16.dp),
        )

        PortfolioRangeSelector(
            selectedRange = selectedRange,
            onRangeSelected = { selectedRange = it },
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PortfolioHeroMetric(
                label = "Invested",
                value = settings.formatMoney(summary?.totalInvested ?: 0.0),
                color = TextPrimary,
            )
            PortfolioHeroMetric(
                label = "Unrealized",
                value = settings.formatSignedMoney(summary?.unrealizedProfitLoss ?: 0.0),
                color = pnlColor(summary?.unrealizedProfitLoss ?: 0.0),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            PortfolioHeroMetric(
                label = "Realized",
                value = settings.formatSignedMoney(summary?.realizedProfitLoss ?: 0.0),
                color = pnlColor(summary?.realizedProfitLoss ?: 0.0),
                horizontalAlignment = Alignment.End,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PortfolioHeroMetric(
                label = "Fees",
                value = settings.formatMoney(summary?.totalFeesPaid ?: 0.0),
                color = TextSecondary,
            )
            PortfolioHeroMetric(
                label = "24H",
                value = "${settings.formatSignedMoney(dayChange)} / ${formatSignedPercent(dayChangePercent)}",
                color = if (isDayUp) GreenUp else RedDown,
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            PortfolioHeroMetric(
                label = "Assets",
                value = "${summary?.assets?.size ?: 0}",
                color = TextPrimary,
                horizontalAlignment = Alignment.End,
            )
        }
    }

    if (showAllocation) {
        AllocationScreenDialog(
            summary = summary,
            settings = settings,
            onDismiss = { showAllocation = false },
        )
    }
}

@Composable
private fun AllocationScreenDialog(
    summary: PortfolioSummary?,
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    var selectedRange by remember { mutableStateOf("24H") }
    val assets = remember(summary) {
        summary?.assets
            ?.filter { it.currentValue > 0.0 || it.quantity > 0.0 }
            ?.sortedByDescending { it.currentValue }
            ?: emptyList()
    }
    val totalValue = assets.sumOf { it.currentValue }
    val displayAssets = assets.take(6)
    val totalPnl = summary?.totalProfitLoss ?: 0.0
    val totalPnlPercent = summary?.totalProfitLossPercent ?: 0.0
    val isProfit = totalPnl >= 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AllocationScreenHeader(onDismiss = onDismiss)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SurfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Portfolio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(326.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PortfolioAllocationDonut(
                        assets = displayAssets,
                        totalValue = totalValue,
                        modifier = Modifier.size(292.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Allocation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            settings.formatMoney(totalValue),
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                settings.formatSignedMoney(totalPnl),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isProfit) GreenUp else RedDown,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                formatSignedPercent(totalPnlPercent),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isProfit) GreenUp else RedDown,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                AllocationRangeSelector(
                    selectedRange = selectedRange,
                    onRangeSelected = { selectedRange = it },
                    modifier = Modifier.padding(horizontal = 46.dp),
                )

                Spacer(Modifier.height(22.dp))

                AllocationAssetsPanel(
                    assets = displayAssets,
                    totalValue = totalValue,
                    settings = settings,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AllocationScreenHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 20.dp),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(25.dp),
            )
        }
        Text(
            "Portfolio",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "P",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GreenUp),
            )
        }
    }
}

@Composable
private fun AllocationRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranges = listOf("24H", "1W", "1M", "3M", "1Y", "ALL")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ranges.forEach { range ->
            val selected = selectedRange == range
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 2.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    range,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) TextPrimary else TextTertiary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .background(if (selected) TextPrimary else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun AllocationAssetsPanel(
    assets: List<PortfolioAsset>,
    totalValue: Double,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val panelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(SurfaceDark)
            .border(0.7.dp, DividerColor.copy(alpha = 0.36f), panelShape)
            .padding(horizontal = 24.dp)
            .padding(top = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TextTertiary.copy(alpha = 0.38f)),
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Assets",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "By value",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(18.dp))

        if (assets.isEmpty()) {
            EmptyAllocationState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
                    AllocationAssetRow(
                        asset = asset,
                        percent = if (totalValue > 0.0) asset.currentValue / totalValue * 100.0 else asset.allocationPercent,
                        color = allocationColor(index),
                        settings = settings,
                    )
                    if (index != assets.lastIndex) {
                        Divider(color = DividerColor.copy(alpha = 0.36f), thickness = 0.6.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAllocationState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.PieChart,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No allocation yet",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Add assets to see portfolio distribution.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PortfolioAllocationDonut(
    assets: List<PortfolioAsset>,
    totalValue: Double,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val chartPadding = strokeWidth / 2f + 3.dp.toPx()
        val arcSize = size.width.coerceAtMost(size.height) - chartPadding * 2f
        val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)
        var startAngle = -92f

        if (totalValue <= 0.0) {
            drawArc(
                color = SurfaceVariant.copy(alpha = 0.65f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            return@Canvas
        }

        assets.forEachIndexed { index, asset ->
            val rawSweep = (asset.currentValue / totalValue * 360.0).toFloat()
            val sweep = (rawSweep - 3.5f).coerceAtLeast(if (rawSweep > 0f) 1.5f else 0f)
            drawArc(
                color = allocationColor(index),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += rawSweep
        }
    }
}

@Composable
private fun AllocationAssetRow(
    asset: PortfolioAsset,
    percent: Double,
    color: Color,
    settings: AppSettings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(12.dp))
        AsyncImage(
            model = asset.imageUrl,
            contentDescription = asset.symbol,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                asset.symbol.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                asset.name,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${"%.1f".format(percent)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    settings.formatMoney(asset.currentValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    formatSignedPercent(asset.profitLossPercent),
                    style = MaterialTheme.typography.labelSmall,
                    color = pnlColor(asset.profitLossPercent),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun allocationColor(index: Int): Color = when (index % 6) {
    0 -> Color(0xFF20C989)
    1 -> Color(0xFF7A3DFF)
    2 -> Color(0xFFFF5C5C)
    3 -> Color(0xFF28C7C9)
    4 -> Color(0xFF8D9BB0)
    else -> Color(0xFFFFC74D)
}

@Composable
private fun PortfolioRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranges = listOf("24H", "1W", "1M", "3M", "6M", "1Y", "ALL")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ranges.forEach { range ->
            val selected = selectedRange == range
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) SurfaceVariant.copy(alpha = 0.72f) else Color.Transparent)
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    range,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) TextPrimary else TextTertiary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PortfolioHeroMetric(
    label: String,
    value: String,
    color: Color,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PortfolioHeroChart(
    values: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val series = values.ifEmpty { listOf(0.0, 0.0) }
        val paddingH = 4.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingBottom = 18.dp.toPx()
        val chartWidth = size.width - paddingH * 2
        val chartHeight = size.height - paddingTop - paddingBottom
        val minValue = series.minOrNull() ?: 0.0
        val maxValue = series.maxOrNull() ?: 0.0
        val range = (maxValue - minValue).coerceAtLeast(0.000001)

        fun xFor(index: Int): Float =
            if (series.size == 1) size.width / 2f else paddingH + index.toFloat() / series.lastIndex * chartWidth

        fun yFor(value: Double): Float =
            (paddingTop + (1.0 - (value - minValue) / range) * chartHeight).toFloat()

        repeat(3) { line ->
            val y = paddingTop + chartHeight * line / 2f
            drawLine(
                color = DividerColor.copy(alpha = 0.25f),
                start = Offset(paddingH, y),
                end = Offset(size.width - paddingH, y),
                strokeWidth = 0.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)),
            )
        }

        val points = series.mapIndexed { index, value -> Offset(xFor(index), yFor(value)) }
        val linePath = Path()
        points.firstOrNull()?.let { first ->
            linePath.moveTo(first.x, first.y)
            for (index in 1 until points.size) {
                val current = points[index]
                linePath.lineTo(current.x, current.y)
            }
        }

        if (points.size >= 2) {
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, size.height - paddingBottom)
                lineTo(points.first().x, size.height - paddingBottom)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                    startY = paddingTop,
                    endY = size.height,
                ),
            )
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter,
            ),
        )

        points.lastOrNull()?.let { last ->
            drawCircle(
                color = BackgroundDark,
                radius = 5.5.dp.toPx(),
                center = last,
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = last,
            )
        }
    }
}

private fun portfolioFallbackSeries(totalValue: Double, dayChangePercent: Double): List<Double> {
    val safeTotal = totalValue.coerceAtLeast(0.0)
    if (safeTotal == 0.0) return listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val direction = if (dayChangePercent >= 0.0) 1.0 else -1.0
    val amplitude = (kotlin.math.abs(dayChangePercent) / 100.0).coerceIn(0.012, 0.06)
    val anchors = listOf(
        -0.42, -0.34, -0.48, -0.18, -0.24, 0.08, -0.05, 0.16,
        -0.14, 0.03, 0.28, -0.22, 0.36, 0.18, 0.44, 0.31, 0.52,
    )
    return anchors.map { safeTotal * (1.0 + it * amplitude * direction) }
}

private fun formatSignedPercent(value: Double): String =
    "${if (value >= 0.0) "+" else "-"}${"%.2f".format(kotlin.math.abs(value))}%"

@Composable
private fun PortfolioTimelineCard(
    snapshots: List<PortfolioSnapshot>,
    currentSummary: PortfolioSummary?,
    settings: AppSettings,
) {
    val latest = snapshots.lastOrNull()
    val first = snapshots.firstOrNull()
    val valueChange = if (first != null && latest != null) latest.totalValue - first.totalValue else 0.0
    val isUp = valueChange >= 0.0
    val displaySnapshots = snapshots.ifEmpty {
        currentSummary?.let {
            listOf(
                PortfolioSnapshot(
                    userId = "",
                    dayStart = System.currentTimeMillis(),
                    totalValue = it.totalValue,
                    totalInvested = it.totalInvested,
                    totalProfitLoss = it.totalProfitLoss,
                    totalProfitLossPercent = it.totalProfitLossPercent,
                    assetCount = it.assets.size,
                    createdAt = System.currentTimeMillis(),
                )
            )
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Portfolio Timeline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    if (snapshots.size >= 2) "Daily value snapshots" else "Snapshot starts from today",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            if (snapshots.size >= 2) {
                Text(
                    settings.formatSignedMoney(valueChange, compact = true),
                    color = if (isUp) GreenUp else RedDown,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        PortfolioSnapshotChart(
            snapshots = displaySnapshots,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )

        if (snapshots.size < 2) {
            Spacer(Modifier.height(8.dp))
            Text(
                "More data points will appear as the app records daily portfolio snapshots.",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PortfolioSnapshotChart(
    snapshots: List<PortfolioSnapshot>,
    modifier: Modifier = Modifier,
) {
    if (snapshots.isEmpty()) {
        Box(modifier.background(SurfaceVariant), contentAlignment = Alignment.Center) {
            Text("No snapshot data", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        return
    }

        Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceVariant)) {
        val paddingH = 12.dp.toPx()
        val paddingV = 14.dp.toPx()
        val chartWidth = size.width - paddingH * 2
        val chartHeight = size.height - paddingV * 2
        val minValue = snapshots.minOf { it.totalValue }
        val maxValue = snapshots.maxOf { it.totalValue }
        val range = (maxValue - minValue).coerceAtLeast(0.000001)

        fun xFor(index: Int): Float {
            if (snapshots.size == 1) return size.width / 2f
            return paddingH + (index.toFloat() / (snapshots.lastIndex.toFloat())) * chartWidth
        }

        fun yFor(value: Double): Float =
            (paddingV + (1.0 - (value - minValue) / range) * chartHeight).toFloat()

        repeat(3) { line ->
            val y = paddingV + chartHeight * line / 2f
            drawLine(
                color = DividerColor.copy(alpha = 0.7f),
                start = Offset(paddingH, y),
                end = Offset(size.width - paddingH, y),
                strokeWidth = 0.7f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }

        val lineColor = if ((snapshots.last().totalValue - snapshots.first().totalValue) >= 0) GreenUp else RedDown
        if (snapshots.size == 1) {
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = Offset(xFor(0), yFor(snapshots.first().totalValue)),
            )
        } else {
            val path = Path()
            snapshots.forEachIndexed { index, snapshot ->
                val point = Offset(xFor(index), yFor(snapshot.totalValue))
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 2.2.dp.toPx()))
            snapshots.forEachIndexed { index, snapshot ->
                drawCircle(
                    color = lineColor,
                    radius = if (index == snapshots.lastIndex) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(xFor(index), yFor(snapshot.totalValue)),
                )
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(summary: PortfolioSummary, settings: AppSettings) {
    val isProfit = summary.isProfit
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Total Value", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(settings.formatMoney(summary.totalValue), fontSize = 34.sp,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (isProfit) GreenUpSurface else RedDownSurface)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isProfit) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isProfit) GreenUp else RedDown,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("${"%.2f".format(kotlin.math.abs(summary.totalProfitLossPercent))}%",
                        color = if (isProfit) GreenUp else RedDown,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(settings.formatSignedMoney(summary.totalProfitLoss),
                color = if (isProfit) GreenUp else RedDown,
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        val isUp24h = summary.isUp24h
        Text(
            "${settings.formatSignedMoney(summary.totalChange24h)} " +
                "(${"%.2f".format(kotlin.math.abs(summary.totalChange24hPercent))}%) today",
            color = if (isUp24h) GreenUp else RedDown,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric("Invested", settings.formatMoney(summary.totalInvested), TextPrimary, Modifier.weight(1f))
            SummaryMetric("Unrealized", settings.formatSignedMoney(summary.unrealizedProfitLoss), pnlColor(summary.unrealizedProfitLoss), Modifier.weight(1f))
            SummaryMetric("Realized", settings.formatSignedMoney(summary.realizedProfitLoss), pnlColor(summary.realizedProfitLoss), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric("Fees", settings.formatMoney(summary.totalFeesPaid), TextSecondary, Modifier.weight(1f))
            SummaryMetric("Assets", "${summary.assets.size}", TextPrimary, Modifier.weight(1f))
            SummaryMetric("24h", "${if (summary.isUp24h) "+" else "-"}${"%.2f".format(kotlin.math.abs(summary.totalChange24hPercent))}%", if (summary.isUp24h) GreenUp else RedDown, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

private fun pnlColor(value: Double): Color =
    if (value >= 0.0) GreenUp else RedDown

@Composable
private fun AssetsTab(
    assets: List<PortfolioAsset>,
    settings: AppSettings,
    onCoinClick: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (assets.isEmpty()) {
        EmptyPortfolioState(Icons.Outlined.AccountBalanceWallet, "No assets yet", "Tap + to record your first buy")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        items(assets, key = { it.id }) { asset ->
            AssetItem(
                asset = asset,
                settings = settings,
                onCoinClick = { onCoinClick(asset.coinId) },
                onDelete = { onDelete(asset.id) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetItem(
    asset: PortfolioAsset,
    settings: AppSettings,
    onCoinClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isProfit = asset.isProfit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
                        .combinedClickable(onClick = onCoinClick, onLongClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = asset.imageUrl, contentDescription = asset.name,
            modifier = Modifier.size(40.dp).clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(asset.symbol, style = MaterialTheme.typography.titleSmall,
                color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text("Qty: ${"%.6f".format(asset.quantity).trimEnd('0').trimEnd('.')}",
                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Avg: ${settings.formatMoney(asset.averageBuyPrice)}",
                style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text("${"%.1f".format(asset.allocationPercent)}% allocation",
                style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(settings.formatMoney(asset.currentValue), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(settings.formatSignedMoney(asset.profitLoss),
                style = MaterialTheme.typography.labelSmall, color = if (isProfit) GreenUp else RedDown)
            Text("(${"%.2f".format(asset.profitLossPercent)}%)",
                style = MaterialTheme.typography.labelSmall, color = if (isProfit) GreenUp else RedDown)
            val dayChange = asset.change24h
            Text(
                "24h ${settings.formatSignedMoney(dayChange)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (dayChange >= 0.0) GreenUp else RedDown,
            )
        }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, containerColor = SurfaceDark,
            titleContentColor = TextPrimary, textContentColor = TextSecondary,
            title = { Text("Remove ${asset.name}?", fontWeight = FontWeight.Bold) },
            text  = { Text("This removes the asset from your portfolio.") },
            confirmButton = { TextButton(onClick = { showDialog = false; onDelete() }) {
                Text("Remove", color = RedDown, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) {
                Text("Cancel", color = TextSecondary) } },
        )
    }
}

@Composable
private fun TransactionsTab(
    transactions: List<Transaction>,
    settings: AppSettings,
    onDelete: (String) -> Unit,
) {
    if (transactions.isEmpty()) {
        EmptyPortfolioState(Icons.Outlined.Receipt, "No transactions", "Your history will appear here")
        return
    }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        items(transactions, key = { it.id }) { tx ->
            TransactionItem(
                tx = tx,
                settings = settings,
                fmt = fmt,
                onDelete = { onDelete(tx.id) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    tx: Transaction,
    settings: AppSettings,
    fmt: SimpleDateFormat,
    onDelete: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isBuy = tx.type == TransactionType.BUY
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
                        .combinedClickable(onClick = { showDialog = true }, onLongClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(if (isBuy) GreenUpSurface else RedDownSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (isBuy) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = tx.type.name,
                tint = if (isBuy) GreenUp else RedDown, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(if (isBuy) "Buy" else "Sell",
                    color = if (isBuy) GreenUp else RedDown,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(" ${tx.symbol}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            }
            Text(fmt.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${"%.4f".format(tx.quantity).trimEnd('0').trimEnd('.')} ${tx.symbol}",
                style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            Text("@ ${settings.formatMoney(tx.price)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(settings.formatMoney(tx.total), style = MaterialTheme.typography.labelSmall,
                color = if (isBuy) GreenUp else RedDown)
        }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, containerColor = SurfaceDark,
            titleContentColor = TextPrimary, textContentColor = TextSecondary,
            title = { Text("Delete transaction?", fontWeight = FontWeight.Bold) },
            text  = { Text("This action cannot be undone.") },
            confirmButton = { TextButton(onClick = { showDialog = false; onDelete() }) {
                Text("Delete", color = RedDown, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) {
                Text("Cancel", color = TextSecondary) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, TransactionType, String, String, String, String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(TransactionType.BUY) }
    var coinId   by remember { mutableStateOf("") }
    var symbol   by remember { mutableStateOf("") }
    var name     by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price    by remember { mutableStateOf("") }
    var fee      by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }
    var coinError by remember { mutableStateOf<String?>(null) }

    val popularCoins = listOf(
        Triple("bitcoin",      "BTC",  "Bitcoin"),
        Triple("ethereum",     "ETH",  "Ethereum"),
        Triple("binancecoin",  "BNB",  "BNB"),
        Triple("solana",       "SOL",  "Solana"),
        Triple("ripple",       "XRP",  "XRP"),
        Triple("dogecoin",     "DOGE", "Dogecoin"),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceDark,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(SurfaceVariant))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Add Transaction", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(20.dp))

            // Buy/Sell Toggle
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceVariant)) {
                TransactionType.entries.forEach { type ->
                    val isSel = selectedType == type
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) (if (type == TransactionType.BUY) GreenUp else RedDown) else Color.Transparent)
                            .clickable { selectedType = type }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(type.name,
                            color = if (isSel) Color.White else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Coin chips
            Text("Select Coin", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularCoins) { (cId, cSym, cName) ->
                    val isSel = coinId == cId
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) BinanceYellow.copy(0.15f) else SurfaceVariant)
                                                        .clickable { coinId = cId; symbol = cSym; name = cName; imageUrl = ""; coinError = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(cSym, color = if (isSel) BinanceYellow else TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            coinError?.let { Text(it, color = RedDown, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)) }

            Spacer(Modifier.height(16.dp))
            SheetField(quantity, { quantity = it }, "Quantity", KeyboardType.Decimal, "e.g. 0.5")
            Spacer(Modifier.height(12.dp))
            SheetField(price, { price = it }, "Price per coin (USD)", KeyboardType.Decimal, "e.g. 65000", "$")
            Spacer(Modifier.height(12.dp))
            SheetField(fee, { fee = it }, "Fee (optional)", KeyboardType.Decimal, "e.g. 1.5", "$")
            Spacer(Modifier.height(12.dp))
            SheetField(note, { note = it }, "Note (optional)", KeyboardType.Text, "e.g. DCA strategy")

            val total = (quantity.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(settings.formatMoney(total), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = BinanceYellow)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (coinId.isBlank()) { coinError = "Please select a coin"; return@Button }
                    onConfirm(coinId, symbol, name, imageUrl, selectedType, quantity, price, fee, note)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.BUY) GreenUp else RedDown,
                    contentColor   = Color.White,
                ),
            ) { Text("Confirm ${selectedType.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun SheetField(
    value: String, onValueChange: (String) -> Unit,
    label: String, keyboardType: KeyboardType, placeholder: String, leadingText: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextTertiary) },
        leadingIcon = leadingText?.let { { Text(it, color = TextSecondary, modifier = Modifier.padding(start = 4.dp)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
            focusedLabelColor  = BinanceYellow, unfocusedLabelColor  = TextSecondary,
            focusedTextColor   = TextPrimary,   unfocusedTextColor   = TextPrimary,
            cursorColor        = BinanceYellow,
            focusedContainerColor = SurfaceVariant, unfocusedContainerColor = SurfaceVariant,
        ),
    )
}

@Composable
private fun EmptyPortfolioState(icon: ImageVector, title: String, message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}
