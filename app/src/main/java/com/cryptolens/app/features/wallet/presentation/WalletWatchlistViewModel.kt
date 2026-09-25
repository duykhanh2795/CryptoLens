package com.cryptolens.app.features.wallet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem
import com.cryptolens.app.features.wallet.domain.model.WalletWatchListItem
import com.cryptolens.app.features.wallet.domain.usecase.AddWalletToWatchlistUseCase
import com.cryptolens.app.features.wallet.domain.usecase.DeleteWalletWatchItemUseCase
import com.cryptolens.app.features.wallet.domain.usecase.GetConnectedWalletPortfoliosUseCase
import com.cryptolens.app.features.wallet.domain.usecase.ObserveWalletWatchlistUseCase
import com.cryptolens.app.features.wallet.domain.usecase.RefreshWalletWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletWatchlistUiState(
    val wallets: List<WalletWatchListItem> = emptyList(),
    val portfolios: Map<String, ConnectedWalletPortfolioUiState> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingPortfolios: Boolean = false,
    val isSaving: Boolean = false,
    val showAddSheet: Boolean = false,
    val selectedChain: WalletChain = WalletChain.ETHEREUM,
    val labelInput: String = "",
    val addressInput: String = "",
    val inputError: String? = null,
)

data class ConnectedWalletPortfolioUiState(
    val detail: TrendingWalletDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class WalletWatchlistEvent {
    data object ShowAddSheet : WalletWatchlistEvent()
    data object HideAddSheet : WalletWatchlistEvent()
    data class ChainChanged(val chain: WalletChain) : WalletWatchlistEvent()
    data class LabelChanged(val value: String) : WalletWatchlistEvent()
    data class AddressChanged(val value: String) : WalletWatchlistEvent()
    data object AddWallet : WalletWatchlistEvent()
    data class DeleteWallet(val id: String) : WalletWatchlistEvent()
    data object RefreshAll : WalletWatchlistEvent()
}

sealed class WalletWatchlistEffect {
    data class ShowMessage(val message: String) : WalletWatchlistEffect()
}

@HiltViewModel
class WalletWatchlistViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val observeWallets: ObserveWalletWatchlistUseCase,
    private val addWallet: AddWalletToWatchlistUseCase,
    private val deleteWallet: DeleteWalletWatchItemUseCase,
    private val refreshWallets: RefreshWalletWatchlistUseCase,
    private val getConnectedWalletPortfolios: GetConnectedWalletPortfoliosUseCase,
    private val getTopCoins: GetTopCoinsUseCase,
    private val observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val userId = getCurrentUser()?.id.orEmpty()
    private val rawWallets = MutableStateFlow<List<WalletWatchItem>>(emptyList())
    private val nativePrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    private var lastPortfolioLoadKey: String? = null
    private var portfolioLoadGeneration: Long = 0L

    private val _uiState = MutableStateFlow(WalletWatchlistUiState())
    val uiState: StateFlow<WalletWatchlistUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WalletWatchlistEffect>()
    val effect: SharedFlow<WalletWatchlistEffect> = _effect.asSharedFlow()

    init {
        observeAppSettings()
        observeWalletList()
        observeNativePrices()
        combineWalletsWithPrices()
        refreshAll()
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: WalletWatchlistEvent) {
        when (event) {
            WalletWatchlistEvent.ShowAddSheet ->
                _uiState.update { it.copy(showAddSheet = true, inputError = null) }
            WalletWatchlistEvent.HideAddSheet ->
                _uiState.update {
                    it.copy(
                        showAddSheet = false,
                        labelInput = "",
                        addressInput = "",
                        inputError = null,
                    )
                }
            is WalletWatchlistEvent.ChainChanged ->
                _uiState.update { it.copy(selectedChain = event.chain, inputError = null) }
            is WalletWatchlistEvent.LabelChanged ->
                _uiState.update { it.copy(labelInput = event.value, inputError = null) }
            is WalletWatchlistEvent.AddressChanged ->
                _uiState.update { it.copy(addressInput = event.value, inputError = null) }
            WalletWatchlistEvent.AddWallet -> saveWallet()
            is WalletWatchlistEvent.DeleteWallet -> delete(event.id)
            WalletWatchlistEvent.RefreshAll -> refreshAll()
        }
    }

    private fun observeWalletList() {
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            observeWallets(userId).collect { wallets ->
                rawWallets.value = wallets
                _uiState.update { it.copy(isLoading = false) }
                loadConnectedWalletPortfolios(wallets)
            }
        }
    }

    private fun observeNativePrices() {
        viewModelScope.launch {
            getTopCoins(currency = "usd", perPage = 250, page = 1).collect { result ->
                if (result is Resource.Success) {
                    nativePrices.value = result.data.associate { coin ->
                        coin.id to coin.currentPrice
                    }
                }
            }
        }
    }

    private fun combineWalletsWithPrices() {
        viewModelScope.launch {
            combine(rawWallets, nativePrices) { wallets, prices ->
                wallets.map { wallet ->
                    WalletWatchListItem(
                        wallet = wallet,
                        nativePriceUsd = wallet.chain.priceCoinIds.firstNotNullOfOrNull { prices[it] },
                    )
                }
            }.collect { items ->
                _uiState.update { it.copy(wallets = items) }
            }
        }
    }

    private fun saveWallet() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true, inputError = null) }

            when (
                val result = addWallet(
                    userId = userId,
                    label = state.labelInput,
                    address = state.addressInput,
                    chain = state.selectedChain,
                )
            ) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showAddSheet = false,
                            labelInput = "",
                            addressInput = "",
                            inputError = null,
                        )
                    }
                    _effect.emit(WalletWatchlistEffect.ShowMessage("Wallet added"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSaving = false, inputError = result.message) }
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            when (val result = deleteWallet(id, userId)) {
                is Resource.Success -> _effect.emit(WalletWatchlistEffect.ShowMessage("Wallet removed"))
                is Resource.Error -> _effect.emit(WalletWatchlistEffect.ShowMessage(result.message))
                Resource.Loading -> Unit
            }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch {
            if (userId.isBlank()) {
                _effect.emit(WalletWatchlistEffect.ShowMessage("Please log in before tracking wallets"))
                return@launch
            }

            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = refreshWallets(userId)) {
                is Resource.Error -> _effect.emit(WalletWatchlistEffect.ShowMessage(result.message))
                else -> Unit
            }
            loadConnectedWalletPortfolios(rawWallets.value, force = true)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadConnectedWalletPortfolios(
        wallets: List<WalletWatchItem>,
        force: Boolean = false,
    ) {
        if (wallets.isEmpty()) {
            lastPortfolioLoadKey = null
            _uiState.update { it.copy(portfolios = emptyMap(), isLoadingPortfolios = false) }
            return
        }

        val loadKey = wallets.joinToString(separator = "|") {
            "${it.id}:${it.address}:${it.chain.name}:${it.nativeBalance}:${it.lastSyncedAt}"
        }
        if (!force && loadKey == lastPortfolioLoadKey) return
        lastPortfolioLoadKey = loadKey
        val generation = ++portfolioLoadGeneration

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoadingPortfolios = true,
                    portfolios = wallets.associate { wallet ->
                        wallet.id to (state.portfolios[wallet.id]?.copy(isLoading = true, error = null)
                            ?: ConnectedWalletPortfolioUiState(isLoading = true))
                    },
                )
            }

            val results = getConnectedWalletPortfolios(wallets)
            if (generation != portfolioLoadGeneration) return@launch
            _uiState.update { state ->
                state.copy(
                    isLoadingPortfolios = false,
                    portfolios = wallets.associate { wallet ->
                        val result = results[wallet.id]
                        wallet.id to when (result) {
                            is Resource.Success -> ConnectedWalletPortfolioUiState(detail = result.data)
                            is Resource.Error -> ConnectedWalletPortfolioUiState(error = result.message)
                            Resource.Loading, null -> ConnectedWalletPortfolioUiState(isLoading = true)
                        }
                    },
                )
            }
        }
    }
}
