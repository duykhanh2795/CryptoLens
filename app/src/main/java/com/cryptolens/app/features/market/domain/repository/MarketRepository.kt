package com.cryptolens.app.features.market.domain.repository

import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.core.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface MarketRepository {

    // CoinGecko — market list
    fun getTopCoins(
        currency: String = "usd",
        perPage: Int = 100,
        page: Int = 1,
    ): Flow<Resource<List<Coin>>>

    // CoinGecko — coin detail
    suspend fun getCoinDetail(coinId: String): Resource<CoinDetail>

    // Binance REST — klines for chart
    suspend fun getKlines(
        symbol: String,
        interval: String = "1h",
        limit: Int = 100,
    ): Resource<List<Kline>>

    // Binance WebSocket — realtime price ticker
    fun observePriceTicker(symbol: String): Flow<Resource<PriceTicker>>

    // Binance WebSocket — multiple tickers (market list realtime)
    fun observeMultipleTickers(symbols: List<String>): Flow<Resource<Map<String, PriceTicker>>>
}
