package com.cryptolens.app.features.news.data.remote

import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.model.NewsSentiment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoPanicClient @Inject constructor(
    private val client: OkHttpClient,
) {
    private val apiKey: String get() = BuildConfig.CRYPTOPANIC_API_KEY.trim()
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun fetchPosts(
        filter: NewsFilter,
        currencySymbol: String?,
        limit: Int,
    ): Resource<List<NewsItem>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Resource.Error("Add CRYPTOPANIC_API_KEY to local.properties to enable crypto news.")
        }

        try {
            val urlBuilder = "https://cryptopanic.com/api/developer/v2/posts/".toHttpUrl().newBuilder()
                .addQueryParameter("auth_token", apiKey)
                .addQueryParameter("public", "true")
                .addQueryParameter("kind", "news")
                .addQueryParameter("regions", "en")

            filter.apiValue?.let { urlBuilder.addQueryParameter("filter", it) }
            currencySymbol
                ?.trim()
                ?.uppercase()
                ?.takeIf { it.isNotBlank() }
                ?.let { urlBuilder.addQueryParameter("currencies", it) }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Resource.Error(body.toCryptoPanicError(response.code))
                }

                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: JSONArray()
                val news = buildList {
                    for (index in 0 until results.length()) {
                        add(results.getJSONObject(index).toNewsItem())
                    }
                }
                    .filter { it.title.isNotBlank() && it.url.isNotBlank() }
                    .take(limit.coerceIn(1, 50))

                Resource.Success(news)
            }
        } catch (e: Exception) {
            Resource.Error("Could not load crypto news: ${e.message}", e)
        }
    }

    private fun JSONObject.toNewsItem(): NewsItem {
        val source = optJSONObject("source") ?: JSONObject()
        val votes = optJSONObject("votes") ?: JSONObject()
        val currencies = optJSONArray("currencies").toSymbols()
        val positive = votes.optInt("positive", 0)
        val negative = votes.optInt("negative", 0)
        val important = votes.optInt("important", 0)

        return NewsItem(
            id = optLong("id", 0L).takeIf { it > 0L }?.toString() ?: optString("slug"),
            title = optString("title"),
            url = optString("url"),
            sourceTitle = source.optString("title").ifBlank { optString("domain") },
            sourceDomain = source.optString("domain").ifBlank { optString("domain") },
            publishedAt = optString("published_at").toEpochMillis(),
            currencies = currencies,
            sentiment = resolveSentiment(positive, negative, important),
            positiveVotes = positive,
            negativeVotes = negative,
            importantVotes = important,
        )
    }

    private fun JSONArray?.toSymbols(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val code = item.optString("code").ifBlank { item.optString("symbol") }
                if (code.isNotBlank()) add(code.uppercase())
            }
        }
    }

    private fun resolveSentiment(
        positive: Int,
        negative: Int,
        important: Int,
    ): NewsSentiment =
        when {
            important > positive && important > negative -> NewsSentiment.IMPORTANT
            positive > negative -> NewsSentiment.BULLISH
            negative > positive -> NewsSentiment.BEARISH
            else -> NewsSentiment.NEUTRAL
        }

    private fun String.toEpochMillis(): Long =
        runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

    private fun String.toCryptoPanicError(code: Int): String {
        val text = trim()
        if (code == 401 || code == 403 || text.contains("auth", ignoreCase = true)) {
            return "CryptoPanic token is not accepted for the current API endpoint. Check CRYPTOPANIC_API_KEY and rebuild the app."
        }
        return text.ifBlank { "CryptoPanic HTTP $code" }
    }
}
