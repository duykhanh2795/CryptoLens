package com.cryptolens.app.features.wallet.data.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.data.local.dao.WalletWatchDao
import com.cryptolens.app.features.wallet.data.local.entity.WalletWatchEntity
import com.cryptolens.app.features.wallet.data.remote.WalletBalanceClient
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem
import com.cryptolens.app.features.wallet.domain.repository.WalletWatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletWatchRepositoryImpl @Inject constructor(
    private val dao: WalletWatchDao,
    private val balanceClient: WalletBalanceClient,
) : WalletWatchRepository {

    override fun observeWallets(userId: String): Flow<List<WalletWatchItem>> =
        dao.observeWallets(userId).map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    override suspend fun addWallet(
        userId: String,
        label: String,
        address: String,
        chain: WalletChain,
    ): Resource<Unit> {
        return try {
            val normalizedAddress = normalizeAddress(chain, address)
            if (dao.exists(userId, chain.name, normalizedAddress)) {
                return Resource.Error("This wallet is already in your watchlist")
            }

            val entity = WalletWatchEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                label = label.trim().ifBlank { "${chain.label} wallet" },
                address = normalizedAddress,
                chain = chain.name,
                nativeBalance = 0.0,
                txCount = 0,
                lastSyncedAt = null,
                syncError = null,
                createdAt = System.currentTimeMillis(),
            )
            dao.insert(entity)
            refreshWallet(entity.id, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to add wallet: ${e.message}", e)
        }
    }

    override suspend fun deleteWallet(id: String, userId: String): Resource<Unit> = try {
        dao.delete(id, userId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Failed to delete wallet: ${e.message}", e)
    }

    override suspend fun refreshWallet(id: String, userId: String): Resource<Unit> {
        val wallet = dao.getWallet(id, userId) ?: return Resource.Error("Wallet not found")
        val chain = WalletChain.fromStored(wallet.chain) ?: return Resource.Error("Unsupported wallet chain")

        return when (val result = balanceClient.fetchBalance(chain, wallet.address)) {
            is Resource.Success -> {
                dao.updateSyncSuccess(
                    id = wallet.id,
                    userId = userId,
                    nativeBalance = result.data.nativeBalance,
                    txCount = result.data.txCount,
                    lastSyncedAt = System.currentTimeMillis(),
                )
                Resource.Success(Unit)
            }
            is Resource.Error -> {
                dao.updateSyncError(
                    id = wallet.id,
                    userId = userId,
                    lastSyncedAt = System.currentTimeMillis(),
                    syncError = result.message,
                )
                Resource.Error(result.message, result.throwable)
            }
            Resource.Loading -> Resource.Success(Unit)
        }
    }

    override suspend fun refreshWallets(userId: String): Resource<Unit> {
        val wallets = dao.getWallets(userId)
        var firstError: String? = null

        wallets.forEach { wallet ->
            when (val result = refreshWallet(wallet.id, userId)) {
                is Resource.Error -> if (firstError == null) firstError = result.message
                else -> Unit
            }
        }

        return firstError?.let { Resource.Error(it) } ?: Resource.Success(Unit)
    }

    private fun normalizeAddress(chain: WalletChain, address: String): String {
        val trimmed = address.trim()
        return when (chain) {
            WalletChain.ETHEREUM,
            WalletChain.BNB_CHAIN,
            WalletChain.POLYGON -> trimmed.lowercase()
            WalletChain.BITCOIN -> trimmed
        }
    }
}
