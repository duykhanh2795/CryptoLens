package com.cryptolens.app.features.ai.data.remote

import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.ai.domain.model.AiSignal
import com.cryptolens.app.features.ai.domain.model.SignalType
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.Kline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    val temperature: Double = 0.3,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 512,
)

@Singleton
class GeminiAiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    }

    suspend fun analyzeSignal(
        coin: Coin,
        klines: List<Kline>,
    ): Resource<AiSignal> {
        return try {
            val prompt = buildPrompt(coin, klines)
            val requestBody = json.encodeToString(
                GeminiRequest.serializer(),
                GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt))))
                )
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body     = response.body?.string() ?: return Resource.Error("Empty response")

            if (!response.isSuccessful) {
                return Resource.Error("AI service error: ${response.code}")
            }

            val signal = parseGeminiResponse(body, coin)
            Resource.Success(signal)
        } catch (e: Exception) {
            Resource.Error("AI analysis failed: ${e.message}", e)
        }
    }

    private fun buildPrompt(coin: Coin, klines: List<Kline>): String {
        val rsi  = calculateRsi(klines)
        val macd = calculateMacd(klines)
        val last5 = klines.takeLast(5)
        val priceChange = if (klines.size >= 2) {
            ((klines.last().close - klines.first().close) / klines.first().close * 100)
        } else 0.0

        return """
Phân tích kỹ thuật crypto cho ${coin.name} (${coin.symbol}):

Dữ liệu thị trường:
- Giá hiện tại: $${coin.currentPrice}
- Thay đổi 24h: ${"%.2f".format(coin.priceChangePercent24h)}%
- Volume 24h: $${coin.volume24h}
- Market Cap: $${coin.marketCap}
- 52w High: $${coin.high24h} | Low: $${coin.low24h}

Dữ liệu kỹ thuật:
- RSI(14): ${"%.1f".format(rsi)}
- MACD: ${"%.4f".format(macd.first)} | Signal: ${"%.4f".format(macd.second)}
- Thay đổi giá trong kỳ: ${"%.2f".format(priceChange)}%
- 5 nến gần nhất: ${last5.joinToString { "O:${"%.2f".format(it.open)} H:${"%.2f".format(it.high)} L:${"%.2f".format(it.low)} C:${"%.2f".format(it.close)}" }}

Hãy phân tích và trả về JSON hợp lệ (không markdown, không giải thích thêm):
{
  "signal": "BUY|SELL|HOLD",
  "confidence": 0-100,
  "summary": "Tóm tắt ngắn gọn 1 câu bằng tiếng Việt",
  "reasoning": "Phân tích chi tiết bằng tiếng Việt (2-3 câu)",
  "support": số,
  "resistance": số
}
        """.trimIndent()
    }

    private fun parseGeminiResponse(body: String, coin: Coin): AiSignal {
        return try {
            val jsonObj = json.parseToJsonElement(body)
                .let { it as? kotlinx.serialization.json.JsonObject }
            val text = jsonObj
                ?.get("candidates")
                ?.let { it as? kotlinx.serialization.json.JsonArray }
                ?.firstOrNull()
                ?.let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("content")
                ?.let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("parts")
                ?.let { it as? kotlinx.serialization.json.JsonArray }
                ?.firstOrNull()
                ?.let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("text")
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                ?.content ?: ""

            // Extract JSON from response
            val cleanJson = text.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val parsed = json.parseToJsonElement(cleanJson) as kotlinx.serialization.json.JsonObject

            val signalStr = parsed["signal"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content ?: "HOLD"
            val confidence = parsed["confidence"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.toIntOrNull() ?: 50
            val summary = parsed["summary"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content ?: ""
            val reasoning = parsed["reasoning"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content ?: ""
            val support = parsed["support"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.toDoubleOrNull()
                ?: coin.low24h
            val resistance = parsed["resistance"]
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.toDoubleOrNull()
                ?: coin.high24h

            AiSignal(
                signal          = SignalType.valueOf(signalStr),
                confidence      = confidence.coerceIn(0, 100),
                summary         = summary,
                reasoning       = reasoning,
                supportLevel    = support,
                resistanceLevel = resistance,
            )
        } catch (e: Exception) {
            AiSignal(
                signal          = SignalType.HOLD,
                confidence      = 50,
                summary         = "Không thể phân tích tín hiệu",
                reasoning       = "Đã xảy ra lỗi khi phân tích. Vui lòng thử lại.",
                supportLevel    = coin.low24h,
                resistanceLevel = coin.high24h,
            )
        }
    }

    // ── Technical Indicator Calculations ────────────────────────────────────

    private fun calculateRsi(klines: List<Kline>, period: Int = 14): Double {
        if (klines.size < period + 1) return 50.0
        val changes = klines.zipWithNext { a, b -> b.close - a.close }
        val recent  = changes.takeLast(period)
        val gains   = recent.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
        val losses  = recent.filter { it < 0 }.map { -it }.average().takeIf { !it.isNaN() } ?: 0.0
        if (losses == 0.0) return 100.0
        val rs = gains / losses
        return 100 - (100 / (1 + rs))
    }

    private fun calculateMacd(klines: List<Kline>): Pair<Double, Double> {
        if (klines.size < 26) return Pair(0.0, 0.0)
        val closes  = klines.map { it.close }
        val ema12   = ema(closes, 12)
        val ema26   = ema(closes, 26)
        val macdLine = ema12 - ema26
        val signal   = macdLine * 0.9  // simplified signal
        return Pair(macdLine, signal)
    }

    private fun ema(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return prices.average()
        val k      = 2.0 / (period + 1)
        var result = prices.take(period).average()
        prices.drop(period).forEach { price -> result = price * k + result * (1 - k) }
        return result
    }
}
