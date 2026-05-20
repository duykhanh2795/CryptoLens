package com.cryptolens.app.features.ai.data.remote

import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.ai.domain.model.AiChatMessage
import com.cryptolens.app.features.ai.domain.model.AiSignal
import com.cryptolens.app.features.ai.domain.model.AiMessageRole
import com.cryptolens.app.features.ai.domain.model.SignalType
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.Kline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig(),
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.35,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 900,
    @SerialName("responseMimeType") val responseMimeType: String = "text/plain",
)

@Singleton
class GeminiAiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    suspend fun sendChatMessage(
        message: String,
        marketContext: List<Coin>,
        recentMessages: List<AiChatMessage>,
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext Resource.Error("Add GEMINI_API_KEY to local.properties to enable AI chat.")
            }

            val prompt = buildChatPrompt(message, marketContext, recentMessages)
            val body = executeGeminiRequest(
                GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.45,
                        maxOutputTokens = 1200,
                        responseMimeType = "text/plain",
                    ),
                ),
            )

            when (body) {
                is Resource.Success -> Resource.Success(parseTextResponse(body.data).ifBlank {
                    "Mình chưa có đủ dữ liệu để trả lời câu này."
                })
                is Resource.Error -> body
                Resource.Loading -> Resource.Loading
            }
        } catch (e: Exception) {
            Resource.Error("AI chat failed: ${e.message}", e)
        }
    }

    suspend fun analyzeSignal(
        coin: Coin,
        klines: List<Kline>,
    ): Resource<AiSignal> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext Resource.Error("Add GEMINI_API_KEY to local.properties to enable AI Signal.")
            }

            val prompt = buildSignalPrompt(coin, klines)
            val body = executeGeminiRequest(
                GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.25,
                        maxOutputTokens = 520,
                        responseMimeType = "application/json",
                    ),
                ),
            )

            when (body) {
                is Resource.Success -> Resource.Success(parseSignalResponse(body.data, coin))
                is Resource.Error -> body
                Resource.Loading -> Resource.Loading
            }
        } catch (e: Exception) {
            Resource.Error("AI analysis failed: ${e.message}", e)
        }
    }

    private fun executeGeminiRequest(requestPayload: GeminiRequest): Resource<String> {
        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            requestPayload,
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return Resource.Error("Empty AI response.")
            if (!response.isSuccessful) {
                return Resource.Error("AI service error: ${response.code}")
            }
            return Resource.Success(body)
        }
    }

    private fun buildChatPrompt(
        message: String,
        marketContext: List<Coin>,
        recentMessages: List<AiChatMessage>,
    ): String {
        val marketLines = marketContext.take(12).joinToString("\n") { coin ->
            "- ${coin.symbol}: price=$${coin.currentPrice}, 24h=${"%.2f".format(coin.priceChangePercent24h)}%, volume=$${coin.volume24h}, marketCap=$${coin.marketCap}"
        }
        val history = recentMessages.takeLast(8).joinToString("\n") { chat ->
            val role = if (chat.role == AiMessageRole.USER) "User" else "Assistant"
            "$role: ${chat.text}"
        }

        return """
You are CryptoLens AI, a crypto dashboard assistant.
Answer in Vietnamese unless the user explicitly asks for another language.
Be concise, practical, and transparent about uncertainty.
Do not pretend to place trades or access private wallets.
If the question asks for investment advice, frame the answer as informational only.

Current market context:
$marketLines

Recent conversation:
$history

User question:
$message
        """.trimIndent()
    }

    private fun buildSignalPrompt(coin: Coin, klines: List<Kline>): String {
        val rsi = calculateRsi(klines)
        val macd = calculateMacd(klines)
        val last5 = klines.takeLast(5)
        val priceChange = if (klines.size >= 2 && klines.first().close > 0.0) {
            ((klines.last().close - klines.first().close) / klines.first().close * 100)
        } else {
            0.0
        }

        return """
Analyze the technical crypto signal for ${coin.name} (${coin.symbol}).
Return valid JSON only. Use Vietnamese text for summary and reasoning.

Market data:
- Current price: $${coin.currentPrice}
- 24h change: ${"%.2f".format(coin.priceChangePercent24h)}%
- 24h volume: $${coin.volume24h}
- Market cap: $${coin.marketCap}
- 24h high: $${coin.high24h}
- 24h low: $${coin.low24h}

Technical data:
- RSI(14): ${"%.1f".format(rsi)}
- MACD: ${"%.4f".format(macd.first)}
- MACD signal: ${"%.4f".format(macd.second)}
- Window price change: ${"%.2f".format(priceChange)}%
- Last 5 candles: ${last5.joinToString { "O:${"%.2f".format(it.open)} H:${"%.2f".format(it.high)} L:${"%.2f".format(it.low)} C:${"%.2f".format(it.close)}" }}

Schema:
{
  "signal": "BUY|SELL|HOLD",
  "confidence": 0-100,
  "summary": "one Vietnamese sentence",
  "reasoning": "2-3 Vietnamese sentences",
  "support": number,
  "resistance": number
}
        """.trimIndent()
    }

    private fun parseTextResponse(body: String): String {
        val root = json.parseToJsonElement(body) as? JsonObject
        return root
            ?.get("candidates").asArrayOrNull()
            ?.firstOrNull().asObjectOrNull()
            ?.get("content").asObjectOrNull()
            ?.get("parts").asArrayOrNull()
            ?.firstOrNull().asObjectOrNull()
            ?.get("text").asPrimitiveOrNull()
            ?.contentOrNull
            .orEmpty()
            .trim()
    }

    private fun parseSignalResponse(body: String, coin: Coin): AiSignal {
        return try {
            val text = parseTextResponse(body)
            val cleanJson = text
                .substringAfter("{", missingDelimiterValue = text)
                .substringBeforeLast("}", missingDelimiterValue = text)
                .let { "{$it}" }
            val parsed = json.parseToJsonElement(cleanJson) as JsonObject

            val signalStr = parsed["signal"].asPrimitiveOrNull()?.contentOrNull ?: "HOLD"
            val confidence = parsed["confidence"].asPrimitiveOrNull()?.contentOrNull?.toIntOrNull() ?: 50
            val summary = parsed["summary"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
            val reasoning = parsed["reasoning"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
            val support = parsed["support"].asPrimitiveOrNull()?.contentOrNull?.toDoubleOrNull() ?: coin.low24h
            val resistance = parsed["resistance"].asPrimitiveOrNull()?.contentOrNull?.toDoubleOrNull() ?: coin.high24h

            AiSignal(
                signal = runCatching { SignalType.valueOf(signalStr.uppercase()) }
                    .getOrDefault(SignalType.HOLD),
                confidence = confidence.coerceIn(0, 100),
                summary = summary,
                reasoning = reasoning,
                supportLevel = support,
                resistanceLevel = resistance,
            )
        } catch (e: Exception) {
            AiSignal(
                signal = SignalType.HOLD,
                confidence = 50,
                summary = "Khong the phan tich tin hieu",
                reasoning = "Da xay ra loi khi phan tich phan hoi AI. Vui long thu lai.",
                supportLevel = coin.low24h,
                resistanceLevel = coin.high24h,
            )
        }
    }

    private fun calculateRsi(klines: List<Kline>, period: Int = 14): Double {
        if (klines.size < period + 1) return 50.0
        val changes = klines.zipWithNext { a, b -> b.close - a.close }
        val recent = changes.takeLast(period)
        val gains = recent.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
        val losses = recent.filter { it < 0 }.map { -it }.average().takeIf { !it.isNaN() } ?: 0.0
        if (losses == 0.0) return 100.0
        val rs = gains / losses
        return 100 - (100 / (1 + rs))
    }

    private fun calculateMacd(klines: List<Kline>): Pair<Double, Double> {
        if (klines.size < 26) return Pair(0.0, 0.0)
        val closes = klines.map { it.close }
        val ema12 = ema(closes, 12)
        val ema26 = ema(closes, 26)
        val macdLine = ema12 - ema26
        val signal = macdLine * 0.9
        return Pair(macdLine, signal)
    }

    private fun ema(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return prices.average()
        val multiplier = 2.0 / (period + 1)
        var result = prices.take(period).average()
        prices.drop(period).forEach { price ->
            result = price * multiplier + result * (1 - multiplier)
        }
        return result
    }

    private fun kotlinx.serialization.json.JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray
    private fun kotlinx.serialization.json.JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun kotlinx.serialization.json.JsonElement?.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}
