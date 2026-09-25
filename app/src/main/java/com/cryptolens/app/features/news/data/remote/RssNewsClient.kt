package com.cryptolens.app.features.news.data.remote

import android.util.Xml
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.model.NewsSentiment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.supervisorScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssNewsClient @Inject constructor(
    private val client: OkHttpClient,
) {
    private val cacheMutex = Mutex()

    @Volatile
    private var cachedArticles: List<NewsItem> = emptyList()

    @Volatile
    private var cacheUpdatedAt: Long = 0L

    suspend fun fetchPosts(
        filter: NewsFilter,
        currencySymbol: String?,
        limit: Int,
    ): Resource<List<NewsItem>> {
        val articlesResult = loadArticles()
        val articles = articlesResult.getOrElse { cause ->
            return Resource.Error(
                message = "News providers are temporarily unavailable. Please try again.",
                throwable = cause,
            )
        }

        val symbol = currencySymbol?.trim()?.uppercase()?.takeIf(String::isNotBlank)
        val filtered = articles
            .asSequence()
            .filter { symbol == null || it.matchesCurrency(symbol) }
            .filter { item ->
                when (filter) {
                    NewsFilter.ALL, NewsFilter.HOT -> true
                    NewsFilter.BULLISH -> item.sentiment == NewsSentiment.BULLISH
                    NewsFilter.BEARISH -> item.sentiment == NewsSentiment.BEARISH
                    NewsFilter.IMPORTANT -> item.sentiment == NewsSentiment.IMPORTANT
                }
            }
            .let { sequence ->
                if (filter == NewsFilter.HOT) {
                    sequence.sortedWith(compareByDescending<NewsItem> { it.hotScore() }.thenByDescending { it.publishedAt })
                } else {
                    sequence.sortedByDescending(NewsItem::publishedAt)
                }
            }
            .take(limit.coerceIn(1, 50))
            .toList()

        return Resource.Success(filtered)
    }

    private suspend fun loadArticles(): Result<List<NewsItem>> = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedArticles.isNotEmpty() && now - cacheUpdatedAt < CACHE_DURATION_MS) {
            return@withLock Result.success(cachedArticles)
        }

        supervisorScope {
            val feedResults = FEEDS.map { feed ->
                async(Dispatchers.IO) {
                    runCatching { fetchFeed(feed) }
                }
            }.awaitAll()
            val articles = feedResults
                .mapNotNull { it.getOrNull() }
                .flatten()
                .distinctBy { it.url.substringBefore('?').trimEnd('/') }

            if (articles.isEmpty()) {
                Result.failure(
                    feedResults.firstNotNullOfOrNull { it.exceptionOrNull() }
                        ?: IllegalStateException("No articles returned"),
                )
            } else {
                cachedArticles = articles
                cacheUpdatedAt = now
                Result.success(articles)
            }
        }
    }

    private fun fetchFeed(feed: Feed): List<NewsItem> {
        val request = Request.Builder()
            .url(feed.url)
            .header("Accept", "application/rss+xml, application/xml, text/xml")
            .header("User-Agent", "CryptoLens/1.0 (Android crypto portfolio app)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("${feed.title} HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) error("${feed.title} returned an empty feed")
            return parseRss(body, feed)
        }
    }

    private fun parseRss(xml: String, feed: Feed): List<NewsItem> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        val items = mutableListOf<NewsItem>()
        var current: MutableRssItem? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(':').lowercase()) {
                    "item", "entry" -> current = MutableRssItem()
                    "title" -> current?.title = parser.safeNextText()
                    "link" -> current?.let {
                        it.url = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                            .ifBlank { parser.safeNextText() }
                    }
                    "guid", "id" -> current?.id = parser.safeNextText()
                    "pubdate", "published", "updated" -> current?.publishedAt = parser.safeNextText()
                    "category" -> current?.categories?.add(parser.safeNextText())
                }

                XmlPullParser.END_TAG -> if (
                    parser.name.substringAfter(':').lowercase() in ITEM_TAGS
                ) {
                    current?.toNewsItem(feed)?.let(items::add)
                    current = null
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun MutableRssItem.toNewsItem(feed: Feed): NewsItem? {
        val cleanTitle = title.trim()
        val cleanUrl = url.trim()
        if (cleanTitle.isBlank() || cleanUrl.isBlank()) return null

        val symbols = extractSymbols(cleanTitle, categories)
        val sentiment = classifySentiment(cleanTitle, categories)
        return NewsItem(
            id = "${feed.domain}:${id.ifBlank { cleanUrl }}",
            title = cleanTitle,
            url = cleanUrl,
            sourceTitle = feed.title,
            sourceDomain = feed.domain,
            publishedAt = publishedAt.toEpochMillis(),
            currencies = symbols,
            sentiment = sentiment,
            positiveVotes = if (sentiment == NewsSentiment.BULLISH) 1 else 0,
            negativeVotes = if (sentiment == NewsSentiment.BEARISH) 1 else 0,
            importantVotes = if (sentiment == NewsSentiment.IMPORTANT) 1 else 0,
        )
    }

    private fun NewsItem.matchesCurrency(symbol: String): Boolean {
        if (symbol in currencies) return true
        val aliases = COIN_ALIASES[symbol].orEmpty() + symbol.lowercase()
        return aliases.any { title.containsWord(it) }
    }

    private fun NewsItem.hotScore(): Int {
        val ageHours = ((System.currentTimeMillis() - publishedAt).coerceAtLeast(0L) / 3_600_000L).toInt()
        val recency = (48 - ageHours).coerceAtLeast(0)
        val impact = when (sentiment) {
            NewsSentiment.IMPORTANT -> 30
            NewsSentiment.BULLISH, NewsSentiment.BEARISH -> 12
            NewsSentiment.NEUTRAL -> 0
        }
        return recency + impact + currencies.size.coerceAtMost(5)
    }

    private fun extractSymbols(title: String, categories: List<String>): List<String> {
        val text = (listOf(title) + categories).joinToString(" ").lowercase()
        return COIN_ALIASES.mapNotNull { (symbol, aliases) ->
            symbol.takeIf {
                text.containsWord(symbol.lowercase()) || aliases.any { alias -> text.containsWord(alias) }
            }
        }.take(6)
    }

    private fun classifySentiment(title: String, categories: List<String>): NewsSentiment {
        val text = (listOf(title) + categories).joinToString(" ").lowercase()
        val bullish = BULLISH_TERMS.count { term -> text.containsWord(term) }
        val bearish = BEARISH_TERMS.count { term -> text.containsWord(term) }
        return when {
            IMPORTANT_TERMS.any { term -> text.containsWord(term) } -> NewsSentiment.IMPORTANT
            bullish > bearish && bullish > 0 -> NewsSentiment.BULLISH
            bearish > bullish && bearish > 0 -> NewsSentiment.BEARISH
            else -> NewsSentiment.NEUTRAL
        }
    }

    private fun String.containsWord(term: String): Boolean =
        Regex("(?i)(^|[^a-z0-9])${Regex.escape(term)}([^a-z0-9]|$)").containsMatchIn(this)

    private fun String.toEpochMillis(): Long {
        val value = trim()
        return runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }

    private fun XmlPullParser.safeNextText(): String = runCatching { nextText().trim() }.getOrDefault("")

    private data class MutableRssItem(
        var id: String = "",
        var title: String = "",
        var url: String = "",
        var publishedAt: String = "",
        val categories: MutableList<String> = mutableListOf(),
    )

    private data class Feed(
        val title: String,
        val domain: String,
        val url: String,
    )

    private companion object {
        const val CACHE_DURATION_MS = 2 * 60 * 1_000L
        val ITEM_TAGS = setOf("item", "entry")

        val FEEDS = listOf(
            Feed("CoinDesk", "coindesk.com", "https://www.coindesk.com/arc/outboundfeeds/rss/"),
            Feed("Cointelegraph", "cointelegraph.com", "https://cointelegraph.com/rss"),
            Feed("Decrypt", "decrypt.co", "https://decrypt.co/feed"),
        )

        val COIN_ALIASES = linkedMapOf(
            "BTC" to listOf("bitcoin"),
            "ETH" to listOf("ethereum", "ether"),
            "SOL" to listOf("solana"),
            "BNB" to listOf("bnb", "binance coin"),
            "XRP" to listOf("xrp", "ripple"),
            "ADA" to listOf("cardano"),
            "DOGE" to listOf("dogecoin"),
            "AVAX" to listOf("avalanche"),
            "LINK" to listOf("chainlink"),
            "SUI" to listOf("sui"),
            "TON" to listOf("toncoin", "the open network"),
            "TRX" to listOf("tron"),
            "DOT" to listOf("polkadot"),
            "MATIC" to listOf("polygon", "pol"),
        )

        val IMPORTANT_TERMS = listOf(
            "sec", "etf", "regulation", "lawsuit", "hack", "exploit", "breach", "bankruptcy",
            "approval", "ban", "investigation", "election", "federal reserve", "interest rate",
        )
        val BULLISH_TERMS = listOf(
            "surge", "rally", "gain", "breakout", "record high", "all-time high", "adoption",
            "upgrade", "launch", "partnership", "inflow", "buy", "bullish", "rebound",
        )
        val BEARISH_TERMS = listOf(
            "drop", "fall", "crash", "sell-off", "outflow", "liquidation", "fraud", "scam",
            "bearish", "decline", "loss", "plunge", "fear", "warning",
        )
    }
}
