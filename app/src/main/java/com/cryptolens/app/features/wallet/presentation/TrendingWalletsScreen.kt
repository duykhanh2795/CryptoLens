package com.cryptolens.app.features.wallet.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptolens.app.R
import com.cryptolens.app.core.domain.util.formatLargeNumber
import com.cryptolens.app.core.domain.util.formatMoney
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletTransaction
import com.cryptolens.app.features.wallet.domain.model.WalletTransactionType
import com.cryptolens.app.features.wallet.domain.model.shortWalletAddress
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrendingWalletsScreen(
    onNavigateBack: () -> Unit,
    onWalletClick: (WalletChain, String) -> Unit,
    viewModel: TrendingWalletsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        TrendingWalletsTopBar(
            query = uiState.query,
            onQueryChange = { viewModel.onEvent(TrendingWalletsEvent.QueryChanged(it)) },
            onBack = onNavigateBack,
            onRefresh = { viewModel.onEvent(TrendingWalletsEvent.Refresh) },
        )

        Text(
            "Trending Addresses",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BinanceYellow)
            }
            uiState.error != null -> WalletError(message = uiState.error!!, onRetry = {
                viewModel.onEvent(TrendingWalletsEvent.Refresh)
            })
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(uiState.filteredWallets, key = { "${it.chain.name}:${it.address}" }) { wallet ->
                    TrendingWalletRow(
                        wallet = wallet,
                        settings = uiState.settings,
                        onClick = { onWalletClick(wallet.chain, wallet.address) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingWalletsTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = TextPrimary)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).height(52.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.wallet_explore_any_address)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
)
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
        }
    }
}

@Composable
fun TrendingWalletRow(
    wallet: TrendingWallet,
    onClick: () -> Unit,
    compact: Boolean = false,
    settings: AppSettings = AppSettings(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 0.dp else 20.dp, vertical = if (compact) 8.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletAvatar(chain = wallet.chain, seed = wallet.avatarSeed, modifier = Modifier.size(if (compact) 34.dp else 46.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                wallet.displayName,
                color = TextPrimary,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                wallet.chain.label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                wallet.valueUsd?.let { settings.formatMoney(it, compact = true) } ?: "Syncing",
                color = if (wallet.isPositive) GreenUp else RedDown,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            ChangePill(percent = wallet.changePercent24h)
        }
    }
}

@Composable
fun WalletAvatar(
    chain: WalletChain,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        BinanceYellow,
        Color(0xFF8A8F98),
        Color(0xFF7C6FE8),
        Color(0xFF56606B),
        Color(0xFFFF7182),
    )
    val base = colors[abs(seed) % colors.size]
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                                .padding(1.dp),
        ) {
            drawCircle(base.copy(alpha = 0.95f))
            val cell = size.width / 5f
            for (x in 0..4) {
                for (y in 0..4) {
                    if (((x * 31 + y * 17 + seed) % 3) == 0) {
                        drawRect(
                            color = colors[(x + y + seed).mod(colors.size)].copy(alpha = 0.9f),
                            topLeft = Offset(x * cell, y * cell),
                            size = androidx.compose.ui.geometry.Size(cell, cell),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(17.dp)
                .background(SurfaceDark, CircleShape)
                ,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                chain.nativeSymbol.take(1),
                color = BinanceYellow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChangePill(percent: Double) {
    val positive = percent >= 0.0
    Box(
        modifier = Modifier
            .background(if (positive) GreenUpSurface else RedDownSurface, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "${if (positive) "+" else "-"}${"%.2f".format(abs(percent))}%",
            color = if (positive) GreenUp else RedDown,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private enum class WalletHistoryFilter(val label: String) {
    ALL("All"),
    RECEIVED("Received"),
    SENT("Sent"),
    EXECUTED("Contract"),
    TOKEN("Token")
}

@Composable
fun TrendingWalletDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrendingWalletDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TrendingWalletDetailEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
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
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BinanceYellow)
            }
            uiState.error != null -> WalletError(message = uiState.error!!, onRetry = {
                viewModel.onEvent(TrendingWalletDetailEvent.Retry)
            })
            uiState.detail != null -> TrendingWalletDetailContent(
                detail = uiState.detail!!,
                settings = uiState.settings,
                selectedTab = uiState.selectedTab,
                isAdding = uiState.isAddingToWatchlist,
                onBack = onNavigateBack,
                onTabChange = { viewModel.onEvent(TrendingWalletDetailEvent.TabChanged(it)) },
                onAddToWatchlist = { viewModel.onEvent(TrendingWalletDetailEvent.AddToWatchlist) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TrendingWalletDetailContent(
    detail: TrendingWalletDetail,
    settings: AppSettings,
    selectedTab: WalletDetailTab,
    isAdding: Boolean,
    onBack: () -> Unit,
    onTabChange: (WalletDetailTab) -> Unit,
    onAddToWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wallet = detail.wallet
    var historyFilter by remember { mutableStateOf(WalletHistoryFilter.ALL) }
    var historyQuery by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<WalletTransaction?>(null) }

    Column(modifier.fillMaxSize().background(BackgroundDark)) {
        WalletDetailHero(
            detail = detail,
            settings = settings,
            isAdding = isAdding,
            onBack = onBack,
            onAddToWatchlist = onAddToWatchlist,
            selectedTab = selectedTab,
            onTabChange = onTabChange,
        )

        when (selectedTab) {
            WalletDetailTab.ASSETS -> AssetsTab(assets = detail.assets, settings = settings, note = detail.historyNote)
            WalletDetailTab.HISTORY -> HistoryTab(
                history = detail.history,
                settings = settings,
                note = detail.historyNote,
                filter = historyFilter,
                query = historyQuery,
                onFilterChange = { historyFilter = it },
                onQueryChange = { historyQuery = it },
                onTransactionClick = { selectedTransaction = it },
            )
        }
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            settings = settings,
            onDismiss = { selectedTransaction = null },
        )
    }
}

@Composable
private fun WalletDetailHero(
    detail: TrendingWalletDetail,
    settings: AppSettings,
    isAdding: Boolean,
    onBack: () -> Unit,
    onAddToWatchlist: () -> Unit,
    selectedTab: WalletDetailTab,
    onTabChange: (WalletDetailTab) -> Unit,
) {
    val wallet = detail.wallet
    Column(
        Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(bottom = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = TextPrimary)
            }
            Text(settings.currency.code, color = TextSecondary, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(SurfaceVariant, RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(wallet.displayName, color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.weight(1f))
            WalletAvatar(chain = wallet.chain, seed = wallet.avatarSeed, modifier = Modifier.size(42.dp))
        }

        Text(
            wallet.chain.label,
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            detail.totalValueUsd?.let { settings.formatMoney(it, compact = true) }
                ?: wallet.valueUsd?.let { settings.formatMoney(it, compact = true) }
                ?: "Value unavailable",
            color = TextPrimary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${wallet.nativeBalance.formatNative()} ${wallet.chain.nativeSymbol}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(8.dp))
            ChangePill(percent = wallet.changePercent24h)
        }

        WalletMiniChart(isPositive = wallet.isPositive)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onAddToWatchlist,
                enabled = !isAdding,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wallet_add_to_watchlist), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Surface(
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text("${wallet.txCount} tx", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        WalletTabs(selectedTab = selectedTab, onTabChange = onTabChange)
    }
}

@Composable
private fun WalletMiniChart(isPositive: Boolean) {
    val color = if (isPositive) GreenUp else RedDown
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        val path = Path()
        val points = listOf(0.18f, 0.34f, 0.28f, 0.48f, 0.26f, 0.62f, 0.42f, 0.55f, 0.72f, 0.50f, 0.66f, 0.78f)
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.lastIndex.coerceAtLeast(1))
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = 0.95f), style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun WalletTabs(
    selectedTab: WalletDetailTab,
    onTabChange: (WalletDetailTab) -> Unit,
) {
    val tabs = WalletDetailTab.entries
    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        containerColor = SurfaceDark,
        contentColor = BinanceYellow,
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(positions[tabs.indexOf(selectedTab)]),
                color = BinanceYellow,
                height = 2.dp,
            )
        },
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabChange(tab) },
                text = {
                    Text(
                        tab.name,
                        color = if (selectedTab == tab) BinanceYellow else TextSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

@Composable
private fun AssetsTab(
    assets: List<WalletAsset>,
    settings: AppSettings,
    note: String?,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        if (note != null) {
            item {
                WalletInfoNotice(
                    message = note,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.wallet_qty_total), color = TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("24h", color = TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(stringResource(R.string.wallet_price), color = TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        items(assets, key = { it.symbol }) { asset ->
            AssetRow(asset, settings)
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(start = 76.dp))
        }
    }
}

@Composable
private fun AssetRow(asset: WalletAsset, settings: AppSettings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletAvatar(chain = asset.chain, seed = asset.symbol.hashCode(), modifier = Modifier.size(42.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${asset.symbol} ${asset.quantity.formatNative()}",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${asset.networkLabel}  •  ${asset.valueUsd?.let { settings.formatMoney(it, compact = true) } ?: "Value unavailable"}",
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(
                "${if (asset.changePercent24h >= 0) "+" else "-"}${"%.2f".format(abs(asset.changePercent24h))}%",
                color = if (asset.changePercent24h >= 0) GreenUp else RedDown,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            asset.priceUsd?.let { settings.formatMoney(it) } ?: "-",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun HistoryTab(
    history: List<WalletTransaction>,
    settings: AppSettings,
    note: String?,
    filter: WalletHistoryFilter,
    query: String,
    onFilterChange: (WalletHistoryFilter) -> Unit,
    onQueryChange: (String) -> Unit,
    onTransactionClick: (WalletTransaction) -> Unit,
) {
    val filteredHistory = remember(history, filter, query) {
        history
            .filter { tx ->
                when (filter) {
                    WalletHistoryFilter.ALL -> true
                    WalletHistoryFilter.RECEIVED -> tx.type == WalletTransactionType.RECEIVED
                    WalletHistoryFilter.SENT -> tx.type == WalletTransactionType.SENT
                    WalletHistoryFilter.EXECUTED -> tx.type == WalletTransactionType.EXECUTED
                    WalletHistoryFilter.TOKEN -> tx.symbol !in setOf("ETH", "BTC", "BNB", "POL", "MATIC")
                }
            }
            .filter { tx ->
                query.isBlank() ||
                    tx.symbol.contains(query, ignoreCase = true) ||
                    tx.id.contains(query, ignoreCase = true) ||
                    tx.counterparty.orEmpty().contains(query, ignoreCase = true) ||
                    tx.networkLabel.contains(query, ignoreCase = true)
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (note != null) {
            item { WalletInfoNotice(note) }
        }
        item {
            HistoryControls(
                query = query,
                selectedFilter = filter,
                onQueryChange = onQueryChange,
                onFilterChange = onFilterChange,
            )
        }
        if (filteredHistory.isEmpty()) {
            item {
                Text(
                    if (history.isEmpty()) {
                        "No transaction history available for this address yet."
                    } else {
                        "No transactions match this filter."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                )
            }
        } else {
            filteredHistory.groupByDay().forEach { (day, transactions) ->
                item {
                    Text(day, color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column {
                            transactions.forEachIndexed { index, tx ->
                                TransactionRow(
                                    tx = tx,
                                    settings = settings,
                                    onClick = { onTransactionClick(tx) },
                                )
                                if (index < transactions.lastIndex) {
                                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(start = 72.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryControls(
    query: String,
    selectedFilter: WalletHistoryFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WalletHistoryFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.wallet_search_history)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WalletHistoryFilter.entries, key = { it.name }) { item ->
                FilterChip(
                    selected = selectedFilter == item,
                    onClick = { onFilterChange(item) },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.16f),
                        selectedLabelColor = BinanceYellow,
                        labelColor = TextSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun WalletInfoNotice(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun TransactionRow(
    tx: WalletTransaction,
    settings: AppSettings,
    onClick: () -> Unit,
) {
    val positive = tx.type == WalletTransactionType.RECEIVED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(if (positive) GreenUpSurface else RedDownSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(tx.symbol.take(1), color = if (positive) GreenUp else RedDown, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (tx.type) {
                    WalletTransactionType.RECEIVED -> "Received"
                    WalletTransactionType.SENT -> "Sent"
                    WalletTransactionType.EXECUTED -> "Executed"
                },
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                tx.counterparty?.let {
                    "${if (positive) "from" else "to"} ${it.shortWalletAddress()}"
                } ?: tx.networkLabel.ifBlank { DateUtils.getRelativeTimeSpanString(tx.timestamp).toString() },
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (positive) "+" else "-"}${tx.amount.formatNative()} ${tx.symbol}",
                color = if (positive) GreenUp else RedDown,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(tx.valueUsd?.let { settings.formatMoney(it) } ?: "", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    transaction: WalletTransaction,
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val explorerUrl = transaction.explorerUrl()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (transaction.type == WalletTransactionType.RECEIVED) GreenUpSurface else RedDownSurface,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        transaction.symbol.take(1),
                        color = if (transaction.type == WalletTransactionType.RECEIVED) GreenUp else RedDown,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.type.label,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(transaction.networkLabel, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            DetailRow("Amount", "${transaction.signedPrefix()}${transaction.amount.formatNative()} ${transaction.symbol}")
            DetailRow("Value", transaction.valueUsd?.let { settings.formatMoney(it) } ?: "Unavailable")
            DetailRow("Time", transaction.timestamp.fullDateTime())
            DetailRow("Tx hash", transaction.id.shortWalletAddress())
            DetailRow("Counterparty", transaction.counterparty?.shortWalletAddress() ?: "Unavailable")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(transaction.id)) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Copy Hash", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        transaction.counterparty?.let { clipboard.setText(AnnotatedString(it)) }
                    },
                    enabled = transaction.counterparty != null,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Copy Address", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = { explorerUrl?.let(uriHandler::openUri) },
                enabled = explorerUrl != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BinanceYellow,
                    contentColor = Color(0xFF1A1400),
                ),
            ) {
                Text("Open Explorer", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Close", color = TextTertiary)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
    }
}

@Composable
private fun WalletError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = Color(0xFF1A1400)),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

private fun List<WalletTransaction>.groupByDay(): Map<String, List<WalletTransaction>> {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return groupBy { formatter.format(Date(it.timestamp)) }
}

private val WalletTransactionType.label: String
    get() = when (this) {
        WalletTransactionType.RECEIVED -> "Received"
        WalletTransactionType.SENT -> "Sent"
        WalletTransactionType.EXECUTED -> "Contract"
    }

private fun WalletTransaction.signedPrefix(): String =
    when (type) {
        WalletTransactionType.RECEIVED -> "+"
        WalletTransactionType.SENT -> "-"
        WalletTransactionType.EXECUTED -> ""
    }

private fun Long.fullDateTime(): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(this))

private fun WalletTransaction.explorerUrl(): String? {
    if (id.isBlank() || id.contains(":")) return null
    val base = when (networkLabel.lowercase(Locale.US)) {
        "ethereum" -> "https://etherscan.io/tx/"
        "polygon" -> "https://polygonscan.com/tx/"
        "bnb chain" -> "https://bscscan.com/tx/"
        "base" -> "https://basescan.org/tx/"
        "arbitrum" -> "https://arbiscan.io/tx/"
        "optimism" -> "https://optimistic.etherscan.io/tx/"
        "bitcoin" -> "https://blockstream.info/tx/"
        else -> return null
    }
    return base + id
}

private fun Double.formatNative(): String =
    when {
        this >= 1.0 -> "%.6f".format(this).trimEnd('0').trimEnd('.')
        this > 0.0 -> formatPrice()
        else -> "0"
    }
