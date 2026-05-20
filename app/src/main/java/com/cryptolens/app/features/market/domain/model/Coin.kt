package com.cryptolens.app.features.market.domain.model

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
    val priceChangePercent24h: Double,
    val priceChange24h: Double,
    val marketCap: Double,
    val volume24h: Double,
    val high24h: Double,
    val low24h: Double,
    val circulatingSupply: Double,
    val rank: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isPositive: Boolean get() = priceChangePercent24h >= 0
}

data class CoinDetail(
    val coin: Coin,
    val description: String,
    val homepageUrl: String,
    val githubUrl: String,
    val allTimeHigh: Double,
    val allTimeHighDate: String,
    val allTimeLow: Double,
    val allTimeLowDate: String,
    val totalSupply: Double = 0.0,
    val maxSupply: Double = 0.0,
)

data class Kline(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
)

enum class ChartMarketType {
    SPOT,
    FUTURES
}

data class FuturesMarketInfo(
    val symbol: String,
    val markPrice: Double,
    val indexPrice: Double,
    val lastFundingRate: Double,
    val nextFundingTime: Long,
    val openInterest: Double,
    val openInterestChangePercent: Double = 0.0,
    val time: Long,
)

enum class FuturesSentimentBias {
    BULLISH,
    BEARISH,
    LONG_CROWDED,
    SHORT_CROWDED,
    NEUTRAL,
    UNAVAILABLE
}

enum class FuturesRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}

data class FuturesSentiment(
    val bias: FuturesSentimentBias,
    val riskLevel: FuturesRiskLevel,
    val title: String,
    val summary: String,
    val fundingRatePercent: Double,
    val premiumPercent: Double,
    val openInterestChangePercent: Double,
    val signals: List<String>,
)

data class OrderBook(
    val bids: List<Pair<Double, Double>>,  // price, qty
    val asks: List<Pair<Double, Double>>,
)

data class Trade(
    val id: Long,
    val price: Double,
    val quantity: Double,
    val time: Long,
    val isBuyerMaker: Boolean,
)

data class PriceTicker(
    val symbol: String,
    val price: Double,
    val priceChangePercent: Double,
    val volume: Double,
    val high: Double,
    val low: Double,
)
