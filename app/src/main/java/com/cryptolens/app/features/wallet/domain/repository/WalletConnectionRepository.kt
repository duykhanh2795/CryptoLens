package com.cryptolens.app.features.wallet.domain.repository

import com.cryptolens.app.features.wallet.domain.model.WalletConnectionEvent
import kotlinx.coroutines.flow.Flow

interface WalletConnectionRepository {
    val events: Flow<WalletConnectionEvent>

    fun connectMetaMask()
}
