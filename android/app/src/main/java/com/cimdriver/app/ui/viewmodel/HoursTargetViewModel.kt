package com.cimdriver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.HoursTargetWithDetails
import com.cimdriver.app.data.local.entity.HoursTarget
import com.cimdriver.app.util.employmentTargetFactor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TargetProgress(
    val targetDetails: HoursTargetWithDetails,
    val accumulatedHours: Double,
    val accumulatedRevenue: Double,
    val accumulatedValue: Double, // General value for percentage calc
    val percentage: Float,
    val isBehind: Boolean,
    val diffValue: Double,
    val effectiveTargetValue: Double, // Pro-rata adjusted target
    val proRataFactor: Double // 1.0 = full year, 0.5 = half year, etc.
)

@HiltViewModel
class HoursTargetViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val hoursTargetDao = database.hoursTargetDao()
    private val workDayDao = database.workDayDao()
    
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear
    
    private val settingsDao = database.settingsDao()

    // Combining targets for the year with workdays for the year to compute progress
    val targetProgresses: StateFlow<List<TargetProgress>> = combine(
        _currentYear,
        database.clientDao().getAllClients(), // Just to trigger updates if clients change
        database.projectCodeDao().getActiveProjectCodes() // Just to trigger updates if projects change
    ) { year: Int, _, _ ->
        year
    }.flatMapLatest { year: Int ->
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        
        combine(
            hoursTargetDao.getActiveTargetsForYear(year),
            workDayDao.getWorkDaysInRange(startCal.timeInMillis, endCal.timeInMillis),
            database.projectCodeDao().getAllProjectCodes(),
            settingsDao.getSettings()
        ) { targets, workDays, allProjectCodes, settings ->
            // Filter approved workdays
            val approvedDays = workDays.filter { it.status == "APPROVED" }
            
            // Map project codes by ID for quick lookup
            val projectMap = allProjectCodes.associateBy { it.id }
            
            // Pro-rata: determine the effective start of the year for this user
            val totalDaysInYear = if (java.util.GregorianCalendar().isLeapYear(year)) 366.0 else 365.0
            // The employment start day within the year (or day 1 if not set / before this year)
            val employmentStartDayOfYear: Int = settings?.employmentStartDate?.let { empTs ->
                val empCal = Calendar.getInstance().apply { timeInMillis = empTs }
                if (empCal.get(Calendar.YEAR) == year) empCal.get(Calendar.DAY_OF_YEAR) else if (empCal.get(Calendar.YEAR) > year) totalDaysInYear.toInt() + 1 else 1
            } ?: 1

            // Effective working days in this year (from employment start to end of year)
            val effectiveDaysInYear = (totalDaysInYear - employmentStartDayOfYear + 1).coerceAtLeast(0.0)
            val proRataFactor = employmentTargetFactor(settings?.employmentStartDate, year)

            // Calculate elapsed ratio (linear, but only within the employment window)
            val now = Calendar.getInstance()
            val isCurrentYear = now.get(Calendar.YEAR) == year
            
            val elapsedRatio = if (isCurrentYear) {
                val dayOfYear = now.get(Calendar.DAY_OF_YEAR).coerceAtLeast(employmentStartDayOfYear)
                val daysElapsedSinceStart = (dayOfYear - employmentStartDayOfYear + 1).coerceAtLeast(0)
                if (effectiveDaysInYear > 0) daysElapsedSinceStart / effectiveDaysInYear else 0.0
            } else if (year < now.get(Calendar.YEAR)) {
                1.0
            } else {
                0.0
            }
            
            targets.map { td ->
                // Calculate valid hours for this target
                val validDays = approvedDays.filter { wd ->
                    val wdProjectCode = wd.projectCodeId?.let { projectMap[it] }
                    
                    val clientMatch = if (td.target.clientId == null) {
                        true // No client filter -> applies to all
                    } else {
                        wdProjectCode?.clientId == td.target.clientId
                    }
                    
                    val projectMatch = if (td.target.projectCodeId == null) {
                        true // No project filter -> applies to all matching the client filter
                    } else {
                        wd.projectCodeId == td.target.projectCodeId
                    }
                    
                    clientMatch && projectMatch
                }
                
                var totalMs = 0L
                var totalRevenue = 0.0
                validDays.forEach { wd ->
                    val start = wd.roundedArrivalTime ?: wd.arrivalTime
                    val end = wd.roundedDepartureTime ?: wd.departureTime ?: wd.lastArrivalTime ?: start
                    val workedMs = ((end - start) - (wd.breakMinutes * 60000L)).coerceAtLeast(0L)
                    totalMs += workedMs
                    val projectRate = wd.projectCodeId?.let { projectMap[it]?.hourlyRate?.takeIf { rate -> rate > 0.0 } }
                        ?: td.target.hourlyRate
                    totalRevenue += (workedMs / 3600000.0) * projectRate
                }
                
                val accHours = if (totalMs > 0) totalMs / 3600000.0 else 0.0
                val accRevenue = totalRevenue

                val isRevenue = td.target.targetType == "REVENUE"
                // Apply pro-rata to the full year target to get the effective target for this year
                val fullYearTargetValue = if (isRevenue) td.target.targetRevenue else td.target.targetHours
                val effectiveTargetValue = fullYearTargetValue * proRataFactor
                val accumulatedValue = if (isRevenue) accRevenue else accHours
                
                val expectedValue = effectiveTargetValue * elapsedRatio
                val diffValue = accumulatedValue - expectedValue
                val isBehind = diffValue < 0 && elapsedRatio > 0 && elapsedRatio < 1
                
                TargetProgress(
                    targetDetails = td,
                    accumulatedHours = accHours,
                    accumulatedRevenue = accRevenue,
                    accumulatedValue = accumulatedValue,
                    percentage = if (effectiveTargetValue > 0) (accumulatedValue / effectiveTargetValue).toFloat() else 0f,
                    isBehind = isBehind,
                    diffValue = diffValue,
                    effectiveTargetValue = effectiveTargetValue,
                    proRataFactor = proRataFactor
                )
            }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    fun addTarget(target: HoursTarget) {
        viewModelScope.launch {
            hoursTargetDao.insertTarget(target)
        }
    }

    fun updateTarget(target: HoursTarget) {
        viewModelScope.launch {
            hoursTargetDao.updateTarget(target)
        }
    }

    fun deleteTarget(target: HoursTarget) {
        viewModelScope.launch {
            hoursTargetDao.updateTarget(target.copy(isActive = false))
        }
    }
}
