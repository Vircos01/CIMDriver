package com.cimdriver.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cimdriver.app.worker.BackupWorker
import com.cimdriver.app.worker.DataLifecycleWorker
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
    }
}
