package com.cimdriver.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cimdriver.app.MainActivity
import com.cimdriver.app.R
import com.cimdriver.app.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyClosingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            // Only trigger in the first 7 days of the month
            if (dayOfMonth > 7) {
                return Result.success()
            }

            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
            val prefs = applicationContext.getSharedPreferences("monthly_closing_prefs", Context.MODE_PRIVATE)
            val lastNotifiedMonth = prefs.getString("last_notified_month", null)

            if (lastNotifiedMonth == currentMonthKey) {
                return Result.success()
            }

            // Get previous month name in Dutch (e.g., "Oktober 2026")
            val prevCal = Calendar.getInstance()
            prevCal.add(Calendar.MONTH, -1)
            val prevMonthFormatter = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("nl-NL"))
            val prevMonthName = prevMonthFormatter.format(prevCal.time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("nl-NL")) else it.toString() }

            // Ensure database has trips in previous month before bothering user
            val db = AppDatabase.getDatabase(applicationContext)
            val allTrips = db.tripDao().getAllTripsSync()
            
            prevCal.set(Calendar.DAY_OF_MONTH, 1)
            prevCal.set(Calendar.HOUR_OF_DAY, 0)
            prevCal.set(Calendar.MINUTE, 0)
            prevCal.set(Calendar.SECOND, 0)
            val prevMonthStart = prevCal.timeInMillis

            val nextCal = Calendar.getInstance()
            nextCal.set(Calendar.DAY_OF_MONTH, 1)
            nextCal.set(Calendar.HOUR_OF_DAY, 0)
            nextCal.set(Calendar.MINUTE, 0)
            nextCal.set(Calendar.SECOND, 0)
            val prevMonthEnd = nextCal.timeInMillis

            val prevTrips = allTrips.filter { it.startTime in prevMonthStart until prevMonthEnd }
            if (prevTrips.isEmpty()) {
                // No trips recorded in previous month, skip notification
                return Result.success()
            }

            // Create notification channel
            val channelId = "monthly_closing"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Fiscale Maandafsluiting",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Herinneringen om de fiscale rittenadministratie per maand af te sluiten en te delen."
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Intent to open MainActivity and navigate to export
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_MONTHLY_CLOSING", true)
                putExtra("MONTHLY_CLOSING_TITLE", prevMonthName)
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                8888,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Fiscale Maandafsluiting")
                .setContentText("De rittenadministratie van $prevMonthName staat klaar. Tik om te controleren en te delen.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("De rittenadministratie van $prevMonthName staat klaar voor fiscale controle en export. Tik om direct te controleren en te delen met je boekhouder."))
                .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(8888, notification)
            prefs.edit().putString("last_notified_month", currentMonthKey).apply()

            Log.d("MonthlyClosingWorker", "Monthly closing notification sent for $prevMonthName")
            Result.success()
        } catch (e: Exception) {
            Log.e("MonthlyClosingWorker", "Error in MonthlyClosingWorker", e)
            Result.retry()
        }
    }
}

