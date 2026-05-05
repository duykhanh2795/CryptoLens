package com.cryptolens.app.features.portfolio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.features.portfolio.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val isLoading: Boolean               = true,
    val summary: PortfolioSummary?       = null,
    val transactions: List<Transaction>  = emptyList(),
    val selectedTab: PortfolioTab        = PortfolioTab.ASSETS,
    val error: String?                   = null,
    val showAddSheet: Boolean            = false,
)

enum class PortfolioTab { ASSETS, TRANSACTIONS }

sealed class PortfolioEvent {
    data class TabSelected(val tab: PortfolioTab)     : PortfolioEvent()
    data object ShowAddSheet                          : PortfolioEvent()
    data object HideAddSheet                          : PortfolioEvent()
    data class DeleteAsset(val assetId: String)       : PortfolioEvent()
    data class DeleteTransaction(val txId: String)    : PortfolioEvent()
    data class AddTransaction(
        val coinId: String,
        val symbol: String,
        val name: String,
        val imageUrl: String,
        val type: TransactionType,
        val quantityStr: String,
        val priceStr: String,
        val fee: Double = 0.0,
        val note: String = "",
    ) : PortfolioEvent()
}

sealed class PortfolioEffect {
    data class ShowError(val message: String)   : PortfolioEffect()
    data class ShowSuccess(val message: String) : PortfolioEffect()
}

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val observePortfolioSummary: ObservePortfolioSummaryUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val deleteAsset: DeleteAssetUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PortfolioEffect>()
    val effect: SharedFlow<PortfolioEffect> = _effect.asSharedFlow()

    private val userId get() = getCurrentUser()?.id ?: ""

    init {
        if (userId.isNotBlank()) {
            observeSummary()
            observeTxs()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: PortfolioEvent) {
        when (event) {
            is PortfolioEvent.TabSelected      -> _uiState.update { it.copy(selectedTab = event.tab) }
            is PortfolioEvent.ShowAddSheet     -> _uiState.update { it.copy(showAddSheet = true) }
            is PortfolioEvent.HideAddSheet     -> _uiState.update { it.copy(showAddSheet = false) }
            is PortfolioEvent.AddTransaction   -> handleAddTransaction(event)
            is PortfolioEvent.DeleteAsset      -> handleDeleteAsset(event.assetId)
            is PortfolioEvent.DeleteTransaction -> handleDeleteTransaction(event.txId)
        }
    }

    private fun observeSummary() {
        viewModelScope.launch {
            observePortfolioSummary(userId).collect { summary ->
                _uiState.update { it.copy(isLoading = false, summary = summary) }
            }
        }
    }

    private fun observeTxs() {
        viewModelScope.launch {
            observeTransactions(userId).collect { txs ->
                _uiState.update { it.copy(transactions = txs) }
            }
        }
    }

    private fun handleAddTransaction(event: PortfolioEvent.AddTransaction) {
        viewModelScope.launch {
            val result = addTransaction(
                userId    = userId,
                coinId    = event.coinId,
                symbol    = event.symbol,
                name      = event.name,
                imageUrl  = event.imageUrl,
                type      = event.type,
                quantityStr = event.quantityStr,
                priceStr  = event.priceStr,
                fee       = event.fee,
                note      = event.note,
            )
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(showAddSheet = false) }
                    _effect.emit(PortfolioEffect.ShowSuccess(
                        "${if (event.type == TransactionType.BUY) "Buy" else "Sell"} recorded!"
                    ))
                }
                is Resource.Error -> _effect.emit(PortfolioEffect.ShowError(result.message))
                else -> Unit
            }
        }
    }

    private fun handleDeleteAsset(assetId: String) {
        viewModelScope.launch {
            when (val r = deleteAsset(assetId, userId)) {
                is Resource.Success -> _effect.emit(PortfolioEffect.ShowSuccess("Asset removed"))
                is Resource.Error   -> _effect.emit(PortfolioEffect.ShowError(r.message))
                else -> Unit
            }
        }
    }

    private fun handleDeleteTransaction(txId: String) {
        viewModelScope.launch {
            when (val r = deleteTransaction(txId, userId)) {
                is Resource.Success -> _effect.emit(PortfolioEffect.ShowSuccess("Transaction deleted"))
                is Resource.Error   -> _effect.emit(PortfolioEffect.ShowError(r.message))
                else -> Unit
            }
        }
    }
}
