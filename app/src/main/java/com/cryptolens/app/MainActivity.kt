package com.cryptolens.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.cryptolens.app.features.alerts.data.worker.AlertScheduler
import com.cryptolens.app.features.settings.domain.model.AppSettings
import com.cryptolens.app.features.settings.domain.usecase.ObserveSettingsUseCase
import com.cryptolens.app.navigation.CryptoLensNavHost
import com.cryptolens.app.ui.theme.CryptoLensTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var alertScheduler: AlertScheduler
    @Inject lateinit var observeSettings: ObserveSettingsUseCase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Permission denial is non-blocking; alerts still work in-app.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alertScheduler.schedulePeriodicCheck()
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val settings = observeSettings().collectAsStateWithLifecycle(initialValue = AppSettings())
            CryptoLensTheme(appTheme = settings.value.theme) {
                CryptoLensNavHost()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
