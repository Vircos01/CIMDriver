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

    @Test
    fun adjustsDistanceWhenExitingParkingGarageWithDelayedGpsFix() {
        // Driver exits parking garage: GPS missed first ~1.4 km
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 6_800,
            expectedDistanceMeters = 8_200,
            isDelayedGpsFix = true
        )
        assertEquals(8_200, adjusted)
    }

    @Test
    fun doesNotReduceDistanceWhenUserDroveFurtherThanRouteQuery() {
        // Driver took a detour (e.g. 10.5 km instead of 8.2 km)
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 10_500,
            expectedDistanceMeters = 8_200,
            isDelayedGpsFix = false
        )
        assertEquals(10_500, adjusted)
    }

    @Test
    fun preservesRecordedDistanceWhenExpectedDistanceIsNull() {
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 6_800,
            expectedDistanceMeters = null,
            isDelayedGpsFix = true
        )
        assertEquals(6_800, adjusted)
    }

    @Test
    fun doesNotAdjustMinorGpsJitterWithoutDelayedFix() {
        // 50m difference without delayed fix (within threshold of 300m)
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 8_150,
            expectedDistanceMeters = 8_200,
            isDelayedGpsFix = false,
            minDeviationThresholdMeters = 300
        )
        assertEquals(8_150, adjusted)
    }
}
