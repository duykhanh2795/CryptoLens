package com.cryptolens.app.features.wallet.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchListItem
import com.cryptolens.app.ui.theme.AppAccent
import com.cryptolens.app.ui.theme.AppAccentContainer
import com.cryptolens.app.ui.theme.BackgroundDark
import com.cryptolens.app.ui.theme.BinanceYellow
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
fun WalletWatchlistScreen(
    onNavigateBack: () -> Unit,
    onWalletDetailsClick: (WalletChain, String) -> Unit,
    onAssetClick: (String) -> Unit,
    viewModel: WalletWatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WalletWatchlistEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
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
                onClick = { viewModel.onEvent(WalletWatchlistEvent.ShowAddSheet) },
                containerColor = AppAccent,
                contentColor = BackgroundDark,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add wallet")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .background(BackgroundDark),
        ) {
            WalletTopBar(
                isRefreshing = uiState.isRefreshing,
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.onEvent(WalletWatchlistEvent.RefreshAll) },
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BinanceYellow)
                }
            } else if (uiState.wallets.isEmpty()) {
                EmptyWallets()
            } else {
                WalletList(
                    wallets = uiState.wallets,
                    portfolios = uiState.portfolios,
                    settings = uiState.settings,
                    onDelete = { viewModel.onEvent(WalletWatchlistEvent.DeleteWallet(it)) },
                    onWalletDetailsClick = onWalletDetailsClick,
                    onAssetClick = onAssetClick,
                )
            }
        }
    }

    if (uiState.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WalletWatchlistEvent.HideAddSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
        ) {
            AddWalletSheet(
                state = uiState,
                onEvent = viewModel::onEvent,
            )
        }
    }
}

@Composable
private fun WalletTopBar(
    isRefreshing: Boolean,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
) {
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
            text = "Wallet Watchlist",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = BinanceYellow,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh wallets", tint = TextPrimary)
            }
        }
    }
}

@Composable
private fun EmptyWallets() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "No tracked wallets yet",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add a public address to monitor native balance, transaction count and whale-size exposure.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WalletList(
    wallets: List<WalletWatchListItem>,
    portfolios: Map<String, ConnectedWalletPortfolioUiState>,
    settings: AppSettings,
    onDelete: (String) -> Unit,
    onWalletDetailsClick: (WalletChain, String) -> Unit,
    onAssetClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WalletScopeNotice()
        }
        items(wallets, key = { it.wallet.id }) { item ->
            WalletCard(
                item = item,
                portfolio = portfolios[item.wallet.id],
                settings = settings,
                onDelete = onDelete,
                onWalletDetailsClick = onWalletDetailsClick,
                onAssetClick = onAssetClick,
            )
        }
    }
}

@Composable
private fun WalletScopeNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "Connected wallets are read-only. Native balances load from public RPCs; token holdings, USD values and transfer history use the configured Alchemy or Moralis indexer.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun WalletCard(
    item: WalletWatchListItem,
    portfolio: ConnectedWalletPortfolioUiState?,
    settings: AppSettings,
    onDelete: (String) -> Unit,
    onWalletDetailsClick: (WalletChain, String) -> Unit,
    onAssetClick: (String) -> Unit,
) {
    val wallet = item.wallet

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        wallet.chain.nativeSymbol.take(3),
                        color = BinanceYellow,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            wallet.label,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (item.isNativeWhale) {
                            Spacer(Modifier.width(8.dp))
                            WhaleBadge()
                        }
                    }
                    Text(
                        "${wallet.chain.label} - ${wallet.address.shortAddress()}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onDelete(wallet.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete wallet", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WalletMetric(
                    label = "Balance",
                    value = "${wallet.nativeBalance.formatNative()} ${wallet.chain.nativeSymbol}",
                    modifier = Modifier.weight(1f),
                )
                WalletMetric(
                    label = "Native value",
                    value = item.nativeValueUsd?.let { settings.formatMoney(it, compact = true) } ?: "Price unavailable",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WalletMetric(
                    label = "Tx count",
                    value = wallet.txCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                WalletMetric(
                    label = "Last sync",
                    value = wallet.lastSyncedAt?.relativeTime().orEmpty().ifBlank { "Never" },
                    modifier = Modifier.weight(1f),
                )
            }

            if (wallet.syncError != null) {
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = RedDown,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        wallet.syncError,
                        color = RedDown,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
            ConnectedWalletPortfolioSection(
                portfolio = portfolio,
                nativeFallbackValueUsd = item.nativeValueUsd,
                settings = settings,
                onViewDetails = { onWalletDetailsClick(wallet.chain, wallet.address) },
                onAssetClick = onAssetClick,
            )
        }
    }
}

@Composable
private fun ConnectedWalletPortfolioSection(
    portfolio: ConnectedWalletPortfolioUiState?,
    nativeFallbackValueUsd: Double?,
    settings: AppSettings,
    onViewDetails: () -> Unit,
    onAssetClick: (String) -> Unit,
) {
    when {
        portfolio == null || portfolio.isLoading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = BinanceYellow,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Loading token holdings and wallet value...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        portfolio.detail != null -> {
            WalletPortfolioLoaded(
                detail = portfolio.detail,
                fallbackValueUsd = nativeFallbackValueUsd,
                settings = settings,
                onViewDetails = onViewDetails,
                onAssetClick = onAssetClick,
            )
        }
        portfolio.error != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    portfolio.error,
                    color = RedDown,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletMetric(
                        label = "Portfolio value",
                        value = nativeFallbackValueUsd?.let { settings.formatMoney(it, compact = true) } ?: "Unavailable",
                        modifier = Modifier.weight(1f),
                    )
                    WalletMetric(
                        label = "Assets",
                        value = "Native only",
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = onViewDetails, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.wallet_open_details), color = BinanceYellow, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WalletPortfolioLoaded(
    detail: TrendingWalletDetail,
    fallbackValueUsd: Double?,
    settings: AppSettings,
    onViewDetails: () -> Unit,
    onAssetClick: (String) -> Unit,
) {
    val totalValue = detail.totalValueUsd ?: fallbackValueUsd
    val change24h = detail.weightedChangePercent24h()
    val topAssets = detail.assets
        .filter { it.quantity > 0.0 }
        .sortedByDescending { it.valueUsd ?: 0.0 }
        .take(4)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WalletMetric(
                label = "Portfolio value",
                value = totalValue?.let { settings.formatMoney(it, compact = true) } ?: "Unavailable",
                modifier = Modifier.weight(1f),
            )
            WalletMetric(
                label = "24h change",
                value = "${if (change24h >= 0) "+" else ""}${"%.2f".format(change24h)}%",
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WalletMetric(
                label = "Assets",
                value = detail.assets.size.toString(),
                modifier = Modifier.weight(1f),
            )
            WalletMetric(
                label = "Transfers",
                value = detail.history.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        if (topAssets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                topAssets.forEach { asset ->
                    WalletAssetPreviewRow(
                        asset = asset,
                        settings = settings,
                        onAssetClick = onAssetClick,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            detail.historyNote?.takeIf { topAssets.size <= 1 }?.let { note ->
                Text(
                    note,
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = onViewDetails) {
                Text(stringResource(R.string.wallet_open_details), color = BinanceYellow, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WalletAssetPreviewRow(
    asset: WalletAsset,
    settings: AppSettings,
    onAssetClick: (String) -> Unit,
) {
    val clickableModifier = asset.coinId?.let { coinId ->
        Modifier.clickable { onAssetClick(coinId) }
    } ?: Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .background(SurfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(SurfaceDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                asset.symbol.take(3).uppercase(),
                color = BinanceYellow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                asset.symbol.uppercase(),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${asset.quantity.formatNative()} - ${asset.networkLabel}",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                asset.valueUsd?.let { settings.formatMoney(it, compact = true) } ?: "-",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${if (asset.changePercent24h >= 0) "+" else ""}${"%.2f".format(asset.changePercent24h)}%",
                color = if (asset.changePercent24h >= 0) GreenUp else RedDown,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WhaleBadge() {
    Row(
        modifier = Modifier
            .background(GreenUpSurface, RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Whale",
            color = GreenUp,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WalletMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddWalletSheet(
    state: WalletWatchlistUiState,
    onEvent: (WalletWatchlistEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Text(
            "Track Wallet",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))

        Text(stringResource(R.string.wallet_chain), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(WalletChain.entries.toList()) { chain ->
                FilterChip(
                    selected = state.selectedChain == chain,
                    onClick = { onEvent(WalletWatchlistEvent.ChainChanged(chain)) },
                    label = { Text(chain.nativeSymbol) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        selectedLabelColor = BinanceYellow,
                        labelColor = TextSecondary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.labelInput,
            onValueChange = { onEvent(WalletWatchlistEvent.LabelChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.wallet_label)) },
            placeholder = { Text(stringResource(R.string.wallet_default_label, state.selectedChain.label)) },
            singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
)

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.addressInput,
            onValueChange = { onEvent(WalletWatchlistEvent.AddressChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.wallet_public_address)) },
            singleLine = true,
            isError = state.inputError != null,
            supportingText = state.inputError?.let { message ->
                { Text(message) }
            },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
)

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onEvent(WalletWatchlistEvent.HideAddSheet) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.action_cancel), color = TextSecondary)
            }
            Button(
                onClick = { onEvent(WalletWatchlistEvent.AddWallet) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppAccent,
                    contentColor = BackgroundDark,
                    disabledContainerColor = AppAccentContainer,
                    disabledContentColor = TextTertiary,
                ),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = BackgroundDark,
                    )
                } else {
                    Text(stringResource(R.string.common_add), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun String.shortAddress(): String =
    if (length <= 14) this else "${take(6)}...${takeLast(6)}"

private fun Double.formatNative(): String =
    when {
        this >= 1.0 -> "%.6f".format(this).trimEnd('0').trimEnd('.')
        this > 0.0 -> formatPrice()
        else -> "0"
    }

private fun TrendingWalletDetail.weightedChangePercent24h(): Double {
    val valuedAssets = assets.filter { (it.valueUsd ?: 0.0) > 0.0 }
    val totalValue = valuedAssets.sumOf { it.valueUsd ?: 0.0 }
    return if (totalValue > 0.0) {
        valuedAssets.sumOf { (it.valueUsd ?: 0.0) * it.changePercent24h } / totalValue
    } else {
        wallet.changePercent24h
    }
}

private fun Long.relativeTime(): String =
    DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
