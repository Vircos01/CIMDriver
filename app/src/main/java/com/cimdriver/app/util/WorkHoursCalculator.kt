package com.cimdriver.app.util

object WorkHoursCalculator {
    const val FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000L

    fun effectiveBreakMinutes(
        startTime: Long,
        endTime: Long,
        configuredBreakMinutes: Int,
        toleranceMinutes: Int
    ): Int {
        val duration = endTime - startTime
        return if (duration > FOUR_HOURS_MILLIS + toleranceMinutes.coerceAtLeast(0) * 60_000L) {
            configuredBreakMinutes.coerceAtLeast(0)
        } else {
            0
        }
    }

    fun netDurationMillis(
        startTime: Long,
        endTime: Long,
        configuredBreakMinutes: Int,
        toleranceMinutes: Int
    ): Long {
        val breakMinutes = effectiveBreakMinutes(startTime, endTime, configuredBreakMinutes, toleranceMinutes)
        return (endTime - startTime - breakMinutes * 60_000L).coerceAtLeast(0L)
    }
}
