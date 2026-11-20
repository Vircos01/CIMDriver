package com.cimdriver.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LeapmotorParserTest {

    private val sampleScreenshotText = """
        19:16
        Energieverbruik per gereden afstand
        U hebt B10 uitgegeven bij
        308 km
        Week Maand Jaar
        Rijden 80.4 % Airco 13.7 % Overig 5.9 %
        Ritgegevens
        2026/11/19 18:58-19:18
        2026/11/19 18:13-18:29
        2026/11/19 17:12-17:14
        2026/11/19 16:57-17:00
        2026/11/19 16:47-16:51
        2026/11/19 14:24-14:28
        2026/11/19 14:09-14:14
        Afgelegde afstand
        2 km
        Maximale snelheid
        48 km/h
        Totale energieverbruik
        0.1 kWh
    """.trimIndent()

    @Test
    fun testParseSampleScreenshot() {
        val result = LeapmotorParser.parseFromText(sampleScreenshotText)

        assertEquals(308, result.detectedOdometerKm)
        assertEquals(7, result.trips.size)

        // Trip 1
        val t1 = result.trips[0]
        assertEquals("2026/11/19", t1.dateText)
        assertEquals("18:58", t1.startTimeText)
        assertEquals("19:18", t1.endTimeText)
        assertEquals(20, t1.durationMinutes)
        assertNull(t1.distanceKm)

        // Trip 3: 2-minute trip (17:12 - 17:14)
        val t3 = result.trips[2]
        assertEquals("17:12", t3.startTimeText)
        assertEquals("17:14", t3.endTimeText)
        assertEquals(2, t3.durationMinutes)

        // Trip 7: expanded details (14:09 - 14:14)
        val t7 = result.trips[6]
        assertEquals("14:09", t7.startTimeText)
        assertEquals("14:14", t7.endTimeText)
        assertEquals(5, t7.durationMinutes)
        assertEquals(2.0, t7.distanceKm ?: 0.0, 0.01)
        assertEquals(48, t7.maxSpeedKmh)
        assertEquals(0.1, t7.energyKwh ?: 0.0, 0.01)
    }

    @Test
    fun testParseMultilineDatesAndDashes() {
        val multilineText = """
            Ritgegevens
            2026-11-20
            08:15 – 08:45
            Afgelegde afstand 25,5 km
            Maximale snelheid 112 km/h
            Totale energieverbruik 4,8 kWh
        """.trimIndent()

        val result = LeapmotorParser.parseFromText(multilineText)
        assertEquals(1, result.trips.size)
        val trip = result.trips[0]
        assertEquals("2026-11-20", trip.dateText)
        assertEquals("08:15", trip.startTimeText)
        assertEquals("08:45", trip.endTimeText)
        assertEquals(30, trip.durationMinutes)
        assertEquals(25.5, trip.distanceKm ?: 0.0, 0.01)
        assertEquals(112, trip.maxSpeedKmh)
        assertEquals(4.8, trip.energyKwh ?: 0.0, 0.01)
    }

    @Test
    fun testMidnightCrossing() {
        val midnightText = """
            Ritgegevens
            2026/11/19 23:50-00:15
        """.trimIndent()

        val result = LeapmotorParser.parseFromText(midnightText)
        assertEquals(1, result.trips.size)
        val trip = result.trips[0]
        assertEquals(25, trip.durationMinutes)
    }
}
