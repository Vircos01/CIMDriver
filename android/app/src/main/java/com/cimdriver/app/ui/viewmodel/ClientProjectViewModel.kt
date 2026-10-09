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

@HiltViewModel
class ClientProjectViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val clientDao = database.clientDao()
    private val projectCodeDao = database.projectCodeDao()

    val clients: StateFlow<List<Client>> = clientDao.getActiveClients()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val projectCodes: StateFlow<List<ProjectCode>> = projectCodeDao.getActiveProjectCodes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun deleteProjectCode(projectCode: ProjectCode) {
        viewModelScope.launch {
            // Soft delete
            projectCodeDao.updateProjectCode(projectCode.copy(isActive = false))
        }
    }
}
