package com.cimdriver.app.ui.viewmodel

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.BuildConfig
import com.cimdriver.app.data.local.dao.WorkDayDao
import com.cimdriver.app.data.local.entity.WorkDay
import com.cimdriver.app.service.TrackingRecoveryManager
import com.cimdriver.app.service.TrackingStatus
import com.cimdriver.app.service.TrackingStatusStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SystemDiagnosticsState(
    val appVersion: String = "",
    val isIgnoringBatteryOptimizations: Boolean = true,
    val activeWorkDay: WorkDay? = null,
    val activeTripIdInRecovery: Long? = null,
    val recoveryAttempts: Int = 0,
    val formattedWorkDayStart: String = ""
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workDayDao: WorkDayDao
) : ViewModel() {

    val trackingStatus: StateFlow<TrackingStatus> = TrackingStatusStore.status

    private val _systemState = MutableStateFlow(SystemDiagnosticsState())
    val systemState = _systemState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val recoveryManager = TrackingRecoveryManager(context)

    init {
        loadSystemDiagnostics()
    }

    fun loadSystemDiagnostics() {
        viewModelScope.launch {
            // App Version
            val version = BuildConfig.VERSION_NAME

            // Battery Optimization
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)

            // Active Work Day
            val activeWorkDay = workDayDao.getIncompleteWorkDaysSync().firstOrNull()
            val formattedStart = activeWorkDay?.let { timeFormat.format(Date(it.arrivalTime)) } ?: ""

            // Recovery Status
            val tripToRecover = recoveryManager.getTripToRecover()
            val tripId = tripToRecover?.id
            
            // To get attempts we can't easily read it directly from shared prefs without reproducing the key,
            // but we can at least show if there's a trip waiting.
            val prefs = context.getSharedPreferences("tracking_recovery_prefs", Context.MODE_PRIVATE)
            val attempts = tripId?.let { prefs.getInt("recovery_attempts_$it", 0) } ?: 0

            _systemState.value = SystemDiagnosticsState(
                appVersion = version,
                isIgnoringBatteryOptimizations = isIgnoringBattery,
                activeWorkDay = activeWorkDay,
                activeTripIdInRecovery = tripId,
                recoveryAttempts = attempts,
                formattedWorkDayStart = formattedStart
            )
        }
    }
}
