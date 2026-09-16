package com.cimdriver.app.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.util.ExportUtil
import java.io.File

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            
            val settings = db.settingsDao().getSettingsSync()
            if (settings?.backupReminder != true) {
                return Result.success()
            }
            
            val trips = db.tripDao().getAllTripsSync()
            if (trips.isEmpty()) {
                return Result.success()
            }
            
            val rules = db.classificationRuleDao().getAllRulesSync()
            val addresses = db.savedAddressDao().getAllAddressesSync()
            
            // Backup to App-specific Documents/CIMDriver
            val documentsDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val cimDriverDir = File(documentsDir, "CIMDriver/Backups")
            if (!cimDriverDir.exists()) {
                cimDriverDir.mkdirs()
            }
            
            val backupFile = File(cimDriverDir, "cimdriver_backup_${System.currentTimeMillis()}.csv")
            val uri = android.net.Uri.fromFile(backupFile)
            
            ExportUtil.exportToCsv(applicationContext, uri, trips, addresses, rules)
            
            Log.d("BackupWorker", "Backup created successfully at ${backupFile.absolutePath}")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            Result.retry()
        }
    }
}
