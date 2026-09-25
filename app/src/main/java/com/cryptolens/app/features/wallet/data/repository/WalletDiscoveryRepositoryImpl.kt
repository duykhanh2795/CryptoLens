package com.cryptolens.app.features.wallet.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.data.remote.AlchemyWalletIndexerClient
import com.cryptolens.app.features.wallet.data.remote.MoralisWalletIndexerClient
import com.cryptolens.app.features.wallet.data.remote.WalletBalanceClient
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletTransaction
import com.cryptolens.app.features.wallet.domain.repository.WalletDiscoveryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class WalletDiscoveryRepositoryImpl @Inject constructor(
    private val balanceClient: WalletBalanceClient,
    private val alchemyIndexer: AlchemyWalletIndexerClient,
    private val moralisIndexer: MoralisWalletIndexerClient,
) : WalletDiscoveryRepository {

    override suspend fun getTrendingWallets(nativePricesUsd: Map<String, Double>): Resource<List<TrendingWallet>> =
        try {
            val wallets = coroutineScope {
                TRENDING_WALLETS.mapIndexed { index, seed ->
                    async { seed.toTrendingWallet(index, nativePricesUsd) }
                }.awaitAll()
            }
            Resource.Success(
                wallets.sortedByDescending { it.valueUsd ?: 0.0 },
            )
        } catch (e: Exception) {
            Resource.Error("Could not load trending wallets: ${e.message}", e)
        }

    override suspend fun getTrendingWalletDetail(
        chainName: String,
        address: String,
        nativePricesUsd: Map<String, Double>,
        pricesBySymbolUsd: Map<String, Double>,
        changesBySymbol24h: Map<String, Double>,
    ): Resource<TrendingWalletDetail> {
        val chain = WalletChain.fromStored(chainName) ?: return Resource.Error("Unsupported wallet chain")
        val seed = TRENDING_WALLETS.firstOrNull {
            it.chain == chain && it.address.equals(address, ignoreCase = true)
        } ?: TrendingWalletSeed(
            label = "${chain.label} wallet",
            address = address,
            chain = chain,
            changePercent24h = 0.0,
            avatarSeed = abs(address.hashCode()),
        )

        return try {
            val wallet = seed.toTrendingWallet(index = seed.avatarSeed, nativePricesUsd = nativePricesUsd)
            val nativePrice = wallet.nativePrice(nativePricesUsd)
            val nativeAsset = WalletAsset(
                symbol = chain.nativeSymbol,
                name = chain.label,
                quantity = wallet.nativeBalance,
                priceUsd = nativePrice,
                valueUsd = wallet.valueUsd,
                changePercent24h = wallet.changePercent24h,
                chain = chain,
                coinId = chain.priceCoinIds.firstOrNull(),
            )

            val (assetLoad, historyResult) = coroutineScope {
                val assetsRequest = async {
                    loadIndexedAssets(
                        wallet = wallet,
                        changesBySymbol24h = changesBySymbol24h,
                    )
                }
                val historyRequest = async {
                    loadHistory(chain = chain, address = wallet.address)
                }
                assetsRequest.await() to historyRequest.await()
            }
            val indexedAssets = assetLoad.assets
            val indexerErrors = assetLoad.errors
            val history = when (historyResult) {
                is Resource.Success -> historyResult.data.withUsdValues(pricesBySymbolUsd)
                else -> emptyList()
            }
            val note = when {
                chain == WalletChain.BITCOIN -> null
                !alchemyIndexer.isConfigured && !moralisIndexer.isConfigured ->
                    "Add ALCHEMY_API_KEY or MORALIS_API_KEY to local.properties to enable indexed token assets and EVM history."
                indexedAssets.isEmpty() && indexerErrors.isNotEmpty() -> indexerErrors.distinct().joinToString("\n")
                indexedAssets.isEmpty() -> "No non-zero ERC-20 token balances were returned by the indexer for this address."
                history.isEmpty() -> "Assets are indexed. No recent transfer history was returned for this network."
                else -> null
            }

            Resource.Success(
                TrendingWalletDetail(
                    wallet = wallet,
                    assets = (listOf(nativeAsset) + indexedAssets)
                        .sortedByDescending { it.valueUsd ?: 0.0 },
                    history = history,
                    historyNote = note,
                ),
            )
        } catch (e: Exception) {
            Resource.Error("Could not load wallet detail: ${e.message}", e)
        }
    }

    private suspend fun loadIndexedAssets(
        wallet: TrendingWallet,
        changesBySymbol24h: Map<String, Double>,
    ): IndexedAssetLoad {
        if (!wallet.address.startsWith("0x", ignoreCase = true)) return IndexedAssetLoad()

        val errors = mutableListOf<String>()
        val alchemyAssets = when (
            val result = withTimeoutOrNull(INDEXER_TIMEOUT_MS) {
                alchemyIndexer.fetchTokenAssets(
                    address = wallet.address,
                    nativeChangesBySymbol = changesBySymbol24h,
                )
            } ?: Resource.Error("Alchemy token request timed out.")
        ) {
            is Resource.Success -> result.data
            is Resource.Error -> {
                errors += result.message
                emptyList()
            }
            Resource.Loading -> emptyList()
        }

        val moralisAssets = if (moralisIndexer.isConfigured) {
            when (
                val result = withTimeoutOrNull(INDEXER_TIMEOUT_MS) {
                    moralisIndexer.fetchTokenAssets(wallet.address)
                } ?: Resource.Error("Moralis token request timed out.")
            ) {
                is Resource.Success -> result.data
                is Resource.Error -> {
                    errors += result.message
                    emptyList()
                }
                Resource.Loading -> emptyList()
            }
        } else {
            emptyList()
        }

        return IndexedAssetLoad(
            assets = mergeAssets(alchemyAssets + moralisAssets),
            errors = errors,
        )
    }

    private suspend fun loadHistory(
        chain: WalletChain,
        address: String,
    ): Resource<List<WalletTransaction>> =
        withTimeoutOrNull(INDEXER_TIMEOUT_MS) {
            if (chain == WalletChain.BITCOIN) {
                balanceClient.fetchHistory(chain, address)
            } else {
                val moralisHistory = if (moralisIndexer.isConfigured) {
                    moralisIndexer.fetchHistory(address, chain)
                } else {
                    Resource.Error("Moralis is not configured.")
                }
                if (moralisHistory is Resource.Success && moralisHistory.data.isNotEmpty()) {
                    moralisHistory
                } else {
                    alchemyIndexer.fetchTransferHistory(address, chain)
                }
            }
        } ?: Resource.Error("Wallet history request timed out.")

    private suspend fun TrendingWalletSeed.toTrendingWallet(
        index: Int,
        nativePricesUsd: Map<String, Double>,
    ): TrendingWallet {
        val balance = when (val result = balanceClient.fetchBalance(chain, address)) {
            is Resource.Success -> result.data
            else -> null
        }
        val nativePrice = chain.priceCoinIds.firstNotNullOfOrNull { nativePricesUsd[it] }
        val nativeBalance = balance?.nativeBalance ?: 0.0
        return TrendingWallet(
            label = label,
            address = address,
            chain = chain,
            nativeBalance = nativeBalance,
            txCount = balance?.txCount ?: 0L,
            valueUsd = nativePrice?.let { nativeBalance * it },
            changePercent24h = changePercent24h,
            avatarSeed = avatarSeed + index,
        )
    }

    private fun TrendingWallet.nativePrice(nativePricesUsd: Map<String, Double>): Double? =
        chain.priceCoinIds.firstNotNullOfOrNull { nativePricesUsd[it] }

    private fun List<WalletTransaction>.withUsdValues(pricesBySymbolUsd: Map<String, Double>): List<WalletTransaction> =
        map { transaction ->
            val price = pricesBySymbolUsd[transaction.symbol.uppercase()]
            transaction.copy(valueUsd = price?.let { transaction.amount * it })
        }

    private fun mergeAssets(assets: List<WalletAsset>): List<WalletAsset> =
        assets
            .groupBy { "${it.networkLabel}:${it.contractAddress ?: it.symbol}" }
            .map { (_, duplicates) ->
                duplicates.maxByOrNull { asset ->
                    var score = 0
                    if ((asset.valueUsd ?: 0.0) > 0.0) score += 4
                    if ((asset.priceUsd ?: 0.0) > 0.0) score += 2
                    if (asset.name.isNotBlank()) score += 1
                    score
                } ?: duplicates.first()
            }
            .sortedWith(compareByDescending<WalletAsset> { it.valueUsd ?: 0.0 }.thenBy { it.symbol })

    private data class TrendingWalletSeed(
        val label: String,
        val address: String,
        val chain: WalletChain,
        val changePercent24h: Double,
        val avatarSeed: Int,
    )

    private data class IndexedAssetLoad(
        val assets: List<WalletAsset> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    private companion object {
        const val INDEXER_TIMEOUT_MS = 15_000L

        val TRENDING_WALLETS = listOf(
            TrendingWalletSeed(
                label = "Binance BTC Cold",
                address = "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo",
                chain = WalletChain.BITCOIN,
                changePercent24h = 0.02,
                avatarSeed = 11,
            ),
            TrendingWalletSeed(
                label = "Genesis Wallet",
                address = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                chain = WalletChain.BITCOIN,
                changePercent24h = -0.27,
                avatarSeed = 23,
            ),
            TrendingWalletSeed(
                label = "Mt. Gox Trustee",
                address = "1FeexV6bAHb8ybZjqQMjJrcCrHGW9sb6uF",
                chain = WalletChain.BITCOIN,
                changePercent24h = -1.18,
                avatarSeed = 29,
            ),
            TrendingWalletSeed(
                label = "Beacon Deposit",
                address = "0x00000000219ab540356cBB839Cbe05303d7705Fa",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 3.73,
                avatarSeed = 37,
            ),
            TrendingWalletSeed(
                label = "Binance 14",
                address = "0x21a31Ee1afC51d94C2eFcCAa2092aD1028285549",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 0.82,
                avatarSeed = 39,
            ),
            TrendingWalletSeed(
                label = "Binance ETH",
                address = "0x28C6c06298d514Db089934071355E5743bf21d60",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 1.64,
                avatarSeed = 41,
            ),
            TrendingWalletSeed(
                label = "Bitfinex Cold",
                address = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
                chain = WalletChain.ETHEREUM,
                changePercent24h = -0.34,
                avatarSeed = 43,
            ),
            TrendingWalletSeed(
                label = "Kraken",
                address = "0x267be1C1D684F78cb4F6a176C4911b741E4Ffdc0",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 0.45,
                avatarSeed = 47,
            ),
            TrendingWalletSeed(
                label = "Crypto.com",
                address = "0x46340b20830761efd32832A74d7169B29FEB9758",
                chain = WalletChain.ETHEREUM,
                changePercent24h = -0.91,
                avatarSeed = 49,
            ),
            TrendingWalletSeed(
                label = "Vitalik",
                address = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045",
                chain = WalletChain.ETHEREUM,
                changePercent24h = -2.2,
                avatarSeed = 53,
            ),
            TrendingWalletSeed(
                label = "Ethereum Foundation",
                address = "0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 0.18,
                avatarSeed = 59,
            ),
            TrendingWalletSeed(
                label = "Uniswap Treasury",
                address = "0x1a9C8182C09F50C8318d769245beA52c32BE35BC",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 2.41,
                avatarSeed = 61,
            ),
            TrendingWalletSeed(
                label = "Aave Collector",
                address = "0x25F2226B597E8F9514B3F68F00f494cF4f286491",
                chain = WalletChain.ETHEREUM,
                changePercent24h = 1.09,
                avatarSeed = 63,
            ),
            TrendingWalletSeed(
                label = "ENS DAO",
                address = "0xFe89cc7aBB2C4183683ab71653C4CDC9B02D44b7",
                chain = WalletChain.ETHEREUM,
                changePercent24h = -0.58,
                avatarSeed = 65,
            ),
            TrendingWalletSeed(
                label = "Polygon Foundation",
                address = "0x0000000000000000000000000000000000001010",
                chain = WalletChain.POLYGON,
                changePercent24h = 0.96,
                avatarSeed = 67,
            ),
            TrendingWalletSeed(
                label = "Polygon Bridge",
                address = "0xA0c68C638235ee32657e8f720a23ceC1bFc77C77",
                chain = WalletChain.POLYGON,
                changePercent24h = 1.22,
                avatarSeed = 69,
            ),
            TrendingWalletSeed(
                label = "BNB Whale",
                address = "0x8894E0a0c962CB723c1976a4421c95949bE2D4E3",
                chain = WalletChain.BNB_CHAIN,
                changePercent24h = -0.73,
                avatarSeed = 71,
            ),
            TrendingWalletSeed(
                label = "Binance BNB Hot",
                address = "0xF977814e90dA44bFA03b6295A0616a897441aceC",
                chain = WalletChain.BNB_CHAIN,
                changePercent24h = 0.31,
                avatarSeed = 73,
            ),
        )
    }
}
