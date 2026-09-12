package com.cryptolens.app.features.watchlist.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.cryptolens.app.core.domain.util.formatMoney
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.watchlist.domain.model.WatchlistComparePreset
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
    var activeCompare by remember { mutableStateOf<WatchlistComparePreset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WatchlistEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
                is WatchlistEffect.ShowError   -> snackbar.showSnackbar(effect.message)
                is WatchlistEffect.OpenCompare -> activeCompare = effect.preset
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
                .navigationBarsPadding()
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
                onEditGroups = { viewModel.onEvent(WatchlistEvent.EnterGroupEdit) },
                onCreateGroup = { viewModel.onEvent(WatchlistEvent.ShowCreateGroupSheet) },
            )
            BinanceWatchlistTabsAndFilters(
                query = uiState.searchQuery,
                filter = uiState.filter,
                sortOrder = uiState.sortOrder,
                groups = uiState.groups,
                selectedGroupId = uiState.selectedGroupId,
                showSearch = showSearch || uiState.searchQuery.isNotBlank(),
                onQueryChange = { viewModel.onEvent(WatchlistEvent.SearchChanged(it)) },
                onFilterChange = { viewModel.onEvent(WatchlistEvent.FilterChanged(it)) },
                onSortChange = { viewModel.onEvent(WatchlistEvent.SortChanged(it)) },
                onGroupSelected = { viewModel.onEvent(WatchlistEvent.GroupSelected(it)) },
            )

            if (uiState.comparePresets.isNotEmpty()) {
                SavedComparePresetsRow(
                    presets = uiState.comparePresets,
                    onPresetClick = { viewModel.onEvent(WatchlistEvent.OpenComparePreset(it)) },
                )
            }

            when {
                uiState.items.isEmpty() && !uiState.isLoadingPrices ->
                    WatchlistEmptyState(
                        isFiltered = uiState.searchQuery.isNotBlank() ||
                            uiState.filter != WatchlistFilter.ALL,
                    )

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 0.dp, top = 6.dp, end = 0.dp, bottom = 24.dp),
                    ) {
                        items(
                            items = uiState.items,
                            key   = { it.coinId },
                        ) { item ->
                            BinanceWatchlistCoinRow(
                                item      = item,
                                settings  = uiState.settings,
                                onClick   = { onCoinClick(item.coinId) },
                                onRemove  = { viewModel.onEvent(WatchlistEvent.Remove(item.coinId)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditingGroups) {
        WatchlistGroupEditSheet(
            state = uiState,
            settings = uiState.settings,
            onEvent = viewModel::onEvent,
        )
    }

    if (uiState.showCreateGroupSheet) {
        CreateWatchlistGroupSheet(
            title = "Group name",
            actionLabel = "Save",
            name = uiState.groupNameInput,
            onNameChange = { viewModel.onEvent(WatchlistEvent.GroupNameChanged(it)) },
            onDismiss = { viewModel.onEvent(WatchlistEvent.HideCreateGroupSheet) },
            onSave = { viewModel.onEvent(WatchlistEvent.CreateGroup) },
        )
    }

    if (uiState.showRenameGroupSheet) {
        CreateWatchlistGroupSheet(
            title = "Rename group",
            actionLabel = "Update",
            name = uiState.groupNameInput,
            onNameChange = { viewModel.onEvent(WatchlistEvent.GroupNameChanged(it)) },
            onDismiss = { viewModel.onEvent(WatchlistEvent.HideRenameGroupSheet) },
            onSave = { viewModel.onEvent(WatchlistEvent.RenameGroup) },
        )
    }

    if (uiState.showAddToGroupSheet) {
        AddToWatchlistGroupSheet(
            groups = uiState.groups,
            selectedGroupId = uiState.selectedGroupId,
            onDismiss = { viewModel.onEvent(WatchlistEvent.HideAddToGroupSheet) },
            onGroupSelected = { viewModel.onEvent(WatchlistEvent.AddSelectedToGroup(it)) },
        )
    }

    activeCompare?.let { preset ->
        WatchlistCompareScreen(
            preset = preset,
            items = uiState.allItems,
            settings = uiState.settings,
            savedPresets = uiState.comparePresets,
            onPresetSelected = { activeCompare = it },
            onDeletePreset = {
                viewModel.onEvent(WatchlistEvent.DeleteComparePreset(it.id))
                activeCompare = uiState.comparePresets.firstOrNull { presetItem -> presetItem.id != it.id }
            },
            onBack = { activeCompare = null },
        )
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
    onEditGroups: () -> Unit,
    onCreateGroup: () -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

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

        Box {
            IconButton(onClick = { showGroupMenu = true }) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit groups",
                    tint = TextSecondary,
                    modifier = Modifier.size(23.dp),
                )
            }
            DropdownMenu(
                expanded = showGroupMenu,
                onDismissRequest = { showGroupMenu = false },
                modifier = Modifier.background(SurfaceDark),
            ) {
                DropdownMenuItem(
                    text = { Text("Edit watchlist", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showGroupMenu = false
                        onEditGroups()
                    },
                )
                DropdownMenuItem(
                    text = { Text("New group", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showGroupMenu = false
                        onCreateGroup()
                    },
                )
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
    groups: List<com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup>,
    selectedGroupId: String,
    showSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WatchlistFilter) -> Unit,
    onSortChange: (WatchlistSortOrder) -> Unit,
    onGroupSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 8.dp),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(groups, key = { it.id }) { group ->
                WatchlistGroupTab(
                    label = group.name,
                    selected = selectedGroupId == group.id,
                    onClick = { onGroupSelected(group.id) },
                )
            }
        }

        if (showSearch) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
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
private fun WatchlistGroupTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) TextPrimary else TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) BinanceYellow else Color.Transparent),
        )
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

@Composable
private fun SavedComparePresetsRow(
    presets: List<WatchlistComparePreset>,
    onPresetClick: (WatchlistComparePreset) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 18.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(0.6.dp, DividerColor, RoundedCornerShape(10.dp))
                    .clickable { onPresetClick(preset) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CompareArrows,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    preset.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WatchlistCompareScreen(
    preset: WatchlistComparePreset,
    items: List<WatchlistItem>,
    settings: AppSettings,
    savedPresets: List<WatchlistComparePreset>,
    onPresetSelected: (WatchlistComparePreset) -> Unit,
    onDeletePreset: (WatchlistComparePreset) -> Unit,
    onBack: () -> Unit,
) {
    val itemMap = remember(items) { items.associateBy { it.coinId } }
    val compareItems = remember(preset, items) {
        preset.coinIds.mapNotNull { itemMap[it] }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
                Text("Compare", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDeletePreset(preset) }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete compare", tint = TextSecondary, modifier = Modifier.size(19.dp))
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 0.6.dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                if (savedPresets.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedPresets, key = { it.id }) { saved ->
                                val selected = saved.id == preset.id
                                Text(
                                    saved.name,
                                    color = if (selected) BackgroundDark else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selected) BinanceYellow else SurfaceDark)
                                        .border(0.6.dp, DividerColor, RoundedCornerShape(9.dp))
                                        .clickable { onPresetSelected(saved) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
                item {
                    Text(preset.name, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${compareItems.size} assets compared", color = TextTertiary, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(compareItems, key = { it.coinId }) { item ->
                            CompareCoinCard(item = item, settings = settings)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    CompareMetricsTable(items = compareItems, settings = settings)
                }
            }
        }
    }
}

@Composable
private fun CompareCoinCard(item: WatchlistItem, settings: AppSettings) {
    val changeColor = if (item.isPositive) GreenUp else RedDown
    Column(
        modifier = Modifier
            .width(146.dp)
            .height(154.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(0.6.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        AsyncImage(model = item.imageUrl, contentDescription = item.name, modifier = Modifier.size(34.dp).clip(CircleShape))
        Spacer(Modifier.weight(1f))
        Text(item.symbol, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(
            settings.formatMoney(item.currentPrice),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${if (item.priceChangePercent24h >= 0) "+" else ""}${"%.2f".format(item.priceChangePercent24h)}%",
            color = changeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(if (item.isPositive) GreenUpSurface else RedDownSurface)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CompareMetricsTable(items: List<WatchlistItem>, settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(0.6.dp, DividerColor, RoundedCornerShape(12.dp)),
    ) {
        CompareMetricRow("Price", items.map { settings.formatMoney(it.currentPrice) })
        CompareMetricRow(
            "24h",
            items.map { "${if (it.priceChangePercent24h >= 0) "+" else ""}${"%.2f".format(it.priceChangePercent24h)}%" },
            percentValues = items.map { it.priceChangePercent24h },
        )
        CompareMetricRow("Market cap", items.map { settings.formatMoney(it.coin?.marketCap ?: 0.0) })
        CompareMetricRow("Volume", items.map { settings.formatMoney(it.coin?.volume24h ?: 0.0) })
        CompareMetricRow("Rank", items.map { "#${it.coin?.rank ?: 0}" }, showDivider = false)
    }
}

@Composable
private fun CompareMetricRow(
    label: String,
    values: List<String>,
    percentValues: List<Double> = emptyList(),
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) Modifier.border(
                    width = 0.3.dp,
                    color = DividerColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(0.dp),
                ) else Modifier,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(label, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(9.dp))
        Row {
            values.forEachIndexed { index, value ->
                Text(
                    value,
                    color = if (percentValues.isNotEmpty()) {
                        if ((percentValues.getOrNull(index) ?: 0.0) >= 0) GreenUp else RedDown
                    } else {
                        TextPrimary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WatchlistGroupEditSheet(
    state: WatchlistUiState,
    settings: AppSettings,
    onEvent: (WatchlistEvent) -> Unit,
) {
    val allSelected = state.items.isNotEmpty() &&
        state.selectedCoinIds.containsAll(state.items.map { it.coinId })

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onEvent(WatchlistEvent.ExitGroupEdit) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
                Text(
                    "Edit watchlist",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.selectedGroupId != com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup.DEFAULT_ID) {
                    IconButton(
                        onClick = { onEvent(WatchlistEvent.ShowRenameGroupSheet(state.selectedGroupId)) },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Rename group", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onEvent(WatchlistEvent.DeleteCurrentGroup) },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete group", tint = RedDown, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { onEvent(WatchlistEvent.ShowCreateGroupSheet) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "New group", tint = BinanceYellow, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 0.6.dp)

            LazyRow(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.groups, key = { it.id }) { group ->
                    WatchlistGroupTab(
                        label = group.name,
                        selected = state.selectedGroupId == group.id,
                        onClick = { onEvent(WatchlistEvent.GroupSelected(group.id)) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pair", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("Pin", color = TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                Text("Top", color = TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(54.dp))
                Text("Sort", color = TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp)) {
                items(state.items, key = { it.coinId }) { item ->
                    WatchlistEditCoinRow(
                        item = item,
                        settings = settings,
                        selected = state.selectedCoinIds.contains(item.coinId),
                        onSelect = { onEvent(WatchlistEvent.ToggleCoinSelection(item.coinId)) },
                        onPin = { onEvent(WatchlistEvent.TogglePin(item.coinId)) },
                        onTop = { onEvent(WatchlistEvent.MoveToTop(item.coinId)) },
                    )
                }
            }

            Surface(color = BackgroundDark, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEvent(WatchlistEvent.ToggleSelectAll) }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { onEvent(WatchlistEvent.ToggleSelectAll) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BinanceYellow,
                                uncheckedColor = TextTertiary,
                                checkmarkColor = BackgroundDark,
                            ),
                        )
                        Text("Select all", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onEvent(WatchlistEvent.ShowAddToGroupSheet) },
                            enabled = state.selectedCoinIds.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = TextPrimary),
                        ) {
                            Text("Group", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onEvent(WatchlistEvent.CompareSelected) },
                            enabled = state.selectedCoinIds.size >= 2,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = TextPrimary),
                        ) {
                            Text("Compare", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onEvent(WatchlistEvent.RemoveSelectedFromCurrentGroup) },
                            enabled = state.selectedCoinIds.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = TextPrimary),
                        ) {
                            Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistEditCoinRow(
    item: WatchlistItem,
    settings: AppSettings,
    selected: Boolean,
    onSelect: () -> Unit,
    onPin: () -> Unit,
    onTop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = BinanceYellow,
                uncheckedColor = TextTertiary,
                checkmarkColor = BackgroundDark,
            ),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(10.dp))
        AsyncImage(model = item.imageUrl, contentDescription = item.name, modifier = Modifier.size(34.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(item.symbol, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.width(3.dp))
                Text("/USDT", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            if (item.hasMarketData) {
                Text(settings.formatMoney(item.currentPrice), color = TextTertiary, fontSize = 11.sp, maxLines = 1)
            }
        }
        IconButton(onClick = onPin, modifier = Modifier.size(42.dp)) {
            Icon(
                if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin",
                tint = if (item.isPinned) RedDown else TextTertiary,
                modifier = Modifier.size(19.dp),
            )
        }
        IconButton(onClick = onTop, modifier = Modifier.size(46.dp)) {
            Icon(Icons.Outlined.VerticalAlignTop, contentDescription = "Move to top", tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
        Icon(Icons.Outlined.DragHandle, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(22.dp))
    }
    HorizontalDivider(color = DividerColor.copy(alpha = 0.45f), thickness = 0.6.dp, modifier = Modifier.padding(start = 76.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateWatchlistGroupSheet(
    title: String,
    actionLabel: String,
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(SurfaceVariant),
            )
        },
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Enter group name", color = TextTertiary) },
                trailingIcon = { Text("${name.length}/20", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = SurfaceVariant,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = BinanceYellow,
                ),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onSave,
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceYellow, contentColor = BackgroundDark),
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToWatchlistGroupSheet(
    groups: List<com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup>,
    selectedGroupId: String,
    onDismiss: () -> Unit,
    onGroupSelected: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        contentColor = TextPrimary,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text("Add to group", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            groups.filterNot { it.id == selectedGroupId }.forEach { group ->
                ListItem(
                    headlineContent = { Text(group.name, color = TextPrimary, fontSize = 14.sp) },
                    leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    colors = ListItemDefaults.colors(containerColor = BackgroundDark),
                    modifier = Modifier.clickable { onGroupSelected(group.id) },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinanceWatchlistCoinRow(
    item: WatchlistItem,
    settings: AppSettings,
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
                        text       = settings.formatMoney(price),
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
