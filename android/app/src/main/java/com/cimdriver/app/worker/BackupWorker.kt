package com.cimdriver.app.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.util.BackupUtil
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
            
            // Backup to App-specific Documents/CIMDriver/Backups
            val documentsDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val cimDriverDir = File(documentsDir, "CIMDriver/Backups")
            if (!cimDriverDir.exists()) {
                cimDriverDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()

            // 1. Create a restorable SQLite database backup (.zip)
            val dbBackupFile = File(cimDriverDir, "cimdriver_backup_$timestamp.zip")
            val dbSuccess = BackupUtil.createBackupToFile(applicationContext, dbBackupFile)
            if (dbSuccess) {
                Log.d("BackupWorker", "Database backup created successfully at ${dbBackupFile.absolutePath}")
            } else {
                Log.w("BackupWorker", "Database backup creation failed")
            }

            // 2. Create periodic Trips CSV export (.csv) with complete classification context
            val trips = db.tripDao().getAllTripsSync()
            if (trips.isNotEmpty()) {
                val rules = db.classificationRuleDao().getAllRulesSync()
                val addresses = db.savedAddressDao().getAllAddressesSync()
                val vehicles = db.vehicleDao().getAllVehiclesSync()
                
                val csvExportFile = File(cimDriverDir, "cimdriver_export_$timestamp.csv")
                val uri = Uri.fromFile(csvExportFile)
                
                ExportUtil.exportToCsv(
                    context = applicationContext,
                    uri = uri,
                    trips = trips,
                    savedAddresses = addresses,
                    rules = rules,
                    settings = settings,
                    vehicles = vehicles
                )
                
                Log.d("BackupWorker", "CSV export created successfully at ${csvExportFile.absolutePath}")
            }

            // 3. Prune old files to keep storage clean (max 5 each)
            pruneOldFiles(cimDriverDir, "zip", maxToKeep = 5)
            pruneOldFiles(cimDriverDir, "csv", maxToKeep = 5)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup work failed", e)
            Result.retry()
        }
    }

    private fun pruneOldFiles(directory: File, extension: String, maxToKeep: Int) {
        try {
            val files = directory.listFiles { f -> f.extension.equals(extension, ignoreCase = true) }
            if (files != null && files.size > maxToKeep) {
                files.sortedByDescending { it.lastModified() }
                    .drop(maxToKeep)
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w("BackupWorker", "Failed to prune old $extension files", e)
        }
    }
}
