package com.cryptolens.app.features.wallet.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase
import com.cryptolens.app.features.market.domain.model.Coin
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail
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
