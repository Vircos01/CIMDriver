package com.cimdriver.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "CIMDriver app started after boot or update. Checking for active trips...")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val recoveryManager = TrackingRecoveryManager(context)
                    val activeTrip = recoveryManager.getTripToRecover()
                    
                    if (activeTrip != null) {
                        Log.w("BootReceiver", "Found incomplete trip ${activeTrip.id}, attempting recovery.")
                        if (recoveryManager.shouldAttemptRecovery(activeTrip.id)) {
                            recoveryManager.incrementRecoveryAttempt(activeTrip.id)
                            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                                action = "RECOVER_TRACKING"
                                putExtra("TRIP_ID", activeTrip.id)
                            }
                            context.startForegroundService(serviceIntent)
                        }
                    } else {
                        Log.d("BootReceiver", "No active trips found. Ready for Bluetooth events.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error during boot recovery check", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            Log.w("BootReceiver", "Received unexpected intent action: ${intent.action}")
        }
    }
}
