package com.cimdriver.app.util

import kotlin.math.roundToInt

/** Keeps conversions between stored trip metres and whole-kilometre odometer readings consistent. */
object TripDistance {
    fun metersToOdometerKilometers(meters: Int): Int = (meters / 1_000f).roundToInt()

    fun updatedOdometer(currentKilometers: Int, distanceMeters: Int): Int =
        currentKilometers + metersToOdometerKilometers(distanceMeters)
}
