package com.cryptolens.app.features.wallet.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.usecase.AddWalletToWatchlistUseCase
import com.cryptolens.app.features.wallet.domain.usecase.GetTrendingWalletDetailUseCase
import com.cryptolens.app.features.wallet.domain.usecase.GetTrendingWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingWalletsUiState(
    val wallets: List<TrendingWallet> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val filteredWallets: List<TrendingWallet>
        get() = if (query.isBlank()) wallets else wallets.filter {
            it.address.contains(query, ignoreCase = true) ||
                it.label.contains(query, ignoreCase = true) ||
                it.chain.label.contains(query, ignoreCase = true)
        }
}

sealed class TrendingWalletsEvent {
    data object Refresh : TrendingWalletsEvent()
    data class QueryChanged(val value: String) : TrendingWalletsEvent()
}

@HiltViewModel
class TrendingWalletsViewModel @Inject constructor(
    private val getTrendingWallets: GetTrendingWalletsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingWalletsUiState())
    val uiState: StateFlow<TrendingWalletsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: TrendingWalletsEvent) {
        when (event) {
            TrendingWalletsEvent.Refresh -> load()
            is TrendingWalletsEvent.QueryChanged -> _uiState.update { it.copy(query = event.value) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getTrendingWallets()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, wallets = result.data, error = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }
}

enum class WalletDetailTab {
    ASSETS,
    HISTORY
}

data class TrendingWalletDetailUiState(
    val detail: TrendingWalletDetail? = null,
    val selectedTab: WalletDetailTab = WalletDetailTab.ASSETS,
    val isLoading: Boolean = true,
    val isAddingToWatchlist: Boolean = false,
    val error: String? = null,
)

sealed class TrendingWalletDetailEvent {
    data class TabChanged(val tab: WalletDetailTab) : TrendingWalletDetailEvent()
    data object AddToWatchlist : TrendingWalletDetailEvent()
    data object Retry : TrendingWalletDetailEvent()
}

sealed class TrendingWalletDetailEffect {
    data class ShowMessage(val message: String) : TrendingWalletDetailEffect()
}

@HiltViewModel
class TrendingWalletDetailViewModel @Inject constructor(
    private val getDetail: GetTrendingWalletDetailUseCase,
    private val addWallet: AddWalletToWatchlistUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chainName: String = checkNotNull(savedStateHandle["chain"])
    private val address: String = checkNotNull(savedStateHandle["address"])

    private val _uiState = MutableStateFlow(TrendingWalletDetailUiState())
    val uiState: StateFlow<TrendingWalletDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<TrendingWalletDetailEffect>()
    val effect: SharedFlow<TrendingWalletDetailEffect> = _effect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: TrendingWalletDetailEvent) {
        when (event) {
            is TrendingWalletDetailEvent.TabChanged ->
                _uiState.update { it.copy(selectedTab = event.tab) }
            TrendingWalletDetailEvent.AddToWatchlist -> addCurrentWallet()
            TrendingWalletDetailEvent.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getDetail(chainName, address)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, detail = result.data, error = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun addCurrentWallet() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToWatchlist = true) }
            val wallet = detail.wallet
            when (
                val result = addWallet(
                    userId = getCurrentUser()?.id.orEmpty(),
                    label = wallet.label,
                    address = wallet.address,
                    chain = wallet.chain,
                )
            ) {
                is Resource.Success -> _effect.emit(TrendingWalletDetailEffect.ShowMessage("Added to wallet watchlist"))
                is Resource.Error -> _effect.emit(TrendingWalletDetailEffect.ShowMessage(result.message))
                Resource.Loading -> Unit
            }
            _uiState.update { it.copy(isAddingToWatchlist = false) }
        }
    }
}
