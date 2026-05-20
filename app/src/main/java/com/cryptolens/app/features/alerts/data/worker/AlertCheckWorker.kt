package com.cryptolens.app.features.alerts.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptolens.app.MainActivity
import com.cryptolens.app.R
import com.cryptolens.app.core.domain.util.formatPrice
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.usecase.GetActiveAlertsUseCase
import com.cryptolens.app.features.alerts.domain.usecase.MarkAlertTriggeredUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val getActiveAlerts: GetActiveAlertsUseCase,
    private val markAlertTriggered: MarkAlertTriggeredUseCase,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "price_alert_check"
        private const val CHANNEL_ID = "price_alerts"
        private const val CHANNEL_NAME = "Price alerts"
        private const val COINGECKO_MARKETS_PAGE_SIZE = 250
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val activeAlerts = getActiveAlerts()
            if (activeAlerts.isEmpty()) return@withContext Result.success()

            ensureNotificationChannel()
            val valuesByAlertId = fetchMarketValues(activeAlerts)

            activeAlerts.forEach { alert ->
                val currentValue = valuesByAlertId[alert.id] ?: return@forEach
                if (alert.isTriggered(currentValue)) {
                    markAlertTriggered(alert, currentValue)
                    sendNotification(alert, currentValue)
                }
            }

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }

    private fun fetchMarketValues(alerts: List<PriceAlert>): Map<String, Double> {
        val coinIds = alerts.map { it.coinId }.distinct()
        if (coinIds.isEmpty()) return emptyMap()

        val marketByCoinId = coinIds
            .chunked(COINGECKO_MARKETS_PAGE_SIZE)
            .flatMap { fetchCoinGeckoMarkets(it).entries }
            .associate { it.key to it.value }
        return alerts.mapNotNull { alert ->
            val market = marketByCoinId[alert.coinId] ?: return@mapNotNull null
            val value = when (alert.type) {
                AlertType.PRICE_LIMIT -> market.price
                AlertType.VOLUME -> market.volume24h
                AlertType.MARKET_CAP -> market.marketCap
            }
            if (value > 0.0) alert.id to value else null
        }.toMap()
    }

    private fun fetchCoinGeckoMarkets(coinIds: List<String>): Map<String, AlertMarketValue> = runCatching {
        val request = Request.Builder()
            .url(
                "https://api.coingecko.com/api/v3/coins/markets" +
                    "?vs_currency=usd&ids=${coinIds.joinToString(",")}&per_page=${coinIds.size.coerceAtLeast(1)}",
            )
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emptyMap()
            } else {
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                buildMap {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val id = item.optString("id")
                        if (id.isBlank()) continue
                        put(
                            id,
                            AlertMarketValue(
                                price = item.optDouble("current_price", 0.0),
                                volume24h = item.optDouble("total_volume", 0.0),
                                marketCap = item.optDouble("market_cap", 0.0),
                            ),
                        )
                    }
                }
            }
        }
    }.getOrDefault(emptyMap())

    private fun sendNotification(alert: PriceAlert, currentValue: Double): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return false
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val target = if (alert.valueType == AlertValueType.PERCENT) {
            "${alert.condition.movementLabel.lowercase()} ${alert.targetPrice.formatPrice()}%"
        } else {
            "${alert.condition.label.lowercase()} ${formatMetric(alert.type, alert.targetPrice)}"
        }
        val body = "${alert.name} ${alert.type.label} $target. Current: ${formatMetric(alert.type, currentValue)}" +
            if (alert.frequency == AlertFrequency.PERSISTENT) " (persistent)" else ""

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${alert.symbol} ${alert.type.label} alert triggered")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(alert.id.hashCode(), notification)
            true
        }.getOrDefault(false)
    }

    private fun formatMetric(type: AlertType, value: Double): String = when (type) {
        AlertType.PRICE_LIMIT -> "$${value.formatPrice()}"
        AlertType.VOLUME -> "$${value.formatCompactUsd()}"
        AlertType.MARKET_CAP -> "$${value.formatCompactUsd()}"
    }

    private fun Double.formatCompactUsd(): String = when {
        this >= 1_000_000_000 -> "%.2fB".format(this / 1_000_000_000)
        this >= 1_000_000 -> "%.2fM".format(this / 1_000_000)
        this >= 1_000 -> "%.2fK".format(this / 1_000)
        else -> "%.2f".format(this)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when crypto price targets are hit"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private data class AlertMarketValue(
        val price: Double,
        val volume24h: Double,
        val marketCap: Double,
    )
}
