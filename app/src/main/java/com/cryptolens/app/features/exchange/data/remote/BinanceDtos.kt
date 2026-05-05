package com.cryptolens.app.features.exchange.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Account Info (used for validation) ────────────────────────────────────────

@Serializable
data class BinanceAccountDto(
    @SerialName("accountType")  val accountType: String  = "",
    @SerialName("canTrade")     val canTrade: Boolean     = false,
    @SerialName("canWithdraw")  val canWithdraw: Boolean  = false,
    @SerialName("canDeposit")   val canDeposit: Boolean   = false,
    @SerialName("balances")     val balances: List<BinanceBalanceDto> = emptyList(),
)

@Serializable
data class BinanceBalanceDto(
    @SerialName("asset")  val asset: String = "",
    @SerialName("free")   val free: String  = "0",
    @SerialName("locked") val locked: String = "0",
)

// ── Trade History ─────────────────────────────────────────────────────────────

@Serializable
data class BinanceTradeDto(
    @SerialName("id")              val id: Long    = 0,
    @SerialName("orderId")         val orderId: Long = 0,
    @SerialName("symbol")          val symbol: String = "",
    @SerialName("price")           val price: String  = "0",
    @SerialName("qty")             val qty: String    = "0",
    @SerialName("quoteQty")        val quoteQty: String = "0",
    @SerialName("commission")      val commission: String = "0",
    @SerialName("commissionAsset") val commissionAsset: String = "",
    @SerialName("time")            val time: Long  = 0,
    @SerialName("isBuyer")         val isBuyer: Boolean = false,
    @SerialName("isMaker")         val isMaker: Boolean = false,
)

// ── Exchange Info (for symbol→base/quote mapping) ─────────────────────────────

@Serializable
data class BinanceExchangeInfoDto(
    @SerialName("symbols") val symbols: List<BinanceSymbolDto> = emptyList(),
)

@Serializable
data class BinanceSymbolDto(
    @SerialName("symbol")      val symbol: String      = "",
    @SerialName("baseAsset")   val baseAsset: String   = "",
    @SerialName("quoteAsset")  val quoteAsset: String  = "",
    @SerialName("status")      val status: String      = "",
)

// ── Error response ─────────────────────────────────────────────────────────────

@Serializable
data class BinanceErrorDto(
    @SerialName("code") val code: Int    = 0,
    @SerialName("msg")  val msg: String  = "",
)
