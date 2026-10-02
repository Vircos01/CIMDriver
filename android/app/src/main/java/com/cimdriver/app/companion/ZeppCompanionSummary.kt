package com.cimdriver.app.companion

import kotlinx.serialization.Serializable

@Serializable
data class ZeppCompanionSummary(
    val tripStatus: String = "idle",
    val distanceKm: Double = 0.0,
    val fuelStatus: String = "ready",
    val lastUpdated: Long = System.currentTimeMillis()
)
