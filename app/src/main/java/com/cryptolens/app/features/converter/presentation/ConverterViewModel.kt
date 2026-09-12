package com.cryptolens.app.features.converter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConverterAsset(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val priceUsd: Double,
    val priceChangePercent24h: Double = 0.0,
    val rank: Int = 0,
    val isFiat: Boolean = false,
) {
    val isPositive: Boolean get() = priceChangePercent24h >= 0.0
}

enum class ConverterPickerTarget {
    FROM,
    TO,
}

data class ConverterUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val assets: List<ConverterAsset> = emptyList(),
    val fromAsset: ConverterAsset? = null,
    val toAsset: ConverterAsset? = null,
    val amountInput: String = "1",
    val searchQuery: String = "",
    val pickerTarget: ConverterPickerTarget? = null,
    val error: String? = null,
) {
    val amount: Double get() = amountInput.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    val convertedAmount: Double get() {
        val from = fromAsset ?: return 0.0
        val to = toAsset ?: return 0.0
        if (from.priceUsd <= 0.0 || to.priceUsd <= 0.0) return 0.0
        return (amount * from.priceUsd) / to.priceUsd
    }
    val directRate: Double get() {
        val from = fromAsset ?: return 0.0
        val to = toAsset ?: return 0.0
        if (from.priceUsd <= 0.0 || to.priceUsd <= 0.0) return 0.0
        return from.priceUsd / to.priceUsd
    }
    val inverseRate: Double get() {
        val rate = directRate
        return if (rate > 0.0) 1.0 / rate else 0.0
    }
    val filteredAssets: List<ConverterAsset> get() {
        val query = searchQuery.trim()
        if (query.isBlank()) return assets
        return assets.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }
    }
}

sealed class ConverterEvent {
    data class AmountChanged(val value: String) : ConverterEvent()
    data class ShowPicker(val target: ConverterPickerTarget) : ConverterEvent()
    data object HidePicker : ConverterEvent()
    data class SearchChanged(val value: String) : ConverterEvent()
    data class AssetSelected(val assetId: String) : ConverterEvent()
    data object SwapAssets : ConverterEvent()
    data object Refresh : ConverterEvent()
    data class PairSelected(val fromId: String, val toId: String) : ConverterEvent()
}

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val getTopCoins: GetTopCoinsUseCase,
    private val observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        observeAppSettings()
        loadAssets()
    }

    private fun observeAppSettings() {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: ConverterEvent) {
        when (event) {
            is ConverterEvent.AmountChanged ->
                _uiState.update { it.copy(amountInput = sanitizeAmount(event.value)) }
            is ConverterEvent.ShowPicker ->
                _uiState.update { it.copy(pickerTarget = event.target, searchQuery = "") }
            ConverterEvent.HidePicker ->
                _uiState.update { it.copy(pickerTarget = null, searchQuery = "") }
            is ConverterEvent.SearchChanged ->
                _uiState.update { it.copy(searchQuery = event.value) }
            is ConverterEvent.AssetSelected -> selectAsset(event.assetId)
            ConverterEvent.SwapAssets -> swapAssets()
            ConverterEvent.Refresh -> loadAssets(force = true)
            is ConverterEvent.PairSelected -> selectPair(event.fromId, event.toId)
        }
    }

    private fun loadAssets(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getTopCoins(perPage = 250).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val assets = buildAssets(resource.data)
                        _uiState.update { state ->
                            val previousFromId = state.fromAsset?.id ?: "bitcoin"
                            val previousToId = state.toAsset?.id ?: "ethereum"
                            state.copy(
                                isLoading = false,
                                assets = assets,
                                fromAsset = assets.firstOrNull { it.id == previousFromId }
                                    ?: assets.firstOrNull { it.id == "bitcoin" }
                                    ?: assets.firstOrNull(),
                                toAsset = assets.firstOrNull { it.id == previousToId }
                                    ?: assets.firstOrNull { it.id == "ethereum" }
                                    ?: assets.firstOrNull { it.id != previousFromId },
                                error = null,
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                    Resource.Loading -> if (force) {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                }
            }
        }
    }

    private fun buildAssets(coins: List<Coin>): List<ConverterAsset> {
        val usd = ConverterAsset(
            id = USD_ID,
            symbol = "USD",
            name = "US Dollar",
            imageUrl = "",
            priceUsd = 1.0,
            isFiat = true,
        )
        return listOf(usd) + coins
            .filter { it.currentPrice > 0.0 }
            .map {
                ConverterAsset(
                    id = it.id,
                    symbol = it.symbol.uppercase(),
                    name = it.name,
                    imageUrl = it.imageUrl,
                    priceUsd = it.currentPrice,
                    priceChangePercent24h = it.priceChangePercent24h,
                    rank = it.rank,
                )
            }
    }

    private fun selectAsset(assetId: String) {
        val state = _uiState.value
        val asset = state.assets.firstOrNull { it.id == assetId } ?: return
        when (state.pickerTarget) {
            ConverterPickerTarget.FROM -> {
                _uiState.update {
                    val nextTo = if (asset.id == it.toAsset?.id) it.fromAsset else it.toAsset
                    it.copy(fromAsset = asset, toAsset = nextTo, pickerTarget = null, searchQuery = "")
                }
            }
            ConverterPickerTarget.TO -> {
                _uiState.update {
                    val nextFrom = if (asset.id == it.fromAsset?.id) it.toAsset else it.fromAsset
                    it.copy(fromAsset = nextFrom, toAsset = asset, pickerTarget = null, searchQuery = "")
                }
            }
            null -> Unit
        }
    }

    private fun swapAssets() {
        _uiState.update {
            it.copy(fromAsset = it.toAsset, toAsset = it.fromAsset)
        }
    }

    private fun selectPair(fromId: String, toId: String) {
        _uiState.update { state ->
            val from = state.assets.firstOrNull { it.id == fromId } ?: state.fromAsset
            val to = state.assets.firstOrNull { it.id == toId } ?: state.toAsset
            state.copy(fromAsset = from, toAsset = to)
        }
    }

    private fun sanitizeAmount(value: String): String {
        val normalized = value.replace(",", ".")
        val builder = StringBuilder()
        var dotSeen = false
        normalized.forEach { char ->
            when {
                char.isDigit() -> builder.append(char)
                char == '.' && !dotSeen -> {
                    dotSeen = true
                    builder.append(char)
                }
            }
        }
        return builder.toString().take(18)
    }

    private companion object {
        const val USD_ID = "usd"
    }
}
