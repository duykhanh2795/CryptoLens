package com.cryptolens.app.features.market.data.remote

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
}
