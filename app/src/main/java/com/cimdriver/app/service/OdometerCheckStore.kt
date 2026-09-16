package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OdometerCheckRequest(
    val showDialog: Boolean = false,
    val vehicleId: Long? = null
)

object OdometerCheckStore {
    private val _request = MutableStateFlow(OdometerCheckRequest())
    val request: StateFlow<OdometerCheckRequest> = _request.asStateFlow()

    fun requestCheck(vehicleId: Long) {
        _request.value = OdometerCheckRequest(showDialog = true, vehicleId = vehicleId)
    }

    fun dismiss() {
        _request.value = OdometerCheckRequest()
    }
}
