package com.cryptolens.app.features.wallet.data.remote

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.WalletBalance
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletTransaction
import com.cryptolens.app.features.wallet.domain.model.WalletTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletBalanceClient @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun fetchBalance(chain: WalletChain, address: String): Resource<WalletBalance> =
        withContext(Dispatchers.IO) {
            try {
                when (chain) {
                    WalletChain.ETHEREUM,
                    WalletChain.BNB_CHAIN,
                    WalletChain.POLYGON -> fetchEvmBalance(chain, address)
                    WalletChain.BITCOIN -> fetchBitcoinBalance(address)
                }
            } catch (e: Exception) {
                Resource.Error("Could not load wallet data: ${e.message}", e)
            }
        }

    suspend fun fetchHistory(chain: WalletChain, address: String): Resource<List<WalletTransaction>> =
        withContext(Dispatchers.IO) {
            try {
                when (chain) {
                    WalletChain.BITCOIN -> fetchBitcoinTransactions(address)
                    WalletChain.ETHEREUM,
                    WalletChain.BNB_CHAIN,
                    WalletChain.POLYGON -> Resource.Success(emptyList())
                }
            } catch (e: Exception) {
                Resource.Error("Could not load wallet history: ${e.message}", e)
            }
        }

    private fun fetchEvmBalance(chain: WalletChain, address: String): Resource<WalletBalance> {
        val endpoint = when (chain) {
            WalletChain.ETHEREUM -> "https://ethereum-rpc.publicnode.com"
            WalletChain.BNB_CHAIN -> "https://bsc-rpc.publicnode.com"
            WalletChain.POLYGON -> "https://polygon-bor-rpc.publicnode.com"
            WalletChain.BITCOIN -> error("Bitcoin is not an EVM chain")
        }

        val balanceHex = callJsonRpc(endpoint, "eth_getBalance", listOf(address, "latest"))
            ?: return Resource.Error("RPC returned an empty balance")
        val txCountHex = callJsonRpc(endpoint, "eth_getTransactionCount", listOf(address, "latest"))
            ?: "0x0"

        val balanceWei = parseHexInteger(balanceHex)
        val txCount = parseHexInteger(txCountHex).toLong()
        val nativeBalance = BigDecimal(balanceWei)
            .divide(BigDecimal.TEN.pow(EVM_NATIVE_DECIMALS))
            .toDouble()

        return Resource.Success(
            WalletBalance(
                nativeBalance = nativeBalance,
                txCount = txCount,
            ),
        )
    }

    private fun callJsonRpc(endpoint: String, method: String, params: List<String>): String? {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("method", method)
            .put("params", org.json.JSONArray(params))
            .put("id", 1)
            .toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("RPC HTTP ${response.code}")
            }

            val json = JSONObject(body)
            if (json.has("error")) {
                val error = json.optJSONObject("error")
                throw IllegalStateException(error?.optString("message").orEmpty().ifBlank { "RPC error" })
            }
            return json.optString("result").takeIf { it.isNotBlank() }
        }
    }

    private fun fetchBitcoinBalance(address: String): Resource<WalletBalance> {
        val request = Request.Builder()
            .url("https://blockstream.info/api/address/$address")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Resource.Error("Bitcoin API HTTP ${response.code}")
            }

            val json = JSONObject(body)
            val chainStats = json.getJSONObject("chain_stats")
            val mempoolStats = json.optJSONObject("mempool_stats") ?: JSONObject()

            val funded = chainStats.optLong("funded_txo_sum") + mempoolStats.optLong("funded_txo_sum")
            val spent = chainStats.optLong("spent_txo_sum") + mempoolStats.optLong("spent_txo_sum")
            val txCount = chainStats.optLong("tx_count") + mempoolStats.optLong("tx_count")
            val balanceBtc = (funded - spent).toDouble() / SATOSHIS_PER_BTC

            return Resource.Success(
                WalletBalance(
                    nativeBalance = balanceBtc,
                    txCount = txCount,
                ),
            )
        }
    }

    private fun fetchBitcoinTransactions(address: String): Resource<List<WalletTransaction>> {
        val request = Request.Builder()
            .url("https://blockstream.info/api/address/$address/txs")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Resource.Error("Bitcoin history API HTTP ${response.code}")
            }

            val txs = JSONArray(body)
            val history = buildList {
                for (index in 0 until txs.length()) {
                    val tx = txs.getJSONObject(index)
                    val txId = tx.optString("txid")
                    val timestampSeconds = tx.optJSONObject("status")?.optLong("block_time") ?: 0L
                    val amountSats = calculateBitcoinAddressDelta(tx, address)
                    if (amountSats == 0L) continue

                    add(
                        WalletTransaction(
                            id = txId,
                            type = if (amountSats > 0L) WalletTransactionType.RECEIVED else WalletTransactionType.SENT,
                            symbol = "BTC",
                            amount = kotlin.math.abs(amountSats).toDouble() / SATOSHIS_PER_BTC,
                            valueUsd = null,
                            counterparty = findBitcoinCounterparty(tx, address, amountSats),
                            timestamp = if (timestampSeconds > 0L) timestampSeconds * 1000L else System.currentTimeMillis(),
                            networkLabel = WalletChain.BITCOIN.label,
                        ),
                    )
                }
            }

            return Resource.Success(history)
        }
    }

    private fun calculateBitcoinAddressDelta(tx: JSONObject, address: String): Long {
        var received = 0L
        var spent = 0L

        val outputs = tx.optJSONArray("vout") ?: JSONArray()
        for (index in 0 until outputs.length()) {
            val output = outputs.getJSONObject(index)
            if (output.optString("scriptpubkey_address") == address) {
                received += output.optLong("value")
            }
        }

        val inputs = tx.optJSONArray("vin") ?: JSONArray()
        for (index in 0 until inputs.length()) {
            val previousOutput = inputs.getJSONObject(index).optJSONObject("prevout") ?: continue
            if (previousOutput.optString("scriptpubkey_address") == address) {
                spent += previousOutput.optLong("value")
            }
        }

        return received - spent
    }

    private fun findBitcoinCounterparty(tx: JSONObject, address: String, amountSats: Long): String? {
        val arrayName = if (amountSats > 0L) "vin" else "vout"
        val entries = tx.optJSONArray(arrayName) ?: return null

        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val candidate = if (amountSats > 0L) {
                entry.optJSONObject("prevout")?.optString("scriptpubkey_address")
            } else {
                entry.optString("scriptpubkey_address")
            }
            if (!candidate.isNullOrBlank() && candidate != address) return candidate
        }
        return null
    }

    private fun parseHexInteger(value: String): BigInteger {
        val normalized = value.removePrefix("0x").ifBlank { "0" }
        return BigInteger(normalized, 16)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val EVM_NATIVE_DECIMALS = 18
        const val SATOSHIS_PER_BTC = 100_000_000.0
    }
}
