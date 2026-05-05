package com.cryptolens.app.features.exchange.domain.model

/**
 * Supported exchanges. Easy to extend later.
 */
enum class ExchangeType(
    val displayName: String,
    val logoUrl: String,
    val apiDocsUrl: String,
    val requiresSecret: Boolean,
) {
    BINANCE(
        displayName    = "Binance",
        logoUrl        = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Binance_Logo.svg/200px-Binance_Logo.svg.png",
        apiDocsUrl     = "https://www.binance.com/en/my/settings/api-management",
        requiresSecret = true,
    ),
    OKX(
        displayName    = "OKX",
        logoUrl        = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/OKX_logo.svg/200px-OKX_logo.svg.png",
        apiDocsUrl     = "https://www.okx.com/account/api",
        requiresSecret = true,
    ),
    BYBIT(
        displayName    = "Bybit",
        logoUrl        = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Bybit_logo.svg/200px-Bybit_logo.svg.png",
        apiDocsUrl     = "https://www.bybit.com/app/user/api-management",
        requiresSecret = true,
    ),
}

/**
 * A stored exchange connection (API key pair).
 * Secret is stored encrypted — never exposed in plain text after save.
 */
data class ExchangeConnection(
    val id: String,
    val exchangeType: ExchangeType,
    val label: String,              // user-defined nickname e.g. "My Binance Main"
    val apiKey: String,             // stored encrypted
    val isActive: Boolean,
    val lastSyncAt: Long?,          // epoch millis, null = never synced
    val createdAt: Long,
)

/**
 * Result of a single imported trade from exchange API.
 */
data class ExchangeTrade(
    val orderId: String,
    val symbol: String,             // e.g. "BTCUSDT"
    val coinId: String,             // CoinGecko id e.g. "bitcoin"
    val baseAsset: String,          // e.g. "BTC"
    val quoteAsset: String,         // e.g. "USDT"
    val side: TradeSide,
    val quantity: Double,
    val price: Double,
    val fee: Double,
    val feeAsset: String,
    val timestamp: Long,
)

enum class TradeSide { BUY, SELL }

/**
 * Summary returned after an import sync.
 */
data class SyncResult(
    val exchangeType: ExchangeType,
    val tradesImported: Int,
    val tradesSkipped: Int,
    val syncedAt: Long = System.currentTimeMillis(),
)

/**
 * Validation result when user first enters API credentials.
 */
data class ApiKeyValidation(
    val isValid: Boolean,
    val accountType: String = "",   // e.g. "Spot", "Unified Trading"
    val canRead: Boolean = false,
    val canTrade: Boolean = false,  // should be false for safety
    val errorMessage: String? = null,
)
