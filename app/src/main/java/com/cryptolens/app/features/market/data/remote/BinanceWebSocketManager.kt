package com.cryptolens.app.features.market.data.remote

import com.cryptolens.app.features.market.domain.model.Kline
import com.cryptolens.app.features.market.domain.model.PriceTicker
import com.cryptolens.app.core.domain.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

// ── Binance REST DTOs ────────────────────────────────────────────────────────

// Klines come as JSON arrays: [openTime, open, high, low, close, volume, ...]
typealias KlineArray = List<kotlinx.serialization.json.JsonElement>

// ── Binance WebSocket DTOs ───────────────────────────────────────────────────

@Serializable
data class BinanceTickerWsDto(
    @SerialName("s")  val symbol: String = "",
    @SerialName("c")  val lastPrice: String = "0",
    @SerialName("P")  val priceChangePercent: String = "0",
    @SerialName("v")  val volume: String = "0",
    @SerialName("h")  val high: String = "0",
    @SerialName("l")  val low: String = "0",
)

@Serializable
data class BinanceMiniTickerDto(
    @SerialName("e")  val eventType: String = "",
    @SerialName("s")  val symbol: String = "",
    @SerialName("c")  val close: String = "0",
    @SerialName("o")  val open: String = "0",
    @SerialName("h")  val high: String = "0",
    @SerialName("l")  val low: String = "0",
    @SerialName("v")  val volume: String = "0",
)

// ── Binance REST Service ─────────────────────────────────────────────────────

interface BinanceApiService {
    @GET("klines")
    suspend fun getKlines(
        @Query("symbol")   symbol: String,
        @Query("interval") interval: String = "1h",
        @Query("limit")    limit: Int = 100,
    ): List<List<kotlinx.serialization.json.JsonElement>>
}

// ── Binance WebSocket Manager ────────────────────────────────────────────────

class BinanceWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val WS_BASE = "wss://stream.binance.com:9443/ws"
    }

    /**
     * Observe a single ticker symbol in realtime.
     * Stream: <symbol>@ticker
     */
    fun observeTicker(symbol: String): Flow<Resource<PriceTicker>> = callbackFlow {
        val streamName = "${symbol.lowercase()}@ticker"
        val request = Request.Builder()
            .url("$WS_BASE/$streamName")
            .build()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySend(Resource.Loading)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val dto = json.decodeFromString<BinanceTickerWsDto>(text)
                    val ticker = PriceTicker(
                        symbol              = dto.symbol,
                        price               = dto.lastPrice.toDoubleOrNull() ?: 0.0,
                        priceChangePercent  = dto.priceChangePercent.toDoubleOrNull() ?: 0.0,
                        volume              = dto.volume.toDoubleOrNull() ?: 0.0,
                        high                = dto.high.toDoubleOrNull() ?: 0.0,
                        low                 = dto.low.toDoubleOrNull() ?: 0.0,
                    )
                    trySend(Resource.Success(ticker))
                } catch (e: Exception) {
                    trySend(Resource.Error("Parse error: ${e.message}", e))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(Resource.Error("WebSocket error: ${t.message}", t))
            }
        })

        awaitClose { ws.close(1000, "Flow cancelled") }
    }

    /**
     * Observe multiple tickers at once via combined stream.
     */
    fun observeMultipleTickers(symbols: List<String>): Flow<Resource<Map<String, PriceTicker>>> {
        val streams = symbols.joinToString("/") { "${it.lowercase()}@miniTicker" }
        val url = "wss://stream.binance.com:9443/stream?streams=$streams"

        return callbackFlow {
            val priceMap = mutableMapOf<String, PriceTicker>()
            val request = Request.Builder().url(url).build()

            val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    trySend(Resource.Loading)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        // Combined stream wraps data in {"stream":"...","data":{...}}
                        val wrapper = json.decodeFromString<CombinedStreamWrapper>(text)
                        val dto = wrapper.data
                        priceMap[dto.symbol] = PriceTicker(
                            symbol             = dto.symbol,
                            price              = dto.close.toDoubleOrNull() ?: 0.0,
                            priceChangePercent = 0.0,
                            volume             = dto.volume.toDoubleOrNull() ?: 0.0,
                            high               = dto.high.toDoubleOrNull() ?: 0.0,
                            low                = dto.low.toDoubleOrNull() ?: 0.0,
                        )
                        trySend(Resource.Success(priceMap.toMap()))
                    } catch (e: Exception) {
                        // silently skip bad frames
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    trySend(Resource.Error("WebSocket error: ${t.message}", t))
                }
            })

            awaitClose { ws.close(1000, "Flow cancelled") }
        }
    }
}

@Serializable
data class CombinedStreamWrapper(
    @SerialName("stream") val stream: String = "",
    @SerialName("data")   val data: BinanceMiniTickerDto = BinanceMiniTickerDto(),
)
