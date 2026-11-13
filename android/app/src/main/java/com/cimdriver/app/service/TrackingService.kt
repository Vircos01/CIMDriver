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
import kotlinx.coroutines.withContext
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.firstOrNull
import com.cimdriver.app.MainActivity
import android.app.PendingIntent
import com.cimdriver.app.companion.ZeppCompanionServer
import com.cimdriver.app.util.TripDistance
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.util.AddressMatching
import androidx.compose.ui.res.stringResource

class TrackingService : Service(), LocationListener {
    
    private val CHANNEL_ID = "TrackingServiceChannel"
    private val FUEL_CHANNEL_ID = "FuelAlertChannel"
    private val NOTIFICATION_ID = 1

    companion object {
        const val ACTION_TOGGLE_TRIP_TYPE = "ACTION_TOGGLE_TRIP_TYPE"
        const val MAX_LOCATION_ACCURACY_METERS = 50f
        const val MAX_PLAUSIBLE_SPEED_METERS_PER_SECOND = 70f
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var database: AppDatabase
    private lateinit var locationManager: LocationManager
    private lateinit var geocoderService: GeocoderService
    
    private var activeTripId: Long? = null
    private var currentTripType: String = "BUSINESS"
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

    private fun showClassificationNotification(tripId: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val createIntent = { type: String ->
            Intent(this, TrackingService::class.java).apply {
                action = "CLASSIFY_TRIP"
                putExtra("TRIP_ID", tripId)
                putExtra("TRIP_TYPE", type)
            }
        }
        
        val businessPending = PendingIntent.getService(this, 1, createIntent("BUSINESS"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val privatePending = PendingIntent.getService(this, 2, createIntent("PRIVATE"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val commutePending = PendingIntent.getService(this, 3, createIntent("COMMUTE"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rit afgerond")
            .setContentText("Hoe wil je deze rit classificeren?")
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Zakelijk", businessPending)
            .addAction(0, "Privé", privatePending)
            .addAction(0, "Woon-Werk", commutePending)
            .build()
            
        notificationManager.notify(tripId.toInt(), notification)
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
                        ZeppCompanionServer.startIfNeeded(this)
                        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.tracking_recording_trip)))
                        startLocationUpdates()
                        TrackingStatusStore.started(activeTripId!!)
                        serviceScope.launch(Dispatchers.IO) {
                            val trip = database.tripDao().getTripById(activeTripId!!)
                            if (trip != null) {
                                currentTripType = trip.tripType
                                withContext(Dispatchers.Main) {
                                    val notificationManager = getSystemService(NotificationManager::class.java)
                                    notificationManager?.notify(NOTIFICATION_ID, createNotification(getString(R.string.tracking_recording_trip)))
                                }
                            }
                            try {
                                com.cimdriver.app.ui.widget.CIMDriverWidget().updateAll(applicationContext)
                            } catch (e: Exception) {
                                Log.w("TrackingService", "Widget update failed", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to start tracking", e)
                        TrackingStatusStore.failed("Fout bij opstarten: ${e.message}")
                        ZeppCompanionServer.stopIfNeeded()
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
                        ZeppCompanionServer.startIfNeeded(this)
                        startForeground(NOTIFICATION_ID, createNotification("Rit wordt hersteld..."))
                        startLocationUpdates()
                        TrackingStatusStore.started(activeTripId!!)
                        serviceScope.launch(Dispatchers.IO) {
                            val trip = database.tripDao().getTripById(activeTripId!!)
                            if (trip != null) {
                                currentTripType = trip.tripType
                                withContext(Dispatchers.Main) {
                                    val notificationManager = getSystemService(NotificationManager::class.java)
                                    notificationManager?.notify(NOTIFICATION_ID, createNotification("Rit wordt hersteld..."))
                                }
                            }
                            try {
                                com.cimdriver.app.ui.widget.CIMDriverWidget().updateAll(applicationContext)
                            } catch (e: Exception) {
                                Log.w("TrackingService", "Widget update failed", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to recover tracking", e)
                        TrackingStatusStore.failed("Fout bij herstel: ${e.message}")
                        ZeppCompanionServer.stopIfNeeded()
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
            ACTION_TOGGLE_TRIP_TYPE -> {
                toggleActiveTripType()
            }
            "CLASSIFY_TRIP" -> {
                val tripId = intent.getLongExtra("TRIP_ID", -1L)
                val tripType = intent.getStringExtra("TRIP_TYPE")
                if (tripId != -1L && tripType != null) {
                    serviceScope.launch {
                        val tripDao = database.tripDao()
                        val trip = tripDao.getTripById(tripId)
                        if (trip != null && trip.status == "TO_REVIEW") {
                            tripDao.updateTrip(trip.copy(tripType = tripType, status = "DONE"))
                        }
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(tripId.toInt())
                    }
                }
            }
        }
        return START_STICKY
    }
    
    private fun cancelDisconnectJob() {
        disconnectJob?.cancel()
        disconnectJob = null
    }

    private fun toggleActiveTripType() {
        val tripId = activeTripId ?: return
        serviceScope.launch(Dispatchers.IO) {
            val trip = database.tripDao().getTripById(tripId) ?: return@launch
            val isCurrentlyPrivate = isPrivateTrip(trip.tripType)
            val newType = if (isCurrentlyPrivate) "Business Meeting" else "PRIVATE"
            val updated = trip.copy(tripType = newType)
            database.tripDao().updateTrip(updated)
            currentTripType = newType

            withContext(Dispatchers.Main) {
                val typeLabel = if (isPrivateTrip(newType)) "Privé" else "Zakelijk"
                val message = "Rit gewijzigd naar $typeLabel"
                TrackingStatusStore.updateMessage(message)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(NOTIFICATION_ID, createNotification(message))
                try {
                    com.cimdriver.app.ui.widget.CIMDriverWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    Log.w("TrackingService", "Widget update failed", e)
                }
            }
        }
    }

    private fun isPrivateTrip(type: String?): Boolean {
        if (type == null) return false
        return type.equals("PRIVATE", ignoreCase = true) || type.equals("PERSONAL", ignoreCase = true)
    }

    private fun startDisconnectCountdown(vehicleId: Long) {
        cancelDisconnectJob()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(getString(R.string.tracking_disconnect_warning)))
        TrackingStatusStore.warning("Bluetoothverbinding verbroken; rit blijft tijdelijk actief")
        
        // Immediate check: did the driver stop at a gas station / charging point during this pause?
        activeTripId?.let { tripId ->
            serviceScope.launch {
                try {
                    val lastPoint = database.locationPointDao().getLastPointForTrip(tripId)
                    if (lastPoint != null) {
                        val currentTrip = database.tripDao().getTripById(tripId)
                        val vehicle = database.vehicleDao().getVehicleById(vehicleId)
                        val points = database.locationPointDao().getPointsForTrip(tripId).firstOrNull() ?: emptyList()
                        var recordedDistance = 0f
                        for (i in 0 until points.size - 1) {
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                points[i].latitude, points[i].longitude,
                                points[i+1].latitude, points[i+1].longitude,
                                results
                            )
                            recordedDistance += results[0]
                        }
                        val tripKm = kotlin.math.round(recordedDistance / 1000.0).toInt()
                        val estimatedOdo = (currentTrip?.odometerStart ?: vehicle?.odometerCurrent ?: 0) + tripKm

                        checkAndCreateFuelDraft(
                            vehicleId = vehicleId,
                            lat = lastPoint.latitude,
                            lon = lastPoint.longitude,
                            estimatedOdometer = estimatedOdo,
                            fallbackAddress = null
                        )
                    }
                } catch (e: Exception) {
                    Log.e("TrackingService", "Failed to check fuel draft on disconnect", e)
                }
            }
        }

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
                    ZeppCompanionServer.stopIfNeeded()
                    // Fetch the updated trip to see if it still needs review
                    val updatedTrip = tripDao.getTripById(tripId)
                    if (updatedTrip?.status == "TO_REVIEW") {
                        showClassificationNotification(tripId)
                    }
                } catch (e: Exception) {
                    Log.e("TrackingService", "Error finalizing trip", e)
                    TrackingStatusStore.failed("Fout bij afronden rit")
                } finally {
                    try {
                        com.cimdriver.app.ui.widget.CIMDriverWidget().updateAll(applicationContext)
                    } catch (e: Exception) {
                        Log.w("TrackingService", "Widget update failed", e)
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } ?: run {
            TrackingStatusStore.stopped("Tracking gestopt")
            ZeppCompanionServer.stopIfNeeded()
            serviceScope.launch {
                try {
                    com.cimdriver.app.ui.widget.CIMDriverWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    Log.w("TrackingService", "Widget update failed", e)
                }
            }
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
        val recordedDistance = totalDistance.toInt()

        val savedAddressDao = database.savedAddressDao()
        val allAddresses = savedAddressDao.getAllAddressesSync()

        // Check where the vehicle was parked prior to this trip (e.g. underground parking garage)
        val previousTrip = tripDao.getPreviousCompletedTripForVehicle(vehicleId, currentTrip.id)
        val prevTripLastPoint = previousTrip?.let { database.locationPointDao().getLastPointForTrip(it.id) }
        val prevSavedAddress = if (!previousTrip?.endAddress.isNullOrBlank()) AddressMatching.findSavedAddress(previousTrip.endAddress, allAddresses) else null
        val parkedLat = prevSavedAddress?.latitude ?: prevTripLastPoint?.latitude
        val parkedLon = prevSavedAddress?.longitude ?: prevTripLastPoint?.longitude
        val parkedAddressLabel = prevSavedAddress?.label ?: previousTrip?.endAddress

        var resolvedStartAddress = currentTrip.startAddress ?: getString(R.string.tracking_retrieving_location)
        var resolvedEndAddress = "Onbekend"
        
        // Use trip GPS points for coordinate-based matching when available
        val rawStartLat = if (points.isNotEmpty()) points.first().latitude else null
        val rawStartLon = if (points.isNotEmpty()) points.first().longitude else null
        val endLat = if (points.isNotEmpty()) points.last().latitude else null
        val endLon = if (points.isNotEmpty()) points.last().longitude else null

        var startMatch: com.cimdriver.app.util.AddressMatching.AddressMatch? = null
        var endMatch: com.cimdriver.app.util.AddressMatching.AddressMatch? = null

        if (points.isNotEmpty()) {
            if (resolvedStartAddress == getString(R.string.tracking_retrieving_location)) {
                resolvedStartAddress = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    geocoderService.getAddressFromLocation(rawStartLat!!, rawStartLon!!)
                } ?: "Onbekend"
            }
            
            resolvedEndAddress = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                geocoderService.getAddressFromLocation(endLat!!, endLon!!)
            } ?: "Onbekend"
            
            startMatch = AddressMatching.findBestMatch(rawStartLat, rawStartLon, resolvedStartAddress, allAddresses)
            endMatch = AddressMatching.findBestMatch(endLat, endLon, resolvedEndAddress, allAddresses)
        }

        // Parking garage / delayed GPS fix check:
        // If standard 200m match didn't find the departure location, check if the first GPS fix is within
        // the exit radius (1500m) of where the vehicle was parked or a known saved address.
        if (startMatch == null) {
            startMatch = AddressMatching.findParkingGarageDepartureMatch(
                firstGpsLat = rawStartLat,
                firstGpsLon = rawStartLon,
                parkedLat = parkedLat,
                parkedLon = parkedLon,
                parkedAddressLabel = parkedAddressLabel,
                addresses = allAddresses,
                maxExitRadiusMeters = 1500f
            )
        }

        if (resolvedStartAddress == getString(R.string.tracking_retrieving_location) && startMatch == null && !parkedAddressLabel.isNullOrBlank()) {
            resolvedStartAddress = parkedAddressLabel
        }

        val finalStartAddress = startMatch?.address?.label ?: resolvedStartAddress
        val finalEndAddress = endMatch?.address?.label ?: resolvedEndAddress

        val trueStartLat = startMatch?.address?.latitude ?: parkedLat ?: rawStartLat
        val trueStartLon = startMatch?.address?.longitude ?: parkedLon ?: rawStartLon
        val trueEndLat = endMatch?.address?.latitude ?: endLat
        val trueEndLon = endMatch?.address?.longitude ?: endLon

        // If the first GPS fix occurred after exiting the garage, prepend a location point at the garage/address
        var distanceToFirstGpsPoint = 0f
        if (trueStartLat != null && trueStartLon != null && points.isNotEmpty()) {
            val distResults = FloatArray(1)
            android.location.Location.distanceBetween(trueStartLat, trueStartLon, points.first().latitude, points.first().longitude, distResults)
            distanceToFirstGpsPoint = distResults[0]
            if (distanceToFirstGpsPoint > 100f) {
                database.locationPointDao().insertPoint(
                    com.cimdriver.app.data.local.entity.LocationPoint(
                        tripId = tripId,
                        latitude = trueStartLat,
                        longitude = trueStartLon,
                        altitude = points.first().altitude,
                        speed = 0f,
                        accuracy = 10f,
                        timestamp = currentTrip.startTime
                    )
                )
            }
        }

        // If the last GPS fix occurred before entering a garage at arrival, append a location point at the destination
        var distanceToLastGpsPoint = 0f
        if (trueEndLat != null && trueEndLon != null && points.isNotEmpty()) {
            val distResults = FloatArray(1)
            android.location.Location.distanceBetween(points.last().latitude, points.last().longitude, trueEndLat, trueEndLon, distResults)
            distanceToLastGpsPoint = distResults[0]
            if (distanceToLastGpsPoint > 100f) {
                database.locationPointDao().insertPoint(
                    com.cimdriver.app.data.local.entity.LocationPoint(
                        tripId = tripId,
                        latitude = trueEndLat,
                        longitude = trueEndLon,
                        altitude = points.last().altitude,
                        speed = 0f,
                        accuracy = 10f,
                        timestamp = System.currentTimeMillis() - gracePeriodMs
                    )
                )
            }
        }
        
        var isCommute = false
        var arrivedAtWork = false
        var leftWork = false
        var arrivedAtHome = false
        var leftHome = false
        var matchedWorkLabel = ""

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
        val projectCodeId: Long? = endLoc?.defaultProjectCodeId ?: startLoc?.defaultProjectCodeId
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

        val matchingRule = TripClassification.findMatchingRule(
            tripType = finalTripType,
            startAddressType = startAddressType,
            endAddressType = endAddressType,
            startAddress = finalStartAddress,
            endAddress = finalEndAddress,
            rules = classificationRules
        )
        val autoApprove = matchingRule?.autoApprove ?: false

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
        if (trueStartLat != null && trueStartLon != null && trueEndLat != null && trueEndLon != null) {
            val distPair = geocoderService.getOsrmDistance(trueStartLat, trueStartLon, trueEndLat, trueEndLon)
            if (distPair != null) {
                expectedDistance = distPair.first.toInt()
            }
        }
        if (expectedDistance == null && finalStartAddress.isNotBlank() && finalEndAddress.isNotBlank()) {
            val distPair = geocoderService.calculateDistance(finalStartAddress, finalEndAddress)
            if (distPair != null) {
                expectedDistance = distPair.first.toInt()
            }
        }

        // Calculate missing segments for parking garage exit/entry
        var missingStartDistanceM = 0
        if (trueStartLat != null && trueStartLon != null && points.isNotEmpty() && distanceToFirstGpsPoint > 100f) {
            val startSegment = geocoderService.getOsrmDistance(
                trueStartLat, trueStartLon,
                points.first().latitude, points.first().longitude
            )
            missingStartDistanceM = startSegment?.first?.toInt() ?: distanceToFirstGpsPoint.toInt()
        }

        var missingEndDistanceM = 0
        if (trueEndLat != null && trueEndLon != null && points.isNotEmpty() && distanceToLastGpsPoint > 100f) {
            val endSegment = geocoderService.getOsrmDistance(
                points.last().latitude, points.last().longitude,
                trueEndLat, trueEndLon
            )
            missingEndDistanceM = endSegment?.first?.toInt() ?: distanceToLastGpsPoint.toInt()
        }

        // Check if GPS was missing/delayed at departure (e.g. parking garage)
        val isDelayedGpsFix = points.isEmpty() ||
                (distanceToFirstGpsPoint > 100f) ||
                (startMatch != null && startMatch.reason == com.cimdriver.app.util.AddressMatching.MatchReason.COORDINATE && startMatch.distanceMeters > 100f)

        // Adjust distance: adds missing garage segments directly to the actual driven route!
        val finalDistanceM = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = recordedDistance,
            expectedDirectDistanceMeters = expectedDistance,
            missingStartDistanceMeters = missingStartDistanceM,
            missingEndDistanceMeters = missingEndDistanceM,
            isDelayedGpsFix = isDelayedGpsFix
        )
        if (finalDistanceM != recordedDistance) {
            Log.i("TrackingService", "Parkeergarage / vertraagde GPS-fix: ritafstand bijgesteld van $recordedDistance m naar $finalDistanceM m (inclusief werkelijk afgelegde route: +$missingStartDistanceM m start, +$missingEndDistanceM m eind, route direct: $expectedDistance m)")
        }

        val odoEnd = TripDistance.updatedOdometer(currentTrip.odometerStart, finalDistanceM)
        val vehicleDao = database.vehicleDao()
        val vehicle = vehicleDao.getVehicleById(vehicleId)
        if (vehicle != null) {
            vehicleDao.updateVehicle(vehicle.copy(odometerCurrent = odoEnd))
        }

        tripDao.updateTrip(currentTrip.copy(
            endTime = System.currentTimeMillis() - gracePeriodMs,
            startAddress = finalStartAddress,
            endAddress = finalEndAddress,
            distanceMeters = finalDistanceM,
            odometerEnd = odoEnd,
            expectedDistanceMeters = expectedDistance,
            tripType = finalTripType,
            projectCode = projectCode,
            projectCodeId = projectCodeId,
            status = if (autoApprove) "DONE" else "TO_REVIEW",
            appliedRuleName = matchingRule?.name
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
                    projectCodeId = projectCodeId,
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

                if (updatedWorkDay.projectCodeId == null && projectCodeId != null) {
                    updatedWorkDay = updatedWorkDay.copy(projectCodeId = projectCodeId)
                }
                if (updatedWorkDay.projectCode == null && projectCode != null) {
                    updatedWorkDay = updatedWorkDay.copy(projectCode = projectCode)
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

        if (endLat != null && endLon != null) {
            checkAndCreateFuelDraft(
                vehicleId = vehicleId,
                lat = endLat,
                lon = endLon,
                estimatedOdometer = odoEnd,
                fallbackAddress = finalEndAddress
            )
        }
    }

    private suspend fun checkAndCreateFuelDraft(
        vehicleId: Long,
        lat: Double,
        lon: Double,
        estimatedOdometer: Int,
        fallbackAddress: String?
    ) {
        val vehicle = database.vehicleDao().getVehicleById(vehicleId)
        val engineType = vehicle?.engineType ?: "ICE"

        // 1. Avoid duplicate drafts: check if there is an uncompleted draft in the last 2 hours
        val savedFillUps = database.fuelFillUpDao().getAllFillUpsSync()
        val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        val hasRecentDraft = savedFillUps.any { fillUp ->
            fillUp.vehicleId == vehicleId &&
            fillUp.status == "DRAFT" &&
            fillUp.dateTimestamp >= twoHoursAgo
        }
        if (hasRecentDraft) {
            Log.d("TrackingService", "Skipping fuel draft check: draft already exists from last 2 hours")
            return
        }

        // 2. Check saved fill-ups within 150m
        var closestFillUp: com.cimdriver.app.data.local.entity.FuelFillUp? = null
        var minDistance = Float.MAX_VALUE

        for (fillUp in savedFillUps) {
            if (fillUp.latitude != null && fillUp.longitude != null && !fillUp.stationName.isNullOrBlank()) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(lat, lon, fillUp.latitude, fillUp.longitude, results)
                if (results[0] < minDistance && results[0] <= 150f) {
                    minDistance = results[0]
                    closestFillUp = fillUp
                }
            }
        }

        val stationName = closestFillUp?.stationName ?: run {
            geocoderService.checkGasStationNearby(lat, lon, engineType)
        }

        if (stationName != null) {
            val stationAddress = closestFillUp?.address ?: fallbackAddress ?: geocoderService.getAddressFromLocation(lat, lon)
            val stationLat = closestFillUp?.latitude ?: lat
            val stationLon = closestFillUp?.longitude ?: lon

            val fuelFillUp = com.cimdriver.app.data.local.entity.FuelFillUp(
                vehicleId = vehicleId,
                dateTimestamp = System.currentTimeMillis(),
                liters = 0.0,
                pricePerLiter = 0.0,
                totalCost = 0.0,
                odometer = estimatedOdometer,
                stationName = stationName,
                address = stationAddress,
                latitude = stationLat,
                longitude = stationLon,
                status = "DRAFT"
            )
            val newFillUpId = database.fuelFillUpDao().insertFillUp(fuelFillUp)
            Log.i("TrackingService", "Created draft fuel fill-up $newFillUpId at $stationName")
            sendFuelDraftNotification(newFillUpId, stationName, engineType)
        }
    }

    private fun sendFuelDraftNotification(fillUpId: Long, stationName: String, engineType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_FILL_UP_ID", fillUpId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, (20000 + fillUpId).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val isEV = engineType == "EV"
        val title = if (isEV) "Laden bij $stationName?" else "Tanken bij $stationName?"
        val content = if (isEV) "Tik om kWh en laadkosten op te slaan." else "Tik om liters en bedrag in te vullen."

        val notification = androidx.core.app.NotificationCompat.Builder(this, FUEL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify((50000 + fillUpId).toInt(), notification)
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
                    val distanceMeters = result[0]
                    // Adaptive tracking: suppress stationary GPS jitter (< 3m at < 0.8 m/s within 30s)
                    if (distanceMeters < 3f && location.speed < 0.8f && elapsedSeconds < 30f) {
                        return@launch
                    }
                    if (distanceMeters / elapsedSeconds > MAX_PLAUSIBLE_SPEED_METERS_PER_SECOND) return@launch
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

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't use binding for this service
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopLocationUpdates()
        ZeppCompanionServer.stopIfNeeded()
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

        val toggleIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_TOGGLE_TRIP_TYPE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            101,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPrivate = isPrivateTrip(currentTripType)
        val toggleLabel = if (isPrivate) "➡️ Naar Zakelijk" else "🔒 Naar Privé"
        val title = if (isPrivate) "CIMDriver Actief (Privé)" else "CIMDriver Actief (Zakelijk)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_cimdriver_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, toggleLabel, togglePendingIntent)
            .addAction(0, getString(R.string.stop_trip), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
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
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(fuelChannel)
    }
}
