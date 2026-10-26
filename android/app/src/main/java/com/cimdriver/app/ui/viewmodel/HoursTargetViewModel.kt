package com.cimdriver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.HoursTargetWithDetails
import com.cimdriver.app.data.local.entity.HoursTarget
import com.cimdriver.app.util.calculateTargetProgress
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HoursTargetViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val hoursTargetDao = database.hoursTargetDao()
    private val workDayDao = database.workDayDao()
    
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear
    
    private val settingsDao = database.settingsDao()

    val targetProgresses: StateFlow<List<TargetProgress>> = _currentYear.flatMapLatest { year ->
        combine(
            hoursTargetDao.getActiveTargetsForYear(year),
            workDayDao.getAllWorkDays(),
            database.projectCodeDao().getAllProjectCodes(),
            settingsDao.getSettings()
        ) { targets, workDays, allProjectCodes, settings ->
            val now = Calendar.getInstance()
            targets.map { td ->
                calculateTargetProgress(
                    targetDetails = td,
                    workDays = workDays,
                    projectCodes = allProjectCodes,
                    settings = settings,
                    targetYear = year,
                    now = now
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
