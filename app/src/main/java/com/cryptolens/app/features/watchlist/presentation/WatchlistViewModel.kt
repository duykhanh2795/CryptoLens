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
import com.cryptolens.app.features.watchlist.domain.model.WatchlistItem
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.RemoveFromWatchlistUseCase
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
}

data class WatchlistUiState(
    val items: List<WatchlistItem>   = emptyList(),
    val isLoadingPrices: Boolean     = false,
    val sortOrder: WatchlistSortOrder = WatchlistSortOrder.DEFAULT,
    val error: String?               = null,
)

sealed class WatchlistEvent {
    data class Remove(val coinId: String)                  : WatchlistEvent()
    data class SortChanged(val order: WatchlistSortOrder)  : WatchlistEvent()
    data object Refresh                                    : WatchlistEvent()
}

sealed class WatchlistEffect {
    data class ShowMessage(val message: String) : WatchlistEffect()
    data class ShowError(val message: String)   : WatchlistEffect()
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val observeWatchlist: ObserveWatchlistUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
    private val getTopCoins: GetTopCoinsUseCase,
    private val observeMultipleTickers: ObserveMultipleTickersUseCase,
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
        observeWatchlistItems()
        fetchMarketPricesIfNeeded()
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.Remove      -> handleRemove(event.coinId)
            is WatchlistEvent.SortChanged -> _uiState.update { it.copy(sortOrder = event.order) }
            is WatchlistEvent.Refresh     -> fetchMarketPrices()
        }
    }

    private fun observeWatchlistItems() {
        viewModelScope.launch {
            combine(
                observeWatchlist(),
                marketCache.coins,
                tickerCache,
                _uiState.map { it.sortOrder }.distinctUntilChanged(),
            ) { watchlistItems, coinsResource, tickers, sort ->
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
                applySortOrder(enriched, sort)
            }.collect { sortedItems ->
                _uiState.update { it.copy(items = sortedItems) }
                // Start realtime tickers for all watchlisted coins
                if (sortedItems.isNotEmpty()) {
                    startRealtimeTickers(sortedItems)
                }
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
                if (resource is Resource.Success) {
                    tickerCache.value = resource.data
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

    private fun applySortOrder(
        items: List<WatchlistItem>,
        order: WatchlistSortOrder,
    ): List<WatchlistItem> = when (order) {
        WatchlistSortOrder.DEFAULT     -> items
        WatchlistSortOrder.NAME_ASC    -> items.sortedBy { it.name }
        WatchlistSortOrder.CHANGE_DESC -> items.sortedByDescending { it.priceChangePercent24h }
        WatchlistSortOrder.CHANGE_ASC  -> items.sortedBy { it.priceChangePercent24h }
        WatchlistSortOrder.PRICE_DESC  -> items.sortedByDescending { it.currentPrice }
    }
}
