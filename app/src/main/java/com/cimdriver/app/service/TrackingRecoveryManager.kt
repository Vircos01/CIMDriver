package com.cimdriver.app.service

import android.content.Context
import android.util.Log
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip

class TrackingRecoveryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("tracking_recovery_prefs", Context.MODE_PRIVATE)

    private suspend fun getMaxAttempts(): Int {
        val db = AppDatabase.getDatabase(context)
        return db.settingsDao().getSettingsSync()?.maxRecoveryAttempts ?: 3
    }

    suspend fun getTripToRecover(): Trip? {
        val db = AppDatabase.getDatabase(context)
        val activeTrips = db.tripDao().getAllTripsSync().filter { it.status == "ACTIVE" }
        return activeTrips.firstOrNull()
    }

    suspend fun shouldAttemptRecovery(tripId: Long): Boolean {
        val attempts = prefs.getInt("recovery_attempts_$tripId", 0)
        val maxAttempts = getMaxAttempts()
        
        if (attempts >= maxAttempts) {
            Log.e("TrackingRecovery", "Max recovery attempts reached for trip $tripId")
            failTrip(tripId, maxAttempts)
            return false
        }
        
        return true
    }

    fun incrementRecoveryAttempt(tripId: Long) {
        val attempts = prefs.getInt("recovery_attempts_$tripId", 0)
        prefs.edit().putInt("recovery_attempts_$tripId", attempts + 1).apply()
        Log.d("TrackingRecovery", "Recovery attempt ${attempts + 1} for trip $tripId")
    }
    
    fun clearRecoveryAttempts(tripId: Long) {
        prefs.edit().remove("recovery_attempts_$tripId").apply()
    }

    suspend fun failTrip(tripId: Long, maxAttempts: Int = 3) {
        try {
            val db = AppDatabase.getDatabase(context)
            val trip = db.tripDao().getTripById(tripId)
            if (trip != null) {
                db.tripDao().updateTrip(trip.copy(
                    status = "FAILED", 
                    note = (trip.note ?: "") + "\nAutomatisch herstel mislukt na $maxAttempts pogingen."
                ))
            }
            TrackingStatusStore.failed("Rit herstel mislukt na $maxAttempts pogingen.")
        } catch (e: Exception) {
            Log.e("TrackingRecovery", "Error failing trip", e)
        }
    }
}
