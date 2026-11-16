package com.cimdriver.app.service

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.WorkDay
import com.cimdriver.app.util.WorkHoursCalculator
import com.cimdriver.app.util.WorkHoursNormalizer
import com.cimdriver.app.util.WorkHoursUtil
import com.cimdriver.app.worker.WifiDisconnectBufferWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WifiWorkHoursManager {
    private const val TAG = "WifiWorkHoursManager"
    const val PREFS_NAME = "wifi_work_hours_prefs"
    const val PREF_LAST_DISCONNECT_TIME = "last_disconnect_time"
    const val PREF_LAST_DISCONNECT_SSID = "last_disconnect_ssid"
    const val PREF_ACTIVE_WIFI_WORK_SSID = "active_wifi_work_ssid"
    const val WORK_NAME_DISCONNECT_BUFFER = "wifi_disconnect_buffer_worker"

    fun getCurrentWifiSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid
            if (ssid != null && ssid != "<unknown ssid>" && ssid.isNotBlank()) {
                cleanSsid(ssid)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading wifi SSID", e)
            null
        }
    }

    fun cleanSsid(ssid: String): String {
        return ssid.trim().removeSurrounding("\"").trim()
    }

    fun handleWifiConnected(context: Context, rawSsid: String) {
        val clean = cleanSsid(rawSsid)
        if (clean.isBlank() || clean == "<unknown ssid>") return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // If auto tracking service is actively tracking a trip, car tracking takes precedence
                if (TrackingStatusStore.status.value.isActive) {
                    Log.d(TAG, "Car tracking is active, skipping Wi-Fi presence check")
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                val settings = db.settingsDao().getSettingsSync()
                if (settings?.wifiWorkHoursEnabled == false) {
                    Log.d(TAG, "Wi-Fi work hours registration disabled in settings")
                    return@launch
                }

                // Check if this SSID matches a saved work location
                val addresses = db.savedAddressDao().getAddressesWithWifiSync()
                val matchedAddress = addresses.firstOrNull { 
                    it.isWorkLocation && cleanSsid(it.wifiSsid ?: "").equals(clean, ignoreCase = true) 
                } ?: return@launch

                Log.d(TAG, "Matched office Wi-Fi '$clean' for address: ${matchedAddress.label}")

                // Cancel any pending disconnect buffer worker
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_DISCONNECT_BUFFER)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastDisconnectTime = prefs.getLong(PREF_LAST_DISCONNECT_TIME, 0L)
                val now = System.currentTimeMillis()
                val startOfDay = getStartOfDay(now)

                val workDayDao = db.workDayDao()
                val existingWorkDay = workDayDao.getWorkDayByDate(startOfDay)

                if (existingWorkDay == null) {
                    // New WorkDay started via Wi-Fi presence!
                    val rawDay = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
                    val dayOfWeek = if (rawDay == Calendar.SUNDAY) 7 else rawDay - 1
                    val workDaysConfig = WorkHoursUtil.parseWorkHoursString(
                        settings?.workDays ?: "1,2,3,4,5",
                        settings?.workStartTime ?: "08:00",
                        settings?.workEndTime ?: "17:00"
                    )
                    val todayConfig = workDaysConfig.find { it.dayOfWeek == dayOfWeek }
                    val configuredStart = todayConfig?.startTime ?: settings?.workStartTime ?: "08:00"
                    val toleranceMins = (settings?.workHoursToleranceMinutes ?: 30).toLong()

                    val normalizedArrival = WorkHoursNormalizer.normalize(
                        now,
                        startOfDay,
                        configuredStart,
                        toleranceMins
                    )
                    val defaultBreak = settings?.defaultBreakMinutes ?: 30

                    val newWorkDay = WorkDay(
                        date = startOfDay,
                        firstDepartureTime = now,
                        arrivalTime = now,
                        departureTime = null,
                        lastArrivalTime = null,
                        roundedArrivalTime = normalizedArrival,
                        roundedDepartureTime = null,
                        breakMinutes = defaultBreak,
                        workLocationLabel = matchedAddress.label.ifBlank { matchedAddress.address },
                        projectCode = matchedAddress.projectCode,
                        projectCodeId = matchedAddress.defaultProjectCodeId,
                        status = "IN_PROGRESS",
                        registrationSource = "WIFI"
                    )
                    workDayDao.insertWorkDay(newWorkDay)
                    prefs.edit().putString(PREF_ACTIVE_WIFI_WORK_SSID, clean).apply()
                    TrackingNotificationHelper(context).sendWorkDayStartedNotification()
                    Log.d(TAG, "Started new Wi-Fi work day on $clean")
                } else if (existingWorkDay.registrationSource == "WIFI") {
                    // Existing Wi-Fi workday - check for midday lunch break reconnection!
                    if (lastDisconnectTime > 0L && now > lastDisconnectTime) {
                        val awayMinutes = ((now - lastDisconnectTime) / 60000L).toInt()
                        prefs.edit().remove(PREF_LAST_DISCONNECT_TIME).apply()

                        // If user was away between 10 and 120 minutes, this was a lunch/break outside!
                        val updatedBreak = if (awayMinutes in 10..120) {
                            maxOf(existingWorkDay.breakMinutes, awayMinutes)
                        } else {
                            existingWorkDay.breakMinutes
                        }

                        val updatedWorkDay = existingWorkDay.copy(
                            departureTime = null,
                            roundedDepartureTime = null,
                            lastArrivalTime = now,
                            breakMinutes = updatedBreak,
                            status = "IN_PROGRESS"
                        )
                        workDayDao.updateWorkDay(updatedWorkDay)
                        Log.d(TAG, "Resumed Wi-Fi work day after lunch/break of $awayMinutes min")
                    } else if (existingWorkDay.status != "APPROVED" && existingWorkDay.status != "DONE") {
                        // Reconnected - ensure status is IN_PROGRESS
                        val updated = existingWorkDay.copy(
                            departureTime = null,
                            roundedDepartureTime = null,
                            lastArrivalTime = now,
                            status = "IN_PROGRESS"
                        )
                        workDayDao.updateWorkDay(updated)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Wi-Fi connect", e)
            }
        }
    }

    fun handleWifiDisconnected(context: Context, rawSsid: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (TrackingStatusStore.status.value.isActive) return@launch

                val db = AppDatabase.getDatabase(context)
                val settings = db.settingsDao().getSettingsSync()
                if (settings?.wifiWorkHoursEnabled == false) return@launch

                val now = System.currentTimeMillis()
                val startOfDay = getStartOfDay(now)
                val workDay = db.workDayDao().getWorkDayByDate(startOfDay)

                // Only handle if today's workday was registered via Wi-Fi and is currently in progress
                if (workDay == null || workDay.registrationSource != "WIFI" || workDay.status != "IN_PROGRESS") {
                    return@launch
                }

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(PREF_LAST_DISCONNECT_TIME, now)
                    .putString(PREF_LAST_DISCONNECT_SSID, rawSsid ?: "")
                    .apply()

                // Schedule buffer worker (e.g. 15 minutes)
                val bufferMinutes = (settings?.wifiDisconnectBufferMinutes ?: 15).coerceAtLeast(5).toLong()
                val workRequest = OneTimeWorkRequestBuilder<WifiDisconnectBufferWorker>()
                    .setInitialDelay(bufferMinutes, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_DISCONNECT_BUFFER,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                Log.d(TAG, "Scheduled Wi-Fi disconnect buffer worker in $bufferMinutes minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Wi-Fi disconnect", e)
            }
        }
    }

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
