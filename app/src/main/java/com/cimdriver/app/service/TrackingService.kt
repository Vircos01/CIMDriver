package com.cimdriver.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cimdriver.app.R
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.LocationPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.cimdriver.app.MainActivity
import android.app.PendingIntent
import com.cimdriver.app.util.TripDistance
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.util.AddressMatching
import androidx.compose.ui.res.stringResource

class TrackingService : Service(), LocationListener {
    
    private val CHANNEL_ID = "TrackingServiceChannel"
    private val NOTIFICATION_ID = 1

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var database: AppDatabase
    private lateinit var locationManager: LocationManager
    private lateinit var geocoderService: GeocoderService
    
    private var activeTripId: Long? = null
    private var disconnectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoderService = GeocoderService(this)
        createNotificationChannel()
        
        serviceScope.launch {
            TrackingStatusStore.status.collect { status ->
                if (status.isActive || status.state == ServiceState.FAILED) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, createNotification(status.message))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        val currentState = TrackingStatusStore.status.value.state
        
        val action = intent.action
        when (action) {
            "START_TRACKING" -> {
                if (currentState == ServiceState.RUNNING || currentState == ServiceState.STARTING || currentState == ServiceState.RECOVERING) {
                    Log.d("TrackingService", "Start ignored: Service already active (state=$currentState)")
                    return START_STICKY
                }
                
                activeTripId = intent.getLongExtra("TRIP_ID", -1L)
                if (activeTripId != -1L) {
                    cancelDisconnectJob()
                    val correlationId = TrackingStatusStore.starting(activeTripId, isRecovery = false)
                    Log.i("TrackingService", "Starting tracking for trip $activeTripId [correlationId=$correlationId]")
                    
                    try {
                        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.tracking_recording_trip)))
                        startLocationUpdates()
                        TrackingStatusStore.started(activeTripId!!)
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to start tracking", e)
                        TrackingStatusStore.failed("Fout bij opstarten: ${e.message}")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    TrackingStatusStore.failed("Fout: Geen rit-ID gevonden.")
                    startForeground(NOTIFICATION_ID, createNotification("Fout: Geen rit-ID gevonden."))
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            "RECOVER_TRACKING" -> {
                if (currentState == ServiceState.RUNNING || currentState == ServiceState.STARTING || currentState == ServiceState.RECOVERING) {
                    return START_STICKY
                }
                
                activeTripId = intent.getLongExtra("TRIP_ID", -1L)
                if (activeTripId != -1L) {
                    cancelDisconnectJob()
                    val correlationId = TrackingStatusStore.starting(activeTripId, isRecovery = true)
                    Log.i("TrackingService", "Recovering tracking for trip $activeTripId [correlationId=$correlationId]")
                    
                    try {
                        startForeground(NOTIFICATION_ID, createNotification("Rit wordt hersteld..."))
                        startLocationUpdates()
                        TrackingStatusStore.started(activeTripId!!)
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to recover tracking", e)
                        TrackingStatusStore.failed("Fout bij herstel: ${e.message}")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
            "DISCONNECT_PENDING" -> {
                val vehicleId = intent.getLongExtra("VEHICLE_ID", -1L)
                startDisconnectCountdown(vehicleId)
            }
            "STOP_TRACKING" -> {
                if (currentState == ServiceState.STOPPED || currentState == ServiceState.STOPPING || currentState == ServiceState.FAILED) {
                    Log.d("TrackingService", "Stop ignored: Service already inactive (state=$currentState)")
                    return START_NOT_STICKY
                }
                TrackingStatusStore.stopping("Rit wordt afgerond")
                finalizeTripAndStop()
            }
        }
        return START_STICKY
    }
    
    private fun cancelDisconnectJob() {
        disconnectJob?.cancel()
        disconnectJob = null
    }

    private fun startDisconnectCountdown(vehicleId: Long) {
        cancelDisconnectJob()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(getString(R.string.tracking_disconnect_warning)))
        TrackingStatusStore.warning("Bluetoothverbinding verbroken; rit blijft tijdelijk actief")
        
        disconnectJob = serviceScope.launch {
            val settings = database.settingsDao().getSettings().firstOrNull()
            val gracePeriodMs = (settings?.gracePeriodSec ?: 180) * 1000L
            
            delay(gracePeriodMs)
            
            // If we reach here, grace period is over
            val tripDao = database.tripDao()
            val currentTrip = tripDao.getLatestTripForVehicle(vehicleId)
            if (currentTrip != null && currentTrip.endTime == null) {
                processTripFinalization(currentTrip.id, vehicleId, gracePeriodMs)
            }
            
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun finalizeTripAndStop() {
        cancelDisconnectJob()
        stopLocationUpdates()
        
        activeTripId?.let { tripId ->
            serviceScope.launch {
                try {
                    val tripDao = database.tripDao()
                    val trip = tripDao.getTripById(tripId)
                    if (trip != null && trip.vehicleId != null) {
                        processTripFinalization(tripId, trip.vehicleId, 0L)
                    }
                    val recoveryManager = TrackingRecoveryManager(this@TrackingService)
                    recoveryManager.clearRecoveryAttempts(tripId)
                    TrackingStatusStore.stopped("Rit succesvol afgerond")
                } catch (e: Exception) {
                    Log.e("TrackingService", "Error finalizing trip", e)
                    TrackingStatusStore.failed("Fout bij afronden rit")
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } ?: run {
            TrackingStatusStore.stopped("Tracking gestopt")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private suspend fun processTripFinalization(tripId: Long, vehicleId: Long, gracePeriodMs: Long) {
        val tripDao = database.tripDao()
        val currentTrip = tripDao.getTripById(tripId) ?: return
        if (currentTrip.status != "ACTIVE") return
        
        val points = database.locationPointDao().getPointsForTrip(tripId).firstOrNull() ?: emptyList()
        var totalDistance = 0f
        for (i in 0 until points.size - 1) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                points[i].latitude, points[i].longitude,
                points[i+1].latitude, points[i+1].longitude,
                results
            )
            totalDistance += results[0]
        }
        
        val distanceM = totalDistance.toInt()
        // Trip distances are stored in metres, while odometer readings are whole kilometres.
        val odoEnd = TripDistance.updatedOdometer(currentTrip.odometerStart, distanceM)
        
        val vehicleDao = database.vehicleDao()
        val vehicle = vehicleDao.getVehicleById(vehicleId)
        if (vehicle != null) {
            vehicleDao.updateVehicle(vehicle.copy(odometerCurrent = odoEnd))
        }

        var resolvedStartAddress = currentTrip.startAddress ?: getString(R.string.tracking_retrieving_location)
        var resolvedEndAddress = "Onbekend"
        
        val savedAddressDao = database.savedAddressDao()
        val allAddresses = savedAddressDao.getAllAddressesSync()
        
        // Use trip GPS points for coordinate-based matching when available
        val startLat = if (points.isNotEmpty()) points.first().latitude else null
        val startLon = if (points.isNotEmpty()) points.first().longitude else null
        val endLat = if (points.isNotEmpty()) points.last().latitude else null
        val endLon = if (points.isNotEmpty()) points.last().longitude else null

        var startMatch: com.cimdriver.app.util.AddressMatching.AddressMatch? = null
        var endMatch: com.cimdriver.app.util.AddressMatching.AddressMatch? = null

        if (points.isNotEmpty()) {
            if (resolvedStartAddress == getString(R.string.tracking_retrieving_location)) {
                resolvedStartAddress = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    geocoderService.getAddressFromLocation(startLat!!, startLon!!)
                } ?: "Onbekend"
            }
            
            resolvedEndAddress = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                geocoderService.getAddressFromLocation(endLat!!, endLon!!)
            } ?: "Onbekend"
            
            startMatch = AddressMatching.findBestMatch(startLat, startLon, resolvedStartAddress, allAddresses)
            endMatch = AddressMatching.findBestMatch(endLat, endLon, resolvedEndAddress, allAddresses)
        }
        
        var isCommute = false
        var arrivedAtWork = false
        var leftWork = false
        var arrivedAtHome = false
        var leftHome = false
        var matchedWorkLabel = ""
        
        val finalStartAddress = startMatch?.address?.label ?: resolvedStartAddress
        val finalEndAddress = endMatch?.address?.label ?: resolvedEndAddress
        
        val startLoc = startMatch?.address
        val endLoc = endMatch?.address
        
        if (startLoc != null) {
            if (startLoc.isHomeLocation) leftHome = true
            if (startLoc.isWorkLocation) {
                leftWork = true
                matchedWorkLabel = startLoc.label
            }
        }
        
        if (endLoc != null) {
            if (endLoc.isHomeLocation) arrivedAtHome = true
            if (endLoc.isWorkLocation) {
                arrivedAtWork = true
                matchedWorkLabel = endLoc.label
            }
        }
        
        if (leftHome && arrivedAtWork) isCommute = true
        if (leftWork && arrivedAtHome) isCommute = true
        
        val projectCode: String? = endLoc?.projectCode?.takeIf { it.isNotBlank() } ?: startLoc?.projectCode?.takeIf { it.isNotBlank() }
        val isCustomerTrip = endLoc?.isCustomerLocation == true || startLoc?.isCustomerLocation == true
        val defaultTripType: String? = endLoc?.defaultTripType?.takeIf { it.isNotBlank() } ?: startLoc?.defaultTripType?.takeIf { it.isNotBlank() }
        
        val startAddressType = startLoc?.addressType ?: when {
            startLoc?.isHomeLocation == true -> "THUIS"
            startLoc?.isWorkLocation == true -> "WERK"
            startLoc?.isCustomerLocation == true -> "KLANT"
            else -> null
        }
        val endAddressType = endLoc?.addressType ?: when {
            endLoc?.isHomeLocation == true -> "THUIS"
            endLoc?.isWorkLocation == true -> "WERK"
            endLoc?.isCustomerLocation == true -> "KLANT"
            else -> null
        }
        
        val baseTripType = defaultTripType ?: if (isCustomerTrip) "Customer Visit" else if (isCommute) "Home To Work" else currentTrip.tripType
        val classificationRules = database.classificationRuleDao().getAllRules().firstOrNull() ?: emptyList()
        val settings = database.settingsDao().getSettingsSync()

        var finalTripType = TripClassification.resolveTripType(baseTripType, startAddressType, endAddressType, finalStartAddress, finalEndAddress, classificationRules) ?: baseTripType
        
        // Time-based classification check
        if (settings != null) {
            val isOutsideHours = TripClassification.isOutsideWorkHours(
                timestamp = currentTrip.startTime,
                workDaysStr = settings.workDays,
                workStartTime = settings.workStartTime,
                workEndTime = settings.workEndTime
            )
            val hasWorkLocation = startAddressType == "WERK" || endAddressType == "WERK"
            val ruleMatched = TripClassification.resolveTripType(baseTripType, startAddressType, endAddressType, finalStartAddress, finalEndAddress, classificationRules) != baseTripType
            
            if (isOutsideHours && !hasWorkLocation && !ruleMatched) {
                finalTripType = TripClassification.DYNAMICS_PERSONAL
            }
        }

        val finalCategory = TripClassification.classify(
            tripType = finalTripType,
            startAddressType = startAddressType,
            endAddressType = endAddressType,
            startAddress = finalStartAddress,
            endAddress = finalEndAddress,
            rules = classificationRules,
            timestamp = currentTrip.startTime,
            workDaysStr = settings?.workDays,
            workStartTime = settings?.workStartTime,
            workEndTime = settings?.workEndTime
        )

        var expectedDistance: Int? = null
        if (startLat != null && startLon != null && endLat != null && endLon != null) {
            val distPair = geocoderService.getOsrmDistance(startLat, startLon, endLat, endLon)
            if (distPair != null) {
                expectedDistance = distPair.first.toInt()
            }
        }

        tripDao.updateTrip(currentTrip.copy(
            endTime = System.currentTimeMillis() - gracePeriodMs,
            startAddress = finalStartAddress,
            endAddress = finalEndAddress,
            distanceMeters = distanceM,
            odometerEnd = odoEnd,
            expectedDistanceMeters = expectedDistance,
            tripType = finalTripType,
            projectCode = projectCode,
            status = "TO_REVIEW"
        ))
        
        // Work Day Logic
        val isBusiness = finalCategory == com.cimdriver.app.util.TripCategory.BUSINESS
        val isCommuteType = finalCategory == com.cimdriver.app.util.TripCategory.COMMUTE
        if (isBusiness || isCommuteType || arrivedAtWork || leftWork) {

            val workDayDao = database.workDayDao()
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = currentTrip.startTime
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            val existingWorkDay = workDayDao.getWorkDayByDate(startOfDay)
            val settings = database.settingsDao().getSettings().firstOrNull()
            val breakMins = settings?.defaultBreakMinutes ?: 30
            val breakToleranceMins = settings?.workHoursToleranceMinutes ?: 30
            
            val rawDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (rawDay == java.util.Calendar.SUNDAY) 7 else rawDay - 1
            val workDaysConfig = com.cimdriver.app.util.WorkHoursUtil.parseWorkHoursString(
                settings?.workDays ?: "",
                settings?.workStartTime ?: "08:00",
                settings?.workEndTime ?: "18:00"
            )
            val todayConfig = workDaysConfig.find { it.dayOfWeek == dayOfWeek }
            
            val configuredStart = todayConfig?.startTime ?: settings?.workStartTime ?: "08:00"
            val configuredEnd = todayConfig?.endTime ?: settings?.workEndTime ?: "18:00"
            
            val endTimeAdjusted = System.currentTimeMillis() - gracePeriodMs
            val normalizedArrival = com.cimdriver.app.util.WorkHoursNormalizer.normalize(
                endTimeAdjusted,
                startOfDay,
                configuredStart,
                breakToleranceMins.toLong()
            )
            val normalizedDeparture = com.cimdriver.app.util.WorkHoursNormalizer.normalize(
                currentTrip.startTime,
                startOfDay,
                configuredEnd,
                breakToleranceMins.toLong()
            )

            if (existingWorkDay == null) {
                val locationLabel = if (arrivedAtWork && matchedWorkLabel.isNotBlank()) matchedWorkLabel else finalEndAddress
                val newWorkDay = com.cimdriver.app.data.local.entity.WorkDay(
                    date = startOfDay,
                    firstDepartureTime = currentTrip.startTime,
                    arrivalTime = endTimeAdjusted,
                    departureTime = null,
                    lastArrivalTime = null,
                    roundedArrivalTime = normalizedArrival,
                    roundedDepartureTime = null,
                    breakMinutes = breakMins,
                    workLocationLabel = locationLabel,
                    projectCode = projectCode,
                    status = "IN_PROGRESS"
                )
                workDayDao.insertWorkDay(newWorkDay)
                sendWorkDayStartedNotification()
            } else {
                var updatedWorkDay = existingWorkDay
                var didFinish = arrivedAtHome

                if (!leftHome) {
                    updatedWorkDay = updatedWorkDay.copy(
                        departureTime = currentTrip.startTime,
                        roundedDepartureTime = normalizedDeparture
                    )
                }

                if (arrivedAtWork) {
                    updatedWorkDay = updatedWorkDay.copy(
                        lastArrivalTime = endTimeAdjusted,
                        workLocationLabel = matchedWorkLabel
                    )
                    didFinish = false
                } else {
                    updatedWorkDay = updatedWorkDay.copy(
                        lastArrivalTime = endTimeAdjusted
                    )
                }
                
                if (didFinish) {
                    val effectiveArrival = updatedWorkDay.roundedArrivalTime ?: updatedWorkDay.arrivalTime
                    val finalDeparture = updatedWorkDay.departureTime ?: currentTrip.startTime
                    val effectiveDeparture = updatedWorkDay.roundedDepartureTime ?: finalDeparture
                    
                    updatedWorkDay = updatedWorkDay.copy(
                        breakMinutes = com.cimdriver.app.util.WorkHoursCalculator.effectiveBreakMinutes(
                            effectiveArrival,
                            effectiveDeparture,
                            breakMins,
                            breakToleranceMins
                        )
                    )
                } else {
                    updatedWorkDay = updatedWorkDay.copy(
                        breakMinutes = breakMins
                    )
                }

                updatedWorkDay = updatedWorkDay.copy(
                    status = if (didFinish) "TO_REVIEW" else "IN_PROGRESS"
                )

                workDayDao.updateWorkDay(updatedWorkDay)

                if (didFinish) {
                    sendWorkDayCompletionNotification(updatedWorkDay.id)
                }
            }
        }

        sendCompletionNotification(tripId, vehicleId)
    }

    private fun sendWorkDayCompletionNotification(workDayId: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We could add an extra to navigate straight to work hours
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.work_day_registered))
            .setContentText(getString(R.string.tap_to_rate_work_hours))
            .setSmallIcon(com.cimdriver.app.R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(workDayId.toInt() + 10000, notification) // Offset ID
    }

    private fun sendWorkDayStartedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.work_day_started))
            .setContentText(getString(R.string.work_day_started_msg))
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 10001, notification)
    }

    private fun sendCompletionNotification(tripId: Long, vehicleId: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_VEHICLE_ID", vehicleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Quick-action intents for classifying directly from the notification
        fun classifyIntent(tripType: String): PendingIntent {
            val classifyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CLASSIFY_TRIP
                putExtra(NotificationActionReceiver.EXTRA_TRIP_ID, tripId)
                putExtra(NotificationActionReceiver.EXTRA_TRIP_TYPE, tripType)
            }
            return PendingIntent.getBroadcast(
                this,
                kotlin.math.abs((tripId * 10 + tripType.hashCode()).toInt()),
                classifyIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.trip_completed))
            .setContentText(getString(R.string.trip_registered_msg))
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✅ Zakelijk", classifyIntent("Business Meeting"))
            .addAction(0, "🏠 Woon-werk", classifyIntent("Home To Work"))
            .addAction(0, "🔒 Privé", classifyIntent("PRIVATE"))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(tripId.toInt(), notification)
    }

    private fun startLocationUpdates() {
        TrackingStatusStore.waitingForGps()
        try {
            // Check permissions before requesting (ideally done in UI)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // minimum time interval between updates (2 seconds)
                5f,    // minimum distance between updates (5 meters)
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Missing location permissions", e)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, createNotification("CIMDriver: Geen locatie permissie. Rit wordt gestopt."))
            finalizeTripAndStop()
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        val tripId = activeTripId ?: return
        if (!location.hasAccuracy() || location.accuracy > MAX_LOCATION_ACCURACY_METERS) return
        TrackingStatusStore.locationReceived(location.accuracy, location.time.takeIf { it > 0L } ?: System.currentTimeMillis())
        serviceScope.launch {
            val timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            val previousPoint = database.locationPointDao().getLastPointForTrip(tripId)
            if (previousPoint != null) {
                val elapsedSeconds = (timestamp - previousPoint.timestamp) / 1_000f
                if (elapsedSeconds > 0f) {
                    val result = FloatArray(1)
                    Location.distanceBetween(
                        previousPoint.latitude, previousPoint.longitude,
                        location.latitude, location.longitude,
                        result
                    )
                    if (result[0] / elapsedSeconds > MAX_PLAUSIBLE_SPEED_METERS_PER_SECOND) return@launch
                }
            }
            val point = LocationPoint(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                accuracy = location.accuracy,
                timestamp = timestamp
            )
            database.locationPointDao().insertPoint(point)
        }
    }

    private companion object {
        const val MAX_LOCATION_ACCURACY_METERS = 50f
        const val MAX_PLAUSIBLE_SPEED_METERS_PER_SECOND = 70f
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't use binding for this service
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopLocationUpdates()
        TrackingStatusStore.stopped()
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = "STOP_TRACKING"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.cimdriver_active))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.stop_trip), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "CIMDriver Tracking Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
