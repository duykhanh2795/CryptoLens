package com.cryptolens.app.features.news.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.news.domain.model.NewsFilter
import com.cryptolens.app.features.news.domain.model.NewsItem
import com.cryptolens.app.features.news.domain.usecase.GetNewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsUiState(
    val news: List<NewsItem> = emptyList(),
    val filter: NewsFilter = NewsFilter.ALL,
    val query: String = "",
    val currencySymbol: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val filteredNews: List<NewsItem>
        get() = if (query.isBlank()) news else news.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.sourceTitle.contains(query, ignoreCase = true) ||
                item.currencies.any { it.contains(query, ignoreCase = true) }
        }
}

sealed class NewsEvent {
    data object Refresh : NewsEvent()
    data class FilterChanged(val filter: NewsFilter) : NewsEvent()
    data class QueryChanged(val value: String) : NewsEvent()
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val getNews: GetNewsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val initialCurrency: String? = savedStateHandle.get<String>("symbol")
        ?.takeIf { it.isNotBlank() && it != "all" }

    private val _uiState = MutableStateFlow(NewsUiState(currencySymbol = initialCurrency))
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: NewsEvent) {
        when (event) {
            NewsEvent.Refresh -> load()
            is NewsEvent.FilterChanged -> {
                _uiState.update { it.copy(filter = event.filter) }
                load()
            }
            is NewsEvent.QueryChanged -> _uiState.update { it.copy(query = event.value) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (
                val result = getNews(
                    filter = state.filter,
                    currencySymbol = state.currencySymbol,
                    limit = 40,
                )
            ) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, news = result.data, error = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }
}

data class NewsPreviewUiState(
    val news: List<NewsItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NewsPreviewViewModel @Inject constructor(
    private val getNews: GetNewsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val currencySymbol: String? = savedStateHandle.get<String>("symbol")
        ?.takeIf { it.isNotBlank() && it != "all" }

    private val _uiState = MutableStateFlow(NewsPreviewUiState(isLoading = true))
    val uiState: StateFlow<NewsPreviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getNews(currencySymbol = currencySymbol, limit = 5)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, news = result.data, error = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }
}
