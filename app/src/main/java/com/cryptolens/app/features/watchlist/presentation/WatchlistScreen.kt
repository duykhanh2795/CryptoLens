package com.cryptolens.app.features.watchlist.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────
            WatchlistTopBar(
                sortOrder  = uiState.sortOrder,
                isLoading  = uiState.isLoadingPrices,
                isRealtimeConnected = uiState.isRealtimeConnected,
                onRefresh  = { viewModel.onEvent(WatchlistEvent.Refresh) },
                onSortChange = { viewModel.onEvent(WatchlistEvent.SortChanged(it)) },
            )
            WatchlistSearchAndFilters(
                query = uiState.searchQuery,
                filter = uiState.filter,
                onQueryChange = { viewModel.onEvent(WatchlistEvent.SearchChanged(it)) },
                onFilterChange = { viewModel.onEvent(WatchlistEvent.FilterChanged(it)) },
            )

            when {
                uiState.items.isEmpty() && !uiState.isLoadingPrices ->
                    WatchlistEmptyState(
                        isFiltered = uiState.searchQuery.isNotBlank() ||
                            uiState.filter != WatchlistFilter.ALL,
                    )

                else -> {
                    // Summary strip
                    if (uiState.items.isNotEmpty()) {
                        WatchlistSummaryStrip(items = uiState.items)
                    }

                    // Coin list
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                    ) {
                        items(
                            items = uiState.items,
                            key   = { it.coinId },
                        ) { item ->
                            WatchlistCoinRow(
                                item      = item,
                                onClick   = { onCoinClick(item.coinId) },
                                onRemove  = { viewModel.onEvent(WatchlistEvent.Remove(item.coinId)) },
                            )
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun WatchlistTopBar(
    sortOrder: WatchlistSortOrder,
    isLoading: Boolean,
    isRealtimeConnected: Boolean,
    onRefresh: () -> Unit,
    onSortChange: (WatchlistSortOrder) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = "Watchlist",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
        )

        Spacer(Modifier.weight(1f))

        // Loading indicator
        AnimatedVisibility(visible = isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp).padding(end = 4.dp),
                color       = BinanceYellow,
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

        // Refresh
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
        }

        // Sort
        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = TextSecondary)
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
                                    color = if (sortOrder == option.order) BinanceYellow else TextPrimary,
                                )
                            }
                        },
                        leadingIcon = {
                            if (sortOrder == option.order) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = BinanceYellow, modifier = Modifier.size(16.dp))
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
private fun WatchlistSearchAndFilters(
    query: String,
    filter: WatchlistFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WatchlistFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search symbol or name", color = TextTertiary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BinanceYellow,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = BinanceYellow,
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchlistFilter.entries.forEach { item ->
                val selected = filter == item
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(item) },
                    label = {
                        Text(
                            item.name.lowercase().replaceFirstChar { it.uppercase() },
                            maxLines = 1,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceVariant,
                        selectedContainerColor = BinanceYellow.copy(alpha = 0.18f),
                        labelColor = TextSecondary,
                        selectedLabelColor = BinanceYellow,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = BorderColor,
                        selectedBorderColor = BinanceYellow,
                    ),
                )
            }
        }
    }
    HorizontalDivider(color = DividerColor)
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
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        SummaryChip(
            label = "Watching",
            value = "${items.size}",
            color = TextPrimary,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = DividerColor)
        SummaryChip(
            label = "Gainers",
            value = "$gainers",
            color = GreenUp,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = DividerColor)
        SummaryChip(
            label = "Losers",
            value = "$losers",
            color = RedDown,
        )
        VerticalDivider(modifier = Modifier.height(24.dp), color = DividerColor)
        SummaryChip(
            label = "Avg 24h",
            value = "${"%.2f".format(avgChange)}%",
            color = if (avgChange >= 0) GreenUp else RedDown,
        )
    }
    HorizontalDivider(color = DividerColor)
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
private fun WatchlistCoinRow(
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
        // Coin image
        AsyncImage(
            model              = item.imageUrl,
            contentDescription = item.name,
            modifier           = Modifier.size(40.dp).clip(CircleShape),
        )

        Spacer(Modifier.width(12.dp))

        // Name + symbol
        Column(Modifier.weight(1f)) {
            Text(
                text       = item.symbol,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Text(
                text     = item.name,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Price & change
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
            // Price not loaded yet
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

        // Remove icon
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
