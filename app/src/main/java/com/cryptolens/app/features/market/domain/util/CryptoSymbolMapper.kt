package com.cryptolens.app.features.market.domain.util

import com.cryptolens.app.features.market.domain.model.Coin

object CryptoSymbolMapper {
    private val binanceSymbolByCoinId = mapOf(
        "bitcoin" to "BTCUSDT",
        "ethereum" to "ETHUSDT",
        "binancecoin" to "BNBUSDT",
        "solana" to "SOLUSDT",
        "ripple" to "XRPUSDT",
        "dogecoin" to "DOGEUSDT",
        "cardano" to "ADAUSDT",
        "avalanche-2" to "AVAXUSDT",
        "polkadot" to "DOTUSDT",
        "matic-network" to "POLUSDT",
        "polygon-ecosystem-token" to "POLUSDT",
        "chainlink" to "LINKUSDT",
        "litecoin" to "LTCUSDT",
        "uniswap" to "UNIUSDT",
        "cosmos" to "ATOMUSDT",
        "ethereum-classic" to "ETCUSDT",
        "tron" to "TRXUSDT",
        "shiba-inu" to "SHIBUSDT",
        "bitcoin-cash" to "BCHUSDT",
        "stellar" to "XLMUSDT",
        "filecoin" to "FILUSDT",
        "vechain" to "VETUSDT",
        "near" to "NEARUSDT",
        "algorand" to "ALGOUSDT",
        "decentraland" to "MANAUSDT",
        "the-sandbox" to "SANDUSDT",
        "axie-infinity" to "AXSUSDT",
        "apecoin" to "APEUSDT",
        "optimism" to "OPUSDT",
        "arbitrum" to "ARBUSDT",
        "sui" to "SUIUSDT",
        "sei-network" to "SEIUSDT",
        "injective-protocol" to "INJUSDT",
        "celestia" to "TIAUSDT",
        "jupiter-exchange-solana" to "JUPUSDT",
        "pepe" to "PEPEUSDT",
    )

    fun binanceTickerSymbol(coin: Coin): String =
        binanceTickerSymbol(coin.id, coin.symbol)

    fun binanceTickerSymbol(coinId: String, symbol: String): String =
        binanceSymbolByCoinId[coinId] ?: "${symbol.uppercase()}USDT"
}
