package com.cimdriver.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Client
import com.cimdriver.app.data.local.entity.ProjectCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

@HiltViewModel
class ClientProjectViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val clientDao = database.clientDao()
    private val projectCodeDao = database.projectCodeDao()

    private val _showArchived = MutableStateFlow(false)
    val showArchived = _showArchived.asStateFlow()

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    val clients: StateFlow<List<Client>> = clientDao.getActiveClients()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val projectCodes: StateFlow<List<ProjectCode>> = combine(
        projectCodeDao.getAllProjectCodes(),
        _showArchived
    ) { codes, showArchived ->
        if (showArchived) codes else codes.filter { it.isActive }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addClient(name: String, color: String = "#1976D2") {
        viewModelScope.launch {
            clientDao.insertClient(
                Client(name = name, color = color)
            )
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            clientDao.updateClient(client)
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            // Soft delete
            clientDao.updateClient(client.copy(isActive = false))
        }
    }

    fun addProjectCode(code: String, description: String?, clientId: Long?, isBillable: Boolean) {
        viewModelScope.launch {
            projectCodeDao.insertProjectCode(
                ProjectCode(
                    code = code,
                    description = description,
                    clientId = clientId,
                    isBillable = isBillable
                )
            )
        }
    }

    fun updateProjectCode(projectCode: ProjectCode) {
        viewModelScope.launch {
            projectCodeDao.updateProjectCode(projectCode)
        }
    }

    fun deleteProjectCode(projectCode: ProjectCode, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val hasTrip = database.tripDao().getTripForProjectSync(projectCode.id) != null
            val hasTarget = database.hoursTargetDao().getHoursTargetForProjectSync(projectCode.id) != null
            
            if (hasTrip || hasTarget) {
                // In use -> we can only archive it (soft delete)
                projectCodeDao.updateProjectCode(projectCode.copy(isActive = false))
                onResult(true, "Projectcode in gebruik, is in plaats daarvan gearchiveerd.")
            } else {
                // Not in use -> hard delete
                projectCodeDao.deleteProjectCode(projectCode)
                onResult(true, "Projectcode verwijderd.")
            }
        }
    }
    
    fun restoreProjectCode(projectCode: ProjectCode) {
        viewModelScope.launch {
            projectCodeDao.updateProjectCode(projectCode.copy(isActive = true))
        }
    }
    
    init {
        viewModelScope.launch {
            archiveUnusedProjectCodes()
        }
    }
    
    private suspend fun archiveUnusedProjectCodes() {
        val settings = database.settingsDao().getSettingsSync()
        val archiveDays = settings?.autoArchiveProjectDays ?: 0
        if (archiveDays <= 0) return
        
        val thresholdTime = System.currentTimeMillis() - (archiveDays * 24L * 60 * 60 * 1000)
        val allActive = database.projectCodeDao().getAllProjectCodesSync().filter { it.isActive }
        
        for (pc in allActive) {
            val lastTripTime = database.tripDao().getLastTripTimeForProjectSync(pc.id) ?: 0L
            val compareTime = maxOf(lastTripTime, pc.createdAt)
            if (compareTime > 0 && compareTime < thresholdTime) {
                // Older than threshold, archive it
                database.projectCodeDao().updateProjectCode(pc.copy(isActive = false))
            }
        }
    }
}
