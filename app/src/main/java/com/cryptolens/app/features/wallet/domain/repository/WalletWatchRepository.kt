package com.cryptolens.app.features.wallet.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem
import kotlinx.coroutines.flow.Flow

interface WalletWatchRepository {
    fun observeWallets(userId: String): Flow<List<WalletWatchItem>>

    suspend fun addWallet(
        userId: String,
        label: String,
        address: String,
        chain: WalletChain,
    ): Resource<Unit>

    suspend fun deleteWallet(id: String, userId: String): Resource<Unit>

    suspend fun refreshWallet(id: String, userId: String): Resource<Unit>

    suspend fun refreshWallets(userId: String): Resource<Unit>
}
