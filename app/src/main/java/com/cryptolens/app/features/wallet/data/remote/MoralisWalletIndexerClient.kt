package com.cryptolens.app.features.wallet.data.remote

import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletTransaction
import com.cryptolens.app.features.wallet.domain.model.WalletTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoralisWalletIndexerClient @Inject constructor(
    private val client: OkHttpClient,
) {
    private val apiKey: String get() = BuildConfig.MORALIS_API_KEY.trim()
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun fetchTokenAssets(address: String): Resource<List<WalletAsset>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Resource.Error(MISSING_KEY_MESSAGE)

            try {
                val assets = mutableListOf<WalletAsset>()
                var cursor: String? = null
                var pageCount = 0

                do {
                    val urlBuilder = "https://api.moralis.com/v1/wallets/$address/tokens"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("chains", "mainnets")
                        .addQueryParameter("excludeSpam", "true")
                        .addQueryParameter("excludeNative", "true")
                        .addQueryParameter("limit", "1000")

                    if (!cursor.isNullOrBlank()) {
                        urlBuilder.addQueryParameter("cursor", cursor)
                    }

                    val response = getJson(urlBuilder.build().toString())
                    val items = response.optJSONArray("result") ?: JSONArray()
                    for (index in 0 until items.length()) {
                        val item = items.getJSONObject(index)
                        if (item.optBoolean("possibleSpam", false)) continue

                        val chainId = item.optString("chainId")
                        val chainInfo = CHAIN_BY_ID[chainId] ?: continue
                        val symbol = item.optString("symbol").ifBlank { "TOKEN" }
                        val quantity = item.optString("balance").toDoubleOrNull()
                            ?: item.optString("balanceFormatted").toDoubleOrNull()
                            ?: item.optString("balanceRaw").toDecimal(item.optInt("decimals", 18))
                        if (quantity <= 0.0) continue

                        val price = item.optDoubleOrNull("usdPrice")
                        val value = item.optDoubleOrNull("usdValue") ?: price?.let { quantity * it }

                        assets.add(
                            WalletAsset(
                                symbol = symbol,
                                name = item.optString("name").ifBlank { symbol },
                                quantity = quantity,
                                priceUsd = price,
                                valueUsd = value,
                                changePercent24h = item.optDoubleOrNull("usdPrice24hrPercentChange") ?: 0.0,
                                chain = chainInfo.chain,
                                networkLabel = chainInfo.label,
                                contractAddress = item.optString("tokenAddress").takeIf { it.isNotBlank() },
                            ),
                        )
                    }

                    cursor = response.optString("cursor").takeIf { it.isNotBlank() && it != "null" }
                    pageCount += 1
                } while (!cursor.isNullOrBlank() && pageCount < MAX_PAGES)

                Resource.Success(
                    assets
                        .distinctBy { "${it.networkLabel}:${it.contractAddress ?: it.symbol}" }
                        .sortedWith(compareByDescending<WalletAsset> { it.valueUsd ?: 0.0 }.thenBy { it.symbol }),
                )
            } catch (e: Exception) {
                Resource.Error("Moralis token indexer failed: ${e.message}", e)
            }
        }

    suspend fun fetchHistory(address: String, chain: WalletChain): Resource<List<WalletTransaction>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Resource.Error(MISSING_KEY_MESSAGE)
            val moralisChain = chain.toMoralisHistoryChain()
                ?: return@withContext Resource.Error("Moralis history is not configured for ${chain.label}")

            try {
                val url = "https://deep-index.moralis.io/api/v2.2/wallets/$address/history"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("chain", moralisChain)
                    .addQueryParameter("order", "DESC")
                    .addQueryParameter("limit", "50")
                    .addQueryParameter("include_internal_transactions", "true")
                    .build()
                    .toString()
                val response = getJson(url)
                val transactions = response.optJSONArray("result") ?: JSONArray()

                Resource.Success(
                    buildList {
                        for (index in 0 until transactions.length()) {
                            val tx = transactions.getJSONObject(index)
                            addAll(parseNativeTransfers(tx, address, chain))
                            addAll(parseErc20Transfers(tx, address, chain.label))
                        }
                    }
                        .distinctBy { "${it.id}:${it.symbol}:${it.amount}:${it.type}" }
                        .sortedByDescending { it.timestamp }
                        .take(50),
                )
            } catch (e: Exception) {
                Resource.Error("Moralis wallet history failed: ${e.message}", e)
            }
        }

    private fun parseNativeTransfers(
        tx: JSONObject,
        address: String,
        chain: WalletChain,
    ): List<WalletTransaction> {
        val transfers = tx.optJSONArray("native_transfers") ?: JSONArray()
        return buildList {
            for (index in 0 until transfers.length()) {
                val transfer = transfers.getJSONObject(index)
                val amount = transfer.optString("value_formatted").toDoubleOrNull()
                    ?: transfer.optString("value").toDecimal(18)
                if (amount <= 0.0) continue
                val incoming = transfer.optString("to_address").equals(address, ignoreCase = true)
                val counterparty = if (incoming) transfer.optString("from_address") else transfer.optString("to_address")
                add(
                    WalletTransaction(
                        id = tx.optString("hash").ifBlank { "native:$index" },
                        type = if (incoming) WalletTransactionType.RECEIVED else WalletTransactionType.SENT,
                        symbol = chain.nativeSymbol,
                        amount = amount,
                        valueUsd = null,
                        counterparty = counterparty.takeIf { it.isNotBlank() },
                        timestamp = tx.optString("block_timestamp").parseIsoTimestamp(),
                        networkLabel = chain.label,
                    ),
                )
            }
        }
    }

    private fun parseErc20Transfers(
        tx: JSONObject,
        address: String,
        networkLabel: String,
    ): List<WalletTransaction> {
        val transfers = tx.optJSONArray("erc20_transfers") ?: JSONArray()
        return buildList {
            for (index in 0 until transfers.length()) {
                val transfer = transfers.getJSONObject(index)
                val symbol = transfer.optString("token_symbol").ifBlank { "TOKEN" }
                val decimals = transfer.optString("token_decimals").toIntOrNull() ?: 18
                val amount = transfer.optString("value_formatted").toDoubleOrNull()
                    ?: transfer.optString("value").toDecimal(decimals)
                if (amount <= 0.0) continue
                val incoming = transfer.optString("to_address").equals(address, ignoreCase = true)
                val counterparty = if (incoming) transfer.optString("from_address") else transfer.optString("to_address")
                add(
                    WalletTransaction(
                        id = tx.optString("hash").ifBlank { "erc20:$index" },
                        type = if (incoming) WalletTransactionType.RECEIVED else WalletTransactionType.SENT,
                        symbol = symbol,
                        amount = amount,
                        valueUsd = null,
                        counterparty = counterparty.takeIf { it.isNotBlank() },
                        timestamp = transfer.optString("block_timestamp")
                            .ifBlank { tx.optString("block_timestamp") }
                            .parseIsoTimestamp(),
                        networkLabel = networkLabel,
                    ),
                )
            }
        }
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("X-API-Key", apiKey)
            .header("accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(body.toMoralisErrorMessage(response.code))
            }
            return JSONObject(body)
        }
    }

    private fun String.toDecimal(decimals: Int): Double =
        runCatching {
            if (isBlank()) {
                0.0
            } else {
                BigDecimal(this).divide(BigDecimal.TEN.pow(decimals.coerceAtLeast(0))).toDouble()
            }
        }.getOrDefault(0.0)

    private fun JSONObject.optDoubleOrNull(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return optString(name).toDoubleOrNull() ?: runCatching { getDouble(name) }.getOrNull()
    }

    private fun String.parseIsoTimestamp(): Long =
        runCatching { java.time.Instant.parse(this).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

    private fun String.toMoralisErrorMessage(code: Int): String {
        val normalized = trim()
        if (code == 401 || code == 403 || normalized.contains("api key", ignoreCase = true)) {
            return "Moralis API key is not authenticated. Add MORALIS_API_KEY to local.properties, then Sync Gradle and reinstall the app."
        }
        return normalized.ifBlank { "Moralis HTTP $code" }
    }

    private fun WalletChain.toMoralisHistoryChain(): String? =
        when (this) {
            WalletChain.ETHEREUM -> "eth"
            WalletChain.POLYGON -> "polygon"
            WalletChain.BNB_CHAIN -> "bsc"
            WalletChain.BITCOIN -> null
        }

    private data class ChainInfo(
        val chain: WalletChain,
        val label: String,
    )

    private companion object {
        const val MISSING_KEY_MESSAGE = "Missing Moralis API key. Add MORALIS_API_KEY to local.properties."
        const val MAX_PAGES = 3
        val CHAIN_BY_ID = mapOf(
            "0x1" to ChainInfo(WalletChain.ETHEREUM, "Ethereum"),
            "0x2105" to ChainInfo(WalletChain.ETHEREUM, "Base"),
            "0xa4b1" to ChainInfo(WalletChain.ETHEREUM, "Arbitrum"),
            "0xa" to ChainInfo(WalletChain.ETHEREUM, "Optimism"),
            "0x89" to ChainInfo(WalletChain.POLYGON, "Polygon"),
            "0x38" to ChainInfo(WalletChain.BNB_CHAIN, "BNB Chain"),
            "0xa86a" to ChainInfo(WalletChain.ETHEREUM, "Avalanche"),
        )
    }
}
