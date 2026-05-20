package com.cryptolens.app.features.news.domain.model

enum class NewsSentiment(val label: String) {
    BULLISH("Bullish"),
    BEARISH("Bearish"),
    IMPORTANT("Important"),
    NEUTRAL("Neutral")
}

enum class NewsFilter(val apiValue: String?, val label: String) {
    ALL(null, "All"),
    BULLISH("bullish", "Bullish"),
    BEARISH("bearish", "Bearish"),
    IMPORTANT("important", "Important"),
    HOT("hot", "Hot")
}

data class NewsItem(
    val id: String,
    val title: String,
    val url: String,
    val sourceTitle: String,
    val sourceDomain: String,
    val publishedAt: Long,
    val currencies: List<String>,
    val sentiment: NewsSentiment,
    val positiveVotes: Int,
    val negativeVotes: Int,
    val importantVotes: Int,
)
