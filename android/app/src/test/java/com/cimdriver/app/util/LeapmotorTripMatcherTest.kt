package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LeapmotorTripMatcherTest {

    private val testVehicle = Vehicle(
        id = 10L,
        name = "Leapmotor B10",
        licensePlate = "NFN-80-F",
        make = "Leapmotor",
        model = "B10",
        odometerStart = 10,
        odometerCurrent = 308,
        engineType = "EV"
    )

    private fun createTime(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 19, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testExactMatchAndDurationMatching() {
        val lmTrips = listOf(
            LeapmotorTelematicsTrip(
                dateText = "2026/11/19",
                startTimeText = "18:58",
                endTimeText = "19:18",
                startTimestamp = createTime(18, 58),
                endTimestamp = createTime(19, 18),
                durationMinutes = 20
            ),
            LeapmotorTelematicsTrip(
                dateText = "2026/11/19",
                startTimeText = "18:13",
                endTimeText = "18:29",
                startTimestamp = createTime(18, 13),
                endTimestamp = createTime(18, 29),
                durationMinutes = 16
            ),
            LeapmotorTelematicsTrip(
                dateText = "2026/11/19",
                startTimeText = "17:12",
                endTimeText = "17:14",
                startTimestamp = createTime(17, 12),
                endTimestamp = createTime(17, 14),
                durationMinutes = 2
            )
        )

        val cimTrips = listOf(
            // Matches 18:58 - 19:18 almost exactly (18:58 - 19:19)
            Trip(
                id = 101L,
                vehicleId = 10L,
                startTime = createTime(18, 58),
                endTime = createTime(19, 19),
                startAddress = "Kantoor",
                endAddress = "Thuis",
                distanceMeters = 15000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 290,
                odometerEnd = 305
            ),
            // Excess stop idle: Leapmotor is 18:13 - 18:29 (16 min), CIMDriver ran until 18:45 (32 min)
            Trip(
                id = 102L,
                vehicleId = 10L,
                startTime = createTime(18, 13),
                endTime = createTime(18, 45),
                startAddress = "Klant A",
                endAddress = "Kantoor",
                distanceMeters = 12000,
                tripType = "BUSINESS",
                note = null,
                status = "DONE",
                isManual = false,
                odometerStart = 278,
                odometerEnd = 290
            )
            // Notice: 17:12 - 17:14 is NOT in cimTrips (it is a missing trip!)
        )

        val report = LeapmotorTripMatcher.match(lmTrips, cimTrips, testVehicle, detectedOdometerKm = 308)

        assertEquals(3, report.items.size)
        assertEquals(1, report.exactMatchesCount)
        assertEquals(1, report.discrepancyCount)
        assertEquals(1, report.missingCount)
        assertEquals(308, report.detectedOdometerKm)

        // Item 0: 17:12 - 17:14 (sorted chronologically) -> MISSING_IN_CIMDRIVER
        val itemMissing = report.items[0]
        assertEquals("17:12", itemMissing.lmTrip.startTimeText)
        assertEquals(LeapmotorMatchStatus.MISSING_IN_CIMDRIVER, itemMissing.status)
        assertEquals(LeapmotorActionType.ADD_MISSING_TRIP, itemMissing.suggestedAction.type)

        // Item 1: 18:13 - 18:29 -> EXCESS_STOP_IDLE
        val itemIdle = report.items[1]
        assertEquals("18:13", itemIdle.lmTrip.startTimeText)
        assertEquals(LeapmotorMatchStatus.EXCESS_STOP_IDLE, itemIdle.status)
        assertEquals(16, itemIdle.endDeltaMinutes)
        assertEquals(LeapmotorActionType.TRIM_END_TIME, itemIdle.suggestedAction.type)

        // Item 2: 18:58 - 19:18 -> EXACT_MATCH
        val itemExact = report.items[2]
        assertEquals("18:58", itemExact.lmTrip.startTimeText)
        assertEquals(LeapmotorMatchStatus.EXACT_MATCH, itemExact.status)
        assertEquals(LeapmotorActionType.CONFIRM_VERIFIED, itemExact.suggestedAction.type)
    }

    @Test
    fun testDelayedStartDetection() {
        val lmTrip = LeapmotorTelematicsTrip(
            dateText = "2026/11/19",
            startTimeText = "14:09",
            endTimeText = "14:14",
            startTimestamp = createTime(14, 9),
            endTimestamp = createTime(14, 14),
            durationMinutes = 5,
            distanceKm = 2.0
        )

        // CIMDriver started 4 minutes late (14:13 instead of 14:09)
        val cimTrip = Trip(
            id = 201L,
            vehicleId = 10L,
            startTime = createTime(14, 13),
            endTime = createTime(14, 14),
            startAddress = "Locatie A",
            endAddress = "Locatie B",
            distanceMeters = 800,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 250,
            odometerEnd = 251
        )

        val report = LeapmotorTripMatcher.match(listOf(lmTrip), listOf(cimTrip), testVehicle)
        val item = report.items.first()
        assertEquals(LeapmotorMatchStatus.DELAYED_START, item.status)
        assertEquals(LeapmotorActionType.EXTEND_START_TIME, item.suggestedAction.type)
        assertEquals(4, item.startDeltaMinutes)
    }

    @Test
    fun testSeparateConsecutiveTripsAreNotGrouped() {
        // Two consecutive Leapmotor trips separated by a 10 min stop
        val lmTrip1 = LeapmotorTelematicsTrip(
            dateText = "2026/11/19",
            startTimeText = "14:09",
            endTimeText = "14:14",
            startTimestamp = createTime(14, 9),
            endTimestamp = createTime(14, 14),
            durationMinutes = 5
        )
        val lmTrip2 = LeapmotorTelematicsTrip(
            dateText = "2026/11/19",
            startTimeText = "14:24",
            endTimeText = "14:28",
            startTimestamp = createTime(14, 24),
            endTimestamp = createTime(14, 28),
            durationMinutes = 4
        )

        // Two corresponding distinct CIMDriver trips
        val cimTrip1 = Trip(
            id = 301L,
            vehicleId = 10L,
            startTime = createTime(14, 9),
            endTime = createTime(14, 14),
            startAddress = "A",
            endAddress = "B",
            distanceMeters = 2000,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 100,
            odometerEnd = 102
        )
        val cimTrip2 = Trip(
            id = 302L,
            vehicleId = 10L,
            startTime = createTime(14, 24),
            endTime = createTime(14, 28),
            startAddress = "B",
            endAddress = "C",
            distanceMeters = 1500,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 102,
            odometerEnd = 104
        )

        val report = LeapmotorTripMatcher.match(listOf(lmTrip1, lmTrip2), listOf(cimTrip1, cimTrip2), testVehicle)
        assertEquals(2, report.items.size)
        assertEquals(2, report.exactMatchesCount)
        assertEquals(0, report.discrepancyCount)

        // Item 0 matches cimTrip1 ONLY
        assertEquals(301L, report.items[0].matchingTrip?.id)
        assertEquals(LeapmotorMatchStatus.EXACT_MATCH, report.items[0].status)

        // Item 1 matches cimTrip2 ONLY
        assertEquals(302L, report.items[1].matchingTrip?.id)
        assertEquals(LeapmotorMatchStatus.EXACT_MATCH, report.items[1].status)
    }

    @Test
    fun testAlreadyVerifiedTripHasNoAction() {
        val lmTrip = LeapmotorTelematicsTrip(
            dateText = "2026/11/19",
            startTimeText = "14:09",
            endTimeText = "14:14",
            startTimestamp = createTime(14, 9),
            endTimestamp = createTime(14, 14),
            durationMinutes = 5
        )
        val verifiedCimTrip = Trip(
            id = 401L,
            vehicleId = 10L,
            startTime = createTime(14, 9),
            endTime = createTime(14, 14),
            startAddress = "A",
            endAddress = "B",
            distanceMeters = 2000,
            tripType = "BUSINESS",
            note = "[Geverifieerd met Leapmotor B10]",
            status = "DONE",
            isManual = false,
            odometerStart = 100,
            odometerEnd = 102
        )

        val report = LeapmotorTripMatcher.match(listOf(lmTrip), listOf(verifiedCimTrip), testVehicle)
        val item = report.items.first()
        assertEquals(LeapmotorMatchStatus.EXACT_MATCH, item.status)
        assertTrue(item.isAlreadyVerified)
        assertEquals(LeapmotorActionType.NONE, item.suggestedAction.type)
        assertEquals("Reeds geverifieerd", item.suggestedAction.title)
    }

    @Test
    fun testDuplicateTripsProduceMergeActionWithCandidateIds() {
        val lmTrip = LeapmotorTelematicsTrip(
            dateText = "2026/11/19",
            startTimeText = "17:12",
            endTimeText = "17:14",
            startTimestamp = createTime(17, 12),
            endTimestamp = createTime(17, 14),
            durationMinutes = 2
        )
        val dup1 = Trip(
            id = 501L,
            vehicleId = 10L,
            startTime = createTime(17, 12),
            endTime = createTime(17, 14),
            startAddress = "Home",
            endAddress = "Locatie",
            distanceMeters = 1500,
            tripType = "BUSINESS",
            note = null,
            status = "DONE",
            isManual = false,
            odometerStart = 100,
            odometerEnd = 102
        )
        val dup2 = Trip(
            id = 502L,
            vehicleId = 10L,
            startTime = createTime(17, 12),
            endTime = createTime(17, 14),
            startAddress = "Home",
            endAddress = "Locatie",
            distanceMeters = 1500,
            tripType = "BUSINESS",
            note = "[Boordcomputer toegevoegd]",
            status = "DONE",
            isManual = true,
            odometerStart = 100,
            odometerEnd = 102
        )

        val report = LeapmotorTripMatcher.match(listOf(lmTrip), listOf(dup1, dup2), testVehicle)
        val item = report.items.first()
        assertEquals(LeapmotorMatchStatus.SPLIT_TRIP_CANDIDATE, item.status)
        assertEquals(LeapmotorActionType.MERGE_TRIPS, item.suggestedAction.type)
        assertEquals(listOf(501L, 502L), item.suggestedAction.candidateTripIds)
    }
}
