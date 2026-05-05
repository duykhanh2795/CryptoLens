package com.cryptolens.app.features.exchange.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.exchange.domain.model.ApiKeyValidation
import com.cryptolens.app.features.exchange.domain.model.ExchangeConnection
import com.cryptolens.app.features.exchange.domain.model.ExchangeType
import com.cryptolens.app.features.exchange.domain.model.SyncResult
import com.cryptolens.app.features.exchange.domain.repository.ExchangeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExchangeConnectionsUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    operator fun invoke(): Flow<List<ExchangeConnection>> = repo.observeConnections()
}

class ValidateApiKeyUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    suspend operator fun invoke(
        exchangeType: ExchangeType,
        apiKey: String,
        apiSecret: String,
    ): Resource<ApiKeyValidation> {
        if (apiKey.isBlank())    return Resource.Error("API Key cannot be empty")
        if (apiSecret.isBlank()) return Resource.Error("API Secret cannot be empty")
        if (apiKey.length < 10)  return Resource.Error("API Key looks too short")
        return repo.validateApiKey(exchangeType, apiKey.trim(), apiSecret.trim())
    }
}

class SaveExchangeConnectionUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    suspend operator fun invoke(
        exchangeType: ExchangeType,
        label: String,
        apiKey: String,
        apiSecret: String,
    ): Resource<ExchangeConnection> {
        val finalLabel = label.trim().ifBlank { exchangeType.displayName }
        return repo.saveConnection(exchangeType, finalLabel, apiKey.trim(), apiSecret.trim())
    }
}

class DeleteExchangeConnectionUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    suspend operator fun invoke(id: String): Resource<Unit> = repo.deleteConnection(id)
}

class SyncExchangeTradesUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    suspend operator fun invoke(connectionId: String): Resource<SyncResult> =
        repo.syncTrades(connectionId)
}

class SetConnectionActiveUseCase @Inject constructor(
    private val repo: ExchangeRepository,
) {
    suspend operator fun invoke(id: String, isActive: Boolean): Resource<Unit> =
        repo.setConnectionActive(id, isActive)
}
