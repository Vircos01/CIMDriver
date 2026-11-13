package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.data.local.entity.Settings
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.WorkDay
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class WorkHoursTriageTest {

    @Test
    fun testCompleteWorkDay_hasHighConfidence() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 11, 0, 0, 0) // Monday
        }
        val date = cal.timeInMillis
        val arrival = date + 8 * 3600_000L + 30 * 60_000L // 08:30
        val departure = date + 17 * 3600_000L // 17:00

        val workDay = WorkDay(
            id = 1L,
            date = date,
            firstDepartureTime = arrival,
            arrivalTime = arrival,
            departureTime = departure,
            lastArrivalTime = departure,
            breakMinutes = 30,
            workLocationLabel = "Kantoor",
            status = "DONE"
        )

        val settings = Settings(workDays = "1,2,3,4,5", workStartTime = "08:30", workEndTime = "17:00")
        val suggestion = WorkHoursTriage.evaluateWorkDay(workDay, settings = settings, nowMillis = date + 2 * 86400_000L)

        assertTrue(suggestion.isHighConfidence)
        assertFalse(suggestion.requiresAttention)
        assertTrue(suggestion.statusText.contains("Vrijwel zeker compleet"))
        assertEquals(30, suggestion.suggestedBreakMinutes)
    }

    @Test
    fun testMissingDeparture_suggestsFromReturnTrip() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 11, 0, 0, 0) // Monday
        }
        val date = cal.timeInMillis
        val arrival = date + 8 * 3600_000L + 30 * 60_000L // 08:30

        val workDay = WorkDay(
            id = 2L,
            date = date,
            firstDepartureTime = arrival,
            arrivalTime = arrival,
            departureTime = null,
            lastArrivalTime = null,
            breakMinutes = 30,
            workLocationLabel = "Kantoor",
            status = "IN_PROGRESS"
        )

        val returnTrip = Trip(
            id = 10L,
            vehicleId = 1L,
            startTime = date + 17 * 3600_000L + 15 * 60_000L, // 17:15
            endTime = date + 17 * 3600_000L + 45 * 60_000L,
            startAddress = "Kantoor, Utrecht",
            endAddress = "Thuis, Amsterdam",
            distanceMeters = 35000,
            tripType = "COMMUTE",
            note = null,
            isManual = false,
            odometerStart = 10000,
            odometerEnd = 10035,
            status = "DONE"
        )

        val suggestion = WorkHoursTriage.evaluateWorkDay(
            workDay = workDay,
            tripsOnDay = listOf(returnTrip),
            nowMillis = date + 2 * 86400_000L // Two days later
        )

        assertTrue(suggestion.requiresAttention)
        assertEquals(returnTrip.startTime, suggestion.suggestedDepartureTime)
        assertTrue(suggestion.statusText.contains("Eindtijd ontbreekt"))
    }

    @Test
    fun testLongDayWithoutBreak_suggestsArboBreak() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 11, 0, 0, 0)
        }
        val date = cal.timeInMillis
        val arrival = date + 8 * 3600_000L // 08:00
        val departure = date + 17 * 3600_000L // 17:00 (9 gross hours)

        val workDay = WorkDay(
            id = 3L,
            date = date,
            firstDepartureTime = arrival,
            arrivalTime = arrival,
            departureTime = departure,
            lastArrivalTime = departure,
            breakMinutes = 0, // No break!
            workLocationLabel = "Kantoor",
            status = "TO_REVIEW"
        )

        val suggestion = WorkHoursTriage.evaluateWorkDay(workDay, nowMillis = date + 86400_000L)

        assertTrue(suggestion.requiresAttention)
        assertEquals(45, suggestion.suggestedBreakMinutes) // Arbowet > 8h requires 45 min
        assertTrue(suggestion.statusText.contains("zonder pauze"))
    }

    @Test
    fun testProjectCodeSuggestion_fromCustomerTrip() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 11, 0, 0, 0)
        }
        val date = cal.timeInMillis
        val arrival = date + 8 * 3600_000L
        val departure = date + 17 * 3600_000L

        val workDay = WorkDay(
            id = 4L,
            date = date,
            firstDepartureTime = arrival,
            arrivalTime = arrival,
            departureTime = departure,
            lastArrivalTime = departure,
            breakMinutes = 30,
            workLocationLabel = "Klant",
            projectCode = null,
            status = "TO_REVIEW"
        )

        val customerAddress = SavedAddress(
            id = 50L,
            label = "Klant Acme",
            address = "Keizersgracht 100, Amsterdam",
            latitude = 52.37,
            longitude = 4.89,
            isCustomerLocation = true,
            projectCode = "PRJ-ACME-2026"
        )

        val clientTrip = Trip(
            id = 11L,
            vehicleId = 1L,
            startTime = date + 10 * 3600_000L,
            endTime = date + 11 * 3600_000L,
            startAddress = "Kantoor, Utrecht",
            endAddress = "Keizersgracht 100, Amsterdam",
            distanceMeters = 40000,
            tripType = "BUSINESS",
            note = null,
            isManual = false,
            odometerStart = 20000,
            odometerEnd = 20040,
            status = "DONE"
        )

        val suggestion = WorkHoursTriage.evaluateWorkDay(
            workDay = workDay,
            tripsOnDay = listOf(clientTrip),
            savedAddresses = listOf(customerAddress),
            nowMillis = date + 86400_000L
        )

        assertEquals("PRJ-ACME-2026", suggestion.suggestedProjectCode)
    }
}
