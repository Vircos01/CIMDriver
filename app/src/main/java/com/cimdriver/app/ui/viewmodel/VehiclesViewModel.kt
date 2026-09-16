package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.cimdriver.app.data.local.dao.VehicleDao
import com.cimdriver.app.data.local.dao.BluetoothDeviceDao
import com.cimdriver.app.data.local.dao.TripDao
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.BluetoothDevice
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.data.local.entity.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.bluetooth.BluetoothManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val bluetoothDeviceDao: BluetoothDeviceDao,
    private val tripDao: TripDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val vehicles = vehicleDao.getAllVehicles()
    val bluetoothDevices = bluetoothDeviceDao.getAllDevices()
    
    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    fun addVehicle(name: String, licensePlate: String, make: String, model: String, odometerStart: Int, odometerCurrent: Int, usageType: String, notes: String?, privateKmYearlyLimit: Int, showPrivateKmWarning: Boolean, odometerCorrectionStrategy: String) {
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
                odometerCorrectionStrategy = odometerCorrectionStrategy
            )
            vehicleDao.insertVehicle(newVehicle)
        }
    }

    fun updateVehicle(vehicle: Vehicle, name: String, licensePlate: String, make: String, model: String, odometerStart: Int, odometerCurrent: Int, usageType: String, notes: String?, privateKmYearlyLimit: Int, showPrivateKmWarning: Boolean, odometerCorrectionStrategy: String) {
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
                odometerCorrectionStrategy = odometerCorrectionStrategy
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

    fun confirmOdometerCheck(vehicle: Vehicle, correctedOdometer: Int) {
        viewModelScope.launch {
            val gap = correctedOdometer - vehicle.odometerCurrent
            if (gap == 0) {
                val updatedVehicle = vehicle.copy(
                    lastOdometerCheckTimestamp = System.currentTimeMillis()
                )
                vehicleDao.updateVehicle(updatedVehicle)
                return@launch
            }

            when (vehicle.odometerCorrectionStrategy) {
                "CREATE_TRIP" -> {
                    if (gap > 0) {
                        val correctionTrip = Trip(
                            vehicleId = vehicle.id,
                            startTime = System.currentTimeMillis() - 1000,
                            endTime = System.currentTimeMillis(),
                            startAddress = "Correctie",
                            endAddress = "Correctie",
                            distanceMeters = gap * 1000,
                            tripType = "PERSONAL",
                            note = "Correctierit",
                            status = "DONE",
                            isManual = true,
                            odometerStart = vehicle.odometerCurrent,
                            odometerEnd = correctedOdometer
                        )
                        tripDao.insertTrip(correctionTrip)
                    }
                }
                "DISTRIBUTE" -> {
                    val trips = tripDao.getTripsByVehicleSync(vehicle.id)
                    var totalDistance = 0.0
                    trips.forEach { totalDistance += (it.distanceMeters / 1000.0) }
                    
                    val actualDistance = correctedOdometer - vehicle.odometerStart
                    if (totalDistance > 0 && actualDistance > 0) {
                        val ratio = actualDistance / totalDistance
                        var currentOdo = vehicle.odometerStart
                        
                        for (trip in trips) {
                            val newDistanceKm = Math.round((trip.distanceMeters / 1000.0) * ratio).toInt()
                            val updatedTrip = trip.copy(
                                distanceMeters = newDistanceKm * 1000,
                                odometerStart = currentOdo,
                                odometerEnd = currentOdo + newDistanceKm
                            )
                            tripDao.updateTrip(updatedTrip)
                            currentOdo += newDistanceKm
                        }
                    }
                }
                "LEAVE_GAP" -> {
                    // Do nothing extra, just update the vehicle's odometer
                }
            }

            val updatedVehicle = vehicle.copy(
                odometerCurrent = correctedOdometer,
                lastOdometerCheckTimestamp = System.currentTimeMillis()
            )
            vehicleDao.updateVehicle(updatedVehicle)
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
