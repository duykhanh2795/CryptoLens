package com.cryptolens.app.features.portfolio.presentation

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.ui.theme.*
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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PortfolioEffect.ShowError   -> snackbar.showSnackbar(effect.message)
                is PortfolioEffect.ShowSuccess -> snackbar.showSnackbar(effect.message)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Portfolio", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
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
                    // Summary Card
                    uiState.summary?.let { PortfolioSummaryCard(it) }
                    PortfolioTimelineCard(
                        snapshots = uiState.snapshots,
                        currentSummary = uiState.summary,
                    )

                    // Tabs
                    TabRow(
                        selectedTabIndex = PortfolioTab.entries.indexOf(uiState.selectedTab),
                        containerColor   = BackgroundDark,
                        contentColor     = BinanceYellow,
                        indicator = { tabs ->
                            val idx = PortfolioTab.entries.indexOf(uiState.selectedTab)
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabs[idx]),
                                height = 2.dp, color = BinanceYellow,
                            )
                        },
                        divider = { HorizontalDivider(color = DividerColor) },
                    ) {
                        PortfolioTab.entries.forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick  = { viewModel.onEvent(PortfolioEvent.TabSelected(tab)) },
                                text = {
                                    Text(
                                        tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (uiState.selectedTab == tab) BinanceYellow else TextSecondary,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            )
                        }
                    }

                    // Tab content
                    when (uiState.selectedTab) {
                        PortfolioTab.ASSETS -> AssetsTab(
                            assets = uiState.summary?.assets ?: emptyList(),
                            onCoinClick = onCoinClick,
                            onDelete = { viewModel.onEvent(PortfolioEvent.DeleteAsset(it)) },
                        )
                        PortfolioTab.TRANSACTIONS -> TransactionsTab(
                            transactions = uiState.transactions,
                            onDelete = { viewModel.onEvent(PortfolioEvent.DeleteTransaction(it)) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddSheet) {
        AddTransactionSheet(
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
}

@Composable
private fun PortfolioTimelineCard(
    snapshots: List<PortfolioSnapshot>,
    currentSummary: PortfolioSummary?,
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
                    "${if (isUp) "+" else "-"}$${kotlin.math.abs(valueChange).formatLargeNumber()}",
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

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceVariant)) {
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
private fun PortfolioSummaryCard(summary: PortfolioSummary) {
    val isProfit = summary.isProfit
    Column(
        modifier = Modifier.fillMaxWidth().background(SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Total Value", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text("$${"%.2f".format(summary.totalValue)}", fontSize = 34.sp,
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
            Text("${if (isProfit) "+" else "-"}$$${"%.2f".format(kotlin.math.abs(summary.totalProfitLoss))}",
                color = if (isProfit) GreenUp else RedDown,
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Invested", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("$${"%.2f".format(summary.totalInvested)}", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            VerticalDivider(Modifier.height(32.dp), color = DividerColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Assets", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("${summary.assets.size}", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun AssetsTab(assets: List<PortfolioAsset>, onCoinClick: (String) -> Unit, onDelete: (String) -> Unit) {
    if (assets.isEmpty()) {
        EmptyPortfolioState(Icons.Outlined.AccountBalanceWallet, "No assets yet", "Tap + to record your first buy")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        items(assets, key = { it.id }) { asset ->
            AssetItem(asset = asset, onCoinClick = { onCoinClick(asset.coinId) }, onDelete = { onDelete(asset.id) })
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetItem(asset: PortfolioAsset, onCoinClick: () -> Unit, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val isProfit = asset.isProfit
    Row(
        modifier = Modifier.fillMaxWidth()
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
            Text("Avg: $${asset.averageBuyPrice.formatPrice()}",
                style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${"%.2f".format(asset.currentValue)}", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${if (isProfit) "+" else ""}${"%.2f".format(asset.profitLoss)}",
                style = MaterialTheme.typography.labelSmall, color = if (isProfit) GreenUp else RedDown)
            Text("(${"%.2f".format(asset.profitLossPercent)}%)",
                style = MaterialTheme.typography.labelSmall, color = if (isProfit) GreenUp else RedDown)
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
private fun TransactionsTab(transactions: List<Transaction>, onDelete: (String) -> Unit) {
    if (transactions.isEmpty()) {
        EmptyPortfolioState(Icons.Outlined.Receipt, "No transactions", "Your history will appear here")
        return
    }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        items(transactions, key = { it.id }) { tx ->
            TransactionItem(tx = tx, fmt = fmt, onDelete = { onDelete(tx.id) })
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(tx: Transaction, fmt: SimpleDateFormat, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val isBuy = tx.type == TransactionType.BUY
    Row(
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { showDialog = true })
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
            Text("@ $${tx.price.formatPrice()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("$${"%.2f".format(tx.total)}", style = MaterialTheme.typography.labelSmall,
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
                            .border(1.dp, if (isSel) BinanceYellow else Color.Transparent, RoundedCornerShape(8.dp))
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
                    Text("$${"%.2f".format(total)}", style = MaterialTheme.typography.bodyMedium,
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
            focusedBorderColor = BinanceYellow, unfocusedBorderColor = BorderColor,
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
