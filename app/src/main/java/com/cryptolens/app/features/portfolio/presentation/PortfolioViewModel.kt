package com.cryptolens.app.features.portfolio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.cryptolens.app.features.portfolio.domain.model.*
import com.cryptolens.app.features.portfolio.domain.usecase.*
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val isLoading: Boolean               = true,
    val settings: AppSettings            = AppSettings(),
    val summary: PortfolioSummary?       = null,
    val snapshots: List<PortfolioSnapshot> = emptyList(),
    val transactions: List<Transaction>  = emptyList(),
    val selectedTab: PortfolioTab        = PortfolioTab.ASSETS,
    val error: String?                   = null,
    val showAddSheet: Boolean            = false,
    val isImportExportBusy: Boolean      = false,
    val importPreview: PortfolioImportPreview? = null,
)

enum class PortfolioTab { ASSETS, TRANSACTIONS }

sealed class PortfolioEvent {
    data class TabSelected(val tab: PortfolioTab)     : PortfolioEvent()
    data object ShowAddSheet                          : PortfolioEvent()
    data object HideAddSheet                          : PortfolioEvent()
    data object ExportCsv                             : PortfolioEvent()
    data class ExportCsvCompleted(val success: Boolean) : PortfolioEvent()
    data class ImportCsvLoaded(val content: String)   : PortfolioEvent()
    data object DismissImportPreview                  : PortfolioEvent()
    data class ConfirmImportCsv(val mode: PortfolioImportMode) : PortfolioEvent()
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
    data class LaunchCsvExport(val fileName: String, val content: String) : PortfolioEffect()
}

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val observePortfolioSummary: ObservePortfolioSummaryUseCase,
    private val observePortfolioSnapshots: ObservePortfolioSnapshotsUseCase,
    private val savePortfolioSnapshot: SavePortfolioSnapshotUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val deleteAsset: DeleteAssetUseCase,
    private val exportPortfolioCsv: ExportPortfolioCsvUseCase,
    private val previewPortfolioCsv: PreviewPortfolioCsvUseCase,
    private val importPortfolioCsv: ImportPortfolioCsvUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PortfolioEffect>()
    val effect: SharedFlow<PortfolioEffect> = _effect.asSharedFlow()

    private val userId get() = getCurrentUser()?.id ?: ""

    init {
        observeAppSettings()
        if (userId.isNotBlank()) {
            observeSummary()
            observeSnapshots()
            observeTxs()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: PortfolioEvent) {
        when (event) {
            is PortfolioEvent.TabSelected      -> _uiState.update { it.copy(selectedTab = event.tab) }
            is PortfolioEvent.ShowAddSheet     -> _uiState.update { it.copy(showAddSheet = true) }
            is PortfolioEvent.HideAddSheet     -> _uiState.update { it.copy(showAddSheet = false) }
            is PortfolioEvent.ExportCsv        -> handleExportCsv()
            is PortfolioEvent.ExportCsvCompleted -> handleExportCsvCompleted(event.success)
            is PortfolioEvent.ImportCsvLoaded  -> handleImportCsvLoaded(event.content)
            is PortfolioEvent.DismissImportPreview ->
                _uiState.update { it.copy(importPreview = null, isImportExportBusy = false) }
            is PortfolioEvent.ConfirmImportCsv -> handleConfirmImport(event.mode)
            is PortfolioEvent.AddTransaction   -> handleAddTransaction(event)
            is PortfolioEvent.DeleteAsset      -> handleDeleteAsset(event.assetId)
            is PortfolioEvent.DeleteTransaction -> handleDeleteTransaction(event.txId)
        }
    }

    private fun observeSummary() {
        viewModelScope.launch {
            observePortfolioSummary(userId).collect { summary ->
                _uiState.update { it.copy(isLoading = false, summary = summary) }
                if (summary.assets.isNotEmpty()) {
                    savePortfolioSnapshot(userId, summary)
                }
            }
        }
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            observePortfolioSnapshots(userId).collect { snapshots ->
                _uiState.update { it.copy(snapshots = snapshots) }
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

    private fun handleExportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportExportBusy = true) }
            when (val result = exportPortfolioCsv(userId)) {
                is Resource.Success -> {
                    if (_uiState.value.transactions.isEmpty()) {
                        _uiState.update { it.copy(isImportExportBusy = false) }
                        _effect.emit(PortfolioEffect.ShowError("No transactions to export"))
                    } else {
                        _effect.emit(
                            PortfolioEffect.LaunchCsvExport(
                                fileName = "cryptolens-portfolio-${System.currentTimeMillis()}.csv",
                                content = result.data,
                            )
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isImportExportBusy = false) }
                    _effect.emit(PortfolioEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isImportExportBusy = false) }
            }
        }
    }

    private fun handleExportCsvCompleted(success: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportExportBusy = false) }
            if (success) {
                _effect.emit(PortfolioEffect.ShowSuccess("Portfolio CSV exported"))
            }
        }
    }

    private fun handleImportCsvLoaded(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportExportBusy = true, importPreview = null) }
            when (val result = previewPortfolioCsv(content)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isImportExportBusy = false, importPreview = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isImportExportBusy = false) }
                    _effect.emit(PortfolioEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isImportExportBusy = false) }
            }
        }
    }

    private fun handleConfirmImport(mode: PortfolioImportMode) {
        viewModelScope.launch {
            val preview = _uiState.value.importPreview ?: return@launch
            _uiState.update { it.copy(isImportExportBusy = true) }
            when (val result = importPortfolioCsv(userId, preview, mode)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isImportExportBusy = false, importPreview = null)
                    }
                    _effect.emit(PortfolioEffect.ShowSuccess("${result.data} transactions imported"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isImportExportBusy = false) }
                    _effect.emit(PortfolioEffect.ShowError(result.message))
                }
                else -> _uiState.update { it.copy(isImportExportBusy = false) }
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
