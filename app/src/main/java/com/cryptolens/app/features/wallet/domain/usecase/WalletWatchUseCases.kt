package com.cryptolens.app.features.wallet.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletWatchItem
import com.cryptolens.app.features.wallet.domain.repository.WalletWatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWalletWatchlistUseCase @Inject constructor(
    private val repository: WalletWatchRepository,
) {
    operator fun invoke(userId: String): Flow<List<WalletWatchItem>> =
        repository.observeWallets(userId)
}

class AddWalletToWatchlistUseCase @Inject constructor(
    private val repository: WalletWatchRepository,
) {
    suspend operator fun invoke(
        userId: String,
        label: String,
        address: String,
        chain: WalletChain,
    ): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Please log in before tracking wallets")

        val trimmedAddress = address.trim()
        if (trimmedAddress.isBlank()) return Resource.Error("Wallet address cannot be empty")
        if (!chain.isValidAddress(trimmedAddress)) {
            return Resource.Error("Invalid ${chain.label} address")
        }

        val trimmedLabel = label.trim()
        if (trimmedLabel.length > MAX_LABEL_LENGTH) {
            return Resource.Error("Label must be $MAX_LABEL_LENGTH characters or less")
        }

        return repository.addWallet(
            userId = userId,
            label = trimmedLabel,
            address = trimmedAddress,
            chain = chain,
        )
    }

    private fun WalletChain.isValidAddress(address: String): Boolean =
        when (this) {
            WalletChain.ETHEREUM,
            WalletChain.BNB_CHAIN,
            WalletChain.POLYGON -> EVM_ADDRESS_REGEX.matches(address)
            WalletChain.BITCOIN -> BTC_ADDRESS_REGEX.matches(address)
        }

    private companion object {
        const val MAX_LABEL_LENGTH = 40
        val EVM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
        val BTC_ADDRESS_REGEX = Regex("^(bc1|BC1|[13])[a-zA-HJ-NP-Z0-9]{25,90}$")
    }
}

class DeleteWalletWatchItemUseCase @Inject constructor(
    private val repository: WalletWatchRepository,
) {
    suspend operator fun invoke(id: String, userId: String): Resource<Unit> =
        repository.deleteWallet(id, userId)
}

class RefreshWalletWatchlistUseCase @Inject constructor(
    private val repository: WalletWatchRepository,
) {
    suspend operator fun invoke(userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Please log in before refreshing wallets")
        return repository.refreshWallets(userId)
    }

    suspend fun one(id: String, userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Please log in before refreshing wallets")
        return repository.refreshWallet(id, userId)
    }
}
