package com.cimdriver.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.service.TrackingNotificationHelper
import com.cimdriver.app.service.WifiWorkHoursManager
import com.cimdriver.app.util.WorkHoursCalculator
import com.cimdriver.app.util.WorkHoursNormalizer
import com.cimdriver.app.util.WorkHoursUtil
import java.util.Calendar

class WifiDisconnectBufferWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val db = AppDatabase.getDatabase(context)

            // Check if phone has reconnected to office Wi-Fi in the meantime
            val currentSsid = WifiWorkHoursManager.getCurrentWifiSsid(context)
            if (currentSsid != null) {
                val workAddresses = db.savedAddressDao().getAddressesWithWifiSync()
                val isReconnected = workAddresses.any {
                    it.isWorkLocation && WifiWorkHoursManager.cleanSsid(it.wifiSsid ?: "").equals(currentSsid, ignoreCase = true)
                }
                if (isReconnected) {
                    Log.d("WifiDisconnectWorker", "Reconnected to office Wi-Fi '$currentSsid', canceling departure")
                    return Result.success()
                }
            }

            // Still disconnected! Retrieve recorded disconnect timestamp
            val prefs = context.getSharedPreferences(WifiWorkHoursManager.PREFS_NAME, Context.MODE_PRIVATE)
            val disconnectTime = prefs.getLong(WifiWorkHoursManager.PREF_LAST_DISCONNECT_TIME, System.currentTimeMillis())
            val startOfDay = WifiWorkHoursManager.getStartOfDay(disconnectTime)

            val workDay = db.workDayDao().getWorkDayByDate(startOfDay)
            if (workDay == null || workDay.registrationSource != "WIFI" || workDay.status != "IN_PROGRESS") {
                return Result.success()
            }

            val settings = db.settingsDao().getSettingsSync()
            val cal = Calendar.getInstance().apply { timeInMillis = disconnectTime }
            val rawDay = cal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (rawDay == Calendar.SUNDAY) 7 else rawDay - 1
            val workDaysConfig = WorkHoursUtil.parseWorkHoursString(
                settings?.workDays ?: "1,2,3,4,5",
                settings?.workStartTime ?: "08:00",
                settings?.workEndTime ?: "17:00"
            )
            val todayConfig = workDaysConfig.find { it.dayOfWeek == dayOfWeek }
            val configuredEnd = todayConfig?.endTime ?: settings?.workEndTime ?: "17:00"
            val toleranceMins = (settings?.workHoursToleranceMinutes ?: 30).toLong()

            val normalizedDeparture = WorkHoursNormalizer.normalize(
                disconnectTime,
                startOfDay,
                configuredEnd,
                toleranceMins
            )
            val effectiveArrival = workDay.roundedArrivalTime ?: workDay.arrivalTime
            val effectiveDeparture = normalizedDeparture
            val tolerance = settings?.workHoursToleranceMinutes ?: 30
            val effectiveBreak = WorkHoursCalculator.effectiveBreakMinutes(
                effectiveArrival,
                effectiveDeparture,
                workDay.breakMinutes,
                tolerance
            )

            val updatedWorkDay = workDay.copy(
                departureTime = disconnectTime,
                roundedDepartureTime = normalizedDeparture,
                breakMinutes = effectiveBreak,
                status = "TO_REVIEW"
            )
            db.workDayDao().updateWorkDay(updatedWorkDay)

            TrackingNotificationHelper(context).sendWorkDayCompletionNotification(updatedWorkDay.id)
            Log.d("WifiDisconnectWorker", "Finalized Wi-Fi work day: $effectiveArrival -> $effectiveDeparture (break: $effectiveBreak min)")

            Result.success()
        } catch (e: Exception) {
            Log.e("WifiDisconnectWorker", "Error in disconnect buffer worker", e)
            Result.retry()
        }
    }
}

