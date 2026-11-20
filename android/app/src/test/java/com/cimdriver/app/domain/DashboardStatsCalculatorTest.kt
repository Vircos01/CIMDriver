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

    @Test
    fun `calculateCombinedProRataLimit combines period from September start across trade-in`() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)

        // Predecessor (Polo): in service from Sep 1 to Oct 31 (start driving for business in September)
        cal.set(currentYear, Calendar.SEPTEMBER, 1, 0, 0, 0)
        val poloStart = cal.timeInMillis
        cal.set(currentYear, Calendar.OCTOBER, 31, 23, 59, 59)
        val poloEnd = cal.timeInMillis

        val polo = Vehicle(
            id = 1L,
            name = "Volkswagen Polo",
            licensePlate = "GHX-78-D",
            make = "Volkswagen",
            model = "Polo",
            odometerStart = 30000,
            odometerCurrent = 33379,
            inServiceDate = poloStart,
            endServiceDate = poloEnd,
            privateKmYearlyLimit = 500
        )

        // Successor (Leapmotor): in service from Nov 1, replacing Polo
        cal.set(currentYear, Calendar.NOVEMBER, 1, 0, 0, 0)
        val leapmotorStart = cal.timeInMillis

        val leapmotor = Vehicle(
            id = 2L,
            name = "Leapmotor B10",
            licensePlate = "NFN-80-F",
            make = "Leapmotor",
            model = "B10",
            odometerStart = 10,
            odometerCurrent = 338,
            inServiceDate = leapmotorStart,
            endServiceDate = null,
            replacesVehicleId = 1L,
            privateKmYearlyLimit = 500
        )

        val vehicles = listOf(polo, leapmotor)

        // Isolated pro-rata on leapmotor alone: Nov to Dec = 2 months -> 2/12 * 500 = 83 km
        val isolatedLimit = DashboardStatsCalculator.calculateProRataLimit(500, leapmotorStart, null)
        assertEquals(83, isolatedLimit)

        // Combined fiscal limit from September (when business driving started) to Dec = 4 months -> 4/12 * 500 = 166 km
        val combinedLimit = DashboardStatsCalculator.calculateCombinedProRataLimit(leapmotor, vehicles, 500)
        assertEquals(166, combinedLimit)
    }

    @Test
    fun `cumulative private km and YTD stats transfer from predecessor to successor on trade-in`() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        cal.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = cal.timeInMillis

        val polo = Vehicle(
            id = 1L,
            name = "Volkswagen Polo",
            licensePlate = "GHX-78-D",
            make = "Volkswagen",
            model = "Polo",
            odometerStart = 30000,
            odometerCurrent = 33379,
            inServiceDate = startOfYear,
            endServiceDate = startOfYear + 1000L * 3600 * 24 * 180,
            privateKmYearlyLimit = 500
        )

        val leapmotor = Vehicle(
            id = 2L,
            name = "Leapmotor B10",
            licensePlate = "NFN-80-F",
            make = "Leapmotor",
            model = "B10",
            odometerStart = 10,
            odometerCurrent = 338,
            inServiceDate = startOfYear + 1000L * 3600 * 24 * 181,
            endServiceDate = null,
            replacesVehicleId = 1L,
            privateKmYearlyLimit = 500
        )

        val vehicles = listOf(polo, leapmotor)

        // Polo: 9.0 km private trip
        val poloTrip = Trip(
            id = 201L,
            vehicleId = 1L,
            startTime = startOfYear + 1000L * 3600 * 24 * 30,
            endTime = startOfYear + 1000L * 3600 * 24 * 30 + 1000 * 1800,
            startAddress = "Home",
            endAddress = "Shop",
            distanceMeters = 9000,
            tripType = "PRIVATE",
            note = null,
            status = TripStatus.DONE.value,
            isManual = false,
            odometerStart = 31000,
            odometerEnd = 31009
        )

        // Leapmotor: 16.9 km private trip
        val leapmotorTrip = Trip(
            id = 202L,
            vehicleId = 2L,
            startTime = startOfYear + 1000L * 3600 * 24 * 200,
            endTime = startOfYear + 1000L * 3600 * 24 * 200 + 1000 * 1800,
            startAddress = "Home",
            endAddress = "Family",
            distanceMeters = 16900,
            tripType = "PRIVATE",
            note = null,
            status = TripStatus.DONE.value,
            isManual = false,
            odometerStart = 100,
            odometerEnd = 117
        )

        val allTrips = listOf(poloTrip, leapmotorTrip)

        // 1. Helper calculateCumulativePrivateKm should sum both (9.0 + 16.9 = 25.9 km)
        val cumulativeKm = DashboardStatsCalculator.calculateCumulativePrivateKm(
            vehicle = leapmotor,
            allVehicles = vehicles,
            allTrips = allTrips,
            startOfYear = startOfYear
        )
        assertEquals(25.9, cumulativeKm, 0.001)

        // 2. calculateDashboardStats with Leapmotor selected should also have ytdPriveKm = 25.9 km
        val stats = DashboardStatsCalculator.calculateDashboardStats(
            tripList = allTrips,
            timeFilter = TimeFilter.YEAR,
            selectedVehicleId = 2L,
            vehicleList = vehicles,
            rules = emptyList(),
            addresses = emptyList()
        )
        assertEquals(25.9, stats.ytdPriveKm, 0.001)
    }

    @Test
    fun testLeapmotorFallbackPredecessorWhenReplacesVehicleIdIsNull() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        cal.set(currentYear, Calendar.SEPTEMBER, 1, 0, 0, 0)
        val poloStart = cal.timeInMillis
        cal.set(currentYear, Calendar.NOVEMBER, 14, 0, 0, 0)
        val leapmotorStart = cal.timeInMillis

        val polo = Vehicle(
            id = 10L,
            name = "Volkswagen Polo",
            licensePlate = "GHX-78-D",
            make = "Volkswagen",
            model = "Polo",
            odometerStart = 31000,
            odometerCurrent = 33379,
            isActive = false,
            isDefault = false,
            inServiceDate = poloStart,
            endServiceDate = leapmotorStart,
            replacesVehicleId = null
        )

        // Leapmotor with replacesVehicleId = null (e.g. before migration runs)
        val leapmotor = Vehicle(
            id = 20L,
            name = "Leapmotor B10",
            licensePlate = "NFN-80-F",
            make = "Leapmotor",
            model = "B10",
            odometerStart = 10,
            odometerCurrent = 338,
            isActive = true,
            isDefault = true,
            inServiceDate = leapmotorStart,
            endServiceDate = null,
            replacesVehicleId = null
        )

        val vehicles = listOf(polo, leapmotor)
        val preds = DashboardStatsCalculator.getPredecessorVehicles(leapmotor, vehicles)
        assertEquals(1, preds.size)
        assertEquals(10L, preds.first().id)

        val limit = DashboardStatsCalculator.calculateCombinedProRataLimit(leapmotor, vehicles)
        assertEquals(166, limit)

        val successor = DashboardStatsCalculator.getSuccessorVehicle(polo, vehicles)
        assertEquals(20L, successor?.id)
    }
}
