package com.cimdriver.app.util

import kotlin.math.roundToInt

/** Keeps conversions between stored trip metres and whole-kilometre odometer readings consistent. */
object TripDistance {
    fun metersToOdometerKilometers(meters: Int): Int = (meters / 1_000f).roundToInt()

    fun updatedOdometer(currentKilometers: Int, distanceMeters: Int): Int =
        currentKilometers + metersToOdometerKilometers(distanceMeters)

    /**
     * Checks if the recorded GPS distance missed parts of the route due to exiting or entering
     * a parking garage (or delayed GPS acquisition), and adjusts the distance accordingly while
     * fully respecting any detours or alternative routes driven.
     */
    fun adjustForParkingGarageOrDelayedGps(
        recordedDistanceMeters: Int,
        expectedDirectDistanceMeters: Int?,
        missingStartDistanceMeters: Int = 0,
        missingEndDistanceMeters: Int = 0,
        isDelayedGpsFix: Boolean = false,
        minDeviationThresholdMeters: Int = 300
    ): Int {
        val actualRouteWithMissingSegments = recordedDistanceMeters + missingStartDistanceMeters + missingEndDistanceMeters
        val directRoute = expectedDirectDistanceMeters ?: 0

        // If specific garage exit/entry segments were missing, add them directly to the actual driven route!
        if (missingStartDistanceMeters > 0 || missingEndDistanceMeters > 0) {
            return maxOf(actualRouteWithMissingSegments, directRoute, recordedDistanceMeters)
        }

        // If GPS was delayed at start and no specific segment could be determined:
        if (isDelayedGpsFix && directRoute > recordedDistanceMeters) {
            return directRoute
        }

        // If recorded distance is substantially shorter than direct route (e.g. general GPS dropouts during trip):
        if (directRoute > 0 && recordedDistanceMeters < directRoute && (directRoute - recordedDistanceMeters >= minDeviationThresholdMeters)) {
            return directRoute
        }

        return maxOf(actualRouteWithMissingSegments, recordedDistanceMeters)
    }
}
