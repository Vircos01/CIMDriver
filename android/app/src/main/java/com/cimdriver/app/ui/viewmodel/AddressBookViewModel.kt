package com.cimdriver.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.cimdriver.app.data.local.dao.SavedAddressDao
import com.cimdriver.app.data.local.dao.TripDao
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.SavedAddress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.cimdriver.app.service.GeocoderService

import com.cimdriver.app.data.local.dao.ClientDao
import com.cimdriver.app.data.local.entity.Client
import com.cimdriver.app.data.local.dao.ProjectCodeDao
import com.cimdriver.app.data.local.entity.ProjectCode

@HiltViewModel
class AddressBookViewModel @Inject constructor(
    private val savedAddressDao: SavedAddressDao,
    private val tripDao: TripDao,
    private val clientDao: ClientDao,
    private val projectCodeDao: ProjectCodeDao,
    private val geocoderService: GeocoderService
) : ViewModel() {

    val addresses: StateFlow<List<SavedAddress>> = savedAddressDao.getAllAddresses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeProjectCodes: StateFlow<List<ProjectCode>> = projectCodeDao.getActiveProjectCodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProjectCode(code: String, description: String, isBillable: Boolean, clientName: String?) {
        viewModelScope.launch {
            var cId: Long? = null
            if (!clientName.isNullOrBlank()) {
                val existingClients = clientDao.getAllClientsSync()
                val existing = existingClients.find { it.name.equals(clientName, ignoreCase = true) }
                if (existing != null) {
                    cId = existing.id
                } else {
                    cId = clientDao.insertClient(Client(name = clientName, color = "#4285F4"))
                }
            }
            projectCodeDao.insertProjectCode(
                ProjectCode(
                    code = code,
                    description = description,
                    isBillable = isBillable,
                    clientId = cId
                )
            )
        }
    }

    private suspend fun ensureClientExists(label: String, isCustomer: Boolean) {
        if (isCustomer && label.isNotBlank()) {
            val existingClients = clientDao.getAllClientsSync()
            if (existingClients.none { it.name.equals(label, ignoreCase = true) }) {
                clientDao.insertClient(Client(name = label, color = "#4285F4")) // Default color
            }
        }
    }

    fun addAddress(label: String, address: String, isWorkLocation: Boolean, isHomeLocation: Boolean, isCustomerLocation: Boolean, defaultTripType: String?, projectCode: String?, notes: String?, addressType: String? = null) {
        viewModelScope.launch {
            val effectiveAddressType = addressType ?: when {
                isHomeLocation -> "THUIS"
                isWorkLocation -> "WERK"
                isCustomerLocation -> "KLANT"
                else -> null
            }
            val effectiveIsWork = isWorkLocation || addressType == "WERK"
            val effectiveIsHome = isHomeLocation || addressType == "THUIS"
            val effectiveIsCustomer = isCustomerLocation || addressType == "KLANT"
            
            ensureClientExists(label, effectiveIsCustomer)
            
            val coords = geocoderService.getCoordinatesForAddress(address)
            savedAddressDao.insertAddress(
                SavedAddress(
                    label = label,
                    address = address,
                    isWorkLocation = effectiveIsWork,
                    isHomeLocation = effectiveIsHome,
                    isCustomerLocation = effectiveIsCustomer,
                    defaultTripType = defaultTripType,
                    projectCode = projectCode,
                    notes = notes,
                    addressType = effectiveAddressType,
                    latitude = coords?.first,
                    longitude = coords?.second
                )
            )
        }
    }

    fun updateAddress(id: Long, label: String, address: String, isWorkLocation: Boolean, isHomeLocation: Boolean, isCustomerLocation: Boolean, defaultTripType: String?, projectCode: String?, notes: String?, addressType: String? = null) {
        viewModelScope.launch {
            val effectiveAddressType = addressType ?: when {
                isHomeLocation -> "THUIS"
                isWorkLocation -> "WERK"
                isCustomerLocation -> "KLANT"
                else -> null
            }
            val effectiveIsWork = isWorkLocation || addressType == "WERK"
            val effectiveIsHome = isHomeLocation || addressType == "THUIS"
            val effectiveIsCustomer = isCustomerLocation || addressType == "KLANT"
            
            ensureClientExists(label, effectiveIsCustomer)
            
            // Re-geocode if address changed, otherwise keep existing coords
            val existing = savedAddressDao.getAllAddressesSync().find { it.id == id }
            val coords = if (existing?.address != address) {
                geocoderService.getCoordinatesForAddress(address)
            } else {
                existing.latitude?.let { lat -> existing.longitude?.let { lng -> Pair(lat, lng) } }
            }
            savedAddressDao.updateAddress(
                SavedAddress(
                    id = id,
                    label = label,
                    address = address,
                    isWorkLocation = effectiveIsWork,
                    isHomeLocation = effectiveIsHome,
                    isCustomerLocation = effectiveIsCustomer,
                    defaultTripType = defaultTripType,
                    projectCode = projectCode,
                    notes = notes,
                    addressType = effectiveAddressType,
                    latitude = coords?.first,
                    longitude = coords?.second
                )
            )
        }
    }

    suspend fun canDeleteAddress(addressString: String): Boolean {
        val count = tripDao.getTripCountForAddress(addressString)
        return count == 0
    }

    fun deleteAddress(savedAddress: SavedAddress) {
        viewModelScope.launch {
            savedAddressDao.deleteAddress(savedAddress)
        }
    }

    suspend fun searchAddress(query: String): List<String> {
        return geocoderService.searchAddress(query)
    }

    fun importAddressesFromCsv(context: android.content.Context, uri: android.net.Uri, onSuccess: (Int) -> Unit, onError: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var importedCount = 0
                val existingAddresses = savedAddressDao.getAllAddressesSync()
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.BufferedReader(java.io.InputStreamReader(inputStream)).use { reader ->
                        val lines = reader.readLines()
                        if (lines.isEmpty()) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError() }
                            return@launch
                        }
                        
                        // Parse each line (super basic CSV parsing for now)
                        for (i in 1 until lines.size) {
                            val line = lines[i]
                            if (line.isBlank()) continue
                            
                            val tokens = line.split(",").map { it.trim() }
                            if (tokens.size >= 4) {
                                val name = tokens[0]
                                val desc = tokens[1]
                                val street = tokens[2]
                                val city = tokens[3]
                                val fullAddress = "$street, $city"
                                
                                // Check if an address with the same name or exact same location already exists
                                val isDuplicate = existingAddresses.any { 
                                    it.label.equals(name, ignoreCase = true) || 
                                    it.address.equals(fullAddress, ignoreCase = true)
                                }
                                
                                if (!isDuplicate) {
                                    val coords = geocoderService.getCoordinatesForAddress(fullAddress)
                                    savedAddressDao.insertAddress(
                                        SavedAddress(
                                            label = name,
                                            address = fullAddress,
                                            notes = desc,
                                            isWorkLocation = false,
                                            isHomeLocation = false,
                                            isCustomerLocation = false,
                                            defaultTripType = null,
                                            projectCode = null,
                                            addressType = null,
                                            latitude = coords?.first,
                                            longitude = coords?.second
                                        )
                                    )
                                    importedCount++
                                }
                            }
                        }
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (importedCount > 0) {
                        onSuccess(importedCount)
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError() }
            }
        }
    }
}
