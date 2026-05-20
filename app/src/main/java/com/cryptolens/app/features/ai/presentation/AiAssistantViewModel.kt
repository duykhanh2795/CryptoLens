package com.cryptolens.app.features.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.ai.domain.model.AiChatMessage
import com.cryptolens.app.features.ai.domain.model.AiMessageRole
import com.cryptolens.app.features.ai.domain.model.AiSignal
import com.cryptolens.app.features.ai.domain.usecase.GenerateAiSignalUseCase
import com.cryptolens.app.features.ai.domain.usecase.SendAiChatMessageUseCase
import com.cryptolens.app.features.market.domain.model.ChartMarketType
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.usecase.GetKlinesUseCase
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.market.domain.util.CryptoSymbolMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantUiState(
    val coins: List<Coin> = emptyList(),
    val selectedCoinId: String? = null,
    val messages: List<AiChatMessage> = emptyList(),
    val input: String = "",
    val signal: AiSignal? = null,
    val isLoadingCoins: Boolean = true,
    val isAnalyzing: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
) {
    val selectedCoin: Coin?
        get() = coins.firstOrNull { it.id == selectedCoinId } ?: coins.firstOrNull()
}

sealed class AiAssistantEvent {
    data object RefreshCoins : AiAssistantEvent()
    data object Analyze : AiAssistantEvent()
    data object SendMessage : AiAssistantEvent()
    data object NewChat : AiAssistantEvent()
    data object DismissError : AiAssistantEvent()
    data class SelectCoin(val coinId: String) : AiAssistantEvent()
    data class InputChanged(val value: String) : AiAssistantEvent()
    data class PromptSelected(val prompt: String) : AiAssistantEvent()
}

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val getTopCoins: GetTopCoinsUseCase,
    private val getKlines: GetKlinesUseCase,
    private val generateAiSignal: GenerateAiSignalUseCase,
    private val sendAiChatMessage: SendAiChatMessageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private var coinsJob: Job? = null
    private var activeJob: Job? = null
    private var nextMessageId = 1L

    init {
        loadCoins()
    }

    fun onEvent(event: AiAssistantEvent) {
        when (event) {
            AiAssistantEvent.RefreshCoins -> loadCoins(force = true)
            AiAssistantEvent.Analyze -> analyzeSelectedCoin()
            AiAssistantEvent.SendMessage -> sendCurrentInput()
            AiAssistantEvent.NewChat -> {
                activeJob?.cancel()
                _uiState.update {
                    it.copy(messages = emptyList(), input = "", signal = null, error = null, isSending = false)
                }
            }
            AiAssistantEvent.DismissError -> _uiState.update { it.copy(error = null) }
            is AiAssistantEvent.InputChanged -> _uiState.update { it.copy(input = event.value) }
            is AiAssistantEvent.PromptSelected -> sendMessage(event.prompt)
            is AiAssistantEvent.SelectCoin -> _uiState.update {
                it.copy(
                    selectedCoinId = event.coinId,
                    signal = null,
                    error = null,
                )
            }
        }
    }

    private fun loadCoins(force: Boolean = false) {
        coinsJob?.cancel()
        coinsJob = viewModelScope.launch {
            getTopCoins(perPage = 30).collectLatest { resource ->
                when (resource) {
                    Resource.Loading -> _uiState.update { state ->
                        if (state.coins.isEmpty() || force) {
                            state.copy(isLoadingCoins = true, error = null)
                        } else {
                            state
                        }
                    }
                    is Resource.Success -> _uiState.update { state ->
                        state.copy(
                            coins = resource.data,
                            selectedCoinId = state.selectedCoinId ?: resource.data.firstOrNull()?.id,
                            isLoadingCoins = false,
                            error = null,
                        )
                    }
                    is Resource.Error -> _uiState.update { state ->
                        state.copy(
                            isLoadingCoins = false,
                            error = if (state.coins.isEmpty()) resource.message else state.error,
                        )
                    }
                }
            }
        }
    }

    private fun sendCurrentInput() {
        sendMessage(_uiState.value.input)
    }

    private fun sendMessage(rawMessage: String) {
        val message = rawMessage.trim()
        if (message.isBlank() || _uiState.value.isSending) return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val userMessage = AiChatMessage(
                id = nextMessageId++,
                role = AiMessageRole.USER,
                text = message,
            )
            val priorMessages = _uiState.value.messages
            _uiState.update {
                it.copy(
                    messages = priorMessages + userMessage,
                    input = "",
                    error = null,
                    isSending = true,
                    signal = null,
                )
            }

            when (
                val result = sendAiChatMessage(
                    message = message,
                    marketContext = _uiState.value.coins,
                    recentMessages = priorMessages.takeLast(8),
                )
            ) {
                is Resource.Success -> {
                    val assistantMessage = AiChatMessage(
                        id = nextMessageId++,
                        role = AiMessageRole.ASSISTANT,
                        text = result.data,
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isSending = false,
                            error = null,
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSending = false, error = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun analyzeSelectedCoin() {
        val coin = _uiState.value.selectedCoin ?: return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, signal = null) }

            val symbol = CryptoSymbolMapper.binanceTickerSymbol(coin)
            when (
                val klines = getKlines(
                    symbol = symbol,
                    interval = "1h",
                    limit = 72,
                    marketType = ChartMarketType.SPOT,
                )
            ) {
                is Resource.Success -> {
                    when (val signal = generateAiSignal(coin, klines.data)) {
                        is Resource.Success -> {
                            val text = buildSignalMessage(coin, signal.data)
                            val assistantMessage = AiChatMessage(
                                id = nextMessageId++,
                                role = AiMessageRole.ASSISTANT,
                                text = text,
                            )
                            _uiState.update {
                                it.copy(
                                    isAnalyzing = false,
                                    signal = signal.data,
                                    messages = it.messages + assistantMessage,
                                    error = null,
                                )
                            }
                        }
                        is Resource.Error -> _uiState.update {
                            it.copy(isAnalyzing = false, error = signal.message)
                        }
                        Resource.Loading -> Unit
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isAnalyzing = false, error = klines.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun buildSignalMessage(coin: Coin, signal: AiSignal): String =
        "${coin.symbol} ${signal.signal.name} (${signal.confidence}% confidence)\n\n" +
            "${signal.summary}\n\n${signal.reasoning}\n\n" +
            "Support: $${signal.supportLevel}\nResistance: $${signal.resistanceLevel}"
}
