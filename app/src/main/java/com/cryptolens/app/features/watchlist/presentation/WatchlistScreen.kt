package com.cryptolens.app.features.watchlist.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.ui.theme.*

@Composable
fun WatchlistScreen(
    onCoinClick: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WatchlistEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
                is WatchlistEffect.ShowError   -> snackbar.showSnackbar(effect.message)
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
                .background(BackgroundDark)
                .padding(padding)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            BinanceWatchlistTopBar(
                sortOrder  = uiState.sortOrder,
                isLoading  = uiState.isLoadingPrices,
                isRealtimeConnected = uiState.isRealtimeConnected,
                isSearchVisible = showSearch || uiState.searchQuery.isNotBlank(),
                onSearchToggle = {
                    showSearch = !showSearch
                    if (!showSearch) viewModel.onEvent(WatchlistEvent.SearchChanged(""))
                },
                onRefresh  = { viewModel.onEvent(WatchlistEvent.Refresh) },
                onSortChange = { viewModel.onEvent(WatchlistEvent.SortChanged(it)) },
            )
            BinanceWatchlistTabsAndFilters(
                query = uiState.searchQuery,
                filter = uiState.filter,
                sortOrder = uiState.sortOrder,
                showSearch = showSearch || uiState.searchQuery.isNotBlank(),
                onQueryChange = { viewModel.onEvent(WatchlistEvent.SearchChanged(it)) },
                onFilterChange = { viewModel.onEvent(WatchlistEvent.FilterChanged(it)) },
                onSortChange = { viewModel.onEvent(WatchlistEvent.SortChanged(it)) },
            )

            when {
                uiState.items.isEmpty() && !uiState.isLoadingPrices ->
                    WatchlistEmptyState(
                        isFiltered = uiState.searchQuery.isNotBlank() ||
                            uiState.filter != WatchlistFilter.ALL,
                    )

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
                    ) {
                        items(
                            items = uiState.items,
                            key   = { it.coinId },
                        ) { item ->
                            BinanceWatchlistCoinRow(
                                item      = item,
                                onClick   = { onCoinClick(item.coinId) },
                                onRemove  = { viewModel.onEvent(WatchlistEvent.Remove(item.coinId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun BinanceWatchlistTopBar(
    sortOrder: WatchlistSortOrder,
    isLoading: Boolean,
    isRealtimeConnected: Boolean,
    isSearchVisible: Boolean,
    onSearchToggle: () -> Unit,
    onRefresh: () -> Unit,
    onSortChange: (WatchlistSortOrder) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AppAccent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BackgroundDark,
            )
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(visible = isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                color       = AppAccent,
                strokeWidth = 2.dp,
            )
        }
        AnimatedVisibility(visible = !isLoading) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isRealtimeConnected) GreenUp else TextTertiary),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (isRealtimeConnected) "LIVE" else "SYNC",
                    color = if (isRealtimeConnected) GreenUp else TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        IconButton(onClick = onSearchToggle) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isSearchVisible) TextPrimary else TextSecondary,
                modifier = Modifier.size(27.dp),
            )
        }

        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.CropFree,
                contentDescription = "Refresh",
                tint = TextSecondary,
                modifier = Modifier.size(27.dp),
            )
        }

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    tint = TextSecondary,
                    modifier = Modifier.size(27.dp),
                )
            }
            DropdownMenu(
                expanded         = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier         = Modifier.background(SurfaceDark),
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text  = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sortOrder == option.order) AppAccent else TextPrimary,
                                )
                            }
                        },
                        leadingIcon = {
                            if (sortOrder == option.order) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = AppAccent, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            onSortChange(option.order)
                            showSortMenu = false
                        },
                    )
                }
            }
        }
    }
}

private data class SortOption(val label: String, val order: WatchlistSortOrder) {
    companion object {
        val entries = listOf(
            SortOption("Date Added",    WatchlistSortOrder.DEFAULT),
            SortOption("Name A→Z",      WatchlistSortOrder.NAME_ASC),
            SortOption("Top Gainers",   WatchlistSortOrder.CHANGE_DESC),
            SortOption("Top Losers",    WatchlistSortOrder.CHANGE_ASC),
            SortOption("Highest Price", WatchlistSortOrder.PRICE_DESC),
            SortOption("Lowest Price",  WatchlistSortOrder.PRICE_ASC),
        )
    }
}

// ── Summary Strip ─────────────────────────────────────────────────────────────

@Composable
private fun BinanceWatchlistTabsAndFilters(
    query: String,
    filter: WatchlistFilter,
    sortOrder: WatchlistSortOrder,
    showSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WatchlistFilter) -> Unit,
    onSortChange: (WatchlistSortOrder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Watchlist",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp),
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
            )
            Spacer(Modifier.width(22.dp))
            Text(
                text = "Coins",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp),
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        if (showSearch) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(AppAccent),
                    decorationBox = { innerTextField ->
                        if (query.isBlank()) {
                            Text(
                                "Search symbol or name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary,
                            )
                        }
                        innerTextField()
                    },
                )
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WatchlistFilter.entries.forEach { item ->
                val selected = filter == item
                BinanceMarketFilterTab(
                    label = when (item) {
                        WatchlistFilter.ALL -> "Popular"
                        WatchlistFilter.GAINERS -> "Gainers"
                        WatchlistFilter.LOSERS -> "Losers"
                    },
                    selected = selected,
                    onClick = { onFilterChange(item) },
                )
                Spacer(Modifier.width(20.dp))
            }
            Spacer(Modifier.weight(1f))
            BinanceSortText(
                label = "Price",
                selected = sortOrder == WatchlistSortOrder.PRICE_DESC || sortOrder == WatchlistSortOrder.PRICE_ASC,
                onClick = {
                    onSortChange(
                        if (sortOrder == WatchlistSortOrder.PRICE_DESC) WatchlistSortOrder.PRICE_ASC
                        else WatchlistSortOrder.PRICE_DESC
                    )
                },
            )
            Spacer(Modifier.width(20.dp))
            BinanceSortText(
                label = "24h Change",
                selected = sortOrder == WatchlistSortOrder.CHANGE_DESC || sortOrder == WatchlistSortOrder.CHANGE_ASC,
                onClick = {
                    onSortChange(
                        if (sortOrder == WatchlistSortOrder.CHANGE_DESC) WatchlistSortOrder.CHANGE_ASC
                        else WatchlistSortOrder.CHANGE_DESC
                    )
                },
            )
        }
    }
}

@Composable
private fun BinanceMarketFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SurfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun BinanceSortText(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = null,
            tint = if (selected) TextPrimary else TextTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun WatchlistSearchAndFilters(
    query: String,
    filter: WatchlistFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WatchlistFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(AppAccent),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text(
                            "Search symbol or name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                        )
                    }
                    innerTextField()
                },
            )
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WatchlistFilter.entries.forEach { item ->
                val selected = filter == item
                WatchlistFilterTab(
                    label = item.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selected,
                    onClick = { onFilterChange(item) },
                )
            }
        }
    }
}

@Composable
private fun WatchlistFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppAccentContainer else SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun WatchlistFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) AppAccent else Color.Transparent),
        )
    }
}

@Composable
private fun WatchlistSummaryStrip(items: List<WatchlistItem>) {
    val withData    = items.filter { it.hasMarketData }
    val gainers     = withData.count { it.isPositive }
    val losers      = withData.count { !it.isPositive }
    val avgChange   = if (withData.isEmpty()) 0.0
                      else withData.sumOf { it.priceChangePercent24h } / withData.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        SummaryChip(
            label = "Watching",
            value = "${items.size}",
            color = TextPrimary,
        )
        Spacer(Modifier.width(12.dp))
        SummaryChip(
            label = "Gainers",
            value = "$gainers",
            color = GreenUp,
        )
        Spacer(Modifier.width(12.dp))
        SummaryChip(
            label = "Losers",
            value = "$losers",
            color = RedDown,
        )
        Spacer(Modifier.width(12.dp))
        SummaryChip(
            label = "Avg 24h",
            value = "${"%.2f".format(avgChange)}%",
            color = if (avgChange >= 0) GreenUp else RedDown,
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

// ── Coin Row ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinanceWatchlistCoinRow(
    item: WatchlistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = { showDeleteDialog = true },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model              = item.imageUrl,
            contentDescription = item.name,
            modifier           = Modifier.size(40.dp).clip(CircleShape),
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text       = item.symbol,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text     = item.name,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (item.hasMarketData) {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedContent(
                    targetState = item.currentPrice,
                    label       = "price_${item.coinId}",
                    transitionSpec = {
                        (fadeIn() + slideInVertically { -it / 3 }) togetherWith
                        (fadeOut() + slideOutVertically { it / 3 })
                    },
                ) { price ->
                    Text(
                        text       = "$${price.formatPrice()}",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color      = TextPrimary,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.isPositive) GreenUpSurface else RedDownSurface
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isPositive)
                                Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint     = if (item.isPositive) GreenUp else RedDown,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text  = "${"%.2f".format(kotlin.math.abs(item.priceChangePercent24h))}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isPositive) GreenUp else RedDown,
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariant),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariant),
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.StarBorder,
                contentDescription = "Remove from watchlist",
                tint     = BinanceYellow,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // ── Remove dialog ──────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest  = { showDeleteDialog = false },
            containerColor    = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            icon = {
                Icon(Icons.Default.StarBorder, contentDescription = null,
                    tint = BinanceYellow, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Remove ${item.name}?", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
            },
            text = {
                Text(
                    "Remove ${item.symbol} from your watchlist?",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onRemove() },
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
                    shape   = RoundedCornerShape(8.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun WatchlistEmptyState(isFiltered: Boolean = false) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector       = Icons.Outlined.StarOutline,
            contentDescription = null,
            tint              = TextTertiary,
            modifier          = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text       = if (isFiltered) "No matches" else "Your watchlist is empty",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text      = "Go to Markets and tap the ★ icon\non any coin to add it here.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
