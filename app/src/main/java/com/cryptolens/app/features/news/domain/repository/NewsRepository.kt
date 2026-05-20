package com.cryptolens.app.features.news.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem

interface NewsRepository {
    suspend fun getNews(
        filter: NewsFilter = NewsFilter.ALL,
        currencySymbol: String? = null,
        limit: Int = 20,
    ): Resource<List<NewsItem>>
}
