package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.cimdriver.app.data.local.dao.VehicleDao
import com.cimdriver.app.data.local.dao.BluetoothDeviceDao
import com.cimdriver.app.data.local.dao.TripDao
import com.cimdriver.app.data.local.dao.OdometerCheckDao
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.BluetoothDevice
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.OdometerCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.bluetooth.BluetoothManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

import com.cimdriver.app.data.local.dao.OdometerGapResolutionDao
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import com.cimdriver.app.util.OdometerGapAnalyzer
import com.cimdriver.app.util.OdometerGap
import com.cimdriver.app.util.FixType
import com.cimdriver.app.util.GapType

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val bluetoothDeviceDao: BluetoothDeviceDao,
    private val tripDao: TripDao,
    private val odometerCheckDao: OdometerCheckDao,
    private val odometerGapResolutionDao: OdometerGapResolutionDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val vehicles = vehicleDao.getAllVehicles()
    val bluetoothDevices = bluetoothDeviceDao.getAllDevices()
    
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    fun addVehicle(name: String, licensePlate: String, make: String, model: String, odometerStart: Int, odometerCurrent: Int, usageType: String, notes: String?, privateKmYearlyLimit: Int, showPrivateKmWarning: Boolean, odometerCorrectionStrategy: String, inServiceDate: Long?, endServiceDate: Long?, engineType: String) {
        viewModelScope.launch {
            val newVehicle = Vehicle(
                name = name,
                licensePlate = licensePlate,
                make = make,
                model = model,
                year = null,
                odometerStart = odometerStart,
                odometerCurrent = odometerCurrent,
                notes = notes,
                usageType = usageType,
                privateKmYearlyLimit = privateKmYearlyLimit,
                showPrivateKmWarning = showPrivateKmWarning,
                odometerCorrectionStrategy = odometerCorrectionStrategy,
                inServiceDate = inServiceDate,
                endServiceDate = endServiceDate,
                engineType = engineType
            )
            vehicleDao.insertVehicle(newVehicle)
        }
    }

    fun updateVehicle(vehicle: Vehicle, name: String, licensePlate: String, make: String, model: String, odometerStart: Int, odometerCurrent: Int, usageType: String, notes: String?, privateKmYearlyLimit: Int, showPrivateKmWarning: Boolean, odometerCorrectionStrategy: String, inServiceDate: Long?, endServiceDate: Long?, engineType: String) {
        viewModelScope.launch {
            val updatedVehicle = vehicle.copy(
                name = name,
                licensePlate = licensePlate,
                make = make,
                model = model,
                odometerStart = odometerStart,
                odometerCurrent = odometerCurrent,
                usageType = usageType,
                notes = notes,
                privateKmYearlyLimit = privateKmYearlyLimit,
                showPrivateKmWarning = showPrivateKmWarning,
                odometerCorrectionStrategy = odometerCorrectionStrategy,
                inServiceDate = inServiceDate,
                endServiceDate = endServiceDate,
                engineType = engineType
            )
            vehicleDao.updateVehicle(updatedVehicle)
        }
    }

    fun updateOdometer(vehicle: Vehicle, newOdometer: Int) {
        viewModelScope.launch {
            val updatedVehicle = vehicle.copy(odometerCurrent = newOdometer)
            vehicleDao.updateVehicle(updatedVehicle)
        }
    }

    fun recalculateOdometerHistory(vehicleId: Long) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return@launch
            val trips = tripDao.getTripsByVehicleSync(vehicleId)
            
            var currentOdo = vehicle.odometerStart
            
            for (trip in trips) {
                val distanceKm = Math.round(trip.distanceMeters / 1000.0).toInt()
                val updatedTrip = trip.copy(
                    odometerStart = currentOdo,
                    odometerEnd = currentOdo + distanceKm
                )
                tripDao.updateTrip(updatedTrip)
                currentOdo += distanceKm
            }
            
            val updatedVehicle = vehicle.copy(odometerCurrent = currentOdo)
            vehicleDao.updateVehicle(updatedVehicle)
        }
    }

    fun getChecksForVehicle(vehicleId: Long) = odometerCheckDao.getChecksForVehicle(vehicleId)
    fun getResolutionsForVehicle(vehicleId: Long) = odometerGapResolutionDao.getResolutionsForVehicle(vehicleId)
    val openGapsCount = odometerGapResolutionDao.getOpenResolutionCount()

    fun getTripsForVehicle(vehicleId: Long) = tripDao.getTripsByVehicle(vehicleId)
    suspend fun getTripsByVehicleSync(vehicleId: Long) = tripDao.getTripsByVehicleSync(vehicleId)
    suspend fun getVehicleById(vehicleId: Long) = vehicleDao.getVehicleById(vehicleId)

    fun scanVehicleGaps(vehicleId: Long, onResult: (List<OdometerGap>) -> Unit) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return@launch onResult(emptyList())
            val trips = tripDao.getTripsByVehicleSync(vehicleId)
            val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
            onResult(gaps)
        }
    }

    fun confirmOdometerCheck(vehicle: Vehicle, correctedOdometer: Int, photoPath: String? = null) {
        confirmOdometerCheckWithDetails(
            vehicle = vehicle,
            correctedOdometer = correctedOdometer,
            photoPath = photoPath,
            strategy = vehicle.odometerCorrectionStrategy,
            reason = null
        )
    }

    fun confirmOdometerCheckWithDetails(
        vehicle: Vehicle,
        correctedOdometer: Int,
        photoPath: String? = null,
        strategy: String = "NONE",
        reason: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val gap = correctedOdometer - vehicle.odometerCurrent
            val affectedTripIds = mutableListOf<Long>()

            if (gap != 0) {
                when (strategy) {
                    "CREATE_TRIP" -> {
                        if (gap > 0) {
                            val lastTrip = tripDao.getTripsByVehicleSync(vehicle.id).maxByOrNull { it.startTime }
                            val startAddr = lastTrip?.endAddress ?: "Laatste geregistreerde bestemming"
                            val endAddr = startAddr
                            val noteText = reason?.takeIf { it.isNotBlank() } ?: "Sluitende correctierit km-controle (${if (gap > 0) "+" else ""}$gap km)"

                            val correctionTrip = Trip(
                                vehicleId = vehicle.id,
                                startTime = timestamp - 1000,
                                endTime = timestamp,
                                startAddress = startAddr,
                                endAddress = endAddr,
                                distanceMeters = gap * 1000,
                                tripType = "PERSONAL",
                                note = noteText,
                                status = "DONE",
                                isManual = true,
                                odometerStart = vehicle.odometerCurrent,
                                odometerEnd = correctedOdometer
                            )
                            val newTripId = tripDao.insertTrip(correctionTrip)
                            affectedTripIds.add(newTripId)
                        }
                    }
                    "GAP_WITH_REASON" -> {
                        val gapResolution = OdometerGapResolution(
                            vehicleId = vehicle.id,
                            detectedAt = timestamp,
                            resolvedAt = timestamp,
                            gapType = if (gap > 0) "MISSING_KM" else "OVERLAPPING_KM",
                            gapStartTimestamp = vehicle.lastOdometerCheckTimestamp.takeIf { it > 0 } ?: (timestamp - 86400000L),
                            gapEndTimestamp = timestamp,
                            expectedOdometer = vehicle.odometerCurrent,
                            actualOdometer = correctedOdometer,
                            gapKm = gap,
                            resolution = "MARK_AS_EXPLAINED",
                            resolutionNote = reason ?: "Tussentijdse km-controle afwijking verklaard",
                            affectedTripIds = null,
                            status = "RESOLVED"
                        )
                        odometerGapResolutionDao.insert(gapResolution)
                    }
                    "MANUAL" -> {
                        val gapResolution = OdometerGapResolution(
                            vehicleId = vehicle.id,
                            detectedAt = timestamp,
                            gapType = if (gap > 0) "MISSING_KM" else "OVERLAPPING_KM",
                            gapStartTimestamp = vehicle.lastOdometerCheckTimestamp.takeIf { it > 0 } ?: (timestamp - 86400000L),
                            gapEndTimestamp = timestamp,
                            expectedOdometer = vehicle.odometerCurrent,
                            actualOdometer = correctedOdometer,
                            gapKm = gap,
                            resolution = "IGNORE",
                            resolutionNote = reason,
                            affectedTripIds = null,
                            status = "OPEN"
                        )
                        odometerGapResolutionDao.insert(gapResolution)
                    }
                    "LEAVE_GAP", "NONE" -> {
                        // Do not modify trips, just update vehicle
                    }
                }
            }

            // Record check with full audit trail
            val check = OdometerCheck(
                vehicleId = vehicle.id,
                timestamp = timestamp,
                registeredOdometer = vehicle.odometerCurrent,
                correctedOdometer = correctedOdometer,
                photoPath = photoPath,
                deviationKm = gap,
                correctionStrategy = strategy,
                correctionReason = reason,
                status = if (strategy == "MANUAL") "PENDING" else "CONFIRMED",
                affectedTripIds = if (affectedTripIds.isNotEmpty()) affectedTripIds.joinToString(",") else null
            )
            odometerCheckDao.insert(check)

            val updatedVehicle = vehicle.copy(
                odometerCurrent = correctedOdometer,
                lastOdometerCheckTimestamp = timestamp
            )
            vehicleDao.updateVehicle(updatedVehicle)
            onComplete()
        }
    }

    fun resolveGap(
        gap: OdometerGap,
        fixType: FixType,
        note: String?,
        customStartAddress: String? = null,
        customEndAddress: String? = null,
        tripType: String = "PERSONAL",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val affectedIds = mutableListOf<Long>()

            when (fixType) {
                FixType.CREATE_PRIVATE_TRIP -> {
                    val startAddr = customStartAddress?.takeIf { it.isNotBlank() }
                        ?: gap.previousTrip?.endAddress
                        ?: "Laatste bekende bestemming"
                    val endAddr = customEndAddress?.takeIf { it.isNotBlank() }
                        ?: gap.nextTrip?.startAddress
                        ?: startAddr
                    val defaultNote = if (tripType == "BUSINESS") "Zakelijke rit voor sluitende tellerstand" else "Privérit voor sluitende tellerstand"
                    val trip = Trip(
                        vehicleId = gap.vehicleId,
                        startTime = gap.gapTimeStart,
                        endTime = gap.gapTimeEnd,
                        startAddress = startAddr,
                        endAddress = endAddr,
                        distanceMeters = kotlin.math.abs(gap.gapKm) * 1000,
                        tripType = tripType,
                        note = note?.takeIf { it.isNotBlank() } ?: defaultNote,
                        status = "DONE",
                        isManual = true,
                        odometerStart = gap.expectedOdometer,
                        odometerEnd = gap.actualOdometer
                    )
                    val newId = tripDao.insertTrip(trip)
                    affectedIds.add(newId)
                }
                FixType.ADJUST_SINGLE_TRIP -> {
                    if (gap.previousTrip != null) {
                        val prevDistKm = kotlin.math.round(gap.previousTrip.distanceMeters / 1000.0).toInt()
                        val newDist = maxOf(1, prevDistKm + gap.gapKm)
                        tripDao.updateTrip(gap.previousTrip.copy(
                            distanceMeters = newDist * 1000,
                            odometerEnd = gap.previousTrip.odometerStart + newDist
                        ))
                        affectedIds.add(gap.previousTrip.id)
                    } else if (gap.nextTrip != null) {
                        val nextDistKm = kotlin.math.round(gap.nextTrip.distanceMeters / 1000.0).toInt()
                        val newDist = maxOf(1, nextDistKm - gap.gapKm)
                        val nextEnd = gap.nextTrip.odometerEnd ?: (gap.nextTrip.odometerStart + nextDistKm)
                        tripDao.updateTrip(gap.nextTrip.copy(
                            distanceMeters = newDist * 1000,
                            odometerStart = nextEnd - newDist
                        ))
                        affectedIds.add(gap.nextTrip.id)
                    }
                }
                FixType.SYNC_VEHICLE_ODOMETER -> {
                    val vehicle = vehicleDao.getVehicleById(gap.vehicleId)
                    if (vehicle != null) {
                        vehicleDao.updateVehicle(vehicle.copy(odometerCurrent = gap.expectedOdometer))
                    }
                }
                FixType.MARK_AS_EXPLAINED, FixType.IGNORE -> {
                    // Stored directly in OdometerGapResolution table below
                }
            }

            val resolution = OdometerGapResolution(
                vehicleId = gap.vehicleId,
                detectedAt = now,
                resolvedAt = now,
                gapType = gap.gapType.name,
                gapStartTimestamp = gap.gapTimeStart,
                gapEndTimestamp = gap.gapTimeEnd,
                expectedOdometer = gap.expectedOdometer,
                actualOdometer = gap.actualOdometer,
                gapKm = gap.gapKm,
                resolution = fixType.name,
                resolutionNote = note,
                affectedTripIds = if (affectedIds.isNotEmpty()) affectedIds.joinToString(",") else null,
                status = if (fixType == FixType.IGNORE) "IGNORED" else "RESOLVED"
            )
            odometerGapResolutionDao.insert(resolution)

            // Ensure vehicle current odometer is at least the latest trip end
            if (fixType != FixType.SYNC_VEHICLE_ODOMETER) {
                val vehicle = vehicleDao.getVehicleById(gap.vehicleId)
                if (vehicle != null) {
                    val latest = tripDao.getTripsByVehicleSync(gap.vehicleId).maxByOrNull { it.startTime }
                    val latestEnd = latest?.odometerEnd ?: latest?.let { it.odometerStart + kotlin.math.round(it.distanceMeters / 1000.0).toInt() }
                    if (latestEnd != null && latestEnd > vehicle.odometerCurrent) {
                        vehicleDao.updateVehicle(vehicle.copy(odometerCurrent = latestEnd))
                    }
                }
            }

            onComplete()
        }
    }

    fun previewRecalculateOdometerChain(
        vehicleId: Long,
        onResult: (com.cimdriver.app.util.RecalculationPreview?) -> Unit
    ) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId)
            if (vehicle == null) {
                onResult(null)
                return@launch
            }
            val trips = tripDao.getTripsByVehicleSync(vehicleId)
            val checks = odometerCheckDao.getChecksForVehicleSync(vehicleId)
            val resolutions = odometerGapResolutionDao.getResolutionsForVehicleSync(vehicleId)

            val preview = com.cimdriver.app.util.OdometerRecalculator.preview(
                vehicle = vehicle,
                trips = trips,
                odometerChecks = checks,
                resolutions = resolutions
            )
            onResult(preview)
        }
    }

    fun applyRecalculateOdometerChain(
        vehicleId: Long,
        preview: com.cimdriver.app.util.RecalculationPreview,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return@launch onComplete(0)

            // 1. Bulk update trips
            if (preview.recalculatedTrips.isNotEmpty()) {
                tripDao.updateTrips(preview.recalculatedTrips)
            }

            // 2. Update vehicle current odometer
            vehicleDao.updateVehicle(vehicle.copy(odometerCurrent = preview.newCurrentOdometer))

            onComplete(preview.modifiedTripsCount)
        }
    }

    fun batchResolveSmallGaps(vehicleId: Long, maxGapKm: Int = 5, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return@launch onComplete(0)
            val trips = tripDao.getTripsByVehicleSync(vehicleId)
            val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
            val smallGaps = gaps.filter { kotlin.math.abs(it.gapKm) <= maxGapKm }

            var resolvedCount = 0
            for (gap in smallGaps) {
                if (gap.gapKm > 0) {
                    resolveGap(gap, FixType.CREATE_PRIVATE_TRIP, "Automatische aansluiting (< ${maxGapKm} km)")
                } else {
                    resolveGap(gap, FixType.ADJUST_SINGLE_TRIP, "Automatische correctie overlap (< ${maxGapKm} km)")
                }
                resolvedCount++
            }
            onComplete(resolvedCount)
        }
    }

    fun setAsDefaultVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.clearDefaultVehicles()
            val updatedVehicle = vehicle.copy(isDefault = true)
            vehicleDao.updateVehicle(updatedVehicle)
        }
    }

    suspend fun canDeleteVehicle(vehicleId: Long): Boolean {
        val count = tripDao.getTripCountForVehicle(vehicleId)
        return count == 0
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.deleteVehicle(vehicle)
        }
    }

    fun addBluetoothDeviceToVehicle(macAddress: String, deviceName: String, vehicleId: Long) {
        viewModelScope.launch {
            val device = BluetoothDevice(
                macAddress = macAddress,
                deviceName = deviceName,
                vehicleId = vehicleId
            )
            bluetoothDeviceDao.insertDevice(device)
        }
    }

    fun removeBluetoothDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothDeviceDao.deleteDevice(device)
        }
    }

    fun getBondedBluetoothDevices(): List<Pair<String, String>> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return emptyList()
        
        // Permission check for BLUETOOTH_CONNECT (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
        }
        
        return adapter.bondedDevices.map { it.address to it.name }
    }
}
