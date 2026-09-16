package com.cimdriver.app.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkHoursNormalizerTest {
    @Test
    fun snapsNearConfiguredStartToConfiguredTime() {
        val dayStart = dayStart()
        val actual = dayStart + (8 * 60 + 20) * 60_000L

        assertEquals(dayStart + 8 * 60 * 60_000L, WorkHoursNormalizer.normalize(actual, dayStart, "08:00"))
    }

    @Test
    fun keepsLargeStartDeviationMeasured() {
        val dayStart = dayStart()
        val actual = dayStart + (9 * 60 + 45) * 60_000L

        assertEquals(actual, WorkHoursNormalizer.normalize(actual, dayStart, "08:00"))
    }

    @Test
    fun snapsNearConfiguredEndToConfiguredTime() {
        val dayStart = dayStart()
        val actual = dayStart + (18 * 60 + 20) * 60_000L

        assertEquals(dayStart + 18 * 60 * 60_000L, WorkHoursNormalizer.normalize(actual, dayStart, "18:00"))
    }

    private fun dayStart(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 5, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
