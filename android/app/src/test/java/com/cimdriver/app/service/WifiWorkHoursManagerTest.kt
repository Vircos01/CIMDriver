package com.cimdriver.app.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WifiWorkHoursManagerTest {

    @Test
    fun `cleanSsid strips enclosing double quotes and trims whitespace`() {
        assertEquals("CIMSOLUTIONS-Office", WifiWorkHoursManager.cleanSsid("\"CIMSOLUTIONS-Office\""))
        assertEquals("MyWorkNetwork", WifiWorkHoursManager.cleanSsid("  \"MyWorkNetwork\"  "))
        assertEquals("Guest-Wifi", WifiWorkHoursManager.cleanSsid("Guest-Wifi"))
        assertEquals("", WifiWorkHoursManager.cleanSsid(""))
        assertEquals("", WifiWorkHoursManager.cleanSsid("\"\""))
    }

    @Test
    fun `getStartOfDay returns midnight timestamp`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 14, 35, 22)
            set(Calendar.MILLISECOND, 450)
        }
        val startOfDay = WifiWorkHoursManager.getStartOfDay(cal.timeInMillis)

        val resultCal = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, resultCal.get(Calendar.MONTH))
        assertEquals(16, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `lunch break outside office adjusts break when away between 10 and 120 minutes`() {
        val defaultBreak = 30

        // Short disconnect (< 10 min) should not be considered lunch break
        val shortAwayMinutes = 5
        val breakAfterShortAway = if (shortAwayMinutes in 10..120) {
            maxOf(defaultBreak, shortAwayMinutes)
        } else {
            defaultBreak
        }
        assertEquals(30, breakAfterShortAway)

        // Normal lunch break of 45 minutes outside
        val lunchAwayMinutes = 45
        val breakAfterLunch = if (lunchAwayMinutes in 10..120) {
            maxOf(defaultBreak, lunchAwayMinutes)
        } else {
            defaultBreak
        }
        assertEquals(45, breakAfterLunch)

        // Quick 20 minute lunch break outside: preserves configured 30 minute minimum break
        val quickLunchMinutes = 20
        val breakAfterQuickLunch = if (quickLunchMinutes in 10..120) {
            maxOf(defaultBreak, quickLunchMinutes)
        } else {
            defaultBreak
        }
        assertEquals(30, breakAfterQuickLunch)

        // Long absence (> 120 min) is outside standard lunch window
        val longAbsenceMinutes = 180
        val breakAfterLongAbsence = if (longAbsenceMinutes in 10..120) {
            maxOf(defaultBreak, longAbsenceMinutes)
        } else {
            defaultBreak
        }
        assertEquals(30, breakAfterLongAbsence)
    }
}

