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

    fun calculateFiscalYearProjection(ytdPrivateKm: Double): Double {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val totalDays = if (cal.getActualMaximum(Calendar.DAY_OF_YEAR) == 366) 366.0 else 365.0
        if (dayOfYear <= 0) return ytdPrivateKm
        return (ytdPrivateKm / dayOfYear.toDouble()) * totalDays
    }

    fun calculateProRataLimit(
        yearlyLimit: Int,
        inServiceDate: Long?,
        endServiceDate: Long?
    ): Int {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        
        // Determine start month
        val startMonth = if (inServiceDate != null) {
            val startCal = Calendar.getInstance().apply { timeInMillis = inServiceDate }
            if (startCal.get(Calendar.YEAR) < currentYear) 1 
            else startCal.get(Calendar.MONTH) + 1
        } else 1

        // Determine end month
        val endMonth = if (endServiceDate != null) {
            val endCal = Calendar.getInstance().apply { timeInMillis = endServiceDate }
            if (endCal.get(Calendar.YEAR) > currentYear) 12
            else endCal.get(Calendar.MONTH) + 1
        } else 12

        val monthsAvailable = (endMonth - startMonth + 1).coerceIn(0, 12)
        return (monthsAvailable.toFloat() / 12f * yearlyLimit).toInt()
    }

    fun isLeapmotor(v: Vehicle?): Boolean {
        if (v == null) return false
        val plate = v.licensePlate.replace("-", "").replace(" ", "").trim()
        return plate.equals("NFN80F", ignoreCase = true) ||
            (v.make.contains("Leapmotor", ignoreCase = true) && v.model.contains("B10", ignoreCase = true))
    }

    fun isPolo(v: Vehicle?): Boolean {
        if (v == null) return false
        val plate = v.licensePlate.replace("-", "").replace(" ", "").trim()
        return plate.equals("GHX78D", ignoreCase = true) ||
            (v.make.contains("Volkswagen", ignoreCase = true) && v.model.contains("Polo", ignoreCase = true)) ||
            v.model.contains("Polo", ignoreCase = true)
    }

    fun getSuccessorVehicle(
        vehicle: Vehicle?,
        allVehicles: List<Vehicle>
    ): Vehicle? {
        if (vehicle == null) return null
        val explicit = allVehicles.find { it.replacesVehicleId == vehicle.id }
        if (explicit != null) return explicit
        if (isPolo(vehicle)) {
            return allVehicles.find { isLeapmotor(it) }
        }
        return null
    }

    fun getPredecessorVehicles(
        vehicle: Vehicle?,
        allVehicles: List<Vehicle>
    ): List<Vehicle> {
        if (vehicle == null) return emptyList()
        val predecessors = mutableListOf<Vehicle>()
        val visited = mutableSetOf(vehicle.id)
        var currentId = vehicle.replacesVehicleId

        // Fallback for Leapmotor B10 replacing Volkswagen Polo if not yet set in database
        if (currentId == null && isLeapmotor(vehicle)) {
            val polo = allVehicles.find { isPolo(it) }
            if (polo != null) {
                currentId = polo.id
            }
        }

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId)
            val pred = allVehicles.find { it.id == currentId }
            if (pred != null) {
                predecessors.add(pred)
                currentId = pred.replacesVehicleId
            } else {
                break
            }
        }
        return predecessors
    }

    fun calculateCombinedProRataLimit(
        vehicle: Vehicle,
        allVehicles: List<Vehicle>,
        yearlyLimit: Int = if (vehicle.privateKmYearlyLimit > 0) vehicle.privateKmYearlyLimit else 500
    ): Int {
        val predecessors = getPredecessorVehicles(vehicle, allVehicles)
        if (predecessors.isEmpty()) {
            return calculateProRataLimit(yearlyLimit, vehicle.inServiceDate, vehicle.endServiceDate)
        }

        val chain = listOf(vehicle) + predecessors
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)

        val chainWithDates = chain.filter { it.inServiceDate != null }
        val earliestStartMonth = if (chainWithDates.isEmpty()) {
            1
        } else {
            chainWithDates.map { v ->
                val startCal = Calendar.getInstance().apply { timeInMillis = v.inServiceDate!! }
                if (startCal.get(Calendar.YEAR) < currentYear) 1
                else startCal.get(Calendar.MONTH) + 1
            }.minOrNull() ?: 1
        }

        val endMonth = if (vehicle.endServiceDate != null) {
            val endCal = Calendar.getInstance().apply { timeInMillis = vehicle.endServiceDate }
            if (endCal.get(Calendar.YEAR) > currentYear) 12
            else endCal.get(Calendar.MONTH) + 1
        } else 12

        val monthsAvailable = (endMonth - earliestStartMonth + 1).coerceIn(0, 12)
        return (monthsAvailable.toFloat() / 12f * yearlyLimit).toInt()
    }

    fun calculateVehiclePrivateKm(
        vehicle: Vehicle,
        allTrips: List<Trip>,
        startOfYear: Long
    ): Double {
        val inService = vehicle.inServiceDate ?: startOfYear
        val privateTrips = allTrips.filter {
            it.vehicleId == vehicle.id && it.startTime >= inService &&
                (it.tripType == "PRIVATE" || it.tripType == com.cimdriver.app.util.TripClassification.DYNAMICS_PERSONAL || it.privateDetourMeters > 0)
        }
        return privateTrips.sumOf {
            if (it.tripType == "PRIVATE" || it.tripType == com.cimdriver.app.util.TripClassification.DYNAMICS_PERSONAL) {
                it.distanceMeters + it.privateDetourMeters
            } else {
                it.privateDetourMeters
            }
        } / 1000.0
    }

    fun calculateCumulativePrivateKm(
        vehicle: Vehicle,
        allVehicles: List<Vehicle>,
        allTrips: List<Trip>,
        startOfYear: Long
    ): Double {
        val chain = listOf(vehicle) + getPredecessorVehicles(vehicle, allVehicles)
        return chain.sumOf { v ->
            calculateVehiclePrivateKm(v, allTrips, startOfYear)
        }
    }

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
        
        val activeVehicleId = if (selectedVehicleId == -1L) null else (selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id)
        val activeVehicle = vehicleList.find { it.id == activeVehicleId }
        val filteredTripList = if (activeVehicleId != null) tripList.filter { it.vehicleId == activeVehicleId } else tripList
        val inServiceDate = activeVehicle?.inServiceDate ?: 0L
        
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
                    
                    if (cal.get(Calendar.YEAR) == currentYear) {
                        if (trip.startTime >= inServiceDate) {
                            if (category == TripCategory.PRIVATE) {
                                ytdPriveMeters += trip.distanceMeters
                            } else if (trip.privateDetourMeters > 0) {
                                ytdPriveMeters += trip.privateDetourMeters
                            }
                        }
                    }

                    if (isMatch) {
                        if (category == TripCategory.BUSINESS || category == TripCategory.COMMUTE) {
                            val detour = trip.privateDetourMeters.coerceIn(0, trip.distanceMeters)
                            val netBusiness = trip.distanceMeters - detour
                            totalZakelijkMeters += netBusiness
                            totalPriveMeters += detour
                            totalZakelijkMs += durationMs
                            if (category == TripCategory.COMMUTE) {
                                totalWoonWerkMeters += netBusiness
                                totalWoonWerkMs += durationMs
                            }
                        } else if (category == TripCategory.PRIVATE) {
                            totalPriveMeters += trip.distanceMeters
                            totalPriveMs += durationMs
                        }
                    }
                    
                    if (isPrevMatch) {
                        if (category == TripCategory.BUSINESS || category == TripCategory.COMMUTE) {
                            val detour = trip.privateDetourMeters.coerceIn(0, trip.distanceMeters)
                            val netBusiness = trip.distanceMeters - detour
                            prevZakelijkMeters += netBusiness
                            prevPriveMeters += detour
                            if (category == TripCategory.COMMUTE) {
                                prevWoonWerkMeters += netBusiness
                            }
                        } else if (category == TripCategory.PRIVATE) {
                            prevPriveMeters += trip.distanceMeters
                        }
                    }
                }
            }
        }
        
        if (activeVehicle != null) {
            val predecessors = getPredecessorVehicles(activeVehicle, vehicleList)
            if (predecessors.isNotEmpty()) {
                val predIds = predecessors.map { it.id }.toSet()
                val predMap = predecessors.associateBy { it.id }
                tripList.filter { it.vehicleId in predIds }.forEach { trip ->
                    val status = TripStatus.from(trip.status)
                    if (status == TripStatus.DONE || status == TripStatus.MERGED || status == TripStatus.TO_REVIEW) {
                        val cal = Calendar.getInstance().apply { timeInMillis = trip.startTime }
                        if (cal.get(Calendar.YEAR) == currentYear) {
                            val pred = predMap[trip.vehicleId]
                            val predStart = pred?.inServiceDate ?: 0L
                            if (trip.startTime >= predStart) {
                                val category = classifyTrip(trip, addresses, rules, settings)
                                if (category == TripCategory.PRIVATE) {
                                    ytdPriveMeters += trip.distanceMeters
                                } else if (trip.privateDetourMeters > 0) {
                                    ytdPriveMeters += trip.privateDetourMeters
                                }
                            }
                        }
                    }
                }
            }
        }
        
        val ytdKm = ytdPriveMeters / 1000.0
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
            ytdPriveKm = ytdKm,
            projectedYearEndPriveKm = calculateFiscalYearProjection(ytdKm)
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
        
        val activeVehicleId = if (selectedVehicleId == -1L) null else (selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id)
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
        
        val activeVehicleId = if (selectedVehicleId == -1L) null else (selectedVehicleId ?: (vehicleList.find { it.isDefault } ?: vehicleList.firstOrNull())?.id)
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
                                val detourKm = (trip.privateDetourMeters.coerceIn(0, trip.distanceMeters)) / 1000.0
                                val netKm = km - detourKm
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) {
                                    zKm += netKm
                                    pKm += detourKm
                                }
                                if (category == TripCategory.COMMUTE) {
                                    wKm += netKm
                                    pKm += detourKm
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
                                val detourKm = (trip.privateDetourMeters.coerceIn(0, trip.distanceMeters)) / 1000.0
                                val netKm = km - detourKm
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) {
                                    zKm += netKm
                                    pKm += detourKm
                                }
                                if (category == TripCategory.COMMUTE) {
                                    wKm += netKm
                                    pKm += detourKm
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
                                val detourKm = (trip.privateDetourMeters.coerceIn(0, trip.distanceMeters)) / 1000.0
                                val netKm = km - detourKm
                                val category = classifyTrip(trip, addresses, rules, settings)
                                
                                if (category == TripCategory.BUSINESS) {
                                    zKm += netKm
                                    pKm += detourKm
                                }
                                if (category == TripCategory.COMMUTE) {
                                    wKm += netKm
                                    pKm += detourKm
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
