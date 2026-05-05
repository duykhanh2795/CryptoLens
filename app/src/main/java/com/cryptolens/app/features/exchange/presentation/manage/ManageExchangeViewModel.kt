package com.cryptolens.app.features.exchange.presentation.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.exchange.domain.model.ExchangeConnection
import com.cryptolens.app.features.exchange.domain.model.SyncResult
import com.cryptolens.app.features.exchange.domain.usecase.DeleteExchangeConnectionUseCase
import com.cryptolens.app.features.exchange.domain.usecase.ObserveExchangeConnectionsUseCase
import com.cryptolens.app.features.exchange.domain.usecase.SetConnectionActiveUseCase
import com.cryptolens.app.features.exchange.domain.usecase.SyncExchangeTradesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageExchangeUiState(
    val connections: List<ExchangeConnection> = emptyList(),
    val isLoading: Boolean                    = false,
    val syncingId: String?                    = null,       // id of connection being synced
    val lastSyncResult: SyncResult?           = null,
)

sealed class ManageExchangeEvent {
    data class DeleteConnection(val id: String)        : ManageExchangeEvent()
    data class SyncConnection(val id: String)          : ManageExchangeEvent()
    data class ToggleActive(val id: String, val isActive: Boolean) : ManageExchangeEvent()
    data object DismissSyncResult                      : ManageExchangeEvent()
}

sealed class ManageExchangeEffect {
    data class ShowError(val message: String)   : ManageExchangeEffect()
    data class ShowSuccess(val message: String) : ManageExchangeEffect()
}

@HiltViewModel
class ManageExchangeViewModel @Inject constructor(
    private val observeConnections: ObserveExchangeConnectionsUseCase,
    private val deleteConnection: DeleteExchangeConnectionUseCase,
    private val syncTrades: SyncExchangeTradesUseCase,
    private val setConnectionActive: SetConnectionActiveUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageExchangeUiState())
    val uiState: StateFlow<ManageExchangeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ManageExchangeEffect>()
    val effect: SharedFlow<ManageExchangeEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            observeConnections().collect { list ->
                _uiState.update { it.copy(connections = list) }
            }
        }
    }

    fun onEvent(event: ManageExchangeEvent) {
        when (event) {
            is ManageExchangeEvent.DeleteConnection  -> handleDelete(event.id)
            is ManageExchangeEvent.SyncConnection    -> handleSync(event.id)
            is ManageExchangeEvent.ToggleActive      -> handleToggleActive(event.id, event.isActive)
            is ManageExchangeEvent.DismissSyncResult ->
                _uiState.update { it.copy(lastSyncResult = null) }
        }
    }

    private fun handleDelete(id: String) {
        viewModelScope.launch {
            when (val r = deleteConnection(id)) {
                is Resource.Success -> _effect.emit(ManageExchangeEffect.ShowSuccess("Connection removed"))
                is Resource.Error   -> _effect.emit(ManageExchangeEffect.ShowError(r.message))
                else -> Unit
            }
        }
    }

    private fun handleSync(id: String) {
        if (_uiState.value.syncingId != null) return   // already syncing
        viewModelScope.launch {
            _uiState.update { it.copy(syncingId = id) }
            when (val r = syncTrades(id)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(syncingId = null, lastSyncResult = r.data) }
                    _effect.emit(ManageExchangeEffect.ShowSuccess(
                        "Sync complete: ${r.data.tradesImported} trades imported"
                    ))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(syncingId = null) }
                    _effect.emit(ManageExchangeEffect.ShowError(r.message))
                }
                else -> _uiState.update { it.copy(syncingId = null) }
            }
        }
    }

    private fun handleToggleActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val r = setConnectionActive(id, isActive)) {
                is Resource.Error -> _effect.emit(ManageExchangeEffect.ShowError(r.message))
                else -> Unit
            }
        }
    }
}
