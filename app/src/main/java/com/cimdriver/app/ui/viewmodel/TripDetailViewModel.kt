package com.cimdriver.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.LocationPoint
import com.cimdriver.app.data.local.entity.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.cimdriver.app.service.GeocoderService
import com.cimdriver.app.data.local.entity.SavedAddress

class TripDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val tripDao = database.tripDao()
    private val pointDao = database.locationPointDao()
    private val savedAddressDao = database.savedAddressDao()
    private val geocoderService = GeocoderService(application)

    val savedAddresses: StateFlow<List<SavedAddress>> = savedAddressDao.getAllAddresses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _points = MutableStateFlow<List<LocationPoint>>(emptyList())
    val points: StateFlow<List<LocationPoint>> = _points.asStateFlow()

    fun loadTrip(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedTrip = tripDao.getTripById(tripId)
            _trip.value = loadedTrip
            
            pointDao.getPointsForTrip(tripId).collect { dbPoints ->
                if (dbPoints.isEmpty() && loadedTrip?.isManual == true) {
                    // Try to geocode start and end address to show something on the map
                    val startCoords = loadedTrip.startAddress?.let { geocoderService.getCoordinatesForAddress(it) }
                    val endCoords = loadedTrip.endAddress?.let { geocoderService.getCoordinatesForAddress(it) }
                    
                    val fakePoints = mutableListOf<LocationPoint>()
                    if (startCoords != null && endCoords != null) {
                        val route = geocoderService.getOsrmRoute(
                            startCoords.first, startCoords.second,
                            endCoords.first, endCoords.second
                        )
                        
                        if (route.isNotEmpty()) {
                            route.forEachIndexed { index, pair ->
                                fakePoints.add(
                                    LocationPoint(
                                        tripId = tripId,
                                        latitude = pair.first,
                                        longitude = pair.second,
                                        altitude = 0.0,
                                        timestamp = 0L,
                                        speed = 0f,
                                        accuracy = 0f
                                    )
                                )
                            }
                        } else {
                            // Fallback to straight line
                            fakePoints.add(LocationPoint(tripId = tripId, latitude = startCoords.first, longitude = startCoords.second, altitude = 0.0, timestamp = 0L, speed = 0f, accuracy = 0f))
                            fakePoints.add(LocationPoint(tripId = tripId, latitude = endCoords.first, longitude = endCoords.second, altitude = 0.0, timestamp = 0L, speed = 0f, accuracy = 0f))
                        }
                    } else if (startCoords != null) {
                        fakePoints.add(LocationPoint(tripId = tripId, latitude = startCoords.first, longitude = startCoords.second, altitude = 0.0, timestamp = 0L, speed = 0f, accuracy = 0f))
                    } else if (endCoords != null) {
                        fakePoints.add(LocationPoint(tripId = tripId, latitude = endCoords.first, longitude = endCoords.second, altitude = 0.0, timestamp = 0L, speed = 0f, accuracy = 0f))
                    }
                    
                    if (fakePoints.isNotEmpty()) {
                        pointDao.insertPoints(fakePoints)
                    }
                    _points.value = fakePoints
                } else {
                    _points.value = dbPoints
                }
            }
        }
    }

    fun saveReview(tripType: String) {
        val currentTrip = _trip.value ?: return
        viewModelScope.launch {
            val updatedTrip = currentTrip.copy(
                tripType = tripType,
                status = "DONE"
            )
            tripDao.updateTrip(updatedTrip)
            _trip.value = updatedTrip
        }
    }

    fun recalculateOdometers() {
        val currentTrip = _trip.value ?: return
        val vehicleId = currentTrip.vehicleId ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            val vehicle = database.vehicleDao().getVehicleById(vehicleId) ?: return@launch
            val trips = tripDao.getTripsByVehicleSync(vehicleId)
            
            val currentIndex = trips.indexOfFirst { it.id == currentTrip.id }
            if (currentIndex == -1) return@launch
            
            val previousTrip = if (currentIndex > 0) trips[currentIndex - 1] else null
            val newOdoStart = previousTrip?.odometerEnd ?: vehicle.odometerStart
            
            val distanceKm = Math.round(currentTrip.distanceMeters / 1000.0).toInt()
            val newOdoEnd = newOdoStart + distanceKm
            
            val updatedTrip = currentTrip.copy(
                odometerStart = newOdoStart,
                odometerEnd = newOdoEnd
            )
            tripDao.updateTrip(updatedTrip)
            
            // If this was the last trip, also update the vehicle's current odometer
            if (currentIndex == trips.size - 1) {
                val updatedVehicle = vehicle.copy(odometerCurrent = newOdoEnd)
                database.vehicleDao().updateVehicle(updatedVehicle)
            }
            
            _trip.value = updatedTrip
        }
    }
}
