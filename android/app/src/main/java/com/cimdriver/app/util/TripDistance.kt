package com.cimdriver.app.util

import kotlin.math.roundToInt

/** Keeps conversions between stored trip metres and whole-kilometre odometer readings consistent. */
object TripDistance {
    fun metersToOdometerKilometers(meters: Int): Int = (meters / 1_000f).roundToInt()

    fun updatedOdometer(currentKilometers: Int, distanceMeters: Int): Int =
        currentKilometers + metersToOdometerKilometers(distanceMeters)

    /**
     * Checks if the recorded GPS distance is smaller than the calculated route distance
     * due to exiting a parking garage or delayed GPS acquisition, and adjusts the distance accordingly.
     */
    fun adjustForParkingGarageOrDelayedGps(
        recordedDistanceMeters: Int,
        expectedDistanceMeters: Int?,
        isDelayedGpsFix: Boolean,
        minDeviationThresholdMeters: Int = 300
    ): Int {
        if (expectedDistanceMeters == null || expectedDistanceMeters <= 0) {
            return recordedDistanceMeters
        }
        if (recordedDistanceMeters < expectedDistanceMeters &&
            (isDelayedGpsFix || (expectedDistanceMeters - recordedDistanceMeters >= minDeviationThresholdMeters))) {
            return expectedDistanceMeters
        }
        return recordedDistanceMeters
    }
}
