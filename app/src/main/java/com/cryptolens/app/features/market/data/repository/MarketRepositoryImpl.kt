package com.cryptolens.app.features.market.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.data.remote.BinanceApiService
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService
import com.cryptolens.app.features.market.data.remote.MarketCache
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val coinGeckoApi: CoinGeckoApiService,
    private val binanceApi: BinanceApiService,
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
        if (cache.isFresh()) return@flow

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
            cache.update(result)
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
                        circulatingSupply     = 0.0,
                        rank                  = 0,
                    ),
                    description  = dto.description.en,
                    homepageUrl  = dto.links.homepage.firstOrNull() ?: "",
                    githubUrl    = dto.links.reposUrl.github.firstOrNull() ?: "",
                    allTimeHigh     = md.ath["usd"] ?: 0.0,
                    allTimeHighDate = md.athDate["usd"] ?: "",
                    allTimeLow      = md.atl["usd"] ?: 0.0,
                    allTimeLowDate  = md.atlDate["usd"] ?: "",
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
    ): Resource<List<Kline>> {
        return try {
            val raw = binanceApi.getKlines(
                symbol   = symbol,
                interval = interval,
                limit    = limit,
            )
            val klines = raw.mapNotNull { arr ->
                if (arr.size < 7) return@mapNotNull null
                val openTime = arr[0].toString().toLongOrNull() ?: return@mapNotNull null
                val open = arr[1].toString().trim('"').toDoubleOrNull() ?: return@mapNotNull null
                val high = arr[2].toString().trim('"').toDoubleOrNull() ?: return@mapNotNull null
                val low = arr[3].toString().trim('"').toDoubleOrNull() ?: return@mapNotNull null
                val close = arr[4].toString().trim('"').toDoubleOrNull() ?: return@mapNotNull null
                val volume = arr[5].toString().trim('"').toDoubleOrNull() ?: return@mapNotNull null
                val closeTime = arr[6].toString().toLongOrNull() ?: return@mapNotNull null
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
            Resource.Success(klines)
        } catch (e: Exception) {
            Resource.Error("Failed to load chart data: ${e.message}", e)
        }
    }

    override fun observePriceTicker(symbol: String): Flow<Resource<PriceTicker>> =
        binanceWs.observeTicker(symbol)

    override fun observeMultipleTickers(
        symbols: List<String>,
    ): Flow<Resource<Map<String, PriceTicker>>> =
        binanceWs.observeMultipleTickers(symbols)
}
