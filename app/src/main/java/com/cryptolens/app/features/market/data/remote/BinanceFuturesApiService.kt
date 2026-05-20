package com.cryptolens.app.features.market.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceFuturesApiService {
    @GET("klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100,
    ): List<List<JsonElement>>

    @GET("markPriceKlines")
    suspend fun getMarkPriceKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100,
    ): List<List<JsonElement>>

    @GET("premiumIndex")
    suspend fun getPremiumIndex(
        @Query("symbol") symbol: String,
    ): PremiumIndexDto

    @GET("openInterest")
    suspend fun getOpenInterest(
        @Query("symbol") symbol: String,
    ): OpenInterestDto
}

@Serializable
data class PremiumIndexDto(
    @SerialName("symbol") val symbol: String = "",
    @SerialName("markPrice") val markPrice: String = "0",
    @SerialName("indexPrice") val indexPrice: String = "0",
    @SerialName("lastFundingRate") val lastFundingRate: String = "0",
    @SerialName("nextFundingTime") val nextFundingTime: Long = 0L,
    @SerialName("time") val time: Long = 0L,
)

@Serializable
data class OpenInterestDto(
    @SerialName("openInterest") val openInterest: String = "0",
    @SerialName("symbol") val symbol: String = "",
    @SerialName("time") val time: Long = 0L,
)

interface BinanceFuturesDataApiService {
    @GET("openInterestHist")
    suspend fun getOpenInterestHistory(
        @Query("symbol") symbol: String,
        @Query("period") period: String = "1h",
        @Query("limit") limit: Int = 24,
    ): List<OpenInterestHistoryDto>
}

@Serializable
data class OpenInterestHistoryDto(
    @SerialName("symbol") val symbol: String = "",
    @SerialName("sumOpenInterest") val sumOpenInterest: String = "0",
    @SerialName("sumOpenInterestValue") val sumOpenInterestValue: String = "0",
    @SerialName("timestamp") val timestamp: Long = 0L,
)
