package com.cryptolens.app.features.news.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.data.remote.CryptoPanicClient
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.repository.NewsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val cryptoPanicClient: CryptoPanicClient,
) : NewsRepository {
    override suspend fun getNews(
        filter: NewsFilter,
        currencySymbol: String?,
        limit: Int,
    ): Resource<List<NewsItem>> =
        cryptoPanicClient.fetchPosts(
            filter = filter,
            currencySymbol = currencySymbol,
            limit = limit,
        )
}
