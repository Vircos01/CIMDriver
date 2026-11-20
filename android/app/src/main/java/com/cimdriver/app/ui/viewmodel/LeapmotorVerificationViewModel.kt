package com.cimdriver.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.dao.OdometerCheckDao
import com.cimdriver.app.data.local.dao.TripDao
import com.cimdriver.app.data.local.dao.VehicleDao
import com.cimdriver.app.data.local.entity.OdometerCheck
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.util.LeapmotorActionType
import com.cimdriver.app.util.LeapmotorComparisonReport
import com.cimdriver.app.util.LeapmotorMatchItem
import com.cimdriver.app.util.LeapmotorMatchStatus
import com.cimdriver.app.util.LeapmotorParser
import com.cimdriver.app.util.LeapmotorScreenshotParseResult
import com.cimdriver.app.util.LeapmotorTripMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class LeapmotorExecutedAction(
    val id: String,
    val tripKey: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tripSummary: String,
    val actionTitle: String,
    val actionDetail: String,
    val type: LeapmotorActionType,
    val targetTripId: Long? = null
)

@HiltViewModel
class LeapmotorVerificationViewModel @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val tripDao: TripDao,
    private val odometerCheckDao: OdometerCheckDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _report = MutableStateFlow<LeapmotorComparisonReport?>(null)
    val report: StateFlow<LeapmotorComparisonReport?> = _report.asStateFlow()

    private val _lastParseResult = MutableStateFlow<LeapmotorScreenshotParseResult?>(null)
    val lastParseResult: StateFlow<LeapmotorScreenshotParseResult?> = _lastParseResult.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // History of actions performed during this verification session
    private val _executedActions = MutableStateFlow<List<LeapmotorExecutedAction>>(emptyList())
    val executedActions: StateFlow<List<LeapmotorExecutedAction>> = _executedActions.asStateFlow()

    // Trips in database already marked as Leapmotor verified/inserted
    private val _previouslyVerifiedTrips = MutableStateFlow<List<Trip>>(emptyList())
    val previouslyVerifiedTrips: StateFlow<List<Trip>> = _previouslyVerifiedTrips.asStateFlow()

    init {
        viewModelScope.launch {
            val all = vehicleDao.getAllVehiclesSync()
            _vehicles.value = all
            val leapmotor = all.find { it.make.contains("Leapmotor", true) || it.model.contains("B10", true) }
                ?: all.firstOrNull { it.isActive }
                ?: all.firstOrNull()
            _selectedVehicle.value = leapmotor
            if (leapmotor != null) {
                loadPreviouslyVerifiedTrips(leapmotor.id)
            }
        }
    }

    private suspend fun loadPreviouslyVerifiedTrips(vehicleId: Long) {
        val trips = tripDao.getTripsByVehicleSync(vehicleId)
        _previouslyVerifiedTrips.value = trips.filter {
            !it.note.isNullOrBlank() && it.note.contains("Leapmotor", ignoreCase = true)
        }.sortedByDescending { it.startTime }
    }

    fun selectVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        viewModelScope.launch {
            loadPreviouslyVerifiedTrips(vehicle.id)
        }
        _lastParseResult.value?.let { parseResult ->
            recalculateReport(vehicle, parseResult)
        }
    }

    fun selectVehicleById(vehicleId: Long) {
        viewModelScope.launch {
            val v = vehicleDao.getVehicleById(vehicleId)
            if (v != null) {
                selectVehicle(v)
            }
        }
    }

    fun processImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        _isProcessing.value = true
        _message.value = null

        viewModelScope.launch {
            try {
                val parseResult = LeapmotorParser.parseFromImageUri(context, uri)
                _lastParseResult.value = parseResult

                val vehicle = _selectedVehicle.value
                if (vehicle == null) {
                    _message.value = "Geen voertuig geselecteerd"
                    _isProcessing.value = false
                    return@launch
                }

                if (parseResult.trips.isEmpty()) {
                    _message.value = "Geen ritten herkend op het screenshot. Zorg dat het weekoverzicht van de Leapmotor app zichtbaar is."
                    _report.value = null
                } else {
                    recalculateReport(vehicle, parseResult)
                }
            } catch (e: Exception) {
                _message.value = "Fout bij verwerken: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processRawText(text: String) {
        _isProcessing.value = true
        _message.value = null

        viewModelScope.launch {
            try {
                val parseResult = LeapmotorParser.parseFromText(text)
                _lastParseResult.value = parseResult

                val vehicle = _selectedVehicle.value
                if (vehicle == null) {
                    _message.value = "Geen voertuig geselecteerd"
                    _isProcessing.value = false
                    return@launch
                }

                if (parseResult.trips.isEmpty()) {
                    _message.value = "Geen ritten gevonden in de tekst."
                    _report.value = null
                } else {
                    recalculateReport(vehicle, parseResult)
                }
            } catch (e: Exception) {
                _message.value = "Fout bij verwerken: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Proactively cleans up identical duplicate trips on the vehicle (e.g. from previous tests or double-adds).
     */
    suspend fun cleanupDuplicateTrips(vehicleId: Long): Int {
        val trips = tripDao.getTripsByVehicleSync(vehicleId)
        val toDelete = mutableListOf<Trip>()
        for (i in 0 until trips.size) {
            for (j in i + 1 until trips.size) {
                val t1 = trips[i]
                val t2 = trips[j]
                if (t1 in toDelete || t2 in toDelete) continue
                // Same vehicle, start time within 2 minutes of each other
                if (abs(t1.startTime - t2.startTime) <= 2 * 60 * 1000L) {
                    val t1Verified = t1.note?.contains("Leapmotor", ignoreCase = true) == true
                    val t2Verified = t2.note?.contains("Leapmotor", ignoreCase = true) == true
                    val keep = when {
                        t1Verified && !t2Verified -> t1
                        t2Verified && !t1Verified -> t2
                        t1.distanceMeters >= t2.distanceMeters -> t1
                        else -> t2
                    }
                    val del = if (keep == t1) t2 else t1
                    toDelete.add(del)
                }
            }
        }
        for (del in toDelete) {
            tripDao.deleteTrip(del)
        }
        return toDelete.size
    }

    private fun recalculateReport(vehicle: Vehicle, parseResult: LeapmotorScreenshotParseResult) {
        viewModelScope.launch {
            // Automatically clean up any identical duplicate trips first
            val cleaned = cleanupDuplicateTrips(vehicle.id)
            if (cleaned > 0) {
                recalculateOdometerChain(vehicle.id)
            }
            val cimTrips = tripDao.getTripsByVehicleSync(vehicle.id)
            val newReport = LeapmotorTripMatcher.match(
                lmTrips = parseResult.trips,
                cimTrips = cimTrips,
                vehicle = vehicle,
                detectedOdometerKm = parseResult.detectedOdometerKm
            )
            _report.value = newReport
            loadPreviouslyVerifiedTrips(vehicle.id)
        }
    }

    private suspend fun performFix(item: LeapmotorMatchItem): LeapmotorExecutedAction? {
        val vehicle = _selectedVehicle.value ?: return null
        val action = item.suggestedAction
        val lm = item.lmTrip
        val tripKey = "${lm.dateText}_${lm.startTimeText}"

        var executedAction: LeapmotorExecutedAction? = null

        when (action.type) {
            LeapmotorActionType.TRIM_END_TIME -> {
                val tripId = action.targetTripId ?: return null
                val trip = tripDao.getTripById(tripId) ?: return null

                val noteSuffix = "[Geverifieerd met Leapmotor B10 - ${item.endDeltaMinutes}m stilstand gecorrigeerd]"
                val updatedTrip = trip.copy(
                    endTime = action.newEndTime ?: trip.endTime,
                    note = appendNote(trip.note, noteSuffix)
                )
                tripDao.updateTrip(updatedTrip)

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "${item.endDeltaMinutes} min loze stilstand afgeknipt",
                    actionDetail = "Eindtijd gecorrigeerd naar ${lm.endTimeText} conform motorstop Leapmotor",
                    type = action.type,
                    targetTripId = tripId
                )
            }

            LeapmotorActionType.EXTEND_START_TIME -> {
                val tripId = action.targetTripId ?: return null
                val trip = tripDao.getTripById(tripId) ?: return null

                val noteSuffix = "[Geverifieerd met Leapmotor B10 - vertrek ${item.startDeltaMinutes}m hersteld]"
                val updatedTrip = trip.copy(
                    startTime = action.newStartTime ?: trip.startTime,
                    note = appendNote(trip.note, noteSuffix)
                )
                tripDao.updateTrip(updatedTrip)

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "Vertrektijd ${item.startDeltaMinutes}m hersteld",
                    actionDetail = "Starttijd gecorrigeerd naar ${lm.startTimeText} conform motorstart Leapmotor",
                    type = action.type,
                    targetTripId = tripId
                )
            }

            LeapmotorActionType.ALIGN_TIMES -> {
                val tripId = action.targetTripId ?: return null
                val trip = tripDao.getTripById(tripId) ?: return null

                val noteSuffix = "[Geverifieerd met Leapmotor B10 - tijden gesynchroniseerd]"
                val updatedTrip = trip.copy(
                    startTime = action.newStartTime ?: trip.startTime,
                    endTime = action.newEndTime ?: trip.endTime,
                    note = appendNote(trip.note, noteSuffix)
                )
                tripDao.updateTrip(updatedTrip)

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "Start- en eindtijd gesynchroniseerd",
                    actionDetail = "Tijdsvenster aangepast naar ${lm.startTimeText} - ${lm.endTimeText} (${lm.durationMinutes} min)",
                    type = action.type,
                    targetTripId = tripId
                )
            }

            LeapmotorActionType.CONFIRM_VERIFIED -> {
                val tripId = action.targetTripId ?: return null
                val trip = tripDao.getTripById(tripId) ?: return null

                val noteSuffix = "[Geverifieerd met Leapmotor B10]"
                val updatedTrip = trip.copy(
                    note = appendNote(trip.note, noteSuffix)
                )
                tripDao.updateTrip(updatedTrip)

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "Rit bevestigd als geverifieerd",
                    actionDetail = "Boordcomputertijden ${lm.startTimeText}-${lm.endTimeText} (${lm.durationMinutes} min) vastgelegd in administratie",
                    type = action.type,
                    targetTripId = tripId
                )
            }

            LeapmotorActionType.ADD_MISSING_TRIP -> {
                val startTime = action.newStartTime ?: return null
                val endTime = action.newEndTime ?: return null
                val allTrips = tripDao.getTripsByVehicleSync(vehicle.id)

                // DEDUP CHECK: Check if a trip already exists covering this time window
                val existingTrip = allTrips.firstOrNull {
                    val eStart = it.startTime
                    val eEnd = it.effectiveEndTime()
                    abs(eStart - startTime) <= 2 * 60 * 1000L || abs(eEnd - endTime) <= 2 * 60 * 1000L ||
                            (eStart < endTime && eEnd > startTime)
                }

                if (existingTrip != null) {
                    val updated = existingTrip.copy(
                        startTime = startTime,
                        endTime = endTime,
                        note = appendNote(existingTrip.note, "[Geverifieerd met Leapmotor B10]")
                    )
                    tripDao.updateTrip(updated)

                    executedAction = LeapmotorExecutedAction(
                        id = "exec_${System.currentTimeMillis()}_${item.id}",
                        tripKey = tripKey,
                        tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                        actionTitle = "Bestaande rit gesynchroniseerd",
                        actionDetail = "Tijdsvenster aangepast naar ${lm.startTimeText} - ${lm.endTimeText} (${lm.durationMinutes} min)",
                        type = action.type,
                        targetTripId = existingTrip.id
                    )
                } else {
                    val prevTrip = allTrips.lastOrNull { (it.endTime ?: it.startTime) <= startTime }
                    val nextTrip = allTrips.firstOrNull { it.startTime >= endTime }

                    val startAddress = prevTrip?.endAddress ?: "Onbekend vertrek"
                    val endAddress = nextTrip?.startAddress ?: prevTrip?.startAddress ?: "Onbekend bestemming"
                    val distanceM = action.estimatedDistanceMeters ?: (item.lmTrip.durationMinutes * 500)
                    val startOdo = prevTrip?.odometerEnd ?: vehicle.odometerCurrent
                    val endOdo = startOdo + (distanceM / 1000.0).roundToInt()

                    val newTrip = Trip(
                        vehicleId = vehicle.id,
                        startTime = startTime,
                        endTime = endTime,
                        startAddress = startAddress,
                        endAddress = endAddress,
                        distanceMeters = distanceM,
                        tripType = "BUSINESS",
                        note = "[Ingevoegd via Leapmotor boordcomputer]",
                        status = "DONE",
                        isManual = true,
                        odometerStart = startOdo,
                        odometerEnd = endOdo
                    )
                    val insertedId = tripDao.insertTrip(newTrip)

                    val kmStr = String.format(Locale.US, "%.1f", distanceM / 1000.0)
                    executedAction = LeapmotorExecutedAction(
                        id = "exec_${System.currentTimeMillis()}_${item.id}",
                        tripKey = tripKey,
                        tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                        actionTitle = "Gemiste rit toegevoegd (${lm.durationMinutes} min)",
                        actionDetail = "Nieuwe rit aangemaakt van ${lm.startTimeText} tot ${lm.endTimeText} ($kmStr km, $startAddress → $endAddress)",
                        type = action.type,
                        targetTripId = insertedId
                    )
                }
            }

            LeapmotorActionType.SPLIT_TRIP -> {
                val tripId = action.targetTripId ?: return null
                val trip = tripDao.getTripById(tripId) ?: return null
                val updatedTrip = trip.copy(
                    startTime = action.newStartTime ?: trip.startTime,
                    endTime = action.newEndTime ?: trip.endTime,
                    note = appendNote(trip.note, "[Gesplitst via Leapmotor boordcomputer]")
                )
                tripDao.updateTrip(updatedTrip)

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "Rit gesplitst",
                    actionDetail = "Rit begrensd op boordcomputervenster ${lm.startTimeText} - ${lm.endTimeText}",
                    type = action.type,
                    targetTripId = tripId
                )
            }

            LeapmotorActionType.MERGE_TRIPS -> {
                val candidateIds = if (action.candidateTripIds.isNotEmpty()) {
                    action.candidateTripIds
                } else {
                    listOfNotNull(action.targetTripId)
                }

                val loadedTrips = candidateIds.mapNotNull { tripDao.getTripById(it) }.sortedBy { it.startTime }

                if (loadedTrips.isEmpty()) {
                    return null
                }

                val primary = loadedTrips.first()
                val redundantTrips = loadedTrips.drop(1)

                val newStart = minOf(loadedTrips.minOf { it.startTime }, lm.startTimestamp)
                val newEnd = maxOf(loadedTrips.maxOf { it.effectiveEndTime() }, lm.endTimestamp)
                val newDist = when {
                    lm.distanceKm != null -> (lm.distanceKm * 1000.0).roundToInt()
                    action.estimatedDistanceMeters != null -> action.estimatedDistanceMeters
                    loadedTrips.all { abs(it.startTime - primary.startTime) <= 3 * 60 * 1000L } -> loadedTrips.maxOf { it.distanceMeters }
                    else -> loadedTrips.sumOf { it.distanceMeters }
                }

                val updatedPrimary = primary.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    startAddress = primary.startAddress,
                    endAddress = loadedTrips.last().endAddress ?: primary.endAddress,
                    distanceMeters = newDist,
                    note = appendNote(primary.note, "[Geverifieerd met Leapmotor B10 - samengevoegd]")
                )
                tripDao.updateTrip(updatedPrimary)

                // CRITICAL: delete redundant / duplicate trips so they are truly removed from the database!
                for (red in redundantTrips) {
                    tripDao.deleteTrip(red)
                }

                executedAction = LeapmotorExecutedAction(
                    id = "exec_${System.currentTimeMillis()}_${item.id}",
                    tripKey = tripKey,
                    tripSummary = "${lm.dateText} ${lm.startTimeText} - ${lm.endTimeText}",
                    actionTitle = "Ritten samengevoegd (${loadedTrips.size} ritten)",
                    actionDetail = "Samengevoegd tot 1 rit (${lm.startTimeText}-${lm.endTimeText}) en ${redundantTrips.size} overbodige/dubbele ritten verwijderd.",
                    type = action.type,
                    targetTripId = primary.id
                )
            }

            LeapmotorActionType.NONE -> {}
        }

        return executedAction
    }

    fun applyFix(item: LeapmotorMatchItem) {
        val vehicle = _selectedVehicle.value ?: return
        viewModelScope.launch {
            val executed = performFix(item)
            if (executed != null) {
                val tripKey = "${item.lmTrip.dateText}_${item.lmTrip.startTimeText}"
                _executedActions.value = _executedActions.value.filter { it.tripKey != tripKey } + executed
            }

            // Recalculate odometer chain for vehicle
            recalculateOdometerChain(vehicle.id)

            // Refresh report immediately against updated database
            _lastParseResult.value?.let { recalculateReport(vehicle, it) }
        }
    }

    private fun appendNote(existingNote: String?, newTag: String): String {
        return if (existingNote.isNullOrBlank()) {
            newTag
        } else if (!existingNote.contains(newTag)) {
            "$existingNote $newTag"
        } else {
            existingNote
        }
    }

    fun applyAllFixes() {
        val currentReport = _report.value ?: return
        val vehicle = _selectedVehicle.value ?: return

        viewModelScope.launch {
            var appliedCount = 0
            for (item in currentReport.items) {
                if (item.suggestedAction.type != LeapmotorActionType.NONE &&
                    item.status != LeapmotorMatchStatus.EXACT_MATCH
                ) {
                    val executed = performFix(item)
                    if (executed != null) {
                        val tripKey = "${item.lmTrip.dateText}_${item.lmTrip.startTimeText}"
                        _executedActions.value = _executedActions.value.filter { it.tripKey != tripKey } + executed
                        appliedCount++
                    }
                }
            }

            recalculateOdometerChain(vehicle.id)
            _lastParseResult.value?.let { recalculateReport(vehicle, it) }
            _message.value = "$appliedCount correcties succesvol uitgevoerd en vastgelegd in actielog!"
        }
    }

    fun confirmOdometerCheckpoint(odometerKm: Int) {
        val vehicle = _selectedVehicle.value ?: return
        viewModelScope.launch {
            val check = OdometerCheck(
                vehicleId = vehicle.id,
                timestamp = System.currentTimeMillis(),
                registeredOdometer = vehicle.odometerCurrent,
                correctedOdometer = odometerKm,
                deviationKm = odometerKm - vehicle.odometerCurrent,
                correctionReason = "Geïmporteerd uit weekoverzicht Leapmotor app",
                status = "CONFIRMED"
            )
            odometerCheckDao.insert(check)

            val updatedVehicle = vehicle.copy(
                odometerCurrent = maxOf(vehicle.odometerCurrent, odometerKm),
                lastOdometerCheckTimestamp = System.currentTimeMillis()
            )
            vehicleDao.updateVehicle(updatedVehicle)
            _selectedVehicle.value = updatedVehicle

            val action = LeapmotorExecutedAction(
                id = "exec_odo_${System.currentTimeMillis()}",
                tripKey = "odometer_$odometerKm",
                tripSummary = "Kilometertellerstand $odometerKm km",
                actionTitle = "Fysiek teller-ijkpunt geregistreerd: $odometerKm km",
                actionDetail = "Opgeslagen als officiële OdometerCheck voor Belastingdienst audit-trail",
                type = LeapmotorActionType.CONFIRM_VERIFIED
            )
            _executedActions.value = _executedActions.value + action
            _message.value = "Tellerstand $odometerKm km geregistreerd als fysiek ijkpunt."
        }
    }

    private suspend fun recalculateOdometerChain(vehicleId: Long) {
        val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return
        val trips = tripDao.getTripsByVehicleSync(vehicleId)
        var currentOdo = vehicle.odometerStart

        for (trip in trips) {
            val distKm = Math.round(trip.distanceMeters / 1000.0).toInt()
            val updated = trip.copy(
                odometerStart = currentOdo,
                odometerEnd = currentOdo + distKm
            )
            tripDao.updateTrip(updated)
            currentOdo += distKm
        }

        val updatedVehicle = vehicle.copy(odometerCurrent = currentOdo)
        vehicleDao.updateVehicle(updatedVehicle)
        _selectedVehicle.value = updatedVehicle
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun Trip.effectiveEndTime(): Long {
        return endTime ?: (startTime + (distanceMeters / 15).coerceAtLeast(60) * 1000L)
    }
}
