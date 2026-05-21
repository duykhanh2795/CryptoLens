package com.cryptolens.app.features.market.domain.usecase

import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.CoinDetail
import com.cryptolens.app.features.market.domain.model.FuturesMarketInfo
import com.cryptolens.app.features.market.domain.model.FuturesRiskLevel
import com.cryptolens.app.features.market.domain.model.FuturesSentiment
import com.cryptolens.app.features.market.domain.model.FuturesSentimentBias
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

class GetFuturesMarketInfoUseCase @Inject constructor(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(symbol: String): Resource<FuturesMarketInfo> =
        repository.getFuturesMarketInfo(symbol)
}

class BuildFuturesSentimentUseCase @Inject constructor() {
    operator fun invoke(info: FuturesMarketInfo): FuturesSentiment {
        val fundingPercent = info.lastFundingRate * 100.0
        val premiumPercent = if (info.indexPrice > 0.0) {
            ((info.markPrice - info.indexPrice) / info.indexPrice) * 100.0
        } else {
            0.0
        }
        val oiChange = info.openInterestChangePercent

        val signals = buildList {
            when {
                fundingPercent >= 0.05 -> add("Funding is elevated; long leverage is crowded.")
                fundingPercent <= -0.03 -> add("Funding is negative; short pressure is elevated.")
                else -> add("Funding is near neutral.")
            }
            when {
                premiumPercent >= 0.08 -> add("Mark price trades above index; futures demand is strong.")
                premiumPercent <= -0.08 -> add("Mark price trades below index; futures discount is visible.")
                else -> add("Futures premium is controlled.")
            }
            when {
                oiChange >= 5.0 -> add("Open interest rose strongly over the recent window.")
                oiChange <= -5.0 -> add("Open interest dropped; leverage is being reduced.")
                else -> add("Open interest change is moderate.")
            }
        }

        val bias = when {
            fundingPercent >= 0.05 && premiumPercent >= 0.08 -> FuturesSentimentBias.LONG_CROWDED
            fundingPercent <= -0.03 && premiumPercent <= -0.08 -> FuturesSentimentBias.SHORT_CROWDED
            oiChange >= 5.0 && premiumPercent > 0.02 -> FuturesSentimentBias.BULLISH
            oiChange >= 5.0 && premiumPercent < -0.02 -> FuturesSentimentBias.BEARISH
            else -> FuturesSentimentBias.NEUTRAL
        }

        val riskLevel = when {
            kotlin.math.abs(fundingPercent) >= 0.05 || kotlin.math.abs(premiumPercent) >= 0.08 -> FuturesRiskLevel.HIGH
            kotlin.math.abs(oiChange) >= 5.0 || kotlin.math.abs(fundingPercent) >= 0.02 -> FuturesRiskLevel.MEDIUM
            else -> FuturesRiskLevel.LOW
        }

        val title = when (bias) {
            FuturesSentimentBias.BULLISH -> "Bullish futures momentum"
            FuturesSentimentBias.BEARISH -> "Bearish futures pressure"
            FuturesSentimentBias.LONG_CROWDED -> "Long crowded"
            FuturesSentimentBias.SHORT_CROWDED -> "Short crowded"
            FuturesSentimentBias.NEUTRAL -> "Neutral futures"
            FuturesSentimentBias.UNAVAILABLE -> "Futures signal unavailable"
        }

        val summary = when (bias) {
            FuturesSentimentBias.BULLISH -> "Open interest and premium suggest buyers are adding leverage."
            FuturesSentimentBias.BEARISH -> "Open interest and discount suggest sellers are adding leverage."
            FuturesSentimentBias.LONG_CROWDED -> "Funding and premium are elevated; reversal risk is higher."
            FuturesSentimentBias.SHORT_CROWDED -> "Funding and premium are deeply negative; squeeze risk is higher."
            FuturesSentimentBias.NEUTRAL -> "Funding, premium and open interest are not showing an extreme."
            FuturesSentimentBias.UNAVAILABLE -> "Not enough futures data to build a signal."
        }

        return FuturesSentiment(
            bias = bias,
            riskLevel = riskLevel,
            title = title,
            summary = summary,
            fundingRatePercent = fundingPercent,
            premiumPercent = premiumPercent,
            openInterestChangePercent = oiChange,
            signals = signals,
        )
    }
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
