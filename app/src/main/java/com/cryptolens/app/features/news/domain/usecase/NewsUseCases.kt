package com.cryptolens.app.features.news.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.repository.NewsRepository
import javax.inject.Inject

class GetNewsUseCase @Inject constructor(
    private val repository: NewsRepository,
) {
    suspend operator fun invoke(
        filter: NewsFilter = NewsFilter.ALL,
        currencySymbol: String? = null,
        limit: Int = 20,
    ): Resource<List<NewsItem>> =
        repository.getNews(filter = filter, currencySymbol = currencySymbol, limit = limit)
}
