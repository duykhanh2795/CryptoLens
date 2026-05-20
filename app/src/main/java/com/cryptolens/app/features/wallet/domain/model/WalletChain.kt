package com.cryptolens.app.features.wallet.domain.model

enum class WalletChain(
    val label: String,
    val nativeSymbol: String,
    val priceCoinIds: List<String>,
    val explorerBaseUrl: String,
) {
    ETHEREUM(
        label = "Ethereum",
        nativeSymbol = "ETH",
        priceCoinIds = listOf("ethereum"),
        explorerBaseUrl = "https://etherscan.io/address/",
    ),
    BNB_CHAIN(
        label = "BNB Chain",
        nativeSymbol = "BNB",
        priceCoinIds = listOf("binancecoin"),
        explorerBaseUrl = "https://bscscan.com/address/",
    ),
    POLYGON(
        label = "Polygon",
        nativeSymbol = "POL",
        priceCoinIds = listOf("polygon-ecosystem-token", "matic-network"),
        explorerBaseUrl = "https://polygonscan.com/address/",
    ),
    BITCOIN(
        label = "Bitcoin",
        nativeSymbol = "BTC",
        priceCoinIds = listOf("bitcoin"),
        explorerBaseUrl = "https://www.blockchain.com/explorer/addresses/btc/",
    );

    companion object {
        fun fromStored(value: String): WalletChain? =
            entries.firstOrNull { it.name == value }
    }
}
