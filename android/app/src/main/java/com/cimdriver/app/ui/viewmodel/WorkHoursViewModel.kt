package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.cimdriver.app.data.local.dao.WorkDayDao
import com.cimdriver.app.data.local.dao.SettingsDao
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.WorkDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.cimdriver.app.util.WorkHoursCalculator

data class WorkHoursChartItem(
    val label: String,
    val hours: Double
)

@HiltViewModel
class WorkHoursViewModel @Inject constructor(
    private val workDayDao: WorkDayDao,
    private val settingsDao: SettingsDao
) : ViewModel() {

    val workDays: StateFlow<List<WorkDay>> = workDayDao.getAllWorkDays()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val workDaysToReviewCount: StateFlow<Int> = workDayDao.getToReviewCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val settings = settingsDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun effectiveBreakMinutes(workDay: WorkDay): Int {
        val start = workDay.roundedArrivalTime ?: workDay.arrivalTime
        val end = workDay.roundedDepartureTime ?: workDay.departureTime ?: workDay.lastArrivalTime ?: start
        return WorkHoursCalculator.effectiveBreakMinutes(
            start,
            end,
            workDay.breakMinutes,
            settings.value?.workHoursToleranceMinutes ?: 30
        )
    }

    fun netDurationMillis(workDay: WorkDay): Long {
        val start = workDay.roundedArrivalTime ?: workDay.arrivalTime
        val end = workDay.roundedDepartureTime ?: workDay.departureTime ?: workDay.lastArrivalTime ?: start
        return end - start - effectiveBreakMinutes(workDay) * 60_000L
    }

    val currentMonthWorkHours = workDays.map { dayList ->
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        var totalMillis = 0L
        
        dayList.forEach { day ->
            if (day.status == "APPROVED" || day.status == "DONE") {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = day.date
                if (cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear) {
                    val arrival = day.roundedArrivalTime ?: day.arrivalTime
                    val departure = day.roundedDepartureTime ?: day.departureTime ?: day.lastArrivalTime ?: arrival
                    val netTime = WorkHoursCalculator.netDurationMillis(
                        arrival,
                        departure,
                        day.breakMinutes,
                        settings.value?.workHoursToleranceMinutes ?: 30
                    )
                    if (netTime > 0) {
                        totalMillis += netTime
                    }
                }
            }
        }
        
        totalMillis / 3600000.0 // Return as double hours
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private val _viewMode = kotlinx.coroutines.flow.MutableStateFlow("WEEK") // "WEEK", "MONTH"
    val viewMode: StateFlow<String> = _viewMode

    private val _selectedPeriod = kotlinx.coroutines.flow.MutableStateFlow("")
    val selectedPeriod: StateFlow<String> = _selectedPeriod

    init {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        _selectedPeriod.value = "$year-W${String.format(Locale.getDefault(), "%02d", week)}"
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        if (mode == "WEEK") {
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            _selectedPeriod.value = "$year-W${String.format(Locale.getDefault(), "%02d", week)}"
        } else {
            val month = cal.get(Calendar.MONTH) + 1
            _selectedPeriod.value = "$year-${String.format(Locale.getDefault(), "%02d", month)}"
        }
    }

    fun setSelectedPeriod(period: String) {
        _selectedPeriod.value = period
    }

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _dashboardTimeFilter = kotlinx.coroutines.flow.MutableStateFlow(com.cimdriver.app.ui.viewmodel.TimeFilter.MONTH)
    val dashboardTimeFilter: StateFlow<com.cimdriver.app.ui.viewmodel.TimeFilter> = _dashboardTimeFilter
    
    fun setDashboardTimeFilter(filter: com.cimdriver.app.ui.viewmodel.TimeFilter) {
        _dashboardTimeFilter.value = filter
    }

    val dashboardWorkHoursChartData = combine(workDays, _dashboardTimeFilter) { dayList, timeFilter ->
        val calNow = Calendar.getInstance()
        val currentWeek = calNow.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)
        
        val items = mutableListOf<WorkHoursChartItem>()
        
        when (timeFilter) {
            com.cimdriver.app.ui.viewmodel.TimeFilter.WEEK -> {
                val shortDays = listOf("Zo", "Ma", "Di", "Wo", "Do", "Vr", "Za")
                for (i in 1..7) {
                    val dayLabel = shortDays[i-1]
                    var h = 0.0
                    dayList.forEach { day ->
                        if (day.status == "APPROVED") {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = day.date
                            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.DAY_OF_WEEK) == i) {
                                val start = day.firstDepartureTime
                                val end = day.departureTime ?: day.lastArrivalTime ?: start
                                val netMs = WorkHoursCalculator.netDurationMillis(start, end, day.breakMinutes, settings.value?.workHoursToleranceMinutes ?: 30)
                                if (netMs > 0) h += (netMs / 3600000.0)
                            }
                        }
                    }
                    items.add(WorkHoursChartItem(dayLabel, h))
                }
                listOf(items[1], items[2], items[3], items[4], items[5], items[6], items[0])
            }
            com.cimdriver.app.ui.viewmodel.TimeFilter.MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                cal.set(Calendar.MONTH, currentMonth)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (i in 1..daysInMonth) {
                    var h = 0.0
                    dayList.forEach { day ->
                        if (day.status == "APPROVED") {
                            val dCal = Calendar.getInstance()
                            dCal.timeInMillis = day.date
                            if (dCal.get(Calendar.YEAR) == currentYear && dCal.get(Calendar.MONTH) == currentMonth && dCal.get(Calendar.DAY_OF_MONTH) == i) {
                                val start = day.roundedArrivalTime ?: day.arrivalTime
                                val end = day.roundedDepartureTime ?: day.departureTime ?: day.lastArrivalTime ?: start
                                val netMs = WorkHoursCalculator.netDurationMillis(start, end, day.breakMinutes, settings.value?.workHoursToleranceMinutes ?: 30)
                                if (netMs > 0) h += (netMs / 3600000.0)
                            }
                        }
                    }
                    items.add(WorkHoursChartItem("$i", h))
                }
                items
            }
            com.cimdriver.app.ui.viewmodel.TimeFilter.YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                val weeksInYear = cal.getActualMaximum(Calendar.WEEK_OF_YEAR)
                for (i in 1..weeksInYear) {
                    var h = 0.0
                    dayList.forEach { day ->
                        if (day.status == "APPROVED") {
                            val dCal = Calendar.getInstance()
                            dCal.timeInMillis = day.date
                            if (dCal.get(Calendar.YEAR) == currentYear && dCal.get(Calendar.WEEK_OF_YEAR) == i) {
                                val start = day.roundedArrivalTime ?: day.arrivalTime
                                val end = day.roundedDepartureTime ?: day.departureTime ?: day.lastArrivalTime ?: start
                                val netMs = WorkHoursCalculator.netDurationMillis(start, end, day.breakMinutes, settings.value?.workHoursToleranceMinutes ?: 30)
                                if (netMs > 0) h += (netMs / 3600000.0)
                            }
                        }
                    }
                    items.add(WorkHoursChartItem("W$i", h))
                }
                items
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availablePeriods: StateFlow<List<String>> = combine(
        workDays,
        _viewMode
    ) { days, mode ->
        val periods = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        
        // Always add current period
        val currentYear = cal.get(Calendar.YEAR)
        if (mode == "WEEK") {
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            periods.add("$currentYear-W${String.format(Locale.getDefault(), "%02d", week)}")
        } else {
            val month = cal.get(Calendar.MONTH) + 1
            periods.add("$currentYear-${String.format(Locale.getDefault(), "%02d", month)}")
        }

        days.forEach { day ->
            cal.timeInMillis = day.date
            val year = cal.get(Calendar.YEAR)
            if (mode == "WEEK") {
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                periods.add("$year-W${String.format(Locale.getDefault(), "%02d", week)}")
            } else {
                val month = cal.get(Calendar.MONTH) + 1
                periods.add("$year-${String.format(Locale.getDefault(), "%02d", month)}")
            }
        }
        
        periods.toList().sortedDescending()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groupedAndFilteredWorkDays: StateFlow<Map<String, List<WorkDay>>> = combine(
        workDays,
        _viewMode,
        _searchQuery,
        _selectedPeriod
    ) { days, mode, query, period ->
        // 1. Filter by search and selected period
        val filtered = days.filter { day ->
            val cal = Calendar.getInstance().apply { timeInMillis = day.date }
            val year = cal.get(Calendar.YEAR)
            val dayPeriod = if (mode == "WEEK") {
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                "$year-W${String.format(Locale.getDefault(), "%02d", week)}"
            } else {
                val month = cal.get(Calendar.MONTH) + 1
                "$year-${String.format(Locale.getDefault(), "%02d", month)}"
            }
            
            val periodMatches = dayPeriod == period

            val searchMatches = if (query.isBlank()) {
                true
            } else {
                val q = query.lowercase(Locale.getDefault())
                val searchFormat = SimpleDateFormat("EEEE d MMMM yyyy", java.util.Locale.forLanguageTag("nl-NL"))
                val dateStr = searchFormat.format(Date(day.date)).lowercase(Locale.getDefault())
                val locStr = day.workLocationLabel?.lowercase(Locale.getDefault()) ?: ""
                val statusStr = day.status.lowercase(Locale.getDefault())
                dateStr.contains(q) || locStr.contains(q) || statusStr.contains(q)
            }
            
            periodMatches && searchMatches
        }

        // 2. Sort descending (newest first)
        val sorted = filtered.sortedByDescending { it.date }

        // 3. Group
        val monthFormat = SimpleDateFormat("MMMM yyyy", java.util.Locale.forLanguageTag("nl-NL"))
        
        sorted.groupBy { day ->
            if (mode == "WEEK") {
                val cal = Calendar.getInstance().apply { timeInMillis = day.date }
                "Week ${cal.get(Calendar.WEEK_OF_YEAR)}, ${cal.get(Calendar.YEAR)}"
            } else {
                monthFormat.format(Date(day.date)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun updateWorkDay(
        id: Long,
        date: Long,
        firstDepartureTime: Long,
        arrivalTime: Long,
        departureTime: Long?,
        lastArrivalTime: Long?,
        roundedArrivalTime: Long?,
        roundedDepartureTime: Long?,
        breakMinutes: Int,
        workLocationLabel: String?,
        projectCode: String?,
        status: String
    ) {
        viewModelScope.launch {
            val effectiveStart = roundedArrivalTime ?: arrivalTime
            val effectiveEnd = roundedDepartureTime ?: departureTime ?: lastArrivalTime ?: effectiveStart
            val effectiveBreak = WorkHoursCalculator.effectiveBreakMinutes(
                effectiveStart,
                effectiveEnd,
                breakMinutes,
                settingsDao.getSettingsSync()?.workHoursToleranceMinutes ?: 30
            )
            workDayDao.updateWorkDay(
                WorkDay(
                    id = id,
                    date = date,
                    firstDepartureTime = firstDepartureTime,
                    arrivalTime = arrivalTime,
                    departureTime = departureTime,
                    lastArrivalTime = lastArrivalTime,
                    roundedArrivalTime = roundedArrivalTime,
                    roundedDepartureTime = roundedDepartureTime,
                    breakMinutes = effectiveBreak,
                    workLocationLabel = workLocationLabel,
                    projectCode = projectCode,
                    status = status
                )
            )
        }
    }

    fun addManualWorkDay(
        date: Long,
        firstDepartureTime: Long,
        arrivalTime: Long,
        departureTime: Long,
        lastArrivalTime: Long,
        breakMinutes: Int,
        workLocationLabel: String?,
        projectCode: String?
    ) {
        viewModelScope.launch {
            val effectiveBreak = WorkHoursCalculator.effectiveBreakMinutes(
                arrivalTime,
                departureTime,
                breakMinutes,
                settingsDao.getSettingsSync()?.workHoursToleranceMinutes ?: 30
            )
            workDayDao.insertWorkDay(
                WorkDay(
                    date = date,
                    firstDepartureTime = firstDepartureTime,
                    arrivalTime = arrivalTime,
                    departureTime = departureTime,
                    lastArrivalTime = lastArrivalTime,
                    roundedArrivalTime = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(arrivalTime),
                    roundedDepartureTime = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(departureTime),
                    breakMinutes = effectiveBreak,
                    workLocationLabel = workLocationLabel,
                    projectCode = projectCode,
                    status = "APPROVED"
                )
            )
        }
    }

    fun deleteWorkDay(workDay: WorkDay) {
        viewModelScope.launch {
            workDayDao.deleteWorkDay(workDay)
        }
    }
}
