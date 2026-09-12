package com.cryptolens.app.features.watchlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.data.remote.MarketCache
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.market.domain.usecase.ObserveMultipleTickersUseCase
import com.cryptolens.app.features.market.domain.util.CryptoSymbolMapper
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.features.watchlist.domain.model.WatchlistGroup
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.model.WatchlistComparePreset
import com.cryptolens.app.features.watchlist.domain.usecase.AddCoinsToWatchlistGroupUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.CreateWatchlistGroupUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.DeleteWatchlistGroupUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.DeleteWatchlistComparePresetUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.MoveWatchlistCoinToTopUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveWatchlistComparePresetsUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveWatchlistGroupsUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.RemoveCoinsFromWatchlistGroupUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.RenameWatchlistGroupUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.SaveWatchlistComparePresetUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.SetWatchlistPinnedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WatchlistSortOrder {
    DEFAULT,
    NAME_ASC,
    CHANGE_DESC,
    CHANGE_ASC,
    PRICE_DESC,
    PRICE_ASC,
}

enum class WatchlistFilter {
    ALL,
    GAINERS,
    LOSERS,
}

data class WatchlistUiState(
    val items: List<WatchlistItem>   = emptyList(),
    val allItems: List<WatchlistItem> = emptyList(),
    val groups: List<WatchlistGroup> = listOf(WatchlistGroup.default),
    val comparePresets: List<WatchlistComparePreset> = emptyList(),
    val selectedGroupId: String      = WatchlistGroup.DEFAULT_ID,
    val isEditingGroups: Boolean     = false,
    val selectedCoinIds: Set<String> = emptySet(),
    val showCreateGroupSheet: Boolean = false,
    val showRenameGroupSheet: Boolean = false,
    val showAddToGroupSheet: Boolean = false,
    val editingGroupId: String?      = null,
    val groupNameInput: String       = "",
    val isLoadingPrices: Boolean     = false,
    val isRealtimeConnected: Boolean = false,
    val settings: AppSettings        = AppSettings(),
    val sortOrder: WatchlistSortOrder = WatchlistSortOrder.DEFAULT,
    val filter: WatchlistFilter      = WatchlistFilter.ALL,
    val searchQuery: String          = "",
    val error: String?               = null,
)

sealed class WatchlistEvent {
    data class Remove(val coinId: String)                  : WatchlistEvent()
    data class SortChanged(val order: WatchlistSortOrder)  : WatchlistEvent()
    data class FilterChanged(val filter: WatchlistFilter)  : WatchlistEvent()
    data class SearchChanged(val query: String)            : WatchlistEvent()
    data class GroupSelected(val groupId: String)          : WatchlistEvent()
    data class GroupNameChanged(val value: String)         : WatchlistEvent()
    data class ToggleCoinSelection(val coinId: String)     : WatchlistEvent()
    data class TogglePin(val coinId: String)               : WatchlistEvent()
    data class MoveToTop(val coinId: String)               : WatchlistEvent()
    data class AddSelectedToGroup(val groupId: String)     : WatchlistEvent()
    data class ShowRenameGroupSheet(val groupId: String)   : WatchlistEvent()
    data class OpenComparePreset(val preset: WatchlistComparePreset) : WatchlistEvent()
    data class DeleteComparePreset(val presetId: String)   : WatchlistEvent()
    data object ShowCreateGroupSheet                       : WatchlistEvent()
    data object HideCreateGroupSheet                       : WatchlistEvent()
    data object HideRenameGroupSheet                       : WatchlistEvent()
    data object CreateGroup                                : WatchlistEvent()
    data object RenameGroup                                : WatchlistEvent()
    data object DeleteCurrentGroup                         : WatchlistEvent()
    data object ShowAddToGroupSheet                        : WatchlistEvent()
    data object HideAddToGroupSheet                        : WatchlistEvent()
    data object EnterGroupEdit                             : WatchlistEvent()
    data object ExitGroupEdit                              : WatchlistEvent()
    data object ToggleSelectAll                            : WatchlistEvent()
    data object RemoveSelectedFromCurrentGroup             : WatchlistEvent()
    data object CompareSelected                            : WatchlistEvent()
    data object Refresh                                    : WatchlistEvent()
}

sealed class WatchlistEffect {
    data class ShowMessage(val message: String) : WatchlistEffect()
    data class ShowError(val message: String)   : WatchlistEffect()
    data class OpenCompare(val preset: WatchlistComparePreset) : WatchlistEffect()
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val observeWatchlist: ObserveWatchlistUseCase,
    private val observeGroups: ObserveWatchlistGroupsUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
    private val createGroup: CreateWatchlistGroupUseCase,
    private val renameGroup: RenameWatchlistGroupUseCase,
    private val deleteGroup: DeleteWatchlistGroupUseCase,
    private val observeComparePresets: ObserveWatchlistComparePresetsUseCase,
    private val saveComparePreset: SaveWatchlistComparePresetUseCase,
    private val deleteComparePreset: DeleteWatchlistComparePresetUseCase,
    private val addCoinsToGroup: AddCoinsToWatchlistGroupUseCase,
    private val removeCoinsFromGroup: RemoveCoinsFromWatchlistGroupUseCase,
    private val setPinned: SetWatchlistPinnedUseCase,
    private val moveToTop: MoveWatchlistCoinToTopUseCase,
    private val getTopCoins: GetTopCoinsUseCase,
    private val observeMultipleTickers: ObserveMultipleTickersUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    // Inject shared cache — read from it instead of calling API again if fresh
    private val marketCache: MarketCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WatchlistEffect>()
    val effect: SharedFlow<WatchlistEffect> = _effect.asSharedFlow()

    // Local ticker cache for realtime prices (Binance WS)
    private val tickerCache = MutableStateFlow<Map<String, PriceTicker>>(emptyMap())

    init {
        observeAppSettings()
        observeWatchlistGroups()
        observeSavedCompares()
        observeWatchlistItems()
        fetchMarketPricesIfNeeded()
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.Remove      -> handleRemove(event.coinId)
            is WatchlistEvent.SortChanged -> _uiState.update { it.copy(sortOrder = event.order) }
            is WatchlistEvent.FilterChanged -> _uiState.update { it.copy(filter = event.filter) }
            is WatchlistEvent.SearchChanged -> _uiState.update { it.copy(searchQuery = event.query) }
            is WatchlistEvent.GroupSelected -> _uiState.update {
                it.copy(selectedGroupId = event.groupId, selectedCoinIds = emptySet())
            }
            is WatchlistEvent.GroupNameChanged -> _uiState.update {
                it.copy(groupNameInput = event.value.take(20))
            }
            is WatchlistEvent.ToggleCoinSelection -> toggleCoinSelection(event.coinId)
            is WatchlistEvent.TogglePin -> togglePin(event.coinId)
            is WatchlistEvent.MoveToTop -> moveCoinToTop(event.coinId)
            is WatchlistEvent.AddSelectedToGroup -> addSelectionToGroup(event.groupId)
            is WatchlistEvent.ShowRenameGroupSheet -> showRenameGroupSheet(event.groupId)
            is WatchlistEvent.OpenComparePreset -> openComparePreset(event.preset)
            is WatchlistEvent.DeleteComparePreset -> deleteSavedCompare(event.presetId)
            WatchlistEvent.ShowCreateGroupSheet ->
                _uiState.update { it.copy(showCreateGroupSheet = true, groupNameInput = "") }
            WatchlistEvent.HideCreateGroupSheet ->
                _uiState.update { it.copy(showCreateGroupSheet = false, groupNameInput = "") }
            WatchlistEvent.HideRenameGroupSheet ->
                _uiState.update {
                    it.copy(
                        showRenameGroupSheet = false,
                        editingGroupId = null,
                        groupNameInput = "",
                    )
                }
            WatchlistEvent.CreateGroup -> createNewGroup()
            WatchlistEvent.RenameGroup -> renameCurrentGroup()
            WatchlistEvent.DeleteCurrentGroup -> deleteCurrentGroup()
            WatchlistEvent.ShowAddToGroupSheet ->
                _uiState.update { it.copy(showAddToGroupSheet = true) }
            WatchlistEvent.HideAddToGroupSheet ->
                _uiState.update { it.copy(showAddToGroupSheet = false) }
            WatchlistEvent.EnterGroupEdit ->
                _uiState.update { it.copy(isEditingGroups = true, selectedCoinIds = emptySet()) }
            WatchlistEvent.ExitGroupEdit ->
                _uiState.update { it.copy(isEditingGroups = false, selectedCoinIds = emptySet()) }
            WatchlistEvent.ToggleSelectAll -> toggleSelectAll()
            WatchlistEvent.RemoveSelectedFromCurrentGroup -> removeSelectionFromCurrentGroup()
            WatchlistEvent.CompareSelected -> compareSelected()
            is WatchlistEvent.Refresh     -> fetchMarketPrices()
        }
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeWatchlistItems() {
        viewModelScope.launch {
            val preferences = combine(
                _uiState.map { it.sortOrder }.distinctUntilChanged(),
                _uiState.map { it.filter }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged(),
                _uiState.map { it.selectedGroupId }.distinctUntilChanged(),
            ) { sort, filter, searchQuery, selectedGroupId ->
                WatchlistPreferences(sort, filter, searchQuery, selectedGroupId)
            }

            combine(
                observeWatchlist(),
                marketCache.coins,
                tickerCache,
                preferences,
            ) { watchlistItems, coinsResource, tickers, prefs ->
                val coinMap: Map<String, Coin> = when (coinsResource) {
                    is Resource.Success -> coinsResource.data.associateBy { it.id }
                    else                -> emptyMap()
                }

                val enriched = watchlistItems.map { item ->
                    val coin = coinMap[item.coinId]
                    if (coin != null) {
                        val liveTicker = tickers[CryptoSymbolMapper.binanceTickerSymbol(coin)]
                        val updatedCoin = if (liveTicker != null && liveTicker.price > 0) {
                            coin.copy(
                                currentPrice = liveTicker.price,
                                priceChangePercent24h = liveTicker.priceChangePercent,
                            )
                        } else coin
                        item.copy(coin = updatedCoin)
                    } else {
                        item
                    }
                }
                val sortedItems = applySortOrder(
                    items = applyFilter(
                        enriched.filter { it.groupIds.contains(prefs.selectedGroupId) },
                        prefs.filter,
                        prefs.searchQuery,
                    ),
                    order = prefs.sortOrder,
                )
                enriched to sortedItems
            }.collect { (allItems, sortedItems) ->
                _uiState.update { it.copy(allItems = allItems, items = sortedItems) }
                // Start realtime tickers for all watchlisted coins
                if (allItems.isNotEmpty()) {
                    startRealtimeTickers(allItems)
                }
            }
        }
    }

    private fun observeWatchlistGroups() {
        viewModelScope.launch {
            observeGroups().collect { groups ->
                val current = _uiState.value.selectedGroupId
                _uiState.update {
                    it.copy(
                        groups = groups,
                        selectedGroupId = if (groups.any { group -> group.id == current }) {
                            current
                        } else {
                            WatchlistGroup.DEFAULT_ID
                        },
                    )
                }
            }
        }
    }

    private fun observeSavedCompares() {
        viewModelScope.launch {
            observeComparePresets().collect { presets ->
                _uiState.update { it.copy(comparePresets = presets) }
            }
        }
    }

    /**
     * Use shared cache if fresh — avoids duplicate CoinGecko calls that cause 429.
     * Only calls API if cache is stale or empty.
     */
    private fun fetchMarketPricesIfNeeded() {
        if (marketCache.isFresh(perPage = 100)) return
        fetchMarketPrices()
    }

    private fun fetchMarketPrices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPrices = true, error = null) }
            getTopCoins(perPage = 100).collect { resource ->
                when (resource) {
                    is Resource.Loading -> Unit  // cache already handles this
                    is Resource.Success -> _uiState.update { it.copy(isLoadingPrices = false) }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoadingPrices = false, error = resource.message)
                    }
                }
            }
        }
    }

    private var tickerJob: kotlinx.coroutines.Job? = null

    private fun startRealtimeTickers(items: List<WatchlistItem>) {
        val symbols = items.mapNotNull { item ->
            item.coin?.let { CryptoSymbolMapper.binanceTickerSymbol(it) }
        }.distinct()
        if (symbols.isEmpty()) return

        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            observeMultipleTickers(symbols).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isRealtimeConnected = false) }
                    is Resource.Success -> {
                        tickerCache.value = resource.data
                        _uiState.update { it.copy(isRealtimeConnected = true) }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isRealtimeConnected = false) }
                }
            }
        }
    }

    private fun handleRemove(coinId: String) {
        viewModelScope.launch {
            when (val r = removeFromWatchlist(coinId)) {
                is Resource.Success ->
                    _effect.emit(WatchlistEffect.ShowMessage("Removed from Watchlist"))
                is Resource.Error   ->
                    _effect.emit(WatchlistEffect.ShowError(r.message))
                else -> Unit
            }
        }
    }

    private fun createNewGroup() {
        viewModelScope.launch {
            val name = _uiState.value.groupNameInput.trim()
            when (val result = createGroup(name)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedGroupId = result.data.id,
                            showCreateGroupSheet = false,
                            groupNameInput = "",
                        )
                    }
                    _effect.emit(WatchlistEffect.ShowMessage("Group created"))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun showRenameGroupSheet(groupId: String) {
        val group = _uiState.value.groups.firstOrNull { it.id == groupId } ?: return
        if (group.id == WatchlistGroup.DEFAULT_ID) return
        _uiState.update {
            it.copy(
                showRenameGroupSheet = true,
                editingGroupId = group.id,
                groupNameInput = group.name,
            )
        }
    }

    private fun renameCurrentGroup() {
        viewModelScope.launch {
            val state = _uiState.value
            val groupId = state.editingGroupId ?: return@launch
            when (val result = renameGroup(groupId, state.groupNameInput)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            showRenameGroupSheet = false,
                            editingGroupId = null,
                            groupNameInput = "",
                        )
                    }
                    _effect.emit(WatchlistEffect.ShowMessage("Group renamed"))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun deleteCurrentGroup() {
        viewModelScope.launch {
            val groupId = _uiState.value.selectedGroupId
            if (groupId == WatchlistGroup.DEFAULT_ID) return@launch
            when (val result = deleteGroup(groupId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedGroupId = WatchlistGroup.DEFAULT_ID,
                            selectedCoinIds = emptySet(),
                        )
                    }
                    _effect.emit(WatchlistEffect.ShowMessage("Group deleted"))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun addSelectionToGroup(groupId: String) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedCoinIds
            when (val result = addCoinsToGroup(selected, groupId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(showAddToGroupSheet = false, selectedCoinIds = emptySet()) }
                    _effect.emit(WatchlistEffect.ShowMessage("Added to group"))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun removeSelectionFromCurrentGroup() {
        viewModelScope.launch {
            val state = _uiState.value
            when (val result = removeCoinsFromGroup(state.selectedCoinIds, state.selectedGroupId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedCoinIds = emptySet()) }
                    _effect.emit(WatchlistEffect.ShowMessage("Removed from group"))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun compareSelected() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedCoinIds.toList()
            if (selected.size < 2) {
                _effect.emit(WatchlistEffect.ShowError("Select at least two coins"))
                return@launch
            }
            when (val result = saveComparePreset(selected)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedCoinIds = emptySet()) }
                    _effect.emit(WatchlistEffect.OpenCompare(result.data))
                }
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun openComparePreset(preset: WatchlistComparePreset) {
        viewModelScope.launch {
            _effect.emit(WatchlistEffect.OpenCompare(preset))
        }
    }

    private fun deleteSavedCompare(presetId: String) {
        viewModelScope.launch {
            when (val result = deleteComparePreset(presetId)) {
                is Resource.Success -> _effect.emit(WatchlistEffect.ShowMessage("Compare removed"))
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun togglePin(coinId: String) {
        viewModelScope.launch {
            val current = _uiState.value.items.firstOrNull { it.coinId == coinId }?.isPinned ?: false
            when (val result = setPinned(coinId, !current)) {
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun moveCoinToTop(coinId: String) {
        viewModelScope.launch {
            when (val result = moveToTop(coinId)) {
                is Resource.Error -> _effect.emit(WatchlistEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun toggleCoinSelection(coinId: String) {
        _uiState.update { state ->
            val selected = state.selectedCoinIds.toMutableSet()
            if (!selected.add(coinId)) selected.remove(coinId)
            state.copy(selectedCoinIds = selected)
        }
    }

    private fun toggleSelectAll() {
        _uiState.update { state ->
            val visibleIds = state.items.map { it.coinId }.toSet()
            val allSelected = visibleIds.isNotEmpty() && state.selectedCoinIds.containsAll(visibleIds)
            state.copy(selectedCoinIds = if (allSelected) emptySet() else visibleIds)
        }
    }

    private fun applySortOrder(
        items: List<WatchlistItem>,
        order: WatchlistSortOrder,
    ): List<WatchlistItem> = when (order) {
        WatchlistSortOrder.DEFAULT     -> items
        WatchlistSortOrder.NAME_ASC    -> items.sortedBy { it.name }
        WatchlistSortOrder.CHANGE_DESC -> items.sortedByDescending { it.priceChangePercent24h }
        WatchlistSortOrder.CHANGE_ASC  -> items.sortedBy { it.priceChangePercent24h }
        WatchlistSortOrder.PRICE_DESC  -> items.sortedByDescending { it.currentPrice }
        WatchlistSortOrder.PRICE_ASC   -> items.sortedBy { it.currentPrice }
    }

    private fun applyFilter(
        items: List<WatchlistItem>,
        filter: WatchlistFilter,
        searchQuery: String,
    ): List<WatchlistItem> {
        val searched = if (searchQuery.isBlank()) {
            items
        } else {
            val query = searchQuery.trim()
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.symbol.contains(query, ignoreCase = true)
            }
        }

        return when (filter) {
            WatchlistFilter.ALL -> searched
            WatchlistFilter.GAINERS -> searched.filter { it.hasMarketData && it.priceChangePercent24h >= 0.0 }
            WatchlistFilter.LOSERS -> searched.filter { it.hasMarketData && it.priceChangePercent24h < 0.0 }
        }
    }

    private data class WatchlistPreferences(
        val sortOrder: WatchlistSortOrder,
        val filter: WatchlistFilter,
        val searchQuery: String,
        val selectedGroupId: String,
    )
}
