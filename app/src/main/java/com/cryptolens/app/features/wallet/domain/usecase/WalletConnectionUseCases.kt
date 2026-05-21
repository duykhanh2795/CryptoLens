package com.cryptolens.app.features.wallet.domain.usecase

import com.cryptolens.app.features.wallet.domain.repository.WalletConnectionRepository
import javax.inject.Inject

class ConnectMetaMaskUseCase @Inject constructor(
    private val repository: WalletConnectionRepository,
) {
    operator fun invoke() = repository.connectMetaMask()
}
