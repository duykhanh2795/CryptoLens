package com.cryptolens.app.features.market.domain.usecase

import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.features.market.domain.repository.MarketRepository
import com.cryptolens.app.core.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTopCoinsUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    operator fun invoke(
        currency: String = "usd",
        perPage: Int = 100,
        page: Int = 1,
    ): Flow<Resource<List<Coin>>> = repository.getTopCoins(currency, perPage, page)
}

class GetCoinDetailUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(coinId: String): Resource<CoinDetail> =
        repository.getCoinDetail(coinId)
}

class GetKlinesUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(
        symbol: String,
        interval: String = "1h",
        limit: Int = 100,
        marketType: ChartMarketType = ChartMarketType.SPOT,
    ): Resource<List<Kline>> = repository.getKlines(symbol, interval, limit, marketType)
}

class GetCoinMarketChartUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(
        coinId: String,
        days: Int,
    ): Resource<List<Kline>> = repository.getCoinMarketChart(coinId, days)
}

class GetMarkPriceKlinesUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(
        symbol: String,
        interval: String = "1h",
        limit: Int = 100,
    ): Resource<List<Kline>> = repository.getMarkPriceKlines(symbol, interval, limit)
}

class ObservePriceTickerUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    operator fun invoke(symbol: String): Flow<Resource<PriceTicker>> =
        repository.observePriceTicker(symbol)
}

class ObserveMultipleTickersUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    operator fun invoke(symbols: List<String>): Flow<Resource<Map<String, PriceTicker>>> =
        repository.observeMultipleTickers(symbols)
}
