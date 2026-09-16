package com.cimdriver.app.data.repository

import com.cimdriver.app.data.local.dao.ClassificationRuleDao
import com.cimdriver.app.data.local.dao.SavedAddressDao
import com.cimdriver.app.data.local.dao.TripDao
import com.cimdriver.app.data.local.dao.VehicleDao
import com.cimdriver.app.data.local.entity.ClassificationRule
import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.service.GeocoderService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TripRepository(
    private val tripDao: TripDao,
    private val vehicleDao: VehicleDao,
    private val classificationRuleDao: ClassificationRuleDao,
    private val savedAddressDao: SavedAddressDao,
    private val geocoderService: GeocoderService
) {
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()
    
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    
    fun getClassificationRules(): Flow<List<ClassificationRule>> = classificationRuleDao.getAllRules()
    
    fun getSavedAddresses(): Flow<List<SavedAddress>> = savedAddressDao.getAllAddresses()

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        vehicleDao.updateVehicle(vehicle)
    }

    suspend fun searchAddress(query: String): List<String> {
        return geocoderService.searchAddress(query)
    }

    suspend fun calculateDistance(start: String, end: String): Pair<Float, Float>? {
        return geocoderService.calculateDistance(start, end)
    }
    
    suspend fun getCoordinatesForAddress(address: String): Pair<Double, Double>? {
        return geocoderService.getCoordinatesForAddress(address)
    }
    
    suspend fun getOsrmRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double): List<Pair<Double, Double>> {
        return geocoderService.getOsrmRoute(startLat, startLng, endLat, endLng)
    }
}
