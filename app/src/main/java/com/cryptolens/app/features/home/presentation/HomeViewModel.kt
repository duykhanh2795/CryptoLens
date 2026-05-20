package com.cryptolens.app.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.model.AuthUser
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.market.data.remote.MarketCache
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.usecase.GetTrendingWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: AuthUser?           = null,
    val trendingCoins: List<Coin> = emptyList(),
    val topGainers: List<Coin>    = emptyList(),
    val topLosers: List<Coin>     = emptyList(),
    val trendingWallets: List<TrendingWallet> = emptyList(),
    val isLoading: Boolean        = true,
    val error: String?            = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTopCoins: GetTopCoinsUseCase,
    private val getTrendingWallets: GetTrendingWalletsUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val marketCache: MarketCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(user = getCurrentUser()) }
        loadMarketData()
        loadTrendingWallets()
    }

    fun refresh() {
        marketCache.invalidate()
        loadMarketData()
        loadTrendingWallets()
    }

    private fun loadMarketData() {
        viewModelScope.launch {
            getTopCoins(perPage = 50).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { state ->
                        if (state.trendingCoins.isEmpty()) state.copy(isLoading = true, error = null)
                        else state
                    }
                    is Resource.Success -> {
                        val coins = resource.data
                        _uiState.update {
                            it.copy(
                                isLoading     = false,
                                trendingCoins = coins.take(10),
                                topGainers    = coins
                                    .filter { c -> c.priceChangePercent24h > 0 }
                                    .sortedByDescending { c -> c.priceChangePercent24h }
                                    .take(5),
                                topLosers     = coins
                                    .filter { c -> c.priceChangePercent24h < 0 }
                                    .sortedBy { c -> c.priceChangePercent24h }
                                    .take(5),
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { state ->
                        if (state.trendingCoins.isEmpty())
                            state.copy(isLoading = false, error = resource.message)
                        else
                            state.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun loadTrendingWallets() {
        viewModelScope.launch {
            when (val result = getTrendingWallets()) {
                is Resource.Success -> _uiState.update {
                    it.copy(trendingWallets = result.data.take(2))
                }
                else -> Unit
            }
        }
    }
}
