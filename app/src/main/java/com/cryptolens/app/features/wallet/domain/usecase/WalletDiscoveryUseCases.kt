package com.cryptolens.app.features.wallet.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
import com.cryptolens.app.features.wallet.domain.model.WalletAsset
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem
import com.cryptolens.app.features.wallet.domain.repository.WalletDiscoveryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetTrendingWalletsUseCase @Inject constructor(
    private val repository: WalletDiscoveryRepository,
    private val getTopCoins: GetTopCoinsUseCase,
) {
    suspend operator fun invoke(): Resource<List<TrendingWallet>> {
        val coins = loadCoins()
        return repository.getTrendingWallets(coins.associate { it.id to it.currentPrice })
    }

    private suspend fun loadCoins(): List<Coin> =
        when (val result = getTopCoins(currency = "usd", perPage = 250, page = 1).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data
            else -> emptyList()
        }
}

class GetTrendingWalletDetailUseCase @Inject constructor(
    private val repository: WalletDiscoveryRepository,
    private val getTopCoins: GetTopCoinsUseCase,
) {
    suspend operator fun invoke(chainName: String, address: String): Resource<TrendingWalletDetail> {
        val coins = loadCoins()
        return repository.getTrendingWalletDetail(
            chainName = chainName,
            address = address,
            nativePricesUsd = coins.associate { it.id to it.currentPrice },
            pricesBySymbolUsd = coins.associate { it.symbol.uppercase() to it.currentPrice },
            changesBySymbol24h = coins.associate { it.symbol.uppercase() to it.priceChangePercent24h },
        )
    }

    private suspend fun loadCoins(): List<Coin> =
        when (val result = getTopCoins(currency = "usd", perPage = 250, page = 1).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data
            else -> emptyList()
        }
}

class GetConnectedWalletPortfoliosUseCase @Inject constructor(
    private val repository: WalletDiscoveryRepository,
    private val getTopCoins: GetTopCoinsUseCase,
) {
    suspend operator fun invoke(wallets: List<WalletWatchItem>): Map<String, Resource<TrendingWalletDetail>> {
        if (wallets.isEmpty()) return emptyMap()

        val coins = loadCoins()
        val nativePricesUsd = coins.associate { it.id to it.currentPrice }
        val pricesBySymbolUsd = coins.associate { it.symbol.uppercase() to it.currentPrice }
        val changesBySymbol24h = coins.associate { it.symbol.uppercase() to it.priceChangePercent24h }
        val coinIdsBySymbol = coins
            .groupBy { it.symbol.uppercase() }
            .mapValues { (_, matches) -> matches.minByOrNull { it.rank }?.id }

        return wallets.associate { wallet ->
            val result = repository.getTrendingWalletDetail(
                chainName = wallet.chain.name,
                address = wallet.address,
                nativePricesUsd = nativePricesUsd,
                pricesBySymbolUsd = pricesBySymbolUsd,
                changesBySymbol24h = changesBySymbol24h,
            ).mapSuccess { detail ->
                detail.copy(
                    wallet = detail.wallet.copy(label = wallet.label),
                    assets = detail.assets.map { asset ->
                        asset.withResolvedCoinId(coinIdsBySymbol)
                    },
                )
            }
            wallet.id to result
        }
    }

    private fun WalletAsset.withResolvedCoinId(coinIdsBySymbol: Map<String, String?>): WalletAsset =
        if (coinId != null) {
            this
        } else {
            copy(coinId = coinIdsBySymbol[symbol.uppercase()])
        }

    private fun Resource<TrendingWalletDetail>.mapSuccess(
        transform: (TrendingWalletDetail) -> TrendingWalletDetail,
    ): Resource<TrendingWalletDetail> =
        when (this) {
            is Resource.Success -> Resource.Success(transform(data))
            is Resource.Error -> this
            Resource.Loading -> Resource.Loading
        }

    private suspend fun loadCoins(): List<Coin> =
        when (val result = getTopCoins(currency = "usd", perPage = 250, page = 1).first { it !is Resource.Loading }) {
            is Resource.Success -> result.data
            else -> emptyList()
        }
}
