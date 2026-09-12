package com.cryptolens.app.features.market.presentation.marketlist

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
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketListUiState(
    val isLoading: Boolean                       = false,
    val coins: List<Coin>                        = emptyList(),
    val realtimePrices: Map<String, PriceTicker> = emptyMap(),
    val watchlistedIds: Set<String>              = emptySet(),
    val settings: AppSettings                    = AppSettings(),
    val error: String?                           = null,
    val searchQuery: String                      = "",
    val selectedTab: MarketTab                   = MarketTab.ALL,
)

enum class MarketTab { ALL, GAINERS, LOSERS, NEW }

sealed class MarketListEvent {
    data class SearchQueryChanged(val query: String) : MarketListEvent()
    data class TabSelected(val tab: MarketTab)       : MarketListEvent()
    data object Refresh                              : MarketListEvent()
}

@HiltViewModel
class MarketListViewModel @Inject constructor(
    private val getTopCoins: GetTopCoinsUseCase,
    private val observeMultipleTickers: ObserveMultipleTickersUseCase,
    private val observeWatchlist: ObserveWatchlistUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val marketCache: MarketCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketListUiState())
    val uiState: StateFlow<MarketListUiState> = _uiState.asStateFlow()
    private var realtimeJob: Job? = null

    val filteredCoins: StateFlow<List<Coin>> = _uiState.map { state ->
        var coins = state.coins.map { coin ->
            val ticker = state.realtimePrices[CryptoSymbolMapper.binanceTickerSymbol(coin)]
            if (ticker != null) coin.copy(
                currentPrice          = ticker.price,
                priceChangePercent24h = ticker.priceChangePercent,
            ) else coin
        }
        if (state.searchQuery.isNotBlank()) {
            coins = coins.filter { coin ->
                coin.name.contains(state.searchQuery, ignoreCase = true) ||
                coin.symbol.contains(state.searchQuery, ignoreCase = true)
            }
        }
        when (state.selectedTab) {
            MarketTab.ALL     -> coins
            MarketTab.GAINERS -> coins
                .filter { it.priceChangePercent24h > 0 }
                .sortedByDescending { it.priceChangePercent24h }
            MarketTab.LOSERS  -> coins
                .filter { it.priceChangePercent24h < 0 }
                .sortedBy { it.priceChangePercent24h }
            MarketTab.NEW     -> coins.sortedByDescending { it.lastUpdated }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeAppSettings()
        loadMarket()
        observeWatchlistedIds()
    }

    fun onEvent(event: MarketListEvent) {
        when (event) {
            is MarketListEvent.SearchQueryChanged ->
                _uiState.update { it.copy(searchQuery = event.query) }
            is MarketListEvent.TabSelected ->
                _uiState.update { it.copy(selectedTab = event.tab) }
            is MarketListEvent.Refresh -> {
                marketCache.invalidate()
                loadMarket()
            }
        }
    }

    private fun observeWatchlistedIds() {
        viewModelScope.launch {
            observeWatchlist().collect { items ->
                _uiState.update { it.copy(watchlistedIds = items.map { i -> i.coinId }.toSet()) }
            }
        }
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun loadMarket() {
        viewModelScope.launch {
            getTopCoins().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { state ->
                        // Only show loading if we have no coins yet
                        if (state.coins.isEmpty()) state.copy(isLoading = true, error = null)
                        else state
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, coins = resource.data) }
                        startRealtimePrices(resource.data)
                    }
                    is Resource.Error -> _uiState.update { state ->
                        if (state.coins.isEmpty())
                            state.copy(isLoading = false, error = resource.message)
                        else
                            state.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun startRealtimePrices(coins: List<Coin>) {
        val symbols = coins.take(50).map { CryptoSymbolMapper.binanceTickerSymbol(it) }
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            observeMultipleTickers(symbols).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { it.copy(realtimePrices = resource.data) }
                }
            }
        }
    }
}
