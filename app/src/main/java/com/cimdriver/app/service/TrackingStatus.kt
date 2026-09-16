package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ServiceState {
    CREATED, STARTING, RUNNING, STOPPING, STOPPED, FAILED, RECOVERING
}

data class TrackingStatus(
    val state: ServiceState = ServiceState.STOPPED,
    val tripId: Long? = null,
    val gpsAccuracyMeters: Float? = null,
    val lastLocationTime: Long? = null,
    val message: String = "Tracking is niet actief",
    val correlationId: String? = null,
    val errorCause: String? = null
) {
    val isActive: Boolean
        get() = state == ServiceState.RUNNING || state == ServiceState.STARTING || state == ServiceState.RECOVERING
}

object TrackingStatusStore {
    private val _status = MutableStateFlow(TrackingStatus())
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    fun starting(tripId: Long? = null, isRecovery: Boolean = false): String {
        val newCorrelationId = UUID.randomUUID().toString()
        _status.value = _status.value.copy(
            state = if (isRecovery) ServiceState.RECOVERING else ServiceState.STARTING,
            tripId = tripId ?: _status.value.tripId,
            message = if (isRecovery) "Rit wordt hersteld..." else "Rit wordt gestart...",
            correlationId = newCorrelationId,
            errorCause = null
        )
        return newCorrelationId
    }

    fun started(tripId: Long) {
        _status.value = _status.value.copy(
            state = ServiceState.RUNNING,
            tripId = tripId,
            message = "Rit wordt geregistreerd"
        )
    }

    fun waitingForGps() {
        _status.value = _status.value.copy(message = "Wachten op een nauwkeurige GPS-locatie")
    }

    fun warning(message: String) {
        _status.value = _status.value.copy(message = message)
    }

    fun locationReceived(accuracyMeters: Float, timestamp: Long) {
        _status.value = _status.value.copy(
            state = ServiceState.RUNNING,
            gpsAccuracyMeters = accuracyMeters,
            lastLocationTime = timestamp,
            message = "GPS actief"
        )
    }
    
    fun stopping(message: String = "Rit wordt afgerond") {
        _status.value = _status.value.copy(
            state = ServiceState.STOPPING,
            message = message
        )
    }

    fun stopped(message: String = "Tracking is niet actief") {
        _status.value = TrackingStatus(
            state = ServiceState.STOPPED,
            message = message
        )
    }

    fun failed(error: String) {
        _status.value = _status.value.copy(
            state = ServiceState.FAILED,
            message = "Fout: $error",
            errorCause = error
        )
    }
}
