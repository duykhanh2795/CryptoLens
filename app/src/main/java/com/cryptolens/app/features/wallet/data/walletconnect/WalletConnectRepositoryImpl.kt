package com.cryptolens.app.features.wallet.data.walletconnect

import android.net.Uri
import com.cryptolens.app.features.wallet.domain.model.ConnectedWalletAccount
import com.cryptolens.app.features.wallet.domain.model.WalletChain
import com.cryptolens.app.features.wallet.domain.model.WalletConnectionEvent
import com.cryptolens.app.features.wallet.domain.repository.WalletConnectionRepository
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletConnectRepositoryImpl @Inject constructor() : WalletConnectionRepository {
    private val _events = MutableSharedFlow<WalletConnectionEvent>(extraBufferCapacity = 8)
    override val events: Flow<WalletConnectionEvent> = _events.asSharedFlow()

    init {
        if (WalletConnectBootstrap.isConfigured) {
            SignClient.setDappDelegate(DappDelegate())
        }
    }

    override fun connectMetaMask() {
        if (!WalletConnectBootstrap.isConfigured) {
            _events.tryEmit(
                WalletConnectionEvent.Error(
                    "WalletConnect is not configured. Add WALLETCONNECT_PROJECT_ID to local.properties.",
                ),
            )
            return
        }

        val proposal = Sign.Model.Namespace.Proposal(
            chains = EVM_CHAINS,
            methods = READ_ONLY_METHODS,
            events = SESSION_EVENTS,
        )
        val pairing = CoreClient.Pairing.create { error: Core.Model.Error ->
            _events.tryEmit(WalletConnectionEvent.Error(error.throwable.message ?: "Could not create WalletConnect pairing"))
        } ?: run {
            _events.tryEmit(WalletConnectionEvent.Error("Could not create WalletConnect pairing"))
            return
        }
        val params = Sign.Params.Connect(
            namespaces = mapOf(EIP155_NAMESPACE to proposal),
            optionalNamespaces = emptyMap(),
            properties = null,
            scopedProperties = null,
            pairing = pairing,
        )

        SignClient.connect(
            params,
            onSuccess = { returnedUri: String ->
                val uri = returnedUri.ifBlank { pairing.uri }
                _events.tryEmit(
                    WalletConnectionEvent.OpenWallet(
                        pairingUri = uri,
                        walletDeepLink = METAMASK_WC_DEEPLINK + Uri.encode(uri),
                    ),
                )
            },
            onError = { error: Sign.Model.Error ->
                _events.tryEmit(WalletConnectionEvent.Error(error.throwable.message ?: "Could not start MetaMask connection"))
            },
        )
    }

    private inner class DappDelegate : SignClient.DappDelegate {
        override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
            val account = approvedSession.namespaces
                .values
                .flatMap { namespace -> namespace.accounts }
                .firstNotNullOfOrNull { account -> account.toConnectedAccount(approvedSession.topic) }

            if (account != null) {
                _events.tryEmit(WalletConnectionEvent.Connected(account))
            } else {
                _events.tryEmit(WalletConnectionEvent.Error("MetaMask did not return a supported EVM account"))
            }
        }

        override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
            _events.tryEmit(WalletConnectionEvent.Error("MetaMask connection was rejected"))
        }

        override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) = Unit

        override fun onSessionExtend(session: Sign.Model.Session) = Unit

        override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) = Unit

        override fun onSessionEvent(sessionEvent: Sign.Model.Event) = Unit

        override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) = Unit

        override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) = Unit

        override fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Sign.Model.SessionAuthenticateResponse) = Unit

        override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
            _events.tryEmit(WalletConnectionEvent.Error("MetaMask connection request expired"))
        }

        override fun onRequestExpired(request: Sign.Model.ExpiredRequest) = Unit

        override fun onConnectionStateChange(state: Sign.Model.ConnectionState) = Unit

        override fun onError(error: Sign.Model.Error) {
            _events.tryEmit(WalletConnectionEvent.Error(error.throwable.message ?: "WalletConnect error"))
        }
    }

    private fun String.toConnectedAccount(topic: String): ConnectedWalletAccount? {
        val parts = split(":")
        if (parts.size != 3 || parts[0] != EIP155_NAMESPACE) return null
        val chain = when (parts[1]) {
            "1" -> WalletChain.ETHEREUM
            "56" -> WalletChain.BNB_CHAIN
            "137" -> WalletChain.POLYGON
            else -> return null
        }
        val address = parts[2]
        if (!EVM_ADDRESS_REGEX.matches(address)) return null

        return ConnectedWalletAccount(
            address = address,
            chain = chain,
            walletName = "MetaMask",
            sessionTopic = topic,
        )
    }

    private companion object {
        const val EIP155_NAMESPACE = "eip155"
        const val METAMASK_WC_DEEPLINK = "metamask://wc?uri="
        val EVM_CHAINS = listOf("eip155:1", "eip155:56", "eip155:137")
        val READ_ONLY_METHODS = listOf("personal_sign")
        val SESSION_EVENTS = listOf("accountsChanged", "chainChanged")
        val EVM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
    }
}
