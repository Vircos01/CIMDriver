package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerGapAnalyzerTest {

    private val vehicle = Vehicle(
        id = 1L,
        name = "Test Auto",
        licensePlate = "AB-123-C",
        make = "Volkswagen",
        model = "Golf",
        year = "2020",
        odometerStart = 10000,
        odometerCurrent = 10300,
        notes = null
    )

    @Test
    fun testAirtightChainHasNoGaps() {
        val trips = listOf(
            Trip(
                id = 1L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 100000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10000,
                odometerEnd = 10100
            ),
            Trip(
                id = 2L,
                vehicleId = 1L,
                startTime = 3000L,
                endTime = 4000L,
                startAddress = "B",
                endAddress = "C",
                distanceMeters = 200000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10100,
                odometerEnd = 10300
            )
        )

        val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
        assertTrue(gaps.isEmpty())
        assertTrue(OdometerGapAnalyzer.isChainAirtight(vehicle, trips))
    }

    @Test
    fun testDetectsMissingKmGapBetweenTrips() {
        val trips = listOf(
            Trip(
                id = 1L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 100000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10000,
                odometerEnd = 10100
            ),
            Trip(
                id = 2L,
                vehicleId = 1L,
                startTime = 3000L,
                endTime = 4000L,
                startAddress = "B",
                endAddress = "C",
                distanceMeters = 150000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10150, // 50 km gap!
                odometerEnd = 10300
            )
        )

        val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
        assertEquals(1, gaps.size)
        val gap = gaps.first()
        assertEquals(GapType.MISSING_KM, gap.gapType)
        assertEquals(50, gap.gapKm)
        assertEquals(10100, gap.expectedOdometer)
        assertEquals(10150, gap.actualOdometer)
        assertFalse(OdometerGapAnalyzer.isChainAirtight(vehicle, trips))
    }

    @Test
    fun testDetectsOverlappingKm() {
        val trips = listOf(
            Trip(
                id = 1L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 100000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10000,
                odometerEnd = 10100
            ),
            Trip(
                id = 2L,
                vehicleId = 1L,
                startTime = 3000L,
                endTime = 4000L,
                startAddress = "B",
                endAddress = "C",
                distanceMeters = 210000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10090, // overlap of 10 km
                odometerEnd = 10300
            )
        )

        val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
        assertEquals(1, gaps.size)
        val gap = gaps.first()
        assertEquals(GapType.OVERLAPPING_KM, gap.gapType)
        assertEquals(-10, gap.gapKm)
    }

    @Test
    fun testDetectsFirstTripGap() {
        val trips = listOf(
            Trip(
                id = 1L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 280000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10020, // vehicle starts at 10000
                odometerEnd = 10300
            )
        )

        val gaps = OdometerGapAnalyzer.analyze(vehicle, trips)
        assertEquals(1, gaps.size)
        assertEquals(GapType.FIRST_TRIP_GAP, gaps[0].gapType)
        assertEquals(20, gaps[0].gapKm)
    }

    @Test
    fun testCalculateDistributionPreview() {
        val trips = listOf(
            Trip(
                id = 1L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 40000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10000,
                odometerEnd = 10040
            ),
            Trip(
                id = 2L,
                vehicleId = 1L,
                startTime = 3000L,
                endTime = 4000L,
                startAddress = "B",
                endAddress = "C",
                distanceMeters = 60000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 10040,
                odometerEnd = 10100
            )
        )

        // Distribute 10 km over a total of 100 km (40 km + 60 km)
        val preview = OdometerGapAnalyzer.calculateDistributionPreview(trips, 10)
        assertEquals(2, preview.size)

        // Trip 1 (40% of 100) should get approx 4 extra km -> 44 km
        assertEquals(44, preview[0].newDistanceKm)
        assertEquals(10000, preview[0].newOdoStart)
        assertEquals(10044, preview[0].newOdoEnd)

        // Trip 2 (60% of 100) should get approx 6 extra km -> 66 km
        assertEquals(66, preview[1].newDistanceKm)
        assertEquals(10044, preview[1].newOdoStart)
        assertEquals(10110, preview[1].newOdoEnd)
    }
}
