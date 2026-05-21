package com.cryptolens.app.features.wallet.data.walletconnect

import android.app.Application
import com.cryptolens.app.BuildConfig
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

object WalletConnectBootstrap {
    private var initialized = false

    val isConfigured: Boolean
        get() = BuildConfig.WALLETCONNECT_PROJECT_ID.isNotBlank()

    fun initialize(application: Application) {
        if (initialized || !isConfigured) return

        val appMetaData = Core.Model.AppMetaData(
            name = "CryptoLens",
            description = "Read-only crypto portfolio tracker",
            url = "https://cryptolens.app",
            icons = listOf("https://cryptolens.app/icon.png"),
            redirect = "cryptolens://wc",
        )

        CoreClient.initialize(
            application = application,
            projectId = BuildConfig.WALLETCONNECT_PROJECT_ID,
            metaData = appMetaData,
            connectionType = ConnectionType.AUTOMATIC,
            onError = {},
        )

        SignClient.initialize(
            init = Sign.Params.Init(core = CoreClient),
            onSuccess = {},
            onError = {},
        )
        initialized = true
    }
}
