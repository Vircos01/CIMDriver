package com.cimdriver.app.util

import com.cimdriver.app.data.local.dao.HoursTargetWithDetails
import com.cimdriver.app.data.local.entity.HoursTarget
import com.cimdriver.app.data.local.entity.ProjectCode
import com.cimdriver.app.data.local.entity.WorkDay
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

    @Test
    fun calculateTargetProgress_countsApprovedAndDoneWorkDays() {
        val target = HoursTargetWithDetails(
            target = HoursTarget(
                id = 1,
                name = "Jaardoel",
                targetHours = 1000.0,
                year = 2026
            ),
            clientName = null,
            projectCodeStr = null
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 8, 30, 0)
        }
        val arrival = cal.timeInMillis
        val departure = arrival + (8 * 3600 * 1000L) + (30 * 60 * 1000L) // 8.5 hours total, 30 min break -> 8.0 hours

        val workDays = listOf(
            WorkDay(
                id = 1,
                date = arrival,
                firstDepartureTime = arrival - 1800000L,
                arrivalTime = arrival,
                departureTime = departure,
                lastArrivalTime = departure + 1800000L,
                breakMinutes = 30,
                workLocationLabel = "Kantoor",
                status = "APPROVED"
            ),
            WorkDay(
                id = 2,
                date = arrival + 86400000L,
                firstDepartureTime = arrival,
                arrivalTime = arrival,
                departureTime = departure,
                lastArrivalTime = departure,
                breakMinutes = 30,
                workLocationLabel = "Kantoor",
                status = "DONE"
            ),
            WorkDay(
                id = 3,
                date = arrival + 172800000L,
                firstDepartureTime = arrival,
                arrivalTime = arrival,
                departureTime = departure,
                lastArrivalTime = departure,
                breakMinutes = 30,
                workLocationLabel = "Kantoor",
                status = "TO_REVIEW" // Should not be counted
            )
        )

        val result = calculateTargetProgress(
            targetDetails = target,
            workDays = workDays,
            projectCodes = emptyList(),
            settings = null,
            targetYear = 2026
        )

        assertEquals(16.0, result.accumulatedHours, 0.01)
        assertEquals(0.016, result.percentage.toDouble(), 0.001)
    }

    @Test
    fun calculateTargetProgress_matchesByProjectCodeStringWhenProjectCodeIdIsNull() {
        val project = ProjectCode(
            id = 42L,
            clientId = 10L,
            code = "CIM-DEV",
            description = "Dev",
            hourlyRate = 95.0
        )

        val targetHours = HoursTargetWithDetails(
            target = HoursTarget(
                id = 1,
                name = "Project Target",
                targetHours = 100.0,
                year = 2026,
                projectCodeId = 42L
            ),
            clientName = "Client A",
            projectCodeStr = "CIM-DEV"
        )

        val targetRevenue = HoursTargetWithDetails(
            target = HoursTarget(
                id = 2,
                name = "Client Revenue Target",
                targetHours = 0.0,
                targetRevenue = 10000.0,
                targetType = "REVENUE",
                year = 2026,
                clientId = 10L
            ),
            clientName = "Client A",
            projectCodeStr = null
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 9, 0, 0)
        }
        val arrival = cal.timeInMillis
        val departure = arrival + (5 * 3600 * 1000L) // 5 hours, 30 min break -> 4.5 hours net

        val workDay = WorkDay(
            id = 1,
            date = arrival,
            firstDepartureTime = arrival,
            arrivalTime = arrival,
            departureTime = departure,
            lastArrivalTime = departure,
            breakMinutes = 30,
            workLocationLabel = "Client Office",
            projectCode = "CIM-DEV",
            projectCodeId = null, // Simulated missing projectCodeId
            status = "APPROVED"
        )

        val resultHours = calculateTargetProgress(
            targetDetails = targetHours,
            workDays = listOf(workDay),
            projectCodes = listOf(project),
            settings = null,
            targetYear = 2026
        )

        assertEquals(4.5, resultHours.accumulatedHours, 0.01)

        val resultRevenue = calculateTargetProgress(
            targetDetails = targetRevenue,
            workDays = listOf(workDay),
            projectCodes = listOf(project),
            settings = null,
            targetYear = 2026
        )

        assertEquals(4.5 * 95.0, resultRevenue.accumulatedRevenue, 0.01)
    }

    private fun utcDate(year: Int, month: Int, day: Int): Long = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day)
    }.timeInMillis
}