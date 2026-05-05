package com.cryptolens.app.features.market.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ── DTOs ────────────────────────────────────────────────────────────────────

@Serializable
data class CoinDto(
    @SerialName("id")                           val id: String,
    @SerialName("symbol")                       val symbol: String,
    @SerialName("name")                         val name: String,
    @SerialName("image")                        val image: String,
    @SerialName("current_price")                val currentPrice: Double = 0.0,
    @SerialName("price_change_percentage_24h")  val priceChangePercent24h: Double = 0.0,
    @SerialName("price_change_24h")             val priceChange24h: Double = 0.0,
    @SerialName("market_cap")                   val marketCap: Double = 0.0,
    @SerialName("total_volume")                 val volume24h: Double = 0.0,
    @SerialName("high_24h")                     val high24h: Double = 0.0,
    @SerialName("low_24h")                      val low24h: Double = 0.0,
    @SerialName("circulating_supply")           val circulatingSupply: Double = 0.0,
    @SerialName("market_cap_rank")              val rank: Int = 0,
    @SerialName("last_updated")                 val lastUpdated: String = "",
)

@Serializable
data class CoinDetailDto(
    @SerialName("id")          val id: String,
    @SerialName("symbol")      val symbol: String,
    @SerialName("name")        val name: String,
    @SerialName("description") val description: DescriptionDto = DescriptionDto(),
    @SerialName("image")       val image: ImageDto = ImageDto(),
    @SerialName("market_data") val marketData: MarketDataDto = MarketDataDto(),
    @SerialName("links")       val links: LinksDto = LinksDto(),
)

@Serializable
data class DescriptionDto(@SerialName("en") val en: String = "")

@Serializable
data class ImageDto(@SerialName("large") val large: String = "")

@Serializable
data class MarketDataDto(
    @SerialName("current_price")                val currentPrice: Map<String, Double> = emptyMap(),
    @SerialName("price_change_percentage_24h")  val priceChangePercent24h: Double = 0.0,
    @SerialName("market_cap")                   val marketCap: Map<String, Double> = emptyMap(),
    @SerialName("total_volume")                 val volume: Map<String, Double> = emptyMap(),
    @SerialName("high_24h")                     val high24h: Map<String, Double> = emptyMap(),
    @SerialName("low_24h")                      val low24h: Map<String, Double> = emptyMap(),
    @SerialName("ath")                          val ath: Map<String, Double> = emptyMap(),
    @SerialName("ath_date")                     val athDate: Map<String, String> = emptyMap(),
    @SerialName("atl")                          val atl: Map<String, Double> = emptyMap(),
    @SerialName("atl_date")                     val atlDate: Map<String, String> = emptyMap(),
)

@Serializable
data class LinksDto(
    @SerialName("homepage")             val homepage: List<String> = emptyList(),
    @SerialName("repos_url")            val reposUrl: ReposUrlDto = ReposUrlDto(),
)

@Serializable
data class ReposUrlDto(@SerialName("github") val github: List<String> = emptyList())

// ── Retrofit Service ─────────────────────────────────────────────────────────

interface CoinGeckoApiService {

    @GET("coins/markets")
    suspend fun getTopCoins(
        @Query("vs_currency") currency: String = "usd",
        @Query("order")       order: String = "market_cap_desc",
        @Query("per_page")    perPage: Int = 100,
        @Query("page")        page: Int = 1,
        @Query("sparkline")   sparkline: Boolean = false,
    ): List<CoinDto>

    @GET("coins/{id}")
    suspend fun getCoinDetail(
        @Path("id") id: String,
        @Query("localization")      localization: Boolean = false,
        @Query("tickers")           tickers: Boolean = false,
        @Query("market_data")       marketData: Boolean = true,
        @Query("community_data")    communityData: Boolean = false,
        @Query("developer_data")    developerData: Boolean = false,
    ): CoinDetailDto
}
