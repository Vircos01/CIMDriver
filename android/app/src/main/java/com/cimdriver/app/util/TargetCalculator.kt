package com.cimdriver.app.util

import com.cimdriver.app.data.local.dao.HoursTargetWithDetails
import com.cimdriver.app.data.local.entity.ProjectCode
import com.cimdriver.app.data.local.entity.Settings
import com.cimdriver.app.data.local.entity.WorkDay
import com.cimdriver.app.ui.viewmodel.TargetProgress
import java.util.Calendar
import java.util.TimeZone

fun employmentTargetFactor(employmentStartDate: Long?, targetYear: Int): Double {
    if (employmentStartDate == null) return 1.0

    val employmentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = employmentStartDate
    }
    val employmentYear = employmentDate.get(Calendar.YEAR)

    return when {
        employmentYear < targetYear -> 1.0
        employmentYear > targetYear -> 0.0
        else -> (12 - employmentDate.get(Calendar.MONTH)) / 12.0
    }
}

fun calculateTargetProgress(
    targetDetails: HoursTargetWithDetails,
    workDays: List<WorkDay>,
    projectCodes: List<ProjectCode>,
    settings: Settings?,
    targetYear: Int,
    now: Calendar = Calendar.getInstance()
): TargetProgress {
    val totalDaysInYear = if (java.util.GregorianCalendar().isLeapYear(targetYear)) 366.0 else 365.0
    val employmentStartDayOfYear: Int = settings?.employmentStartDate?.let { empTs ->
        val empCal = Calendar.getInstance().apply { timeInMillis = empTs }
        if (empCal.get(Calendar.YEAR) == targetYear) empCal.get(Calendar.DAY_OF_YEAR)
        else if (empCal.get(Calendar.YEAR) > targetYear) totalDaysInYear.toInt() + 1
        else 1
    } ?: 1

    val effectiveDaysInYear = (totalDaysInYear - employmentStartDayOfYear + 1).coerceAtLeast(0.0)
    val proRataFactor = employmentTargetFactor(settings?.employmentStartDate, targetYear)

    val isCurrentYear = now.get(Calendar.YEAR) == targetYear
    val elapsedRatio = if (isCurrentYear) {
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR).coerceAtLeast(employmentStartDayOfYear)
        val daysElapsedSinceStart = (dayOfYear - employmentStartDayOfYear + 1).coerceAtLeast(0)
        if (effectiveDaysInYear > 0) daysElapsedSinceStart / effectiveDaysInYear else 0.0
    } else if (targetYear < now.get(Calendar.YEAR)) {
        1.0
    } else {
        0.0
    }

    val projectById = projectCodes.associateBy { it.id }
    val projectByCode = projectCodes.filter { it.code.isNotBlank() }.associateBy { it.code.trim().lowercase() }

    val cal = Calendar.getInstance()
    val approvedDays = workDays.filter { wd ->
        (wd.status == "APPROVED" || wd.status == "DONE") && run {
            cal.timeInMillis = wd.date
            val matchDate = cal.get(Calendar.YEAR) == targetYear
            val matchArrival = if (!matchDate && wd.arrivalTime > 0) {
                cal.timeInMillis = wd.arrivalTime
                cal.get(Calendar.YEAR) == targetYear
            } else false
            matchDate || matchArrival
        }
    }

    val validDays = approvedDays.filter { wd ->
        val wdProject = wd.projectCodeId?.let { projectById[it] }
            ?: wd.projectCode?.trim()?.lowercase()?.let { projectByCode[it] }
        val effectiveProjectId = wd.projectCodeId ?: wdProject?.id

        val clientMatch = if (targetDetails.target.clientId == null) {
            true
        } else {
            wdProject?.clientId == targetDetails.target.clientId
        }

        val projectMatch = if (targetDetails.target.projectCodeId == null) {
            true
        } else {
            effectiveProjectId == targetDetails.target.projectCodeId
        }

        clientMatch && projectMatch
    }

    var totalMs = 0L
    var totalRevenue = 0.0
    val toleranceMinutes = settings?.workHoursToleranceMinutes ?: 30

    validDays.forEach { wd ->
        val start = wd.roundedArrivalTime ?: wd.arrivalTime
        val end = wd.roundedDepartureTime ?: wd.departureTime ?: wd.lastArrivalTime ?: start
        val workedMs = WorkHoursCalculator.netDurationMillis(start, end, wd.breakMinutes, toleranceMinutes)
        totalMs += workedMs

        val wdProject = wd.projectCodeId?.let { projectById[it] }
            ?: wd.projectCode?.trim()?.lowercase()?.let { projectByCode[it] }
        val projectRate = wdProject?.hourlyRate?.takeIf { rate -> rate > 0.0 }
            ?: targetDetails.target.hourlyRate
        totalRevenue += (workedMs / 3600000.0) * projectRate
    }

    val accHours = if (totalMs > 0) totalMs / 3600000.0 else 0.0
    val accRevenue = totalRevenue

    val isRevenue = targetDetails.target.targetType == "REVENUE"
    val fullYearTargetValue = if (isRevenue) targetDetails.target.targetRevenue else targetDetails.target.targetHours
    val effectiveTargetValue = fullYearTargetValue * proRataFactor
    val accumulatedValue = if (isRevenue) accRevenue else accHours

    val expectedValue = effectiveTargetValue * elapsedRatio
    val diffValue = accumulatedValue - expectedValue
    val isBehind = diffValue < 0 && elapsedRatio > 0 && elapsedRatio < 1

    val currentYear = now.get(Calendar.YEAR)
    var remainingWorkDays = 0
    if (targetYear == currentYear) {
        val workDaysConfig = WorkHoursUtil.parseWorkHoursString(
            settings?.workDays ?: "1,2,3,4,5",
            settings?.workStartTime ?: "08:00",
            settings?.workEndTime ?: "18:00"
        )
        val activeDaysSet = workDaysConfig.filter { it.isEnabled }.map { it.dayOfWeek }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5) }

        val tempCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        while (tempCal.get(Calendar.YEAR) == targetYear) {
            val rawDay = tempCal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (rawDay == Calendar.SUNDAY) 7 else rawDay - 1
            if (dayOfWeek in activeDaysSet) {
                remainingWorkDays++
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else if (targetYear > currentYear) {
        val workDaysConfig = WorkHoursUtil.parseWorkHoursString(
            settings?.workDays ?: "1,2,3,4,5",
            settings?.workStartTime ?: "08:00",
            settings?.workEndTime ?: "18:00"
        )
        val activeDaysSet = workDaysConfig.filter { it.isEnabled }.map { it.dayOfWeek }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5) }
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.DAY_OF_YEAR, 1)
        }
        while (tempCal.get(Calendar.YEAR) == targetYear) {
            val rawDay = tempCal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (rawDay == Calendar.SUNDAY) 7 else rawDay - 1
            if (dayOfWeek in activeDaysSet) {
                remainingWorkDays++
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val remainingValue = (effectiveTargetValue - accumulatedValue).coerceAtLeast(0.0)
    val requiredPacePerDay = if (remainingWorkDays > 0) remainingValue / remainingWorkDays else 0.0

    val guidanceMessage = when {
        effectiveTargetValue <= 0.0 -> ""
        accumulatedValue >= effectiveTargetValue -> {
            val surplus = accumulatedValue - effectiveTargetValue
            if (isRevenue) {
                String.format(java.util.Locale.getDefault(), "Doel behaald! (+€ %,.0f)", surplus)
            } else {
                String.format(java.util.Locale.getDefault(), "Doel behaald! (+%.1f uur)", surplus)
            }
        }
        targetYear < currentYear -> {
            if (isRevenue) {
                String.format(java.util.Locale.getDefault(), "Jaar afgesloten: € %,.0f tekort", remainingValue)
            } else {
                String.format(java.util.Locale.getDefault(), "Jaar afgesloten: %.1f uur tekort", remainingValue)
            }
        }
        remainingWorkDays > 0 -> {
            if (isRevenue) {
                String.format(java.util.Locale.getDefault(), "Nog € %,.0f nodig over %d werkdagen (€ %,.0f/dag)", remainingValue, remainingWorkDays, requiredPacePerDay)
            } else {
                String.format(java.util.Locale.getDefault(), "Nog %.1f uur nodig over %d werkdagen (tempo: %.1f u/dag)", remainingValue, remainingWorkDays, requiredPacePerDay)
            }
        }
        else -> {
            if (isRevenue) {
                String.format(java.util.Locale.getDefault(), "Nog € %,.0f nodig", remainingValue)
            } else {
                String.format(java.util.Locale.getDefault(), "Nog %.1f uur nodig", remainingValue)
            }
        }
    }

    return TargetProgress(
        targetDetails = targetDetails,
        accumulatedHours = accHours,
        accumulatedRevenue = accRevenue,
        accumulatedValue = accumulatedValue,
        percentage = if (effectiveTargetValue > 0) (accumulatedValue / effectiveTargetValue).toFloat() else 0f,
        isBehind = isBehind,
        diffValue = diffValue,
        effectiveTargetValue = effectiveTargetValue,
        proRataFactor = proRataFactor,
        remainingWorkDays = remainingWorkDays,
        requiredPacePerDay = requiredPacePerDay,
        guidanceMessage = guidanceMessage
    )
}