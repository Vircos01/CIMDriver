package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import android.app.Application
import androidx.room.withTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.data.repository.TripRepository
import com.cimdriver.app.domain.DashboardStatsCalculator
import com.cimdriver.app.service.GeocoderService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.cimdriver.app.util.TripDistance
import com.cimdriver.app.util.TripCategory
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.util.AddressMatching
import com.cimdriver.app.util.TripStatus
import java.util.Calendar
import java.util.Date

enum class TimeFilter {
    WEEK, MONTH, YEAR
}

data class DashboardStats(
    val zakelijkKm: Double = 0.0,
    val priveKm: Double = 0.0,
    val woonWerkKm: Double = 0.0,
    val zakelijkMin: Long = 0,
    val priveMin: Long = 0,
    val woonWerkMin: Long = 0,
    val prevZakelijkKm: Double = 0.0,
    val prevPriveKm: Double = 0.0,
    val prevWoonWerkKm: Double = 0.0,
    val ytdPriveKm: Double = 0.0
)

data class DistanceChartItem(
    val label: String,
    val zakelijkKm: Double,
    val priveKm: Double,
    val woonWerkKm: Double,
    val totalKm: Double
)

data class CategoryBreakdownItem(
    val categoryName: String,
    val distanceKm: Double,
    val percentage: Float
)

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val database: AppDatabase
) : ViewModel() {
    private data class ClassificationContext(
        val rules: List<com.cimdriver.app.data.local.entity.ClassificationRule>,
        val addresses: List<com.cimdriver.app.data.local.entity.SavedAddress>
    )

    private val classificationRules = tripRepository.getClassificationRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val savedAddresses = tripRepository.getSavedAddresses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val classificationContext = kotlinx.coroutines.flow.combine(classificationRules, savedAddresses) { rules, addresses ->
        ClassificationContext(rules, addresses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClassificationContext(emptyList(), emptyList()))

    val trips: StateFlow<List<Trip>> = tripRepository.getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _dismissedMerges = MutableStateFlow<Set<Pair<Long, Long>>>(emptySet())

    fun acceptMerge(trip1: Trip, trip2: Trip) {
        viewModelScope.launch {
            val mergedTrip = trip1.copy(
                endTime = trip2.endTime,
                endAddress = trip2.endAddress,
                odometerEnd = trip2.odometerEnd,
                distanceMeters = trip1.distanceMeters + trip2.distanceMeters,
                status = TripStatus.TO_REVIEW.value
            )
            tripRepository.updateTrip(mergedTrip)
            tripRepository.deleteTrip(trip2)
        }
    }

    fun rejectMerge(trip1: Trip, trip2: Trip) {
        _dismissedMerges.value = _dismissedMerges.value + Pair(trip1.id, trip2.id)
    }

    private val _tripFilter = kotlinx.coroutines.flow.MutableStateFlow("ALLE") // "ALLE", "BUSINESS", "PRIVATE"
    val tripFilter: StateFlow<String> = _tripFilter.asStateFlow()

    private val _selectedYear = kotlinx.coroutines.flow.MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setTripFilter(filter: String) {
        _tripFilter.value = filter
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun reviewTrip(trip: Trip, tripType: String) {
        viewModelScope.launch {
            tripRepository.updateTrip(trip.copy(tripType = tripType, status = TripStatus.DONE.value))
        }
    }

    val availableYears = trips.map { tripList ->
        val years = tripList.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTime
            cal.get(Calendar.YEAR)
        }.toSet().toMutableList()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (!years.contains(currentYear)) years.add(currentYear)
        years.sortedDescending()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(Calendar.getInstance().get(Calendar.YEAR))
    )

    val vehicles: StateFlow<List<Vehicle>> = tripRepository.getAllVehicles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _dashboardTimeFilter = MutableStateFlow(TimeFilter.MONTH)
    val dashboardTimeFilter: StateFlow<TimeFilter> = _dashboardTimeFilter.asStateFlow()

    fun setDashboardTimeFilter(filter: TimeFilter) {
        _dashboardTimeFilter.value = filter
    }

    private val _globalSelectedVehicleId = MutableStateFlow<Long?>(null)
    val globalSelectedVehicleId: StateFlow<Long?> = _globalSelectedVehicleId.asStateFlow()

    fun setGlobalSelectedVehicleId(vehicleId: Long?) {
        _globalSelectedVehicleId.value = vehicleId
    }

    val mergeSuggestion: StateFlow<Pair<Trip, Trip>?> = kotlinx.coroutines.flow.combine(trips, _dismissedMerges, _globalSelectedVehicleId, vehicles) { tripList, dismissed, selectedVehicleId, vehicleList ->
        var suggestion: Pair<Trip, Trip>? = null
        val activeVehicleId = selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id
        
        val vehicleTrips = tripList.filter { it.vehicleId == activeVehicleId }
        
        for (i in 0 until vehicleTrips.size - 1) {
            val newer = vehicleTrips[i]
            val older = vehicleTrips[i + 1]
            if (newer.vehicleId != null) {
                if (older.endTime != null && newer.startTime - older.endTime in 0..(15 * 60 * 1000L)) {
                    if (!dismissed.contains(Pair(older.id, newer.id))) {
                        suggestion = Pair(older, newer)
                        break
                    }
                }
            }
        }
        suggestion
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            com.cimdriver.app.service.VehicleSelectionStore.requestedVehicleId.collect { id ->
                if (id != null) {
                    _globalSelectedVehicleId.value = id
                    com.cimdriver.app.service.VehicleSelectionStore.clearRequest()
                }
            }
        }
    }

    private val filterState = kotlinx.coroutines.flow.combine(_tripFilter, _selectedYear, _searchQuery) { filter, year, query ->
        Triple(filter, year, query)
    }

    val filteredTrips = kotlinx.coroutines.flow.combine(trips, filterState, _globalSelectedVehicleId, vehicles, classificationContext) { tripList, filters, selectedVehicleId, vehicleList, context ->
        val rules = context.rules
        val addresses = context.addresses
        val (filter, year, query) = filters
        val activeVehicleId = selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id
        
        tripList.filter {
            val isVehicleMatch = if (activeVehicleId != null) it.vehicleId == activeVehicleId else true
            
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTime
            val isYearMatch = cal.get(Calendar.YEAR) == year
            
            val isFilterMatch = if (filter == "ALLE") true else {
                val category = DashboardStatsCalculator.classifyTrip(it, addresses, rules)
                
                when (filter) {
                    "BUSINESS" -> category == TripCategory.BUSINESS
                    "COMMUTE" -> category == TripCategory.COMMUTE
                    "PRIVATE" -> category == TripCategory.PRIVATE
                    else -> it.tripType == filter
                }
            }
            
            val isSearchMatch = if (query.isBlank()) {
                true
            } else {
                val q = query.lowercase()
                (it.startAddress?.lowercase()?.contains(q) == true) ||
                (it.endAddress?.lowercase()?.contains(q) == true) ||
                (it.note?.lowercase()?.contains(q) == true)
            }
            
            isVehicleMatch && isYearMatch && isFilterMatch && isSearchMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dashboardStats = kotlinx.coroutines.flow.combine(trips, _dashboardTimeFilter, _globalSelectedVehicleId, vehicles, classificationContext) { tripList: List<Trip>, timeFilter: TimeFilter, selectedVehicleId: Long?, vehicleList: List<Vehicle>, context ->
        DashboardStatsCalculator.calculateDashboardStats(tripList, timeFilter, selectedVehicleId, vehicleList, context.rules, context.addresses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    val dashboardDistanceChartData = kotlinx.coroutines.flow.combine(trips, _dashboardTimeFilter, _globalSelectedVehicleId, vehicles, classificationContext) { tripList: List<Trip>, timeFilter: TimeFilter, selectedVehicleId: Long?, vehicleList: List<Vehicle>, context ->
        DashboardStatsCalculator.calculateDistanceChartData(tripList, timeFilter, selectedVehicleId, vehicleList, context.rules, context.addresses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dashboardCategoryBreakdown = kotlinx.coroutines.flow.combine(trips, _dashboardTimeFilter, _globalSelectedVehicleId, vehicles, classificationContext) { tripList: List<Trip>, timeFilter: TimeFilter, selectedVehicleId: Long?, vehicleList: List<Vehicle>, context ->
        DashboardStatsCalculator.calculateCategoryBreakdown(tripList, timeFilter, selectedVehicleId, vehicleList, context.rules)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val tripsToReviewCount = trips.map { tripList ->
        tripList.count { it.status == TripStatus.TO_REVIEW.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val tripsScreenUiState: StateFlow<TripsScreenUiState> = kotlinx.coroutines.flow.combine(
        filteredTrips,
        vehicles,
        availableYears,
        mergeSuggestion,
        kotlinx.coroutines.flow.combine(_tripFilter, _selectedYear, _searchQuery, _globalSelectedVehicleId) { a, b, c, d -> arrayOf<Any?>(a, b, c, d) }
    ) { filteredTripsArg, vehiclesArg, availableYearsArg, mergeArg, groupArg ->
        TripsScreenUiState(
            filteredTrips = filteredTripsArg,
            vehicles = vehiclesArg,
            availableYears = availableYearsArg,
            mergeSuggestion = mergeArg,
            tripFilter = groupArg[0] as String,
            selectedYear = groupArg[1] as Int,
            searchQuery = groupArg[2] as String,
            globalSelectedVehicleId = groupArg[3] as Long?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TripsScreenUiState())

    val dashboardUiState: StateFlow<DashboardUiState> = kotlinx.coroutines.flow.combine(
        dashboardStats,
        dashboardDistanceChartData,
        tripsToReviewCount,
        _dashboardTimeFilter,
        kotlinx.coroutines.flow.combine(vehicles, _globalSelectedVehicleId, dashboardCategoryBreakdown) { a, b, c -> Triple(a, b, c) }
    ) { statsArg, chartDataArg, reviewCountArg, timeFilterArg, tripleArg ->
        DashboardUiState(
            dashboardStats = statsArg,
            dashboardDistanceChartData = chartDataArg,
            tripsToReviewCount = reviewCountArg,
            dashboardTimeFilter = timeFilterArg,
            vehicles = tripleArg.first,
            globalSelectedVehicleId = tripleArg.second,
            dashboardCategoryBreakdown = tripleArg.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun addManualTrip(vehicleId: Long?, startAddress: String, endAddress: String, distanceKm: Double, type: String, startTime: Long = System.currentTimeMillis(), endTime: Long = startTime + (1000 * 60 * 30), note: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("CIMDriver", "addManualTrip started for $startAddress to $endAddress")
            var odoStart = 0
            var odoEnd = 0
            var distanceM = (distanceKm * 1000).toInt()
            
            if (distanceM <= 0) {
                val calc = calculateDistance(startAddress, endAddress)
                if (calc != null) {
                    distanceM = calc.first.toInt()
                }
            }

            if (vehicleId != null) {
                val vehicle = tripRepository.getVehicleById(vehicleId)
                if (vehicle != null) {
                    odoStart = vehicle.odometerCurrent
                    odoEnd = TripDistance.updatedOdometer(odoStart, distanceM)
                    tripRepository.updateVehicle(vehicle.copy(odometerCurrent = odoEnd))
                }
            }

            val trip = Trip(
                vehicleId = vehicleId,
                startTime = startTime,
                endTime = endTime,
                startAddress = startAddress,
                endAddress = endAddress,
                distanceMeters = distanceM,
                tripType = type,
                odometerStart = odoStart,
                odometerEnd = odoEnd,
                status = TripStatus.DONE.value,
                note = note,
                isManual = true
            )
            try {
                val id = tripRepository.insertTrip(trip)
                
                if (id > 0) {
                    val startCoords = tripRepository.getCoordinatesForAddress(startAddress)
                    val endCoords = tripRepository.getCoordinatesForAddress(endAddress)
                    if (startCoords != null && endCoords != null) {
                        val routePoints = tripRepository.getOsrmRoute(
                            startCoords.first, startCoords.second,
                            endCoords.first, endCoords.second
                        )
                        if (routePoints.isNotEmpty()) {
                            val pointDao = database.locationPointDao()
                            val points = routePoints.mapIndexed { index, pair ->
                                com.cimdriver.app.data.local.entity.LocationPoint(
                                    tripId = id,
                                    latitude = pair.first,
                                    longitude = pair.second,
                                    altitude = 0.0,
                                    speed = 0f,
                                    accuracy = 0f,
                                    timestamp = startTime + (index * 1000)
                                )
                            }
                            pointDao.insertPoints(points)
                        } else {
                            val pointDao = database.locationPointDao()
                            val points = listOf(
                                com.cimdriver.app.data.local.entity.LocationPoint(tripId = id, latitude = startCoords.first, longitude = startCoords.second, altitude = 0.0, speed = 0f, accuracy = 0f, timestamp = startTime),
                                com.cimdriver.app.data.local.entity.LocationPoint(tripId = id, latitude = endCoords.first, longitude = endCoords.second, altitude = 0.0, speed = 0f, accuracy = 0f, timestamp = startTime + 1000)
                            )
                            pointDao.insertPoints(points)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CIMDriver", "Error inserting trip", e)
            }
        }
    }

    fun updateManualTrip(tripId: Long, vehicleId: Long?, startAddress: String, endAddress: String, distanceKm: Double, type: String, startTime: Long, endTime: Long, note: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existingTrip = tripRepository.getTripById(tripId)
            if (existingTrip != null) {
                var distanceM = (distanceKm * 1000).toInt()
                if (distanceM <= 0) {
                    val calc = calculateDistance(startAddress, endAddress)
                    if (calc != null) {
                        distanceM = calc.first.toInt()
                    }
                }
                reconcileOdometerForTripChange(existingTrip, vehicleId, distanceM)
                val updatedTrip = existingTrip.copy(
                    vehicleId = vehicleId,
                    startAddress = startAddress,
                    endAddress = endAddress,
                    distanceMeters = distanceM,
                    tripType = type,
                    startTime = startTime,
                    endTime = endTime,
                    note = note,
                    odometerEnd = existingTrip.odometerStart +
                        TripDistance.metersToOdometerKilometers(distanceM)
                )
                tripRepository.updateTrip(updatedTrip)
                
                if (existingTrip.isManual) {
                    database.locationPointDao().deletePointsForTrip(tripId)
                    val startCoords = tripRepository.getCoordinatesForAddress(startAddress)
                    val endCoords = tripRepository.getCoordinatesForAddress(endAddress)
                    if (startCoords != null && endCoords != null) {
                        val routePoints = tripRepository.getOsrmRoute(
                            startCoords.first, startCoords.second,
                            endCoords.first, endCoords.second
                        )
                        if (routePoints.isNotEmpty()) {
                            val pointDao = database.locationPointDao()
                            val points = routePoints.mapIndexed { index, pair ->
                                com.cimdriver.app.data.local.entity.LocationPoint(
                                    tripId = tripId,
                                    latitude = pair.first,
                                    longitude = pair.second,
                                    altitude = 0.0,
                                    speed = 0f,
                                    accuracy = 0f,
                                    timestamp = startTime + (index * 1000)
                                )
                            }
                            pointDao.insertPoints(points)
                        } else {
                            val pointDao = database.locationPointDao()
                            val points = listOf(
                                com.cimdriver.app.data.local.entity.LocationPoint(tripId = tripId, latitude = startCoords.first, longitude = startCoords.second, altitude = 0.0, speed = 0f, accuracy = 0f, timestamp = startTime),
                                com.cimdriver.app.data.local.entity.LocationPoint(tripId = tripId, latitude = endCoords.first, longitude = endCoords.second, altitude = 0.0, speed = 0f, accuracy = 0f, timestamp = startTime + 1000)
                            )
                            pointDao.insertPoints(points)
                        }
                    }
                }
            }
        }
    }

    fun mergeTrips(trip1: Trip, trip2: Trip) {
        viewModelScope.launch {
            val firstTrip = if (trip1.startTime < trip2.startTime) trip1 else trip2
            val secondTrip = if (trip1.startTime < trip2.startTime) trip2 else trip1

            val mergedTrip = Trip(
                vehicleId = firstTrip.vehicleId ?: secondTrip.vehicleId,
                startTime = firstTrip.startTime,
                endTime = secondTrip.endTime ?: secondTrip.startTime,
                startAddress = firstTrip.startAddress,
                endAddress = secondTrip.endAddress,
                distanceMeters = firstTrip.distanceMeters + secondTrip.distanceMeters,
                tripType = "UNCLASSIFIED",
                note = "Samengevoegd: ${firstTrip.distanceMeters/1000.0}km + ${secondTrip.distanceMeters/1000.0}km",
                status = TripStatus.TO_REVIEW.value,
                isManual = firstTrip.isManual && secondTrip.isManual,
                odometerStart = firstTrip.odometerStart,
                odometerEnd = secondTrip.odometerEnd ?: TripDistance.updatedOdometer(
                    firstTrip.odometerStart, firstTrip.distanceMeters + secondTrip.distanceMeters
                )
            )

            val newTripId = tripRepository.insertTrip(mergedTrip)
            val pointDao = database.locationPointDao()
            pointDao.updatePointsTripId(firstTrip.id, newTripId)
            pointDao.updatePointsTripId(secondTrip.id, newTripId)
            
            tripRepository.deleteTrip(firstTrip)
            tripRepository.deleteTrip(secondTrip)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            database.withTransaction {
                trip.vehicleId?.let { vehicleId ->
                    tripRepository.getVehicleById(vehicleId)?.let { vehicle ->
                        val correctedOdometer = vehicle.odometerCurrent -
                            TripDistance.metersToOdometerKilometers(trip.distanceMeters)
                        tripRepository.updateVehicle(vehicle.copy(odometerCurrent = correctedOdometer.coerceAtLeast(0)))
                    }
                }
                tripRepository.deleteTrip(trip)
            }
        }
    }

    private suspend fun reconcileOdometerForTripChange(existingTrip: Trip, newVehicleId: Long?, newDistanceMeters: Int) {
        database.withTransaction {
            existingTrip.vehicleId?.let { oldVehicleId ->
                tripRepository.getVehicleById(oldVehicleId)?.let { vehicle ->
                    val correctedOdometer = vehicle.odometerCurrent -
                        TripDistance.metersToOdometerKilometers(existingTrip.distanceMeters)
                    tripRepository.updateVehicle(vehicle.copy(odometerCurrent = correctedOdometer.coerceAtLeast(0)))
                }
            }
            newVehicleId?.let { targetVehicleId ->
                tripRepository.getVehicleById(targetVehicleId)?.let { vehicle ->
                    tripRepository.updateVehicle(vehicle.copy(
                        odometerCurrent = TripDistance.updatedOdometer(vehicle.odometerCurrent, newDistanceMeters)
                    ))
                }
            }
        }
    }

    fun batchUpdateTripTypes(tripIds: List<Long>, tripType: String) {
        viewModelScope.launch {
            database.withTransaction {
                tripIds.forEach { id ->
                    val trip = tripRepository.getTripById(id)
                    if (trip != null) {
                        tripRepository.updateTrip(trip.copy(tripType = tripType, status = TripStatus.DONE.value))
                    }
                }
            }
        }
    }

    suspend fun searchAddress(query: String): List<String> {
        return tripRepository.searchAddress(query)
    }

    suspend fun calculateDistance(start: String, end: String): Pair<Float, Float>? {
        return tripRepository.calculateDistance(start, end)
    }
}
