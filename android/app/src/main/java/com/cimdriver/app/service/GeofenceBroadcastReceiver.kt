package com.cimdriver.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cimdriver.app.R
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error receiving geofence event")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences != null) {
                val transitionTypeStr = if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTER" else "EXIT"
                val locationIds = triggeringGeofences.joinToString { it.requestId }
                Log.d("GeofenceReceiver", "Transition $transitionTypeStr for locations: $locationIds")
                
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = AppDatabase.getDatabase(context)
                            val tripDao = database.tripDao()
                            val vehicleDao = database.vehicleDao()
                            val settings = database.settingsDao().getSettings().firstOrNull()
                            
                            val allVehicles = vehicleDao.getAllVehiclesSync()
                            val vehicle = allVehicles.find { it.isDefault } ?: allVehicles.firstOrNull()
                            val vehicleId = vehicle?.id
                            
                            val activeTripId = if (vehicleId != null) {
                                tripDao.getActiveTripForVehicle(vehicleId)?.id
                            } else null
                            
                            val tripId = activeTripId ?: tripDao.insertTrip(
                                Trip(
                                    vehicleId = vehicleId,
                                    startTime = System.currentTimeMillis(),
                                    endTime = null,
                                    startAddress = context.getString(R.string.tracking_retrieving_location),
                                    endAddress = null,
                                    distanceMeters = 0,
                                    tripType = settings?.defaultTripType ?: "PERSONAL",
                                    note = "Geofence start",
                                    status = "ACTIVE",
                                    isManual = false,
                                    odometerStart = vehicle?.odometerCurrent ?: 0,
                                    odometerEnd = null
                                )
                            )
                            
                            val trackingIntent = Intent(context, TrackingService::class.java).apply {
                                action = "START_TRACKING"
                                putExtra("TRIP_ID", tripId)
                            }
                            try {
                                context.startForegroundService(trackingIntent)
                            } catch (e: Exception) {
                                Log.e("GeofenceReceiver", "Failed to start TrackingService", e)
                                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                val notification = androidx.core.app.NotificationCompat.Builder(context, "TrackingServiceChannel")
                                    .setContentTitle("CIMDriver: Automatische rit mislukt")
                                    .setContentText("Controleer achtergrond permissies.")
                                    .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
                                    .build()
                                nm.notify(999, notification)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
