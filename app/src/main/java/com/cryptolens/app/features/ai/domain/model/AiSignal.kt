package com.cryptolens.app.features.ai.domain.model

enum class SignalType { BUY, SELL, HOLD }

enum class AiMessageRole { USER, ASSISTANT }

data class AiChatMessage(
    val id: Long,
    val role: AiMessageRole,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class AiSignal(
    val signal: SignalType,
    val confidence: Int,           // 0-100
    val reasoning: String,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val summary: String,
    val generatedAt: Long = System.currentTimeMillis(),
)
