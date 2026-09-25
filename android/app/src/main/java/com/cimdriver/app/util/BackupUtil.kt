package com.cimdriver.app.util

import android.content.Context
import android.net.Uri
import com.cimdriver.app.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtil {

    private const val DB_NAME = AppDatabase.DATABASE_NAME
    private val allowedBackupFiles = setOf(DB_NAME, "$DB_NAME-wal", "$DB_NAME-shm")
    
    fun createBackup(context: Context, uri: Uri): Boolean {
        return try {
            AppDatabase.getDatabase(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = context.getDatabasePath("$DB_NAME-wal")
            val shmFile = context.getDatabasePath("$DB_NAME-shm")
            
            val filesToBackup = listOf(dbFile, walFile, shmFile).filter { it.exists() }
            if (filesToBackup.isEmpty()) return false
            
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
            val zos = ZipOutputStream(outputStream)
            
            for (file in filesToBackup) {
                val fis = FileInputStream(file)
                zos.putNextEntry(ZipEntry(file.name))
                fis.copyTo(zos)
                zos.closeEntry()
                fis.close()
            }
            
            zos.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun restoreBackup(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val zis = ZipInputStream(inputStream)
            var restoredDatabase = false
            val restoredEntries = mutableSetOf<String>()

            AppDatabase.closeDatabase()
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFileName = when (entry.name) {
                    "viarit_database" -> DB_NAME
                    "viarit_database-wal" -> "$DB_NAME-wal"
                    "viarit_database-shm" -> "$DB_NAME-shm"
                    else -> entry.name
                }

                if (entry.isDirectory || targetFileName !in allowedBackupFiles) {
                    zis.close()
                    return false
                }
                if (!restoredEntries.add(targetFileName)) {
                    zis.close()
                    return false
                }
                val file = context.getDatabasePath(targetFileName)
                file.parentFile?.mkdirs()
                
                val fos = FileOutputStream(file)
                zis.copyTo(fos)
                fos.close()
                zis.closeEntry()
                if (targetFileName == DB_NAME) restoredDatabase = true
                
                entry = zis.nextEntry
            }
            zis.close()
            if (!restoredDatabase) return false

            if (DB_NAME + "-wal" !in restoredEntries) {
                context.getDatabasePath(DB_NAME + "-wal").delete()
            }
            if (DB_NAME + "-shm" !in restoredEntries) {
                context.getDatabasePath(DB_NAME + "-shm").delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
