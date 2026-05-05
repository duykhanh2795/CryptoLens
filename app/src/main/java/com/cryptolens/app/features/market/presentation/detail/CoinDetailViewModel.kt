package com.cryptolens.app.features.market.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.usecase.GetCoinDetailUseCase
import com.cryptolens.app.features.market.domain.usecase.GetKlinesUseCase
import com.cryptolens.app.features.market.domain.usecase.ObservePriceTickerUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveIsWatchlistedUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChartInterval(val label: String, val binanceInterval: String, val limit: Int) {
    ONE_HOUR("1H",    "1m",  60),
    FOUR_HOUR("4H",   "5m",  48),
    ONE_DAY("1D",     "15m", 96),
    ONE_WEEK("1W",    "4h",  42),
    ONE_MONTH("1M",   "1d",  30),
    THREE_MONTH("3M", "1d",  90),
}

data class CoinDetailUiState(
    val isLoading: Boolean              = true,
    val coinDetail: CoinDetail?         = null,
    val klines: List<Kline>             = emptyList(),
    val liveTicker: PriceTicker?        = null,
    val selectedInterval: ChartInterval = ChartInterval.ONE_DAY,
    val isKlinesLoading: Boolean        = false,
    val isWatchlisted: Boolean          = false,
    val error: String?                  = null,
)

sealed class CoinDetailEvent {
    data class IntervalChanged(val interval: ChartInterval) : CoinDetailEvent()
    data object ToggleWatchlist                             : CoinDetailEvent()
    data object Retry                                       : CoinDetailEvent()
}

sealed class CoinDetailEffect {
    data class ShowMessage(val message: String) : CoinDetailEffect()
}

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    private val getCoinDetail: GetCoinDetailUseCase,
    private val getKlines: GetKlinesUseCase,
    private val observePriceTicker: ObservePriceTickerUseCase,
    private val observeIsWatchlisted: ObserveIsWatchlistedUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val coinId: String = checkNotNull(savedStateHandle["coinId"])

    private val _uiState = MutableStateFlow(CoinDetailUiState())
    val uiState: StateFlow<CoinDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CoinDetailEffect>()
    val effect: SharedFlow<CoinDetailEffect> = _effect.asSharedFlow()

    init {
        observeWatchlistState()
        // Load detail first, then klines after detail is ready
        loadDetailThenKlines(ChartInterval.ONE_DAY)
    }

    fun onEvent(event: CoinDetailEvent) {
        when (event) {
            is CoinDetailEvent.IntervalChanged -> loadKlines(
                symbol   = _uiState.value.coinDetail?.coin?.symbol ?: return,
                interval = event.interval,
            )
            is CoinDetailEvent.ToggleWatchlist -> handleToggleWatchlist()
            is CoinDetailEvent.Retry           -> loadDetailThenKlines(
                _uiState.value.selectedInterval
            )
        }
    }

    /**
     * Step 1: load coin detail
     * Step 2: only after detail succeeds, load klines with correct symbol
     * This fixes the bug where klines loaded with wrong symbol before detail arrived.
     */
    private fun loadDetailThenKlines(interval: ChartInterval) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getCoinDetail(coinId)) {
                is Resource.Success -> {
                    val symbol = result.data.coin.symbol  // e.g. "BTC"
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            coinDetail       = result.data,
                            selectedInterval = interval,
                        )
                    }
                    // Now safe to load klines — symbol is known
                    loadKlines(symbol, interval)
                    // Start live ticker
                    startLiveTicker("${symbol}USDT")
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }
    }

    private fun loadKlines(symbol: String, interval: ChartInterval) {
        viewModelScope.launch {
            _uiState.update { it.copy(isKlinesLoading = true, selectedInterval = interval) }
            when (val result = getKlines("${symbol}USDT", interval.binanceInterval, interval.limit)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isKlinesLoading = false, klines = result.data)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isKlinesLoading = false)
                }
                else -> _uiState.update { it.copy(isKlinesLoading = false) }
            }
        }
    }

    private fun startLiveTicker(binanceSymbol: String) {
        viewModelScope.launch {
            observePriceTicker(binanceSymbol).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { it.copy(liveTicker = resource.data) }
                }
            }
        }
    }

    private fun observeWatchlistState() {
        viewModelScope.launch {
            observeIsWatchlisted(coinId).collect { isWatchlisted ->
                _uiState.update { it.copy(isWatchlisted = isWatchlisted) }
            }
        }
    }

    private fun handleToggleWatchlist() {
        val detail = _uiState.value.coinDetail ?: return
        viewModelScope.launch {
            val result = toggleWatchlist(
                coinId   = detail.coin.id,
                symbol   = detail.coin.symbol,
                name     = detail.coin.name,
                imageUrl = detail.coin.imageUrl,
            )
            if (result is Resource.Success) {
                val msg = if (result.data) "${detail.coin.symbol} added to Watchlist ★"
                          else "${detail.coin.symbol} removed from Watchlist"
                _effect.emit(CoinDetailEffect.ShowMessage(msg))
            }
        }
    }
}
