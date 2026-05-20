package com.cryptolens.app.features.wallet.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.TrendingWallet
import com.cryptolens.app.features.wallet.domain.model.TrendingWalletDetail

interface WalletDiscoveryRepository {
    suspend fun getTrendingWallets(nativePricesUsd: Map<String, Double>): Resource<List<TrendingWallet>>

    suspend fun getTrendingWalletDetail(
        chainName: String,
        address: String,
        nativePricesUsd: Map<String, Double>,
        pricesBySymbolUsd: Map<String, Double>,
        changesBySymbol24h: Map<String, Double>,
    ): Resource<TrendingWalletDetail>
}
