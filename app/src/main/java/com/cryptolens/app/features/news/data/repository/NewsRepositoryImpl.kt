package com.cryptolens.app.features.news.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.data.remote.RssNewsClient
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.repository.NewsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val rssNewsClient: RssNewsClient,
) : NewsRepository {
    override suspend fun getNews(
        filter: NewsFilter,
        currencySymbol: String?,
        limit: Int,
    ): Resource<List<NewsItem>> {
        return rssNewsClient.fetchPosts(
            filter = filter,
            currencySymbol = currencySymbol,
            limit = limit,
        )
    }
}
