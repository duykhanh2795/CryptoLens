package com.cryptolens.app.features.wallet.data.remote

import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletTransaction
import com.cryptolens.app.features.wallet.domain.model.WalletTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlchemyWalletIndexerClient @Inject constructor(
    client: OkHttpClient,
) {
    private val client = client.newBuilder()
        .callTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val apiKey: String get() = BuildConfig.ALCHEMY_API_KEY.trim()
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun fetchTokenAssets(
        address: String,
        nativeChangesBySymbol: Map<String, Double>,
    ): Resource<List<WalletAsset>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Resource.Error(MISSING_KEY_MESSAGE)

        try {
            val portfolioAssets = fetchPortfolioTokenAssets(address, nativeChangesBySymbol)
            Resource.Success(portfolioAssets.take(MAX_ASSETS))
        } catch (e: Exception) {
            Resource.Error("Alchemy token indexer failed: ${e.message}", e)
        }
    }

    private fun fetchPortfolioTokenAssets(
        address: String,
        nativeChangesBySymbol: Map<String, Double>,
    ): List<WalletAsset> {
        val assets = mutableListOf<WalletAsset>()
        var pageKey: String? = null
        var pageCount = 0

        do {
            val body = JSONObject()
                .put(
                    "addresses",
                    JSONArray().put(
                        JSONObject()
                            .put("address", address)
                            .put("networks", JSONArray(PORTFOLIO_NETWORKS.map { it.id })),
                    ),
                )
                .put("withMetadata", true)
                .put("withPrices", true)
                .put("includeNativeTokens", false)
                .put("includeErc20Tokens", true)

            if (!pageKey.isNullOrBlank()) {
                body.put("pageKey", pageKey)
            }

            val response = postJson(
                url = "https://api.g.alchemy.com/data/v1/$apiKey/assets/tokens/by-address",
                body = body,
            )
            val data = response.optJSONObject("data") ?: JSONObject()
            val tokens = data.optJSONArray("tokens") ?: JSONArray()

            for (index in 0 until tokens.length()) {
                val token = tokens.getJSONObject(index)
                val networkId = token.optString("network")
                val network = PORTFOLIO_NETWORKS.firstOrNull { it.id == networkId } ?: continue
                val metadata = token.optJSONObject("tokenMetadata") ?: JSONObject()
                val symbol = metadata.optString("symbol").ifBlank { token.optString("symbol").ifBlank { "TOKEN" } }
                val decimals = metadata.optInt("decimals", 18)
                val quantity = token.optString("tokenBalance").parseTokenBalance(decimals)
                if (quantity <= 0.0) continue
                val price = token.optJSONArray("tokenPrices")?.firstUsdPrice()

                assets.add(
                    WalletAsset(
                        symbol = symbol,
                        name = metadata.optString("name").ifBlank { symbol },
                        quantity = quantity,
                        priceUsd = price,
                        valueUsd = price?.let { quantity * it },
                        changePercent24h = nativeChangesBySymbol[symbol.uppercase()] ?: 0.0,
                        chain = network.chain,
                        networkLabel = network.label,
                        contractAddress = token.optString("tokenAddress").takeIf { it.isNotBlank() },
                    ),
                )
            }

            pageKey = data.optString("pageKey").takeIf { it.isNotBlank() }
            pageCount += 1
        } while (!pageKey.isNullOrBlank() && pageCount < MAX_PORTFOLIO_PAGES)

        return assets
            .distinctBy { "${it.networkLabel}:${it.contractAddress ?: it.symbol}" }
            .sortedWith(compareByDescending<WalletAsset> { it.valueUsd ?: 0.0 }.thenBy { it.symbol })
    }

    suspend fun fetchTransferHistory(address: String, chain: WalletChain): Resource<List<WalletTransaction>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Resource.Error(MISSING_KEY_MESSAGE)

            val network = ALCHEMY_NETWORKS.firstOrNull { it.chain == chain }
                ?: return@withContext Resource.Error("Alchemy history is not configured for ${chain.label}")

            try {
                val (incoming, outgoing) = coroutineScope {
                    val incomingRequest = async(Dispatchers.IO) {
                        fetchTransfers(network, address, incoming = true)
                    }
                    val outgoingRequest = async(Dispatchers.IO) {
                        fetchTransfers(network, address, incoming = false)
                    }
                    incomingRequest.await() to outgoingRequest.await()
                }
                Resource.Success(
                    (incoming + outgoing)
                        .distinctBy { it.id + it.type.name + it.amount + it.counterparty }
                        .sortedByDescending { it.timestamp }
                        .take(50),
                )
            } catch (e: Exception) {
                Resource.Error("Alchemy transfer history failed: ${e.message}", e)
            }
        }

    private fun fetchTransfers(
        network: AlchemyNetwork,
        address: String,
        incoming: Boolean,
    ): List<WalletTransaction> {
        val filter = JSONObject()
            .put("fromBlock", "0x0")
            .put("toBlock", "latest")
            .put("withMetadata", true)
            .put("excludeZeroValue", true)
            .put("maxCount", "0x32")
            .put("order", "desc")
            .put("category", JSONArray().put("external").put("erc20"))

        if (incoming) {
            filter.put("toAddress", address)
        } else {
            filter.put("fromAddress", address)
        }

        val result = callJsonRpc(
            endpoint = network.endpoint,
            method = "alchemy_getAssetTransfers",
            params = JSONArray().put(filter),
        )
        val transfers = result.optJSONArray("transfers") ?: JSONArray()
        return buildList {
            for (index in 0 until transfers.length()) {
                val transfer = transfers.getJSONObject(index)
                val rawValue = transfer.optDouble("value", 0.0)
                if (rawValue <= 0.0) continue
                val symbol = transfer.optString("asset")
                    .trim()
                    .ifBlank { network.chain.nativeSymbol }
                if (symbol.none { it.isLetterOrDigit() }) continue
                val metadata = transfer.optJSONObject("metadata")
                add(
                    WalletTransaction(
                        id = transfer.optString("hash").ifBlank { "${network.id}:$index" },
                        type = if (incoming) WalletTransactionType.RECEIVED else WalletTransactionType.SENT,
                        symbol = symbol,
                        amount = rawValue,
                        valueUsd = null,
                        counterparty = if (incoming) transfer.optString("from") else transfer.optString("to"),
                        timestamp = metadata?.optString("blockTimestamp")?.parseIsoTimestamp()
                            ?: System.currentTimeMillis(),
                        networkLabel = network.label,
                    ),
                )
            }
        }
    }

    private fun callJsonRpc(endpoint: String, method: String, params: JSONArray): JSONObject {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
            .toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(responseBody.toAlchemyErrorMessage(response.code))
            }
            val json = JSONObject(responseBody)
            if (json.has("error")) {
                val message = json.optJSONObject("error")?.optString("message").orEmpty()
                throw IllegalStateException(message.toAlchemyErrorMessage())
            }
            return json.getJSONObject("result")
        }
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(responseBody.toAlchemyErrorMessage(response.code))
            }
            return JSONObject(responseBody)
        }
    }

    private fun String.hexToBigInteger(): BigInteger =
        removePrefix("0x").ifBlank { "0" }.let { BigInteger(it, 16) }

    private fun BigInteger.toDecimal(decimals: Int): Double =
        BigDecimal(this).divide(BigDecimal.TEN.pow(decimals.coerceAtLeast(0))).toDouble()

    private fun String.parseTokenBalance(decimals: Int): Double {
        val trimmed = trim()
        if (trimmed.isBlank()) return 0.0
        if (trimmed.startsWith("0x")) return trimmed.hexToBigInteger().toDecimal(decimals)
        return runCatching {
            val decimal = BigDecimal(trimmed)
            if (trimmed.contains(".")) {
                decimal.toDouble()
            } else {
                decimal.divide(BigDecimal.TEN.pow(decimals.coerceAtLeast(0))).toDouble()
            }
        }.getOrDefault(0.0)
    }

    private fun JSONArray.firstUsdPrice(): Double? {
        for (index in 0 until length()) {
            val price = optJSONObject(index) ?: continue
            if (price.optString("currency").equals("usd", ignoreCase = true)) {
                return price.optString("value").toDoubleOrNull()
            }
        }
        return null
    }

    private fun String.parseIsoTimestamp(): Long? =
        runCatching { java.time.Instant.parse(this).toEpochMilli() }.getOrNull()

    private fun String.toAlchemyErrorMessage(code: Int? = null): String {
        val normalized = trim()
        if (
            normalized.contains("Must be authenticated", ignoreCase = true) ||
            normalized.contains("invalid api key", ignoreCase = true) ||
            code == 401 ||
            code == 403
        ) {
            return "Alchemy API key is not authenticated. Copy the full API key from the Alchemy dashboard, update local.properties, then Sync Gradle and reinstall the app."
        }
        return normalized.ifBlank { code?.let { "Alchemy HTTP $it" } ?: "Alchemy API error" }
    }

    private data class AlchemyNetwork(
        val id: String,
        val label: String,
        val chain: WalletChain,
        val endpoint: String,
    )

    private companion object {
        const val MISSING_KEY_MESSAGE = "Missing Alchemy API key. Add ALCHEMY_API_KEY to local.properties."
        const val REQUEST_TIMEOUT_SECONDS = 12L
        const val MAX_PORTFOLIO_PAGES = 1
        const val MAX_ASSETS = 100
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val PORTFOLIO_NETWORKS = listOf(
            AlchemyNetwork("eth-mainnet", "Ethereum", WalletChain.ETHEREUM, ""),
            AlchemyNetwork("polygon-mainnet", "Polygon", WalletChain.POLYGON, ""),
            AlchemyNetwork("base-mainnet", "Base", WalletChain.ETHEREUM, ""),
            AlchemyNetwork("arb-mainnet", "Arbitrum", WalletChain.ETHEREUM, ""),
            AlchemyNetwork("opt-mainnet", "Optimism", WalletChain.ETHEREUM, ""),
        )
        val ALCHEMY_NETWORKS: List<AlchemyNetwork>
            get() {
                val key = BuildConfig.ALCHEMY_API_KEY
                return listOf(
                    AlchemyNetwork("eth-mainnet", "Ethereum", WalletChain.ETHEREUM, "https://eth-mainnet.g.alchemy.com/v2/$key"),
                    AlchemyNetwork("polygon-mainnet", "Polygon", WalletChain.POLYGON, "https://polygon-mainnet.g.alchemy.com/v2/$key"),
                    AlchemyNetwork("arb-mainnet", "Arbitrum", WalletChain.ETHEREUM, "https://arb-mainnet.g.alchemy.com/v2/$key"),
                    AlchemyNetwork("base-mainnet", "Base", WalletChain.ETHEREUM, "https://base-mainnet.g.alchemy.com/v2/$key"),
                    AlchemyNetwork("opt-mainnet", "Optimism", WalletChain.ETHEREUM, "https://opt-mainnet.g.alchemy.com/v2/$key"),
                )
            }
    }
}
