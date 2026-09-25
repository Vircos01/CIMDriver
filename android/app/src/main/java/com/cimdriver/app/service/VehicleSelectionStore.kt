package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VehicleSelectionStore {
    private val _requestedVehicleId = MutableStateFlow<Long?>(null)
    val requestedVehicleId: StateFlow<Long?> = _requestedVehicleId.asStateFlow()

    fun requestVehicle(vehicleId: Long) {
        _requestedVehicleId.value = vehicleId
    }

    fun clearRequest() {
        _requestedVehicleId.value = null
    }
}
