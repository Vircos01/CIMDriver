package com.cimdriver.app.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetCalculatorTest {
    @Test
    fun septemberStartUsesFourTwelfthsOfAnnualTarget() {
        assertEquals(4.0 / 12.0, employmentTargetFactor(utcDate(2026, Calendar.SEPTEMBER, 1), 2026), 0.000001)
    }

    @Test
    fun noStartDateUsesFullAnnualTarget() {
        assertEquals(1.0, employmentTargetFactor(null, 2026), 0.0)
    }

    @Test
    fun employmentStartingAfterTargetYearUsesNoTarget() {
        assertEquals(0.0, employmentTargetFactor(utcDate(2027, Calendar.JANUARY, 1), 2026), 0.0)
    }

    @Test
    fun employmentStartingBeforeTargetYearUsesFullAnnualTarget() {
        assertEquals(1.0, employmentTargetFactor(utcDate(2025, Calendar.SEPTEMBER, 1), 2026), 0.0)
    }

    private fun utcDate(year: Int, month: Int, day: Int): Long = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day)
    }.timeInMillis
}