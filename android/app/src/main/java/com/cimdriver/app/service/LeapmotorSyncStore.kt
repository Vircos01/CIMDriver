package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LeapmotorSyncRequest(
    val openScreen: Boolean = false,
    val imageUri: String? = null,
    val vehicleId: Long? = null
)

object LeapmotorSyncStore {
    private val _request = MutableStateFlow(LeapmotorSyncRequest())
    val request: StateFlow<LeapmotorSyncRequest> = _request.asStateFlow()

    fun requestSync(imageUri: String? = null, vehicleId: Long? = null) {
        _request.value = LeapmotorSyncRequest(openScreen = true, imageUri = imageUri, vehicleId = vehicleId)
    }

    fun dismiss() {
        _request.value = LeapmotorSyncRequest()
    }
}
