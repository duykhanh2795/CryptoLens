package com.cryptolens.app.features.exchange.data.repository

import androidx.room.withTransaction
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.core.data.local.CryptoDatabase
import com.cryptolens.app.features.exchange.data.local.CryptoManager
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionEntity
import com.cryptolens.app.features.exchange.data.remote.BinanceApiClient
import com.cryptolens.app.features.exchange.domain.model.*
import com.cryptolens.app.features.exchange.domain.repository.ExchangeRepository
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRepositoryImpl @Inject constructor(
    private val database: CryptoDatabase,
    private val dao: ExchangeConnectionDao,
    private val cryptoManager: CryptoManager,
    private val binanceClient: BinanceApiClient,
    private val assetDao: PortfolioAssetDao,
    private val transactionDao: TransactionDao,
) : ExchangeRepository {

    override fun observeConnections(): Flow<List<ExchangeConnection>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getConnection(id: String): ExchangeConnection? =
        dao.getById(id)?.toDomain()

    override suspend fun validateApiKey(
        exchangeType: ExchangeType,
        apiKey: String,
        apiSecret: String,
    ): Resource<ApiKeyValidation> = when (exchangeType) {
        ExchangeType.BINANCE -> binanceClient.validateCredentials(apiKey, apiSecret)
        else -> Resource.Error("${exchangeType.displayName} integration coming soon")
    }

    override suspend fun saveConnection(
        exchangeType: ExchangeType,
        label: String,
        apiKey: String,
        apiSecret: String,
    ): Resource<ExchangeConnection> {
        return try {
            val entity = ExchangeConnectionEntity(
                id                  = UUID.randomUUID().toString(),
                exchangeType        = exchangeType.name,
                label               = label,
                apiKey              = cryptoManager.encrypt(apiKey),
                apiSecretEncrypted  = cryptoManager.encrypt(apiSecret),
                isActive            = true,
                lastSyncAt          = null,
                createdAt           = System.currentTimeMillis(),
            )
            dao.upsert(entity)
            Resource.Success(entity.toDomain())
        } catch (e: Exception) {
            Resource.Error("Failed to save connection: ${e.message}")
        }
    }

    override suspend fun deleteConnection(id: String): Resource<Unit> {
        return try {
            database.withTransaction {
                val importedTransactions = transactionDao.getTransactionsByConnection(id)
                val affectedHoldings = importedTransactions
                    .map { it.userId to it.coinId }
                    .toSet()

                transactionDao.deleteTransactionsByConnection(id)
                transactionDao.deleteAllTransactions("exchange_$id")
                assetDao.deleteAllAssets("exchange_$id")
                dao.deleteById(id)
                affectedHoldings.forEach { (userId, coinId) ->
                    rebuildAssetFromTransactions(userId, coinId)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete: ${e.message}")
        }
    }

    override suspend fun setConnectionActive(id: String, isActive: Boolean): Resource<Unit> {
        return try {
            dao.setActive(id, isActive)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update: ${e.message}")
        }
    }

    override suspend fun syncTrades(connectionId: String, userId: String): Resource<SyncResult> {
        return try {
            val entity = dao.getById(connectionId)
                ?: return Resource.Error("Connection not found")
            if (userId.isBlank()) return Resource.Error("Please log in before syncing exchange trades")

            val apiKey = cryptoManager.decrypt(entity.apiKey)
                ?: return Resource.Error("Failed to decrypt API Key")
            val apiSecret = cryptoManager.decrypt(entity.apiSecretEncrypted)
                ?: return Resource.Error("Failed to decrypt API Secret")

            val exchangeType = ExchangeType.valueOf(entity.exchangeType)

            when (exchangeType) {
                ExchangeType.BINANCE -> syncBinance(
                    connectionId = connectionId,
                    userId       = userId,
                    apiKey       = apiKey,
                    apiSecret    = apiSecret,
                    exchangeType = exchangeType,
                )
                else -> Resource.Error("${exchangeType.displayName} sync not yet supported")
            }
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    // ── Binance Sync Logic ─────────────────────────────────────────────────────

    private suspend fun syncBinance(
        connectionId: String,
        userId: String,
        apiKey: String,
        apiSecret: String,
        exchangeType: ExchangeType,
    ): Resource<SyncResult> {
        // 1. Get all USDT symbols available
        val symbolsResult = binanceClient.getUsdtSymbols()
        if (symbolsResult is Resource.Error) return symbolsResult

        val symbolMap = (symbolsResult as Resource.Success).data
            .associate { it.symbol to Pair(it.baseAsset, it.quoteAsset) }

        // Commonly traded USDT pairs — fetch these first, then expand
        val prioritySymbols = listOf(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT",
            "XRPUSDT", "DOGEUSDT", "ADAUSDT", "AVAXUSDT",
            "DOTUSDT", "MATICUSDT", "LINKUSDT", "LTCUSDT",
            "UNIUSDT", "ATOMUSDT", "ETCUSDT", "TRXUSDT",
        )

        var totalImported = 0
        var totalSkipped  = 0

        // 2. Keep in-memory cache for ids discovered during this sync run.
        // Existing rows in DB are checked via DAO before applying portfolio updates.
        val existingIds = mutableSetOf<String>()

        for (symbol in prioritySymbols) {
            val (baseAsset, _) = symbolMap[symbol] ?: continue

            val tradesResult = binanceClient.getAllTrades(apiKey, apiSecret, symbol)
            if (tradesResult is Resource.Error) continue   // skip symbol on error

            val trades = (tradesResult as Resource.Success).data
            if (trades.isEmpty()) continue

            for (trade in trades) {
                val txId = "binance_${connectionId}_${trade.symbol}_${trade.id}"

                // Skip if already imported
                if (txId in existingIds || transactionDao.getTransactionById(txId) != null) {
                    totalSkipped++
                    existingIds.add(txId)
                    continue
                }

                val type     = if (trade.isBuyer) TransactionType.BUY else TransactionType.SELL
                val quantity = trade.qty.toDoubleOrNull() ?: continue
                val price    = trade.price.toDoubleOrNull() ?: continue
                val fee      = trade.commission.toDoubleOrNull() ?: 0.0
                val coinId   = baseAssetToCoinGeckoId(baseAsset)

                // Save transaction
                val txEntity = TransactionEntity(
                    id        = txId,
                    userId    = userId,
                    coinId    = coinId,
                    symbol    = baseAsset,
                    name      = baseAsset,
                    imageUrl  = "",
                    type      = type.name,
                    quantity  = quantity,
                    price     = price,
                    fee       = fee,
                    note      = "Imported from Binance · ${trade.symbol}",
                    sourceConnectionId = connectionId,
                    timestamp = trade.time,
                )
                database.withTransaction {
                    transactionDao.insertTransaction(txEntity)

                    updatePortfolioAsset(
                        userId    = userId,
                        coinId    = coinId,
                        symbol    = baseAsset,
                        name      = baseAsset,
                        quantity  = quantity,
                        price     = price,
                        type      = type,
                    )
                }

                existingIds.add(txId)
                totalImported++
            }
        }

        // 3. Update lastSyncAt
        dao.updateLastSync(connectionId, System.currentTimeMillis())

        return Resource.Success(
            SyncResult(
                exchangeType    = exchangeType,
                tradesImported  = totalImported,
                tradesSkipped   = totalSkipped,
            )
        )
    }

    private suspend fun updatePortfolioAsset(
        userId: String,
        coinId: String,
        symbol: String,
        name: String,
        quantity: Double,
        price: Double,
        type: TransactionType,
    ) {
        val existing = assetDao.getAsset(userId, coinId)
        when (type) {
            TransactionType.BUY -> {
                val prevQty = existing?.quantity ?: 0.0
                val prevAvg = existing?.averageBuyPrice ?: 0.0
                val newQty  = prevQty + quantity
                val newAvg  = if (prevQty == 0.0) price
                              else (prevQty * prevAvg + quantity * price) / newQty
                assetDao.upsertAsset(
                    PortfolioAssetEntity(
                        id              = existing?.id ?: UUID.randomUUID().toString(),
                        userId          = userId,
                        coinId          = coinId,
                        symbol          = symbol,
                        name            = name,
                        imageUrl        = "",
                        quantity        = newQty,
                        averageBuyPrice = newAvg,
                        createdAt       = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt       = System.currentTimeMillis(),
                    )
                )
            }
            TransactionType.SELL -> {
                val newQty = ((existing?.quantity ?: 0.0) - quantity).coerceAtLeast(0.0)
                if (newQty > 0.0 && existing != null) {
                    assetDao.upsertAsset(existing.copy(quantity = newQty, updatedAt = System.currentTimeMillis()))
                } else if (existing != null) {
                    assetDao.deleteAsset(existing.id, userId)
                }
            }
        }
    }

    private suspend fun rebuildAssetFromTransactions(userId: String, coinId: String) {
        val history = transactionDao.getTransactionsByCoin(userId, coinId)
        val existing = assetDao.getAsset(userId, coinId)
        if (history.isEmpty()) {
            existing?.let { assetDao.deleteAsset(it.id, userId) }
            return
        }

        var quantity = 0.0
        var averageBuyPrice = 0.0
        var lastMetadata: TransactionEntity? = null

        history.forEach { tx ->
            lastMetadata = tx
            when (TransactionType.valueOf(tx.type)) {
                TransactionType.BUY -> {
                    val newQty = quantity + tx.quantity
                    if (newQty > 0.0) {
                        averageBuyPrice = if (quantity == 0.0) tx.price
                        else ((quantity * averageBuyPrice) + (tx.quantity * tx.price)) / newQty
                    }
                    quantity = newQty
                }
                TransactionType.SELL -> {
                    quantity = (quantity - tx.quantity).coerceAtLeast(0.0)
                }
            }
        }

        if (quantity <= 0.0) {
            existing?.let { assetDao.deleteAsset(it.id, userId) }
            return
        }

        val metadata = lastMetadata ?: return
        assetDao.upsertAsset(
            PortfolioAssetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = userId,
                coinId = coinId,
                symbol = metadata.symbol,
                name = metadata.name,
                imageUrl = metadata.imageUrl,
                quantity = quantity,
                averageBuyPrice = averageBuyPrice,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private fun ExchangeConnectionEntity.toDomain(): ExchangeConnection =
        ExchangeConnection(
            id           = id,
            exchangeType = ExchangeType.valueOf(exchangeType),
            label        = label,
            apiKey       = maskApiKey(cryptoManager.decrypt(apiKey) ?: ""),
            isActive     = isActive,
            lastSyncAt   = lastSyncAt,
            createdAt    = createdAt,
        )

    private fun maskApiKey(plain: String): String =
        if (plain.length > 8) plain.take(4) + "****" + plain.takeLast(4)
        else "****"

    // ── CoinGecko id mapping ───────────────────────────────────────────────────

    private fun baseAssetToCoinGeckoId(baseAsset: String): String =
        coinGeckoIdMap[baseAsset.uppercase()] ?: baseAsset.lowercase()

    private val coinGeckoIdMap = mapOf(
        "BTC"   to "bitcoin",
        "ETH"   to "ethereum",
        "BNB"   to "binancecoin",
        "SOL"   to "solana",
        "XRP"   to "ripple",
        "DOGE"  to "dogecoin",
        "ADA"   to "cardano",
        "AVAX"  to "avalanche-2",
        "DOT"   to "polkadot",
        "MATIC" to "matic-network",
        "LINK"  to "chainlink",
        "LTC"   to "litecoin",
        "UNI"   to "uniswap",
        "ATOM"  to "cosmos",
        "ETC"   to "ethereum-classic",
        "TRX"   to "tron",
        "SHIB"  to "shiba-inu",
        "BCH"   to "bitcoin-cash",
        "XLM"   to "stellar",
        "FIL"   to "filecoin",
        "VET"   to "vechain",
        "NEAR"  to "near",
        "FTM"   to "fantom",
        "ALGO"  to "algorand",
        "MANA"  to "decentraland",
        "SAND"  to "the-sandbox",
        "AXS"   to "axie-infinity",
        "APE"   to "apecoin",
        "OP"    to "optimism",
        "ARB"   to "arbitrum",
        "SUI"   to "sui",
        "SEI"   to "sei-network",
        "INJ"   to "injective-protocol",
        "TIA"   to "celestia",
        "JUP"   to "jupiter-exchange-solana",
        "WIF"   to "dogwifcoin",
        "PEPE"  to "pepe",
    )
}
