package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.OdometerCheck
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerRecalculatorTest {

    private val testVehicle = Vehicle(
        id = 1L,
        name = "CIMSOLUTIONS Auto",
        licensePlate = "AA-123-BB",
        make = "Tesla",
        model = "Model 3",
        year = "2023",
        odometerStart = 32800,
        odometerCurrent = 32912,
        notes = null
    )

    @Test
    fun testRecalculateSequentialTripsWithScrambledOdometers() {
        // Two trips where stored odometerStart was completely off (e.g. 50000)
        val trip1 = Trip(
            id = 1L,
            vehicleId = 1L,
            startTime = 1000L,
            endTime = 2000L,
            startAddress = "Groningen",
            endAddress = "Haren",
            distanceMeters = 8000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 50000,
            odometerEnd = 50008
        )
        val trip2 = Trip(
            id = 2L,
            vehicleId = 1L,
            startTime = 3000L,
            endTime = 4000L,
            startAddress = "Haren",
            endAddress = "Assen",
            distanceMeters = 20000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 60000,
            odometerEnd = 60020
        )

        val preview = OdometerRecalculator.preview(
            vehicle = testVehicle,
            trips = listOf(trip1, trip2)
        )

        assertEquals(2, preview.modifiedTripsCount)
        val rec1 = preview.recalculatedTrips[0]
        val rec2 = preview.recalculatedTrips[1]

        // Trip 1 should start at vehicle odometerStart (32800) and end at 32808
        assertEquals(32800, rec1.odometerStart)
        assertEquals(32808, rec1.odometerEnd)

        // Trip 2 should start at 32808 and end at 32828
        assertEquals(32808, rec2.odometerStart)
        assertEquals(32828, rec2.odometerEnd)
    }

    @Test
    fun testOdometerCheckPreservesGap() {
        // Vehicle starts at 32800.
        // Trip 1 is 8 km (32800 -> 32808).
        // Then at time 2500L, driver checks odometer: it reads 32820 km! (a +12 km gap).
        // Trip 2 is 10 km, starts at time 3000L.
        // Trip 2 should start at 32820 km (ending at 32830 km).
        // The +12 km gap between Trip 1 and Trip 2 must remain in remainingGaps!

        val trip1 = Trip(
            id = 1L,
            vehicleId = 1L,
            startTime = 1000L,
            endTime = 2000L,
            startAddress = "A",
            endAddress = "B",
            distanceMeters = 8000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 32800,
            odometerEnd = 32808
        )
        val trip2 = Trip(
            id = 2L,
            vehicleId = 1L,
            startTime = 3000L,
            endTime = 4000L,
            startAddress = "B",
            endAddress = "C",
            distanceMeters = 10000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 32808,
            odometerEnd = 32818
        )

        val check = OdometerCheck(
            id = 10L,
            vehicleId = 1L,
            timestamp = 2500L,
            registeredOdometer = 32808,
            correctedOdometer = 32820,
            photoPath = null,
            deviationKm = 12
        )

        val preview = OdometerRecalculator.preview(
            vehicle = testVehicle.copy(odometerCurrent = 32830),
            trips = listOf(trip1, trip2),
            odometerChecks = listOf(check)
        )

        assertEquals(1, preview.preservedChecksCount)
        val rec1 = preview.recalculatedTrips[0]
        val rec2 = preview.recalculatedTrips[1]

        assertEquals(32800, rec1.odometerStart)
        assertEquals(32808, rec1.odometerEnd)

        // Trip 2 starts at the check's reading (32820)
        assertEquals(32820, rec2.odometerStart)
        assertEquals(32830, rec2.odometerEnd)

        // The gap of 12 km must be detected in remainingGaps!
        assertEquals(1, preview.remainingGaps.size)
        val gap = preview.remainingGaps[0]
        assertEquals(12, gap.gapKm)
        assertEquals(32808, gap.expectedOdometer)
        assertEquals(32820, gap.actualOdometer)
    }

    @Test
    fun testReconstructedUserScreenshotScenario() {
        // Exact numbers from user's screenshot:
        // Trip 81: 26-10 17:05 (8 km)
        // Trip 89: 26-10 16:38 (8 km) - out of order / wrong stands (32904-32912)
        // Trip 83: 27-10 07:43 (8 km)
        // Trip 86: 28-10 20:12 (7 km)
        // Vehicle odometer: 32912

        val trip89 = Trip(
            id = 89L,
            vehicleId = 1L,
            startTime = 1000L, // 16:38
            endTime = 1500L,
            startAddress = "Kempkensberg",
            endAddress = "Mellenssteeg",
            distanceMeters = 8000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = true,
            odometerStart = 32904,
            odometerEnd = 32912
        )
        val trip81 = Trip(
            id = 81L,
            vehicleId = 1L,
            startTime = 2000L, // 17:05
            endTime = 2500L,
            startAddress = "Kempkensberg",
            endAddress = "Mellenssteeg",
            distanceMeters = 8000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 32864,
            odometerEnd = 32872
        )
        val trip83 = Trip(
            id = 83L,
            vehicleId = 1L,
            startTime = 5000L, // 27-10 07:43
            endTime = 5500L,
            startAddress = "Mellenssteeg",
            endAddress = "Duo",
            distanceMeters = 8000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 32872,
            odometerEnd = 32880
        )
        val trip86 = Trip(
            id = 86L,
            vehicleId = 1L,
            startTime = 10000L, // 28-10 20:12
            endTime = 10500L,
            startAddress = "Duo",
            endAddress = "Mellenssteeg",
            distanceMeters = 7000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 32897,
            odometerEnd = 32904
        )

        val vehicle = testVehicle.copy(odometerStart = 32864, odometerCurrent = 32912)

        val preview = OdometerRecalculator.preview(
            vehicle = vehicle,
            trips = listOf(trip89, trip81, trip83, trip86)
        )

        // Trip sequence in time: 89 (1000L), 81 (2000L), 83 (5000L), 86 (10000L)
        val r89 = preview.recalculatedTrips.first { it.id == 89L }
        val r81 = preview.recalculatedTrips.first { it.id == 81L }
        val r83 = preview.recalculatedTrips.first { it.id == 83L }
        val r86 = preview.recalculatedTrips.first { it.id == 86L }

        // Start 32864 -> 89 (8 km) -> 32872
        assertEquals(32864, r89.odometerStart)
        assertEquals(32872, r89.odometerEnd)

        // 81 (8 km) -> 32872 to 32880
        assertEquals(32872, r81.odometerStart)
        assertEquals(32880, r81.odometerEnd)

        // 83 (8 km) -> 32880 to 32888
        assertEquals(32880, r83.odometerStart)
        assertEquals(32888, r83.odometerEnd)

        // 86 (7 km) -> starts after intervening, ends at 32888 + 7 = 32895
        assertEquals(32895, r86.odometerEnd)

        // The remaining gap is now purely between the last trip (32895) and vehicle current (32912) = 17 km
        // And the overlapping/inverted gaps between 81, 89, 83 are completely resolved!
        assertEquals(1, preview.remainingGaps.size)
        assertEquals(GapType.CURRENT_STAND_GAP, preview.remainingGaps[0].gapType)
    }

    @Test
    fun testSyncVehicleOdometerFixType() {
        val gap = OdometerGap(
            id = "gap_last_86",
            vehicleId = 1L,
            gapType = GapType.CURRENT_STAND_GAP,
            previousTrip = Trip(
                id = 86L,
                vehicleId = 1L,
                startTime = 1000L,
                endTime = 2000L,
                startAddress = "A",
                endAddress = "B",
                distanceMeters = 8000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 32896,
                odometerEnd = 32904
            ),
            nextTrip = null,
            expectedOdometer = 32904,
            actualOdometer = 32912,
            gapKm = 8,
            gapTimeStart = 2000L,
            gapTimeEnd = 3000L
        )

        // Verify that SYNC_VEHICLE_ODOMETER is among suggested fixes for CURRENT_STAND_GAP
        val testVehicle = testVehicle.copy(odometerStart = 32896, odometerCurrent = 32912)
        val gaps = OdometerGapAnalyzer.analyze(testVehicle, listOf(gap.previousTrip!!))
        assertEquals(1, gaps.size)
        val detectedGap = gaps.first()
        assertTrue(detectedGap.suggestedFixes.any { it.type == FixType.SYNC_VEHICLE_ODOMETER })
    }
}

