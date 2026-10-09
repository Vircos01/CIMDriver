package com.cimdriver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.entity.FuelFillUp
import com.cimdriver.app.data.repository.FuelRepository
import com.cimdriver.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FuelViewModel @Inject constructor(
    private val fuelRepository: FuelRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    val fillUps: StateFlow<List<FuelFillUp>> = fuelRepository.getAllFillUps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val vehicles = tripRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    // Calculate stats per vehicle
    fun getStatsForVehicle(vehicleId: Long, allFillUps: List<FuelFillUp>): FuelStats {
        val vehicleFillUps = allFillUps.filter { it.vehicleId == vehicleId }.sortedBy { it.dateTimestamp }
        if (vehicleFillUps.isEmpty()) return FuelStats(0.0, 0.0)
        
        val totalCost = vehicleFillUps.sumOf { it.totalCost }
        
        // Calculate consumption (L/100km)
        // Need at least 2 fill-ups to calculate distance
        if (vehicleFillUps.size < 2) return FuelStats(totalCost, 0.0)
        
        val firstOdo = vehicleFillUps.first().odometer
        val lastOdo = vehicleFillUps.last().odometer
        val distance = lastOdo - firstOdo
        
        // Don't include the first fill-up's liters since it was used for the distance before the first fill-up
        val litersUsed = vehicleFillUps.drop(1).sumOf { it.liters }
        
        val consumption = if (distance > 0) (litersUsed / distance) * 100 else 0.0
        
        return FuelStats(totalCost, consumption)
    }

    fun insertFillUp(fillUp: FuelFillUp) {
        viewModelScope.launch {
            fuelRepository.insertFillUp(fillUp)
        }
    }

    fun updateFillUp(fillUp: FuelFillUp) {
        viewModelScope.launch {
            fuelRepository.updateFillUp(fillUp)
        }
    }

    fun deleteFillUp(fillUp: FuelFillUp) {
        viewModelScope.launch {
            fuelRepository.deleteFillUp(fillUp)
        }
    }

    suspend fun getFillUpById(id: Long): FuelFillUp? {
        // Since we don't have a direct DAO method for a single fillUp in repository yet, 
        // we can just find it in the state flow, or we should probably fetch it.
        // Actually, fuelRepository might not have getFillUpById. Let's just find it in the list.
        return fillUps.value.find { it.id == id }
    }

    suspend fun searchAddress(query: String): List<String> {
        return tripRepository.searchAddress(query)
    }

    suspend fun getCoordinatesForAddress(address: String): Pair<Double, Double>? {
        return tripRepository.getCoordinatesForAddress(address)
    }
}

data class FuelStats(
    val totalCost: Double,
    val averageConsumption: Double // L/100km
)
