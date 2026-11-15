package com.cimdriver.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cimdriver.app.MainActivity
import com.cimdriver.app.R

/**
 * Helper class responsible for creating and dispatching notifications related to
 * trip tracking, lifecycle events, quick classification, workday updates, and fuel drafts.
 */
class TrackingNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val FUEL_CHANNEL_ID = "fuel_prompt_channel"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_PERMISSION_DENIED = 2
        const val WORKDAY_COMPLETION_OFFSET = 10000
        const val WORKDAY_START_ID = 10002
        const val FUEL_DRAFT_OFFSET = 50000
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "CIMDriver Tracking Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val fuelChannel = NotificationChannel(
            FUEL_CHANNEL_ID,
            "CIMDriver Brandstof & Laden",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Meldingen bij aankomst of tussenstop bij een tankstation of laadpaal"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(fuelChannel)
    }

    fun createTrackingNotification(contentText: String, currentTripType: String?): Notification {
        val stopIntent = Intent(context, TrackingService::class.java).apply {
            action = "STOP_TRACKING"
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_TOGGLE_TRIP_TYPE
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            101,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPrivate = isPrivateTrip(currentTripType)
        val toggleLabel = if (isPrivate) "➡️ Naar Zakelijk" else "🔒 Naar Privé"
        val title = if (isPrivate) "CIMDriver Actief (Privé)" else "CIMDriver Actief (Zakelijk)"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, toggleLabel, togglePendingIntent)
            .addAction(0, context.getString(R.string.stop_trip), stopPendingIntent)
            .build()
    }

    fun sendCompletionNotification(tripId: Long, vehicleId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_VEHICLE_ID", vehicleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        fun classifyIntent(tripType: String): PendingIntent {
            val classifyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CLASSIFY_TRIP
                putExtra(NotificationActionReceiver.EXTRA_TRIP_ID, tripId)
                putExtra(NotificationActionReceiver.EXTRA_TRIP_TYPE, tripType)
            }
            return PendingIntent.getBroadcast(
                context,
                kotlin.math.abs((tripId * 10 + tripType.hashCode()).toInt()),
                classifyIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.trip_completed))
            .setContentText(context.getString(R.string.trip_registered_msg))
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✅ Zakelijk", classifyIntent("Business Meeting"))
            .addAction(0, "🏠 Woon-werk", classifyIntent("Home To Work"))
            .addAction(0, "🔒 Privé", classifyIntent("PRIVATE"))
            .build()

        notificationManager.notify(tripId.toInt(), notification)
    }

    fun sendWorkDayCompletionNotification(workDayId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.work_day_registered))
            .setContentText(context.getString(R.string.tap_to_rate_work_hours))
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((workDayId.toInt() + WORKDAY_COMPLETION_OFFSET), notification)
    }

    fun sendWorkDayStartedNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.work_day_started))
            .setContentText(context.getString(R.string.work_day_started_msg))
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(WORKDAY_START_ID, notification)
    }

    fun sendFuelDraftNotification(fillUpId: Long, stationName: String, engineType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_FILL_UP_ID", fillUpId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, (20000 + fillUpId).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val isEV = engineType == "EV"
        val title = if (isEV) "Laden bij $stationName?" else "Tanken bij $stationName?"
        val content = if (isEV) "Tik om kWh en laadkosten op te slaan." else "Tik om liters en bedrag in te vullen."

        val notification = NotificationCompat.Builder(context, FUEL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify((FUEL_DRAFT_OFFSET + fillUpId).toInt(), notification)
    }

    private fun isPrivateTrip(tripType: String?): Boolean {
        if (tripType == null) return false
        return tripType.equals("Privé", ignoreCase = true) ||
               tripType.equals("PRIVATE", ignoreCase = true) ||
               tripType.equals("Prive", ignoreCase = true)
    }
}

