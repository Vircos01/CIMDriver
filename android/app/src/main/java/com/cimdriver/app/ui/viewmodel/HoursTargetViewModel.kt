package com.cimdriver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.HoursTargetWithDetails
import com.cimdriver.app.data.local.entity.HoursTarget
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
    val percentage: Float,
    val isBehind: Boolean,
    val diffHours: Double
)

@HiltViewModel
class HoursTargetViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val hoursTargetDao = database.hoursTargetDao()
    private val workDayDao = database.workDayDao()
    
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear
    
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
            database.projectCodeDao().getActiveProjectCodes()
        ) { targets, workDays, allProjectCodes ->
            // Filter approved workdays
            val approvedDays = workDays.filter { it.status == "APPROVED" }
            
            // Map project codes by ID for quick lookup
            val projectMap = allProjectCodes.associateBy { it.id }
            
            // Calculate elapsed ratio (linear)
            val now = Calendar.getInstance()
            val isCurrentYear = now.get(Calendar.YEAR) == year
            
            val elapsedRatio = if (isCurrentYear) {
                val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
                val totalDays = if (now.getActualMaximum(Calendar.DAY_OF_YEAR) > 365) 366.0 else 365.0
                dayOfYear / totalDays
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
                
                // Let's get project codes to accurately match clientId
                // Not ideal to fetch synchronously here, but we will fix this via combine
                
                var totalMs = 0L
                validDays.forEach { wd ->
                    val start = wd.roundedArrivalTime ?: wd.arrivalTime
                    val end = wd.roundedDepartureTime ?: wd.departureTime ?: wd.lastArrivalTime ?: start
                    totalMs += (end - start) - (wd.breakMinutes * 60000L)
                }
                
                val accHours = if (totalMs > 0) totalMs / 3600000.0 else 0.0
                val targetHours = td.target.targetHours
                
                val expectedHours = targetHours * elapsedRatio
                val diffHours = accHours - expectedHours
                val isBehind = diffHours < 0 && elapsedRatio > 0 && elapsedRatio < 1
                
                TargetProgress(
                    targetDetails = td,
                    accumulatedHours = accHours,
                    percentage = if (targetHours > 0) (accHours / targetHours).toFloat() else 0f,
                    isBehind = isBehind,
                    diffHours = diffHours
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
