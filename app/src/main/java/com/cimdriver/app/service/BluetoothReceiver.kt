package com.cimdriver.app.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != BluetoothDevice.ACTION_ACL_CONNECTED && action != BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            Log.w("BluetoothReceiver", "Received unexpected intent action: $action")
            return
        }
        
        val device: BluetoothDevice? = androidx.core.content.IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        
        if (device == null || device.address == null) return

        val database = AppDatabase.getDatabase(context)
        val btDao = database.bluetoothDeviceDao()
        val tripDao = database.tripDao()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
            // Check if this bluetooth device is linked to any of our vehicles
            val linkedDevice = btDao.getDeviceByMac(device.address)
            
            if (linkedDevice != null) {
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        Log.d("BluetoothReceiver", "Connected to known vehicle: ${linkedDevice.vehicleId}")
                        
                        val vehicleDao = database.vehicleDao()
                        val vehicle = vehicleDao.getVehicleById(linkedDevice.vehicleId)
                        val startOdo = vehicle?.odometerCurrent ?: 0
                        val usageType = vehicle?.usageType ?: "MIXED"
                        
                        val settings = database.settingsDao().getSettings().firstOrNull()
                        val defaultTripType = settings?.defaultTripType ?: "PERSONAL"
                        
                        var computedTripType = "UNCLASSIFIED"
                        when (usageType) {
                            "BUSINESS_ONLY" -> computedTripType = "BUSINESS"
                            "PRIVATE_ONLY" -> computedTripType = "PERSONAL"
                            "MIXED" -> {
                                if (settings != null) {
                                    try {
                                        val cal = java.util.Calendar.getInstance()
                                        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                                        // Calendar: 1=Sun, 2=Mon...7=Sat. Settings uses 1=Mon, 5=Fri...
                                        val mappedDay = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1
                                        val workHoursConfig = com.cimdriver.app.util.WorkHoursUtil.parseWorkHoursString(
                                            settings.workDays,
                                            settings.workStartTime,
                                            settings.workEndTime
                                        )
                                        val todayConfig = workHoursConfig.find { it.dayOfWeek == mappedDay }
                                        
                                        if (todayConfig != null && todayConfig.isEnabled) {
                                            val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                            val currentMin = cal.get(java.util.Calendar.MINUTE)
                                            val currentMins = currentHour * 60 + currentMin
                                            
                                            val startParts = todayConfig.startTime.split(":")
                                            val startMins = (startParts[0].toIntOrNull() ?: 8) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                                            
                                            val endParts = todayConfig.endTime.split(":")
                                            val endMins = (endParts[0].toIntOrNull() ?: 18) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
                                            
                                            if (currentMins in startMins..endMins) {
                                                computedTripType = "BUSINESS"
                                            } else {
                                                computedTripType = "PERSONAL"
                                            }
                                        } else {
                                            computedTripType = "PERSONAL"
                                        }
                                    } catch (e: Exception) {
                                        computedTripType = defaultTripType
                                    }
                                } else {
                                    computedTripType = defaultTripType
                                }
                            }
                        }
                        
                        // Reconnecting during the grace period resumes the active trip.
                        val activeTrip = tripDao.getActiveTripForVehicle(linkedDevice.vehicleId)
                        val isNewTrip = activeTrip == null
                        val tripId = activeTrip?.id ?: tripDao.insertTrip(
                            Trip(
                                vehicleId = linkedDevice.vehicleId,
                                startTime = System.currentTimeMillis(),
                                endTime = null,
                                startAddress = context.getString(com.cimdriver.app.R.string.tracking_retrieving_location),
                                endAddress = null,
                                distanceMeters = 0,
                                tripType = computedTripType,
                                note = null,
                                status = "ACTIVE",
                                isManual = false,
                                odometerStart = startOdo,
                                odometerEnd = null
                            )
                        )

                        // Monthly odometer verification check
                        if (isNewTrip && vehicle != null) {
                            val settings = database.settingsDao().getSettingsSync()
                            if (settings?.odometerReminder != false) {
                                val intervalDays = settings?.odometerReminderIntervalDays ?: 30
                                val intervalMs = intervalDays * 24L * 60L * 60L * 1000L
                                val timeSinceLastCheck = System.currentTimeMillis() - vehicle.lastOdometerCheckTimestamp
                                if (timeSinceLastCheck >= intervalMs) {
                                    sendOdometerCheckNotification(context, linkedDevice.vehicleId)
                                }
                            }
                        }

                        // Start Foreground Service for tracking
                        val serviceIntent = Intent(context, TrackingService::class.java).apply {
                            this.action = "START_TRACKING"
                            putExtra("TRIP_ID", tripId)
                        }
                        try {
                            context.startForegroundService(serviceIntent)
                        } catch (e: Exception) {
                            Log.e("BluetoothReceiver", "Failed to start TrackingService", e)
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            val notification = androidx.core.app.NotificationCompat.Builder(context, "TrackingServiceChannel")
                                .setContentTitle("CIMDriver: Automatische rit mislukt")
                                .setContentText("Controleer achtergrond permissies.")
                                .setSmallIcon(com.cimdriver.app.R.mipmap.ic_cimdriver_launcher)
                                .build()
                            nm.notify(998, notification)
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Log.d("BluetoothReceiver", "Disconnected from vehicle: ${linkedDevice.vehicleId}")
                        
                        // Send disconnect pending to trigger grace period
                        val serviceIntent = Intent(context, TrackingService::class.java).apply {
                            this.action = "DISCONNECT_PENDING"
                            putExtra("VEHICLE_ID", linkedDevice.vehicleId)
                        }
                        try {
                            context.startForegroundService(serviceIntent)
                        } catch (e: Exception) {
                            Log.e("BluetoothReceiver", "Failed to start TrackingService for disconnect", e)
                        }
                    }
                }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendOdometerCheckNotification(context: Context, vehicleId: Long) {
        val channelId = "OdometerCheckChannel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create channel if needed
        val channel = android.app.NotificationChannel(
            channelId,
            "Km-stand controle",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Maandelijkse controle van de km-stand"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, com.cimdriver.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_ODOMETER_CHECK", true)
            putExtra("ODOMETER_CHECK_VEHICLE_ID", vehicleId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 9999, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("Km-stand controleren")
            .setContentText("Het is tijd om je km-stand te verifiëren.")
            .setSmallIcon(com.cimdriver.app.R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(9999, notification)
    }
}
