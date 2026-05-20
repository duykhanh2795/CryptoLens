package com.cryptolens.app.features.exchange.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.exchange.domain.model.ApiKeyValidation
import com.cryptolens.app.features.exchange.domain.model.ExchangeConnection
import com.cryptolens.app.features.exchange.domain.model.ExchangeType
import com.cryptolens.app.features.exchange.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface ExchangeRepository {

    /** Observe all saved connections (live). */
    fun observeConnections(): Flow<List<ExchangeConnection>>

    /** Get single connection by id. */
    suspend fun getConnection(id: String): ExchangeConnection?

    /**
     * Validate API key against the real exchange before saving.
     * Never stores anything — purely a network call.
     */
    suspend fun validateApiKey(
        exchangeType: ExchangeType,
        apiKey: String,
        apiSecret: String,
    ): Resource<ApiKeyValidation>

    /**
     * Save a new connection. Secret is encrypted before persistence.
     * Returns the saved [ExchangeConnection] (without plain-text secret).
     */
    suspend fun saveConnection(
        exchangeType: ExchangeType,
        label: String,
        apiKey: String,
        apiSecret: String,
    ): Resource<ExchangeConnection>

    /** Remove a saved connection and all its imported trades. */
    suspend fun deleteConnection(id: String): Resource<Unit>

    /** Toggle active/inactive without deleting. */
    suspend fun setConnectionActive(id: String, isActive: Boolean): Resource<Unit>

    /**
     * Pull trades from exchange and store them as portfolio transactions.
     * Returns a [SyncResult] with counts.
     */
    suspend fun syncTrades(connectionId: String, userId: String): Resource<SyncResult>
}
