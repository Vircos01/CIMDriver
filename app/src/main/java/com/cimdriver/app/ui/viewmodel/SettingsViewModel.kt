package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.cimdriver.app.data.local.dao.SettingsDao
import com.cimdriver.app.data.local.dao.ClassificationRuleDao
import com.cimdriver.app.data.local.dao.LocationPointDao
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Settings
import com.cimdriver.app.data.local.entity.ClassificationRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val classificationRuleDao: ClassificationRuleDao,
    private val locationPointDao: LocationPointDao,
    private val database: AppDatabase
) : ViewModel() {

    init {
        viewModelScope.launch { cleanupLocationHistory() }
    }

    val settings: StateFlow<Settings?> = settingsDao.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val classificationRules: StateFlow<List<ClassificationRule>> = classificationRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (currentSettings != null) {
                settingsDao.insertSettings(currentSettings.copy(themeMode = mode))
            } else {
                settingsDao.insertSettings(Settings(themeMode = mode))
            }
        }
    }

    fun updateOdometerReminder(enabled: Boolean, intervalDays: Int) {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettingsSync() ?: Settings()
            settingsDao.insertSettings(currentSettings.copy(odometerReminder = enabled, odometerReminderIntervalDays = intervalDays))
        }
    }

    fun updateGracePeriod(seconds: Int) {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettingsSync() ?: Settings()
            settingsDao.insertSettings(currentSettings.copy(gracePeriodSec = seconds))
        }
    }

    fun updateWorkingHours(startTime: String, endTime: String, days: String, breakMinutes: Int, toleranceMinutes: Int = 30) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (currentSettings != null) {
                settingsDao.insertSettings(currentSettings.copy(workStartTime = startTime, workEndTime = endTime, workDays = days, defaultBreakMinutes = breakMinutes, workHoursToleranceMinutes = toleranceMinutes.coerceAtLeast(0)))
            } else {
                settingsDao.insertSettings(Settings(workStartTime = startTime, workEndTime = endTime, workDays = days, defaultBreakMinutes = breakMinutes, workHoursToleranceMinutes = toleranceMinutes.coerceAtLeast(0)))
            }
        }
    }

    fun updateClassificationSettings(defaultCategory: String, homeWorkAsCommute: Boolean, customerAsBusiness: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: Settings()
            settingsDao.insertSettings(
                currentSettings.copy(
                    classificationDefault = defaultCategory,
                    classifyHomeWorkAsCommute = homeWorkAsCommute,
                    classifyCustomerAsBusiness = customerAsBusiness
                )
            )
        }
    }

    fun saveClassificationRule(rule: ClassificationRule) {
        viewModelScope.launch {
            if (rule.id == 0L) classificationRuleDao.insertRule(rule) else classificationRuleDao.updateRule(rule)
        }
    }

    fun deleteClassificationRule(rule: ClassificationRule) {
        viewModelScope.launch { classificationRuleDao.deleteRule(rule) }
    }

    fun updateLocationRetentionDays(days: Int) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: Settings()
            settingsDao.insertSettings(currentSettings.copy(locationRetentionDays = days.coerceAtLeast(0)))
            cleanupLocationHistory(days.coerceAtLeast(0))
        }
    }
    
    fun updateBusinessCompensation(amount: Float) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: Settings()
            settingsDao.insertSettings(currentSettings.copy(businessCompensation = amount.coerceAtLeast(0f)))
        }
    }
    
    fun clearLocationHistory() {
        viewModelScope.launch { locationPointDao.deleteAllPoints() }
    }

    fun resetAllData() {
        viewModelScope.launch {
            database.clearAllTables()
            settingsDao.insertSettings(Settings())
            listOf(
                ClassificationRule(name = "Thuis <-> Werk", startAddressType = "THUIS", endAddressType = "WERK", tripType = "Home To Work", category = "COMMUTE"),
                ClassificationRule(name = "Woon-werk rit", tripType = "COMMUTE", category = "COMMUTE"),
                ClassificationRule(name = "Klantbezoek", tripType = "Customer Visit", category = "BUSINESS"),
                ClassificationRule(name = "Zakelijke afspraak", tripType = "Business Meeting", category = "BUSINESS"),
                ClassificationRule(name = "Klant factureerbaar", tripType = "Customer Billable", category = "BUSINESS"),
                ClassificationRule(name = "Opdracht CIMSOLUTIONS", tripType = "Commissioned By CIMSOLUTIONS", category = "BUSINESS"),
                ClassificationRule(name = "Opleiding", tripType = "Exam Course", category = "BUSINESS"),
                ClassificationRule(name = "Auto onderhoud", tripType = "Car Maintenance", category = "BUSINESS"),
                ClassificationRule(name = "Privérit", tripType = "PERSONAL", category = "PRIVATE"),
                ClassificationRule(name = "Privé rit", tripType = "PRIVATE", category = "PRIVATE")
            ).forEach { classificationRuleDao.insertRule(it) }
        }
    }

    private suspend fun cleanupLocationHistory(days: Int? = null) {
        val retentionDays = days ?: settingsDao.getSettingsSync()?.locationRetentionDays ?: 365
        if (retentionDays > 0) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            locationPointDao.deletePointsOlderThan(cutoff)
        }
    }
}
