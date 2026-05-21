package com.cryptolens.app.features.market.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.data.remote.BinanceApiService
import com.cryptolens.app.features.market.data.remote.BinanceFuturesDataApiService
import com.cryptolens.app.features.market.data.remote.BinanceFuturesApiService
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService
import com.cryptolens.app.features.market.data.remote.MarketCache
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.FuturesMarketInfo
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val coinGeckoApi: CoinGeckoApiService,
    private val binanceApi: BinanceApiService,
    private val binanceFuturesApi: BinanceFuturesApiService,
    private val binanceFuturesDataApi: BinanceFuturesDataApiService,
    private val binanceWs: BinanceWebSocketManager,
    private val cache: MarketCache,
) : MarketRepository {

    /**
     * Returns cached data immediately if fresh, otherwise fetches from network.
     * All callers (MarketList, Home, Watchlist) share the same cache — no duplicate calls.
     */
    override fun getTopCoins(
        currency: String,
        perPage: Int,
        page: Int,
    ): Flow<Resource<List<Coin>>> = flow {
        // 1. Emit cached value immediately (even if stale) so UI is not blank
        val cached = cache.coins.value
        if (cached is Resource.Success) emit(cached)
        else emit(Resource.Loading)

        // 2. If cache is fresh, stop here — no network call needed
        if (cache.isFresh(currency, perPage, page)) return@flow

        // 3. Fetch from network
        try {
            val coins = coinGeckoApi.getTopCoins(
                currency = currency,
                perPage  = perPage,
                page     = page,
            ).map { dto ->
                Coin(
                    id                    = dto.id,
                    symbol                = dto.symbol.uppercase(),
                    name                  = dto.name,
                    imageUrl              = dto.image,
                    currentPrice          = dto.currentPrice,
                    priceChangePercent24h = dto.priceChangePercent24h,
                    priceChange24h        = dto.priceChange24h,
                    marketCap             = dto.marketCap,
                    volume24h             = dto.volume24h,
                    high24h               = dto.high24h,
                    low24h                = dto.low24h,
                    circulatingSupply     = dto.circulatingSupply,
                    rank                  = dto.rank,
                )
            }
            val result = Resource.Success(coins)
            cache.update(result, currency, page)
            emit(result)
        } catch (e: Exception) {
            // Only emit error if we have no cached data to show
            if (cache.coins.value !is Resource.Success) {
                emit(Resource.Error("Failed to load market: ${e.message}", e))
            }
        }
    }

    override suspend fun getCoinDetail(coinId: String): Resource<CoinDetail> {
        return try {
            val dto = coinGeckoApi.getCoinDetail(coinId)
            val md  = dto.marketData
            Resource.Success(
                CoinDetail(
                    coin = Coin(
                        id                    = dto.id,
                        symbol                = dto.symbol.uppercase(),
                        name                  = dto.name,
                        imageUrl              = dto.image.large,
                        currentPrice          = md.currentPrice["usd"] ?: 0.0,
                        priceChangePercent24h = md.priceChangePercent24h,
                        priceChange24h        = 0.0,
                        marketCap             = md.marketCap["usd"] ?: 0.0,
                        volume24h             = md.volume["usd"] ?: 0.0,
                        high24h               = md.high24h["usd"] ?: 0.0,
                        low24h                = md.low24h["usd"] ?: 0.0,
                        circulatingSupply     = md.circulatingSupply ?: 0.0,
                        rank                  = dto.marketCapRank ?: 0,
                    ),
                    description  = dto.description.en,
                    homepageUrl  = dto.links.homepage.firstOrNull() ?: "",
                    githubUrl    = dto.links.reposUrl.github.firstOrNull() ?: "",
                    allTimeHigh     = md.ath["usd"] ?: 0.0,
                    allTimeHighDate = md.athDate["usd"] ?: "",
                    allTimeLow      = md.atl["usd"] ?: 0.0,
                    allTimeLowDate  = md.atlDate["usd"] ?: "",
                    totalSupply     = md.totalSupply ?: 0.0,
                    maxSupply       = md.maxSupply ?: 0.0,
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to load coin detail: ${e.message}", e)
        }
    }

    override suspend fun getKlines(
        symbol: String,
        interval: String,
        limit: Int,
        marketType: ChartMarketType,
    ): Resource<List<Kline>> {
        return try {
            val raw = when (marketType) {
                ChartMarketType.SPOT -> binanceApi.getKlines(
                    symbol = symbol,
                    interval = interval,
                    limit = limit,
                )
                ChartMarketType.FUTURES -> binanceFuturesApi.getKlines(
                    symbol = symbol,
                    interval = interval,
                    limit = limit,
                )
            }
            Resource.Success(raw.toKlines())
        } catch (e: Exception) {
            Resource.Error("Failed to load chart data: ${e.message}", e)
        }
    }

    override suspend fun getCoinMarketChart(
        coinId: String,
        days: Int,
    ): Resource<List<Kline>> {
        return try {
            val chart = coinGeckoApi.getMarketChart(
                id = coinId,
                days = days.coerceAtLeast(1),
            )
            val volumeByTime = chart.totalVolumes.mapNotNull { point ->
                val time = point.getOrNull(0)?.toLong() ?: return@mapNotNull null
                val volume = point.getOrNull(1) ?: 0.0
                time to volume
            }.toMap()
            val klines = chart.prices.mapNotNull { point ->
                val openTime = point.getOrNull(0)?.toLong() ?: return@mapNotNull null
                val close = point.getOrNull(1) ?: return@mapNotNull null
                Kline(
                    openTime = openTime,
                    open = close,
                    high = close,
                    low = close,
                    close = close,
                    volume = volumeByTime[openTime] ?: 0.0,
                    closeTime = openTime,
                )
            }
            Resource.Success(klines)
        } catch (e: Exception) {
            Resource.Error("CoinGecko chart is unavailable: ${e.message}", e)
        }
    }

    override suspend fun getMarkPriceKlines(
        symbol: String,
        interval: String,
        limit: Int,
    ): Resource<List<Kline>> {
        return try {
            val raw = binanceFuturesApi.getMarkPriceKlines(
                symbol = symbol,
                interval = interval,
                limit = limit,
            )
            Resource.Success(raw.toKlines())
        } catch (e: Exception) {
            Resource.Error("Failed to load mark price data: ${e.message}", e)
        }
    }

    override suspend fun getFuturesMarketInfo(symbol: String): Resource<FuturesMarketInfo> {
        return try {
            val premium = binanceFuturesApi.getPremiumIndex(symbol)
            val openInterest = binanceFuturesApi.getOpenInterest(symbol)
            val openInterestChangePercent = fetchOpenInterestChangePercent(symbol)
            Resource.Success(
                FuturesMarketInfo(
                    symbol = premium.symbol.ifBlank { symbol },
                    markPrice = premium.markPrice.toDoubleOrNull() ?: 0.0,
                    indexPrice = premium.indexPrice.toDoubleOrNull() ?: 0.0,
                    lastFundingRate = premium.lastFundingRate.toDoubleOrNull() ?: 0.0,
                    nextFundingTime = premium.nextFundingTime,
                    openInterest = openInterest.openInterest.toDoubleOrNull() ?: 0.0,
                    openInterestChangePercent = openInterestChangePercent,
                    time = maxOf(premium.time, openInterest.time),
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to load futures data: ${e.message}", e)
        }
    }

    override fun observePriceTicker(symbol: String): Flow<Resource<PriceTicker>> =
        binanceWs.observeTicker(symbol)

    override fun observeMultipleTickers(
        symbols: List<String>,
    ): Flow<Resource<Map<String, PriceTicker>>> =
        binanceWs.observeMultipleTickers(symbols)

    private fun List<List<JsonElement>>.toKlines(): List<Kline> =
        mapNotNull { arr ->
            if (arr.size < 7) return@mapNotNull null
            val openTime = arr[0].asLongOrNull() ?: return@mapNotNull null
            val open = arr[1].asDoubleOrNull() ?: return@mapNotNull null
            val high = arr[2].asDoubleOrNull() ?: return@mapNotNull null
            val low = arr[3].asDoubleOrNull() ?: return@mapNotNull null
            val close = arr[4].asDoubleOrNull() ?: return@mapNotNull null
            val volume = arr[5].asDoubleOrNull() ?: 0.0
            val closeTime = arr[6].asLongOrNull() ?: return@mapNotNull null
            Kline(
                openTime = openTime,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume,
                closeTime = closeTime,
            )
        }

    private fun JsonElement.asDoubleOrNull(): Double? =
        toString().trim('"').toDoubleOrNull()

    private fun JsonElement.asLongOrNull(): Long? =
        toString().trim('"').toLongOrNull()

    private suspend fun fetchOpenInterestChangePercent(symbol: String): Double {
        val history = runCatching {
            binanceFuturesDataApi.getOpenInterestHistory(
                symbol = symbol,
                period = "1h",
                limit = 24,
            )
        }.getOrDefault(emptyList())

        val first = history.firstOrNull()?.sumOpenInterestValue?.toDoubleOrNull()
            ?: history.firstOrNull()?.sumOpenInterest?.toDoubleOrNull()
            ?: return 0.0
        val last = history.lastOrNull()?.sumOpenInterestValue?.toDoubleOrNull()
            ?: history.lastOrNull()?.sumOpenInterest?.toDoubleOrNull()
            ?: return 0.0

        if (first <= 0.0) return 0.0
        return ((last - first) / first) * 100.0
    }
}
