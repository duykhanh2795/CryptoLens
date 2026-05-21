package com.cryptolens.app.features.wallet.domain.model

data class ConnectedWalletAccount(
    val address: String,
    val chain: WalletChain,
    val walletName: String,
    val sessionTopic: String,
)

sealed class WalletConnectionEvent {
    data class OpenWallet(
        val pairingUri: String,
        val walletDeepLink: String,
    ) : WalletConnectionEvent()

    data class Connected(val account: ConnectedWalletAccount) : WalletConnectionEvent()

    data class Error(val message: String) : WalletConnectionEvent()
}
