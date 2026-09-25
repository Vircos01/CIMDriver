package com.cimdriver.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cimdriver.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DataLifecycleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            
            val settings = db.settingsDao().getSettingsSync()
            val retentionDays = settings?.locationRetentionDays ?: 365
            
            if (retentionDays > 0) {
                val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
                db.locationPointDao().deletePointsOlderThan(cutoff)
                Log.d("DataLifecycleWorker", "Deleted location points older than $retentionDays days.")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("DataLifecycleWorker", "Lifecycle cleanup failed", e)
            Result.retry()
        }
    }
}
