package com.cimdriver.app.domain

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.viewmodel.TimeFilter
import com.cimdriver.app.util.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DashboardStatsCalculatorTest {

    @Test
    fun `calculateProRataLimit calculates pro rata correctly`() {
        // Full year
        val limitFull = DashboardStatsCalculator.calculateProRataLimit(500, null, null)
        assertEquals(500, limitFull)

        // Half year (e.g., July 1st of current year)
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        cal.set(currentYear, Calendar.JULY, 1, 0, 0, 0)
        val inService = cal.timeInMillis

        val limitHalf = DashboardStatsCalculator.calculateProRataLimit(500, inService, null)
        // July to Dec = 6 months -> 6/12 * 500 = 250
        assertEquals(250, limitHalf)
    }

    @Test
    fun `private detour meters count towards YTD private kilometers and shift business to private`() {
        val now = System.currentTimeMillis()
        val vehicle = Vehicle(
            id = 1L,
            name = "Mijn Auto",
            licensePlate = "XX-999-X",
            make = "Tesla",
            model = "Model 3",
            year = "2023",
            odometerStart = 10000,
            odometerCurrent = 11000,
            notes = null,
            isDefault = true,
            inServiceDate = 0L
        )

        // 1 Business trip of 50 km with 10 km private detour
        val businessTripWithDetour = Trip(
            id = 101L,
            vehicleId = 1L,
            startTime = now - 1000 * 3600 * 2,
            endTime = now - 1000 * 3600 * 1,
            startAddress = "Home Address",
            endAddress = "Work Address",
            distanceMeters = 50000, // 50 km
            tripType = "Business Meeting",
            note = null,
            status = TripStatus.DONE.value,
            isManual = false,
            odometerStart = 10000,
            odometerEnd = 10050,
            privateDetourMeters = 10000 // 10 km detour
        )

        // 1 Pure private trip of 20 km
        val privateTrip = Trip(
            id = 102L,
            vehicleId = 1L,
            startTime = now - 1000 * 1800,
            endTime = now,
            startAddress = "Supermarket",
            endAddress = "Home Address",
            distanceMeters = 20000, // 20 km
            tripType = "Private",
            note = null,
            status = TripStatus.DONE.value,
            isManual = false,
            odometerStart = 10050,
            odometerEnd = 10070,
            privateDetourMeters = 0
        )

        val stats = DashboardStatsCalculator.calculateDashboardStats(
            tripList = listOf(businessTripWithDetour, privateTrip),
            timeFilter = TimeFilter.YEAR,
            selectedVehicleId = 1L,
            vehicleList = listOf(vehicle),
            rules = emptyList(),
            addresses = emptyList()
        )

        // Business net distance: 50 km - 10 km detour = 40.0 km
        assertEquals(40.0, stats.zakelijkKm, 0.001)

        // Private distance: 20 km pure private + 10 km detour = 30.0 km
        assertEquals(30.0, stats.priveKm, 0.001)

        // YTD private distance: 20 km + 10 km detour = 30.0 km
        assertEquals(30.0, stats.ytdPriveKm, 0.001)
    }
}
