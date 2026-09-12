package com.cryptolens.app.core.domain.util

import com.cryptolens.app.features.settings.domain.model.AppCurrency
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.model.PriceDisplayFormat
import kotlin.math.abs

private val usdRates = mapOf(
    AppCurrency.USD to 1.0,
    AppCurrency.VND to 25_450.0,
    AppCurrency.EUR to 0.92,
    AppCurrency.GBP to 0.78,
    AppCurrency.JPY to 156.0,
    AppCurrency.KRW to 1_375.0,
    AppCurrency.SGD to 1.35,
    AppCurrency.BTC to 1.0 / 73_500.0,
)

fun AppSettings.convertUsd(valueUsd: Double): Double =
    valueUsd * (usdRates[currency] ?: 1.0)

fun AppSettings.formatMoney(
    valueUsd: Double,
    compact: Boolean = priceFormat == PriceDisplayFormat.COMPACT,
): String {
    val value = convertUsd(valueUsd)
    val number = if (compact) {
        value.formatCompactAmount(currency)
    } else {
        value.formatFullAmount(currency)
    }
    return when (currency) {
        AppCurrency.USD,
        AppCurrency.EUR,
        AppCurrency.GBP,
        AppCurrency.JPY,
        AppCurrency.KRW,
        AppCurrency.BTC,
        -> "${currency.symbol}$number"
        AppCurrency.VND -> "$number ${currency.symbol}"
        AppCurrency.SGD -> "${currency.symbol}$number"
    }
}

fun AppSettings.formatSignedMoney(
    valueUsd: Double,
    compact: Boolean = priceFormat == PriceDisplayFormat.COMPACT,
): String {
    val sign = if (valueUsd >= 0.0) "+" else "-"
    return "$sign${formatMoney(abs(valueUsd), compact)}"
}

fun AppSettings.formatMoneyOrUnavailable(
    valueUsd: Double,
    compact: Boolean = priceFormat == PriceDisplayFormat.COMPACT,
): String = if (valueUsd > 0.0) formatMoney(valueUsd, compact) else "N/A"

private fun Double.formatFullAmount(currency: AppCurrency): String = when (currency) {
    AppCurrency.VND,
    AppCurrency.KRW,
    AppCurrency.JPY,
    -> "%,.0f".format(this)
    AppCurrency.BTC -> "%.8f".format(this)
    else -> when {
        this >= 1_000.0 -> "%,.2f".format(this)
        this >= 1.0 -> "%,.4f".format(this)
        this >= 0.01 -> "%.6f".format(this)
        else -> "%.8f".format(this)
    }
}

private fun Double.formatCompactAmount(currency: AppCurrency): String = when {
    currency == AppCurrency.BTC -> "%.8f".format(this)
    this >= 1_000_000_000_000 -> "%.2fT".format(this / 1_000_000_000_000)
    this >= 1_000_000_000 -> "%.2fB".format(this / 1_000_000_000)
    this >= 1_000_000 -> "%.2fM".format(this / 1_000_000)
    this >= 1_000 -> "%.2fK".format(this / 1_000)
    else -> formatFullAmount(currency)
}
