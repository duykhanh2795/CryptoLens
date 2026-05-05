package com.cryptolens.app.features.exchange.data.remote

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.exchange.domain.model.ApiKeyValidation
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Binance REST client that handles:
 *  - HMAC-SHA256 request signing
 *  - timestamp parameter injection
 *  - error response parsing
 *
 * Uses OkHttp directly (no Retrofit) so we can inject
 * the signature per-call without a custom interceptor.
 */
@Singleton
class BinanceApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val BASE_URL   = "https://api.binance.com"
        private const val RECV_WINDOW = 5000L   // ms Binance allows
        private val QUOTE_ASSETS     = setOf("USDT", "BUSD", "BTC", "ETH", "BNB")
    }

    // ── HMAC ──────────────────────────────────────────────────────────────────

    private fun hmacSha256(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun signedParams(extraParams: String = ""): String {
        val ts   = System.currentTimeMillis()
        val base = "timestamp=$ts&recvWindow=$RECV_WINDOW" +
                   if (extraParams.isNotBlank()) "&$extraParams" else ""
        return base
    }

    private fun buildSignedRequest(
        path: String,
        apiKey: String,
        apiSecret: String,
        extraParams: String = "",
    ): Request {
        val params    = signedParams(extraParams)
        val signature = hmacSha256(apiSecret, params)
        val url       = "$BASE_URL$path?$params&signature=$signature"
        return Request.Builder()
            .url(url)
            .header("X-MBX-APIKEY", apiKey)
            .get()
            .build()
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Validate credentials: calls GET /api/v3/account.
     * Returns [ApiKeyValidation] with account info if valid.
     */
    suspend fun validateCredentials(
        apiKey: String,
        apiSecret: String,
    ): Resource<ApiKeyValidation> {
        return try {
            val request  = buildSignedRequest("/api/v3/account", apiKey, apiSecret)
            val response = okHttpClient.newCall(request).execute()
            val body     = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val err = runCatching { json.decodeFromString<BinanceErrorDto>(body) }.getOrNull()
                val msg = when (err?.code) {
                    -2014 -> "Invalid API Key format"
                    -1022 -> "Invalid signature — check your API Secret"
                    -1100 -> "API Key or Secret contains illegal characters"
                    else  -> err?.msg ?: "Validation failed (${response.code})"
                }
                return Resource.Error(msg)
            }

            val account = json.decodeFromString<BinanceAccountDto>(body)

            if (account.canTrade) {
                // Safety check — we only want READ-ONLY keys
                return Resource.Error(
                    "⚠️ This API Key has Trading permission enabled.\n" +
                    "Please create a Read-Only API Key for safety."
                )
            }

            Resource.Success(
                ApiKeyValidation(
                    isValid      = true,
                    accountType  = account.accountType,
                    canRead      = true,
                    canTrade     = account.canTrade,
                )
            )
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    /**
     * Fetch all trades for [symbol] (e.g. "BTCUSDT").
     * Binance limits 1000 per call; loops with fromId for full history.
     */
    suspend fun getAllTrades(
        apiKey: String,
        apiSecret: String,
        symbol: String,
        limit: Int = 1000,
    ): Resource<List<BinanceTradeDto>> {
        return try {
            val allTrades = mutableListOf<BinanceTradeDto>()
            var fromId: Long? = null

            while (true) {
                val extra  = "symbol=$symbol&limit=$limit" +
                             if (fromId != null) "&fromId=$fromId" else ""
                val request = buildSignedRequest(
                    path        = "/api/v3/myTrades",
                    apiKey      = apiKey,
                    apiSecret   = apiSecret,
                    extraParams = extra,
                )
                val response = okHttpClient.newCall(request).execute()
                val body     = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val err = runCatching { json.decodeFromString<BinanceErrorDto>(body) }.getOrNull()
                    return Resource.Error(err?.msg ?: "Failed to fetch trades (${response.code})")
                }

                val page = json.decodeFromString<List<BinanceTradeDto>>(body)
                if (page.isEmpty()) break
                allTrades.addAll(page)
                if (page.size < limit) break          // no more pages
                fromId = page.last().id + 1
            }

            Resource.Success(allTrades)
        } catch (e: Exception) {
            Resource.Error("Failed to fetch trades: ${e.message}")
        }
    }

    /**
     * Fetch all USDT trading pair symbols from exchange info.
     * Used to discover which symbols to pull trades for.
     */
    suspend fun getUsdtSymbols(): Resource<List<BinanceSymbolDto>> {
        return try {
            val request  = Request.Builder()
                .url("$BASE_URL/api/v3/exchangeInfo")
                .get()
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body     = response.body?.string().orEmpty()

            if (!response.isSuccessful) return Resource.Error("Failed to fetch exchange info")

            val info    = json.decodeFromString<BinanceExchangeInfoDto>(body)
            val symbols = info.symbols.filter { it.status == "TRADING" && it.quoteAsset in QUOTE_ASSETS }
            Resource.Success(symbols)
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
}
