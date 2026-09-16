package com.cimdriver.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkHoursCalculatorTest {
    @Test
    fun doesNotDeductBreakAtFourHoursPlusTolerance() {
        val start = 0L
        val end = 4 * 60 * 60 * 1000L + 30 * 60_000L

        assertEquals(0, WorkHoursCalculator.effectiveBreakMinutes(start, end, 30, 30))
    }

    @Test
    fun deductsBreakAboveFourHoursPlusTolerance() {
        val start = 0L
        val end = 4 * 60 * 60 * 1000L + 31 * 60_000L

        assertEquals(30, WorkHoursCalculator.effectiveBreakMinutes(start, end, 30, 30))
    }
}