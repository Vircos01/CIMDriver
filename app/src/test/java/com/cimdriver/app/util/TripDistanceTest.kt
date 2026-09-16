package com.cimdriver.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TripDistanceTest {
    @Test
    fun convertsMetresToRoundedOdometerKilometres() {
        assertEquals(12, TripDistance.metersToOdometerKilometers(12_499))
        assertEquals(13, TripDistance.metersToOdometerKilometers(12_500))
    }

    @Test
    fun addsOnlyKilometresToTheOdometer() {
        assertEquals(100_012, TripDistance.updatedOdometer(100_000, 12_300))
    }
}
