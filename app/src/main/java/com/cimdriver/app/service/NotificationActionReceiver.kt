package com.cimdriver.app.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cimdriver.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles quick-action buttons on trip completion notifications.
 * Users can classify a trip directly from the notification without opening the app.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLASSIFY_TRIP = "com.cimdriver.app.ACTION_CLASSIFY_TRIP"
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
        const val EXTRA_TRIP_TYPE = "EXTRA_TRIP_TYPE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CLASSIFY_TRIP) return

        val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
        val tripType = intent.getStringExtra(EXTRA_TRIP_TYPE) ?: return

        if (tripId == -1L) return

        Log.d("NotificationAction", "Classifying trip $tripId as $tripType")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val tripDao = database.tripDao()
                val trip = tripDao.getTripById(tripId)
                if (trip != null) {
                    tripDao.updateTrip(trip.copy(tripType = tripType, status = "DONE"))
                    Log.d("NotificationAction", "Trip $tripId classified as $tripType")
                    
                    val typeName = when (tripType) {
                        "Business Meeting" -> "Zakelijk"
                        "Home To Work" -> "Woon-werk"
                        else -> "Privé"
                    }
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Rit opgeslagen als $typeName", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                // Dismiss the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(tripId.toInt())
            } catch (e: Exception) {
                Log.e("NotificationAction", "Failed to classify trip", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
