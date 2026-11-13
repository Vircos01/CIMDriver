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
            expectedDirectDistanceMeters = 8_200,
            isDelayedGpsFix = true
        )
        assertEquals(8_200, adjusted)
    }

    @Test
    fun preservesActualDrivenRouteAndDetoursWhenAddingGarageMissingSegment() {
        // Driver took a significant detour (e.g. 15 km driven on GPS), direct route is only 8.2 km
        // Missed 800m leaving the parking garage before first GPS fix
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 15_000,
            expectedDirectDistanceMeters = 8_200,
            missingStartDistanceMeters = 800,
            isDelayedGpsFix = true
        )
        // Must be 15,000 + 800 = 15,800m (detour fully preserved, garage connection added)
        assertEquals(15_800, adjusted)
    }

    @Test
    fun handlesBothStartAndEndGarageSegmentsWithActualRoute() {
        // Drove 10 km along actual route, 600m missed exiting underground garage at start, 400m missed entering underground garage at destination
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 10_000,
            expectedDirectDistanceMeters = 9_000,
            missingStartDistanceMeters = 600,
            missingEndDistanceMeters = 400,
            isDelayedGpsFix = true
        )
        assertEquals(11_000, adjusted)
    }

    @Test
    fun doesNotReduceDistanceWhenUserDroveFurtherThanRouteQuery() {
        // Driver took a detour (e.g. 10.5 km instead of 8.2 km)
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 10_500,
            expectedDirectDistanceMeters = 8_200,
            isDelayedGpsFix = false
        )
        assertEquals(10_500, adjusted)
    }

    @Test
    fun preservesRecordedDistanceWhenExpectedDistanceIsNull() {
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 6_800,
            expectedDirectDistanceMeters = null,
            isDelayedGpsFix = true
        )
        assertEquals(6_800, adjusted)
    }

    @Test
    fun doesNotAdjustMinorGpsJitterWithoutDelayedFix() {
        // 50m difference without delayed fix (within threshold of 300m)
        val adjusted = TripDistance.adjustForParkingGarageOrDelayedGps(
            recordedDistanceMeters = 8_150,
            expectedDirectDistanceMeters = 8_200,
            isDelayedGpsFix = false,
            minDeviationThresholdMeters = 300
        )
        assertEquals(8_150, adjusted)
    }
}
