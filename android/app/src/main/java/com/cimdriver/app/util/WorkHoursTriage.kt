package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.data.local.entity.Settings
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.WorkDay
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WorkDaySuggestion(
    val suggestedDepartureTime: Long?,
    val suggestedBreakMinutes: Int,
    val suggestedProjectCode: String?,
    val confidenceScore: Int, // 0 - 100
    val statusText: String,
    val isHighConfidence: Boolean,
    val requiresAttention: Boolean
)

data class WorkDayReviewCandidate(
    val workDay: WorkDay,
    val suggestion: WorkDaySuggestion
)

object WorkHoursTriage {

    fun evaluateWorkDay(
        workDay: WorkDay,
        tripsOnDay: List<Trip> = emptyList(),
        settings: Settings? = null,
        savedAddresses: List<SavedAddress> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): WorkDaySuggestion {
        val calWork = Calendar.getInstance().apply { timeInMillis = workDay.date }
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val isToday = calWork.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calWork.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        // Roster / Basisrooster resolution
        val rawDay = calWork.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = if (rawDay == Calendar.SUNDAY) 7 else rawDay - 1
        val roster = WorkHoursUtil.parseWorkHoursString(
            input = settings?.workDays ?: "1,2,3,4,5",
            defaultStart = settings?.workStartTime ?: "08:00",
            defaultEnd = settings?.workEndTime ?: "17:00"
        )
        val todaySchedule = roster.find { it.dayOfWeek == dayOfWeek }
        val isScheduledWorkDay = todaySchedule?.isEnabled ?: (dayOfWeek in 1..5)
        val scheduledEndTime = todaySchedule?.endTime ?: settings?.workEndTime ?: "17:00"
        val toleranceMins = (settings?.workHoursToleranceMinutes ?: 30).toLong()

        // 1. Departure time analysis & suggestion
        val actualDeparture = workDay.departureTime ?: workDay.lastArrivalTime
        val suggestedDeparture: Long
        val departureMissing: Boolean

        if (actualDeparture != null) {
            suggestedDeparture = actualDeparture
            departureMissing = false
        } else {
            departureMissing = true
            // Look for a trip starting from work or returning home on that day
            val returnTrip = tripsOnDay.lastOrNull { trip ->
                val isCommute = trip.tripType == "COMMUTE" || trip.tripType == "Home To Work"
                val endsHome = trip.endAddress?.contains("thuis", ignoreCase = true) == true ||
                        savedAddresses.any { it.isHomeLocation && it.address.equals(trip.endAddress, ignoreCase = true) }
                isCommute || endsHome
            }

            suggestedDeparture = if (returnTrip != null && returnTrip.startTime > workDay.arrivalTime) {
                returnTrip.startTime
            } else {
                WorkHoursNormalizer.normalize(
                    actualTime = workDay.arrivalTime + 8 * 3600_000L,
                    dayStart = workDay.date,
                    configuredTime = scheduledEndTime,
                    toleranceMinutes = toleranceMins
                )
            }
        }

        // 2. Break time calculation & suggestion
        val start = workDay.roundedArrivalTime ?: workDay.arrivalTime
        val end = workDay.roundedDepartureTime ?: suggestedDeparture
        val grossDurationMs = (end - start).coerceAtLeast(0L)
        val grossHours = grossDurationMs / 3600_000.0

        val standardBreak = when {
            grossHours >= 8.0 -> maxOf(45, settings?.defaultBreakMinutes ?: 30)
            grossHours >= 4.5 -> settings?.defaultBreakMinutes ?: 30
            else -> 0
        }
        val suggestedBreak = if (workDay.breakMinutes > 0) workDay.breakMinutes else standardBreak
        val zeroBreakOnLongDay = grossHours >= 7.5 && workDay.breakMinutes == 0

        // 3. Project code suggestion
        val existingProject = workDay.projectCode?.takeIf { it.isNotBlank() }
        val tripWithProject = tripsOnDay.firstOrNull { !it.projectCode.isNullOrBlank() }
        val customerTrip = tripsOnDay.firstOrNull { trip ->
            savedAddresses.any { it.isCustomerLocation && (it.address.equals(trip.startAddress, ignoreCase = true) || it.address.equals(trip.endAddress, ignoreCase = true)) }
        }
        val customerSavedAddress = customerTrip?.let { trip ->
            savedAddresses.firstOrNull { it.isCustomerLocation && (it.address.equals(trip.startAddress, ignoreCase = true) || it.address.equals(trip.endAddress, ignoreCase = true)) }
        }

        val suggestedProject = existingProject ?: tripWithProject?.projectCode ?: customerSavedAddress?.projectCode

        // 4. Weekend / Off-day check
        val workDaysStr = settings?.workDays ?: "1,2,3,4,5"
        val isOffDay = TripClassification.isOutsideWorkHours(workDay.date, workDaysStr, "00:00", "23:59")

        // 5. Scoring & Plain Language Status
        var score = 95
        var statusText: String
        var requiresAttention = false

        if (departureMissing && isToday && nowMillis < workDay.arrivalTime + 9 * 3600_000L) {
            score = 60
            statusText = "Werkdag momenteel actief (gestart ${formatTime(workDay.arrivalTime)})"
            requiresAttention = false
        } else if (departureMissing) {
            score = 55
            statusText = "Eindtijd ontbreekt (voorstel: ${formatTime(suggestedDeparture)})"
            requiresAttention = true
        } else if (zeroBreakOnLongDay) {
            score = 65
            statusText = "Lange werkdag (${String.format(Locale.getDefault(), "%.1f", grossHours)}u) zonder pauze"
            requiresAttention = true
        } else if (grossHours < 4.0 && !departureMissing) {
            score = 75
            statusText = "Korte werkdag (${String.format(Locale.getDefault(), "%.1f", grossHours)} uur)"
            requiresAttention = true
        } else if (isOffDay) {
            score = 70
            statusText = "Geregistreerd buiten reguliere werkdagen"
            requiresAttention = true
        } else {
            val netHours = String.format(Locale.getDefault(), "%.1f", (grossDurationMs - suggestedBreak * 60_000L).coerceAtLeast(0L) / 3600_000.0)
            statusText = "Vrijwel zeker compleet ($netHours uur netto, ${suggestedBreak}m pauze)"
            requiresAttention = false
        }

        return WorkDaySuggestion(
            suggestedDepartureTime = suggestedDeparture,
            suggestedBreakMinutes = suggestedBreak,
            suggestedProjectCode = suggestedProject,
            confidenceScore = score,
            statusText = statusText,
            isHighConfidence = score >= 85,
            requiresAttention = requiresAttention || workDay.status == "TO_REVIEW"
        )
    }

    private fun formatTime(timeMillis: Long): String {
        val format = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(timeMillis))
    }
}
