package com.cimdriver.app.domain

import com.cimdriver.app.data.local.entity.ClassificationRule
import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.viewmodel.DashboardStats
import com.cimdriver.app.ui.viewmodel.DistanceChartItem
import com.cimdriver.app.ui.viewmodel.TimeFilter
import com.cimdriver.app.util.TripCategory
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.util.AddressMatching
import com.cimdriver.app.util.TripStatus
import com.cimdriver.app.ui.viewmodel.CategoryBreakdownItem
import java.util.Calendar

object DashboardStatsCalculator {

    fun classifyTrip(
        trip: Trip,
        addresses: List<SavedAddress>,
        rules: List<ClassificationRule>,
        settings: com.cimdriver.app.data.local.entity.Settings? = null
    ): TripCategory? {
        fun addressType(address: String?): String? = AddressMatching.findSavedAddress(address, addresses)?.let { savedAddress ->
            savedAddress.addressType ?: when {
                savedAddress.isHomeLocation -> "THUIS"
                savedAddress.isWorkLocation -> "WERK"
                savedAddress.isCustomerLocation -> "KLANT"
                else -> null
            }
        }
        return TripClassification.classify(
            tripType = trip.tripType,
            startAddressType = addressType(trip.startAddress),
            endAddressType = addressType(trip.endAddress),
            rules = rules,
            defaultCategory = if (settings?.classificationDefault == "BUSINESS") TripCategory.BUSINESS else TripCategory.PRIVATE,
            homeWorkAsCommute = settings?.classifyHomeWorkAsCommute ?: true,
            customerAsBusiness = settings?.classifyCustomerAsBusiness ?: true,
            timestamp = trip.startTime,
            workDaysStr = settings?.workDays,
            workStartTime = settings?.workStartTime,
            workEndTime = settings?.workEndTime
        )
    }

    fun calculateDashboardStats(
        tripList: List<Trip>,
        timeFilter: TimeFilter,
        selectedVehicleId: Long?,
        vehicleList: List<Vehicle>,
        rules: List<ClassificationRule>,
        addresses: List<SavedAddress>,
        settings: com.cimdriver.app.data.local.entity.Settings? = null
    ): DashboardStats {
        val calNow = Calendar.getInstance()
        val currentWeek = calNow.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)
        
        val calPrev = Calendar.getInstance()
        when (timeFilter) {
            TimeFilter.WEEK -> calPrev.add(Calendar.WEEK_OF_YEAR, -1)
            TimeFilter.MONTH -> calPrev.add(Calendar.MONTH, -1)
            TimeFilter.YEAR -> calPrev.add(Calendar.YEAR, -1)
        }
        val prevWeek = calPrev.get(Calendar.WEEK_OF_YEAR)
        val prevMonth = calPrev.get(Calendar.MONTH)
        val prevYear = calPrev.get(Calendar.YEAR)
        
        var totalZakelijkMeters = 0
        var totalPriveMeters = 0
        var totalWoonWerkMeters = 0
        
        var totalZakelijkMs = 0L
        var totalPriveMs = 0L
        var totalWoonWerkMs = 0L
        
        var prevZakelijkMeters = 0
        var prevPriveMeters = 0
        var prevWoonWerkMeters = 0
        
        var ytdPriveMeters = 0
        
        val activeVehicleId = selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id
        val filteredTripList = if (activeVehicleId != null) tripList.filter { it.vehicleId == activeVehicleId } else tripList
        
        filteredTripList.forEach { trip ->
            val status = TripStatus.from(trip.status)
            if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = trip.startTime
                
                val isMatch = when (timeFilter) {
                    TimeFilter.WEEK -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                    TimeFilter.MONTH -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                    TimeFilter.YEAR -> cal.get(Calendar.YEAR) == currentYear
                }
                
                val isPrevMatch = when (timeFilter) {
                    TimeFilter.WEEK -> cal.get(Calendar.YEAR) == prevYear && cal.get(Calendar.WEEK_OF_YEAR) == prevWeek
                    TimeFilter.MONTH -> cal.get(Calendar.YEAR) == prevYear && cal.get(Calendar.MONTH) == prevMonth
                    TimeFilter.YEAR -> cal.get(Calendar.YEAR) == prevYear
                }
                
                if (isMatch || isPrevMatch || cal.get(Calendar.YEAR) == currentYear) {
                    val durationMs = (trip.endTime ?: trip.startTime) - trip.startTime
                    val category = classifyTrip(trip, addresses, rules, settings)
                    
                    if (cal.get(Calendar.YEAR) == currentYear && category == TripCategory.PRIVATE) {
                        ytdPriveMeters += trip.distanceMeters
                    }

                    if (isMatch) {
                        if (category == TripCategory.BUSINESS || category == TripCategory.COMMUTE) {
                            totalZakelijkMeters += trip.distanceMeters
                            totalZakelijkMs += durationMs
                            if (category == TripCategory.COMMUTE) {
                                totalWoonWerkMeters += trip.distanceMeters
                                totalWoonWerkMs += durationMs
                            }
                        } else if (category == TripCategory.PRIVATE) {
                            totalPriveMeters += trip.distanceMeters
                            totalPriveMs += durationMs
                        }
                    }
                    
                    if (isPrevMatch) {
                        if (category == TripCategory.BUSINESS || category == TripCategory.COMMUTE) {
                            prevZakelijkMeters += trip.distanceMeters
                            if (category == TripCategory.COMMUTE) {
                                prevWoonWerkMeters += trip.distanceMeters
                            }
                        } else if (category == TripCategory.PRIVATE) {
                            prevPriveMeters += trip.distanceMeters
                        }
                    }
                }
            }
        }
        
        return DashboardStats(
            zakelijkKm = totalZakelijkMeters / 1000.0,
            priveKm = totalPriveMeters / 1000.0,
            woonWerkKm = totalWoonWerkMeters / 1000.0,
            zakelijkMin = totalZakelijkMs / (1000 * 60),
            priveMin = totalPriveMs / (1000 * 60),
            woonWerkMin = totalWoonWerkMs / (1000 * 60),
            prevZakelijkKm = prevZakelijkMeters / 1000.0,
            prevPriveKm = prevPriveMeters / 1000.0,
            prevWoonWerkKm = prevWoonWerkMeters / 1000.0,
            ytdPriveKm = ytdPriveMeters / 1000.0
        )
    }

    fun calculateCategoryBreakdown(
        tripList: List<Trip>,
        timeFilter: TimeFilter,
        selectedVehicleId: Long?,
        vehicleList: List<Vehicle>,
        rules: List<ClassificationRule>
    ): List<CategoryBreakdownItem> {
        val calNow = Calendar.getInstance()
        val currentWeek = calNow.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)
        
        val activeVehicleId = selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id
        val filteredTripList = if (activeVehicleId != null) tripList.filter { it.vehicleId == activeVehicleId } else tripList
        
        var totalDistanceMeters = 0
        val distanceByCategory = mutableMapOf<String, Int>()
        
        filteredTripList.forEach { trip ->
            val status = TripStatus.from(trip.status)
            if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = trip.startTime
                
                val isMatch = when (timeFilter) {
                    TimeFilter.WEEK -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                    TimeFilter.MONTH -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                    TimeFilter.YEAR -> cal.get(Calendar.YEAR) == currentYear
                }
                
                if (isMatch) {
                    totalDistanceMeters += trip.distanceMeters
                    
                    val matchingRuleName = rules.find { it.tripType == trip.tripType }?.name ?: trip.tripType
                    val currentCategoryDist = distanceByCategory.getOrDefault(matchingRuleName, 0)
                    distanceByCategory[matchingRuleName] = currentCategoryDist + trip.distanceMeters
                }
            }
        }
        
        if (totalDistanceMeters == 0) return emptyList()
        
        return distanceByCategory.map { (name, distanceMeters) ->
            CategoryBreakdownItem(
                categoryName = name,
                distanceKm = distanceMeters / 1000.0,
                percentage = (distanceMeters.toFloat() / totalDistanceMeters.toFloat()) * 100f
            )
        }.sortedByDescending { it.distanceKm }
    }

    fun calculateDistanceChartData(
        tripList: List<Trip>,
        timeFilter: TimeFilter,
        selectedVehicleId: Long?,
        vehicleList: List<Vehicle>,
        rules: List<ClassificationRule>,
        addresses: List<SavedAddress>,
        settings: com.cimdriver.app.data.local.entity.Settings? = null
    ): List<DistanceChartItem> {
        val calNow = Calendar.getInstance()
        calNow.firstDayOfWeek = Calendar.MONDAY
        val currentWeek = calNow.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)
        
        val activeVehicleId = selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id
        val filteredTripList = if (activeVehicleId != null) tripList.filter { it.vehicleId == activeVehicleId } else tripList
        
        val items = mutableListOf<DistanceChartItem>()
        
        when (timeFilter) {
            TimeFilter.WEEK -> {
                val shortDays = listOf("Zo", "Ma", "Di", "Wo", "Do", "Vr", "Za")
                for (i in 1..7) {
                    val dayLabel = shortDays[i-1]
                    var zKm = 0.0
                    var pKm = 0.0
                    var wKm = 0.0
                    filteredTripList.forEach { trip ->
                        val status = TripStatus.from(trip.status)
                        if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                            val cal = Calendar.getInstance()
                            cal.firstDayOfWeek = Calendar.MONDAY
                            cal.timeInMillis = trip.startTime
                            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.DAY_OF_WEEK) == i) {
                                val km = trip.distanceMeters / 1000.0
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) zKm += km
                                if (category == TripCategory.COMMUTE) {
                                    wKm += km
                                }
                                if (category == TripCategory.PRIVATE) pKm += km
                            }
                        }
                    }
                    items.add(DistanceChartItem(dayLabel, zKm, pKm, wKm, zKm + pKm + wKm))
                }
                return listOf(items[1], items[2], items[3], items[4], items[5], items[6], items[0])
            }
            TimeFilter.MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                cal.set(Calendar.MONTH, currentMonth)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (i in 1..daysInMonth) {
                    var zKm = 0.0
                    var pKm = 0.0
                    var wKm = 0.0
                    filteredTripList.forEach { trip ->
                        val status = TripStatus.from(trip.status)
                        if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                            val tCal = Calendar.getInstance()
                            tCal.timeInMillis = trip.startTime
                            if (tCal.get(Calendar.YEAR) == currentYear && tCal.get(Calendar.MONTH) == currentMonth && tCal.get(Calendar.DAY_OF_MONTH) == i) {
                                val km = trip.distanceMeters / 1000.0
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) zKm += km
                                if (category == TripCategory.COMMUTE) {
                                    wKm += km
                                }
                                if (category == TripCategory.PRIVATE) pKm += km
                            }
                        }
                    }
                    items.add(DistanceChartItem("$i", zKm, pKm, wKm, zKm + pKm + wKm))
                }
                return items
            }
            TimeFilter.YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                val weeksInYear = cal.getActualMaximum(Calendar.WEEK_OF_YEAR)
                for (i in 1..weeksInYear) {
                    var zKm = 0.0
                    var pKm = 0.0
                    var wKm = 0.0
                    filteredTripList.forEach { trip ->
                        val status = TripStatus.from(trip.status)
                        if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                            val tCal = Calendar.getInstance()
                            tCal.timeInMillis = trip.startTime
                            if (tCal.get(Calendar.YEAR) == currentYear && tCal.get(Calendar.WEEK_OF_YEAR) == i) {
                                val km = trip.distanceMeters / 1000.0
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) zKm += km
                                if (category == TripCategory.COMMUTE) {
                                    wKm += km
                                }
                                if (category == TripCategory.PRIVATE) pKm += km
                            }
                        }
                    }
                    items.add(DistanceChartItem("W$i", zKm, pKm, wKm, zKm + pKm + wKm))
                }
                return items
            }
        }
    }
}
