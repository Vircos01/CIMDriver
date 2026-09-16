package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.viewmodel.AddressBookViewModel
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    addressId: Long? = null,
    onBack: () -> Unit,
    viewModel: AddressBookViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val classificationRules by settingsViewModel.classificationRules.collectAsState()
    val existingAddress = addresses.find { it.id == addressId }

    var label by remember(existingAddress) { mutableStateOf(existingAddress?.label ?: "") }
    var address by remember(existingAddress) { mutableStateOf(existingAddress?.address ?: "") }
    var projectCode by remember(existingAddress) { mutableStateOf(existingAddress?.projectCode ?: "") }
    var notes by remember(existingAddress) { mutableStateOf(existingAddress?.notes ?: "") }

    // Adrestype: THUIS, WERK, KLANT of leeg
    var addressType by remember(existingAddress) {
        mutableStateOf(
            existingAddress?.addressType ?: when {
                existingAddress?.isHomeLocation == true -> "THUIS"
                existingAddress?.isWorkLocation == true -> "WERK"
                existingAddress?.isCustomerLocation == true -> "KLANT"
                else -> ""
            }
        )
    }

    // Standaard rittype (apart van adrestype)
    var defaultTripType by remember(existingAddress) {
        mutableStateOf(existingAddress?.defaultTripType ?: "")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (addressId == null) stringResource(R.string.new_address) else stringResource(R.string.edit_address),
                        color = com.cimdriver.app.ui.theme.CIMDriverWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = com.cimdriver.app.ui.theme.CIMDriverWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (label.isNotBlank() && address.isNotBlank()) {
                                val isWork = addressType == "WERK"
                                val isHome = addressType == "THUIS"
                                val isCustomer = addressType == "KLANT"
                                val showProjectCode = addressType in listOf("WERK", "KLANT") ||
                                        defaultTripType in listOf("Business Meeting", "Commissioned By CIMSOLUTIONS", "Customer Visit", "Customer Billable")
                                val finalProjectCode = if (showProjectCode) projectCode.takeIf { it.isNotBlank() } else null
                                val finalNotes = notes.takeIf { it.isNotBlank() }
                                val finalTripType = defaultTripType.takeIf { it.isNotBlank() }
                                val finalAddressType = addressType.takeIf { it.isNotBlank() }

                                if (existingAddress == null) {
                                    viewModel.addAddress(
                                        label, address, isWork, isHome, isCustomer,
                                        finalTripType, finalProjectCode, finalNotes, finalAddressType
                                    )
                                } else {
                                    viewModel.updateAddress(
                                        existingAddress.id, label, address, isWork, isHome, isCustomer,
                                        finalTripType, finalProjectCode, finalNotes, finalAddressType
                                    )
                                }
                                onBack()
                            }
                        },
                        enabled = label.isNotBlank() && address.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.save),
                            tint = if (label.isNotBlank() && address.isNotBlank())
                                com.cimdriver.app.ui.theme.CIMDriverWhite
                            else
                                com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === Adresgegevens ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.address_details), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.address_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AutoCompleteAddressField(
                        value = address,
                        onValueChange = { address = it },
                        label = stringResource(R.string.full_address),
                        onSearchAddress = { query -> viewModel.searchAddress(query) }
                    )
                }
            }

            // === Adrestype ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.address_type), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.address_type_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val addressTypeOptions = (listOf(
                        "" to "Geen adrestype",
                        "THUIS" to stringResource(R.string.home),
                        "WERK" to stringResource(R.string.work),
                        "KLANT" to stringResource(R.string.customer)
                    ) + classificationRules.flatMap { rule ->
                        listOfNotNull(rule.startAddressType, rule.endAddressType)
                            .filter { it !in listOf("THUIS", "WERK", "KLANT") }
                            .map { it to it }
                    }).distinctBy { it.first }
                    var expandedAddressType by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedAddressType,
                        onExpandedChange = { expandedAddressType = it }
                    ) {
                        OutlinedTextField(
                            value = addressTypeOptions.find { it.first == addressType }?.second ?: stringResource(R.string.no_address_type),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.address_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAddressType) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAddressType,
                            onDismissRequest = { expandedAddressType = false }
                        ) {
                            addressTypeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        addressType = option.first
                                        expandedAddressType = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // === Standaard Rittype ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.address_type), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.default_trip_type_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val tripTypeOptions = (listOf(
                        "" to "Geen standaard rittype",
                        "Home To Work" to "Home To Work",
                        "Business Meeting" to "Business Meeting",
                        "Customer Visit" to "Customer Visit",
                        "Customer Billable" to "Customer Billable",
                        "Commissioned By CIMSOLUTIONS" to "Commissioned By CIMSOLUTIONS",
                        "Exam Course" to "Exam Course",
                        "Car Maintenance" to "Car Maintenance"
                    ) + classificationRules.mapNotNull { it.tripType?.let { type -> type to type } })
                        .distinctBy { it.first }
                    var expandedTripType by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedTripType,
                        onExpandedChange = { expandedTripType = it }
                    ) {
                        OutlinedTextField(
                            value = tripTypeOptions.find { it.first == defaultTripType }?.second ?: stringResource(R.string.no_default_trip_type),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.default_trip_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTripType) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTripType,
                            onDismissRequest = { expandedTripType = false }
                        ) {
                            tripTypeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        defaultTripType = option.first
                                        expandedTripType = false
                                    }
                                )
                            }
                        }
                    }

                    val showProjectCode = defaultTripType in listOf(
                        "Business Meeting", "Commissioned By CIMSOLUTIONS", "Customer Visit", "Customer Billable"
                    ) || addressType in listOf("WERK", "KLANT")

                    if (showProjectCode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.project_code_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = projectCode,
                            onValueChange = { projectCode = it },
                            label = { Text(stringResource(R.string.project_code_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else if (defaultTripType == "Home To Work" || addressType == "THUIS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.home_to_work_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.note_optional)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            }
        }
    }
}
