package com.cryptolens.app.features.ai.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.ai.data.remote.GeminiAiService
import com.cryptolens.app.features.ai.domain.model.AiChatMessage
import com.cryptolens.app.features.ai.domain.model.AiSignal
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.market.domain.model.Kline
import javax.inject.Inject

class GenerateAiSignalUseCase @Inject constructor(
    private val geminiAiService: GeminiAiService,
) {
    suspend operator fun invoke(
        coin: Coin,
        klines: List<Kline>,
    ): Resource<AiSignal> = geminiAiService.analyzeSignal(coin, klines)
}

class SendAiChatMessageUseCase @Inject constructor(
    private val geminiAiService: GeminiAiService,
) {
    suspend operator fun invoke(
        message: String,
        marketContext: List<Coin>,
        recentMessages: List<AiChatMessage>,
    ): Resource<String> = geminiAiService.sendChatMessage(
        message = message,
        marketContext = marketContext,
        recentMessages = recentMessages,
    )
}
