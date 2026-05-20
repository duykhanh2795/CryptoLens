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
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.usecase.GetActiveAlertsUseCase
import com.cryptolens.app.features.alerts.domain.usecase.MarkAlertTriggeredUseCase
import com.cryptolens.app.features.market.domain.util.CryptoSymbolMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val activeAlerts = getActiveAlerts()
            if (activeAlerts.isEmpty()) return@withContext Result.success()

            ensureNotificationChannel()
            val pricesByAlertId = fetchPrices(activeAlerts)

            activeAlerts.forEach { alert ->
                val currentPrice = pricesByAlertId[alert.id] ?: return@forEach
                if (alert.isTriggered(currentPrice)) {
                    val notificationSent = sendNotification(alert, currentPrice)
                    if (notificationSent) {
                        markAlertTriggered(alert.id)
                    }
                }
            }

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }

    private fun fetchPrices(alerts: List<PriceAlert>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val pricesByBinanceSymbol = mutableMapOf<String, Double>()

        alerts.forEach { alert ->
            val binanceSymbol = CryptoSymbolMapper.binanceTickerSymbol(alert.coinId, alert.symbol)
            val price = pricesByBinanceSymbol.getOrPut(binanceSymbol) {
                fetchBinancePrice(binanceSymbol) ?: Double.NaN
            }
            if (!price.isNaN()) {
                result[alert.id] = price
            }
        }

        return result
    }

    private fun fetchBinancePrice(binanceSymbol: String): Double? = runCatching {
        val request = Request.Builder()
            .url("https://api.binance.com/api/v3/ticker/price?symbol=$binanceSymbol")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                null
            } else {
                val body = response.body?.string()
                body?.let { JSONObject(it).optString("price").toDoubleOrNull() }
            }
        }
    }.getOrNull()

    private fun sendNotification(alert: PriceAlert, currentPrice: Double): Boolean {
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

        val body = "${alert.name} ${alert.condition.label.lowercase()} " +
            "$${alert.targetPrice.formatPrice()}. Current: $${currentPrice.formatPrice()}"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${alert.symbol} price alert triggered")
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
}
