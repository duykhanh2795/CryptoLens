package com.cryptolens.app.features.market.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.FuturesMarketInfo
import com.cryptolens.app.features.market.domain.model.FuturesSentiment
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.usecase.BuildFuturesSentimentUseCase
import com.cryptolens.app.features.market.domain.usecase.GetCoinDetailUseCase
import com.cryptolens.app.features.market.domain.usecase.GetCoinMarketChartUseCase
import com.cryptolens.app.features.market.domain.usecase.GetFuturesMarketInfoUseCase
import com.cryptolens.app.features.market.domain.usecase.GetKlinesUseCase
import com.cryptolens.app.features.market.domain.usecase.GetMarkPriceKlinesUseCase
import com.cryptolens.app.features.market.domain.usecase.ObservePriceTickerUseCase
import com.cryptolens.app.features.market.domain.util.CryptoSymbolMapper
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.usecase.GetNewsUseCase
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.features.portfolio.domain.usecase.ObservePortfolioSummaryUseCase
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ObserveIsWatchlistedUseCase
import com.cryptolens.app.features.watchlist.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

enum class ChartDisplayType(val label: String) {
    CANDLE("Candle"),
    LINE("Line")
}

data class CoinDetailUiState(
    val isLoading: Boolean              = true,
    val settings: AppSettings           = AppSettings(),
    val coinDetail: CoinDetail?         = null,
    val klines: List<Kline>             = emptyList(),
    val markPriceKlines: List<Kline>    = emptyList(),
    val futuresMarketInfo: FuturesMarketInfo? = null,
    val futuresSentiment: FuturesSentiment? = null,
    val liveTicker: PriceTicker?        = null,
    val selectedInterval: ChartInterval = ChartInterval.ONE_DAY,
    val selectedMarketType: ChartMarketType = ChartMarketType.SPOT,
    val selectedChartType: ChartDisplayType = ChartDisplayType.CANDLE,
    val isKlinesLoading: Boolean        = false,
    val chartError: String?             = null,
    val isWatchlisted: Boolean          = false,
    val portfolioAsset: PortfolioAsset? = null,
    val portfolioTotalValue: Double     = 0.0,
    val relatedNews: List<NewsItem>     = emptyList(),
    val isNewsLoading: Boolean          = false,
    val newsError: String?              = null,
    val error: String?                  = null,
)

sealed class CoinDetailEvent {
    data class IntervalChanged(val interval: ChartInterval) : CoinDetailEvent()
    data class MarketTypeChanged(val marketType: ChartMarketType) : CoinDetailEvent()
    data class ChartTypeChanged(val chartType: ChartDisplayType) : CoinDetailEvent()
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
    private val getCoinMarketChart: GetCoinMarketChartUseCase,
    private val getMarkPriceKlines: GetMarkPriceKlinesUseCase,
    private val getFuturesMarketInfo: GetFuturesMarketInfoUseCase,
    private val buildFuturesSentiment: BuildFuturesSentimentUseCase,
    private val observePriceTicker: ObservePriceTickerUseCase,
    private val observeIsWatchlisted: ObserveIsWatchlistedUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
    private val getNews: GetNewsUseCase,
    private val observePortfolioSummary: ObservePortfolioSummaryUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val coinId: String = checkNotNull(savedStateHandle["coinId"])

    private val _uiState = MutableStateFlow(CoinDetailUiState())
    val uiState: StateFlow<CoinDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CoinDetailEffect>()
    val effect: SharedFlow<CoinDetailEffect> = _effect.asSharedFlow()

    private var liveTickerJob: Job? = null
    private var chartJob: Job? = null

    init {
        observeAppSettings()
        observeWatchlistState()
        observePortfolioPosition()
        // Load detail first, then klines after detail is ready
        loadDetailThenKlines(ChartInterval.ONE_DAY)
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: CoinDetailEvent) {
        when (event) {
            is CoinDetailEvent.IntervalChanged -> loadKlines(
                symbol   = _uiState.value.coinDetail?.coin?.let {
                    CryptoSymbolMapper.binanceTickerSymbol(it)
                } ?: return,
                interval = event.interval,
                marketType = _uiState.value.selectedMarketType,
            )
            is CoinDetailEvent.MarketTypeChanged -> loadKlines(
                symbol = _uiState.value.coinDetail?.coin?.let {
                    CryptoSymbolMapper.binanceTickerSymbol(it)
                } ?: return,
                interval = _uiState.value.selectedInterval,
                marketType = event.marketType,
            )
            is CoinDetailEvent.ChartTypeChanged ->
                _uiState.update { it.copy(selectedChartType = event.chartType) }
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
                    val symbol = CryptoSymbolMapper.binanceTickerSymbol(result.data.coin)
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            coinDetail       = result.data,
                            selectedInterval = interval,
                        )
                    }
                    // Now safe to load klines — symbol is known
                    loadKlines(symbol, interval, ChartMarketType.SPOT)
                    loadRelatedNews(result.data.coin.symbol)
                    // Start live ticker
                    startLiveTicker(symbol)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }
    }

    private fun loadKlines(
        symbol: String,
        interval: ChartInterval,
        marketType: ChartMarketType,
    ) {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isKlinesLoading = true,
                    selectedInterval = interval,
                    selectedMarketType = marketType,
                    chartError = null,
                    markPriceKlines = if (marketType == ChartMarketType.SPOT) emptyList() else it.markPriceKlines,
                    futuresMarketInfo = if (marketType == ChartMarketType.SPOT) null else it.futuresMarketInfo,
                    futuresSentiment = if (marketType == ChartMarketType.SPOT) null else it.futuresSentiment,
                )
            }

            val candlesDeferred = async {
                getKlines(
                    symbol = symbol,
                    interval = interval.binanceInterval,
                    limit = interval.limit,
                    marketType = marketType,
                )
            }
            val markDeferred = if (marketType == ChartMarketType.FUTURES) {
                async {
                    getMarkPriceKlines(
                        symbol = symbol,
                        interval = interval.binanceInterval,
                        limit = interval.limit,
                    )
                }
            } else {
                null
            }
            val futuresInfoDeferred = if (marketType == ChartMarketType.FUTURES) {
                async { getFuturesMarketInfo(symbol) }
            } else {
                null
            }

            val candles = candlesDeferred.await()
            val markPrices = markDeferred?.await()
            val futuresInfo = futuresInfoDeferred?.await()

            when (candles) {
                is Resource.Success -> {
                    if (candles.data.isEmpty() && marketType == ChartMarketType.SPOT) {
                        loadCoinGeckoFallbackChart(interval)
                        return@launch
                    }
                    val markPriceKlines = when (markPrices) {
                        is Resource.Success -> markPrices.data
                        else -> emptyList()
                    }
                    val futuresMarketInfo = when (futuresInfo) {
                        is Resource.Success -> futuresInfo.data
                        else -> null
                    }
                    val sentiment = futuresMarketInfo?.let { buildFuturesSentiment(it) }
                    val chartError = when {
                        marketType == ChartMarketType.FUTURES && markPrices is Resource.Error ->
                            "Futures candles loaded, but mark price overlay is unavailable."
                        marketType == ChartMarketType.FUTURES && futuresInfo is Resource.Error ->
                            "Futures candles loaded, but futures metrics are unavailable."
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            isKlinesLoading = false,
                            klines = candles.data,
                            markPriceKlines = markPriceKlines,
                            futuresMarketInfo = futuresMarketInfo,
                            futuresSentiment = sentiment,
                            chartError = chartError,
                        )
                    }
                }
                is Resource.Error -> {
                    if (marketType == ChartMarketType.SPOT) {
                        loadCoinGeckoFallbackChart(interval)
                    } else {
                        _uiState.update {
                            it.copy(
                                isKlinesLoading = false,
                                klines = emptyList(),
                                markPriceKlines = emptyList(),
                                futuresMarketInfo = null,
                                futuresSentiment = null,
                                chartError = "Futures market data is not available for this symbol.",
                            )
                        }
                    }
                }
                Resource.Loading -> _uiState.update { it.copy(isKlinesLoading = false) }
            }
        }
    }

    private suspend fun loadCoinGeckoFallbackChart(interval: ChartInterval) {
        when (val fallback = getCoinMarketChart(coinId, interval.coinGeckoDays())) {
            is Resource.Success -> _uiState.update {
                it.copy(
                    isKlinesLoading = false,
                    klines = fallback.data,
                    markPriceKlines = emptyList(),
                    futuresMarketInfo = null,
                    futuresSentiment = null,
                    selectedChartType = ChartDisplayType.LINE,
                    chartError = if (fallback.data.isEmpty()) {
                        "Chart history is unavailable for this coin."
                    } else {
                        "Binance chart is unavailable for this coin. Showing CoinGecko price history."
                    },
                )
            }
            is Resource.Error -> _uiState.update {
                it.copy(
                    isKlinesLoading = false,
                    klines = emptyList(),
                    markPriceKlines = emptyList(),
                    futuresMarketInfo = null,
                    futuresSentiment = null,
                    chartError = "Chart history is unavailable for this coin.",
                )
            }
            Resource.Loading -> _uiState.update { it.copy(isKlinesLoading = false) }
        }
    }

    private fun ChartInterval.coinGeckoDays(): Int = when (this) {
        ChartInterval.ONE_HOUR,
        ChartInterval.FOUR_HOUR,
        ChartInterval.ONE_DAY -> 1
        ChartInterval.ONE_WEEK -> 7
        ChartInterval.ONE_MONTH -> 30
        ChartInterval.THREE_MONTH -> 90
    }

    private fun loadRelatedNews(symbol: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isNewsLoading = true, newsError = null) }
            when (
                val result = getNews(
                    filter = NewsFilter.ALL,
                    currencySymbol = symbol,
                    limit = 5,
                )
            ) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        relatedNews = result.data,
                        isNewsLoading = false,
                        newsError = null,
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        relatedNews = emptyList(),
                        isNewsLoading = false,
                        newsError = result.message,
                    )
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun startLiveTicker(binanceSymbol: String) {
        liveTickerJob?.cancel()
        liveTickerJob = viewModelScope.launch {
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

    private fun observePortfolioPosition() {
        val userId = getCurrentUser()?.id.orEmpty()
        if (userId.isBlank()) return
        viewModelScope.launch {
            observePortfolioSummary(userId).collect { summary ->
                _uiState.update {
                    it.copy(
                        portfolioAsset = summary.assets.firstOrNull { asset -> asset.coinId == coinId },
                        portfolioTotalValue = summary.totalValue,
                    )
                }
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
