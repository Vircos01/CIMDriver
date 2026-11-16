package com.cimdriver.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cimdriver.app.worker.BackupWorker
import com.cimdriver.app.worker.DataLifecycleWorker
import com.cimdriver.app.worker.MonthlyClosingWorker
import org.maplibre.android.MapLibre
import java.util.concurrent.TimeUnit

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CIMDriverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre globally before any MapView is created
        MapLibre.getInstance(this)

        // Enqueue daily backup worker
        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackupWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            backupWorkRequest
        )

        // Enqueue daily data lifecycle worker
        val lifecycleWorkRequest = PeriodicWorkRequestBuilder<DataLifecycleWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DataLifecycleWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            lifecycleWorkRequest
        )

        // Enqueue daily monthly closing worker
        val closingWorkRequest = PeriodicWorkRequestBuilder<MonthlyClosingWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MonthlyClosingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            closingWorkRequest
        )

        // Register Wi-Fi presence network callback
        registerWifiNetworkCallback()
    }

    private fun registerWifiNetworkCallback() {
        try {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
            val request = android.net.NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            cm.registerNetworkCallback(request, object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    val ssid = com.cimdriver.app.service.WifiWorkHoursManager.getCurrentWifiSsid(this@CIMDriverApplication)
                    if (ssid != null) {
                        com.cimdriver.app.service.WifiWorkHoursManager.handleWifiConnected(this@CIMDriverApplication, ssid)
                    }
                }

                override fun onLost(network: android.net.Network) {
                    com.cimdriver.app.service.WifiWorkHoursManager.handleWifiDisconnected(this@CIMDriverApplication, null)
                }
            })
        } catch (e: Exception) {
            android.util.Log.w("CIMDriverApp", "Failed to register Wi-Fi network callback", e)
        }
    }
}
