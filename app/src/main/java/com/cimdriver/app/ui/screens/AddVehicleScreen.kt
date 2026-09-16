package com.cimdriver.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.cimdriver.app.ui.dialogs.BluetoothPairDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    vehicleId: Long? = null,
    onBack: () -> Unit,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val existingVehicle = vehicles.find { it.id == vehicleId }

    var name by remember(existingVehicle) { mutableStateOf(existingVehicle?.name ?: "") }
    var plate by remember(existingVehicle) { mutableStateOf(existingVehicle?.licensePlate ?: "") }
    var make by remember(existingVehicle) { mutableStateOf(existingVehicle?.make ?: "") }
    var model by remember(existingVehicle) { mutableStateOf(existingVehicle?.model ?: "") }
    var odometerStart by remember(existingVehicle) { mutableStateOf(existingVehicle?.odometerStart?.toString() ?: "") }
    var odometerCurrent by remember(existingVehicle) { mutableStateOf(existingVehicle?.odometerCurrent?.toString() ?: "") }
    var odometerCorrectionStrategy by remember(existingVehicle) { mutableStateOf(existingVehicle?.odometerCorrectionStrategy ?: "DISTRIBUTE") }
    var usageType by remember(existingVehicle) { mutableStateOf(existingVehicle?.usageType ?: "MIXED") }
    var privateKmYearlyLimitStr by remember(existingVehicle) { mutableStateOf(existingVehicle?.privateKmYearlyLimit?.toString() ?: "500") }
    var showPrivateKmWarning by remember(existingVehicle) { mutableStateOf(existingVehicle?.showPrivateKmWarning ?: true) }
    var notes by remember(existingVehicle) { mutableStateOf(existingVehicle?.notes ?: "") }

    val allBluetoothDevices by viewModel.bluetoothDevices.collectAsState(initial = emptyList())
    val linkedDevices = existingVehicle?.let { vehicle ->
        allBluetoothDevices.filter { it.vehicleId == vehicle.id }
    } ?: emptyList()

    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showOdometerCorrectionDialog by remember { mutableStateOf(false) }
    var odometerCorrectionInput by remember(existingVehicle) { mutableStateOf(existingVehicle?.odometerCurrent?.toString() ?: "") }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            showBluetoothDialog = true
        }
    }

    fun requestBluetoothAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            showBluetoothDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (vehicleId == null) "Nieuwe Auto" else "Auto Bewerken", color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val odoStartInt = odometerStart.toIntOrNull() ?: 0
                            val odoCurrentInt = odometerCurrent.toIntOrNull() ?: 0
                            val privateKmYearlyLimit = privateKmYearlyLimitStr.toIntOrNull() ?: 500
                            if (vehicleId == null) {
                                viewModel.addVehicle(name, plate, make, model, odoStartInt, odoCurrentInt, usageType, notes.takeIf { it.isNotBlank() }, privateKmYearlyLimit, showPrivateKmWarning, odometerCorrectionStrategy)
                            } else {
                                existingVehicle?.let {
                                    viewModel.updateVehicle(it, name, plate, make, model, odoStartInt, odoCurrentInt, usageType, notes.takeIf { it.isNotBlank() }, privateKmYearlyLimit, showPrivateKmWarning, odometerCorrectionStrategy)
                                }
                            }
                            onBack()
                        },
                        enabled = name.isNotBlank() && plate.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.Check, 
                            contentDescription = stringResource(R.string.save), 
                            tint = if (name.isNotBlank() && plate.isNotBlank()) com.cimdriver.app.ui.theme.CIMDriverWhite else com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.5f)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.vehicle_details), style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Naam (bijv. Privé auto)") }, 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = plate, 
                        onValueChange = { plate = it }, 
                        label = { Text(stringResource(R.string.license_plate)) }, 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = make, 
                        onValueChange = { make = it }, 
                        label = { Text(stringResource(R.string.brand)) }, 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = model, 
                        onValueChange = { model = it }, 
                        label = { Text(stringResource(R.string.model)) }, 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = odometerStart, 
                        onValueChange = { odometerStart = it }, 
                        label = { Text("Begin km-stand (start contract)") }, 
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = existingVehicle != null,
                        enabled = existingVehicle == null
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = odometerCurrent, 
                            onValueChange = { odometerCurrent = it }, 
                            label = { Text(stringResource(R.string.current_odometer)) }, 
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            readOnly = existingVehicle != null,
                            enabled = existingVehicle == null
                        )
                        if (existingVehicle != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { 
                                odometerCorrectionInput = odometerCurrent
                                showOdometerCorrectionDialog = true 
                            }) {
                                Text("Corrigeren")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.usage_type), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { usageType = "MIXED" },
                            selected = usageType == "MIXED"
                        ) {
                            Text(stringResource(R.string.mixed))
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { usageType = "BUSINESS_ONLY" },
                            selected = usageType == "BUSINESS_ONLY"
                        ) {
                            Text(stringResource(R.string.business))
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { usageType = "PRIVATE_ONLY" },
                            selected = usageType == "PRIVATE_ONLY"
                        ) {
                            Text(stringResource(R.string.private_usage))
                        }
                    }
                    
                    if (usageType == "MIXED" || usageType == "PRIVATE_ONLY") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Waarschuwing Privégebruik", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = showPrivateKmWarning,
                                onCheckedChange = { showPrivateKmWarning = it }
                            )
                            Text("Toon waarschuwing op dashboard", modifier = Modifier.padding(start = 8.dp))
                        }
                        
                        if (showPrivateKmWarning) {
                            OutlinedTextField(
                                value = privateKmYearlyLimitStr,
                                onValueChange = { privateKmYearlyLimitStr = it.filter { char -> char.isDigit() } },
                                label = { Text("Maximale privékilometers (bijv. 500)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Correctiestrategie KM-stand", style = MaterialTheme.typography.titleSmall)
                    
                    var strategyExpanded by remember { mutableStateOf(false) }
                    val strategyOptions = listOf("DISTRIBUTE" to "Verschil verdelen over eerdere ritten", "CREATE_TRIP" to "Correctierit aanmaken (Privé)", "LEAVE_GAP" to "Gat laten staan")
                    
                    ExposedDropdownMenuBox(
                        expanded = strategyExpanded,
                        onExpandedChange = { strategyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = strategyOptions.find { it.first == odometerCorrectionStrategy }?.second ?: "Verschil verdelen over eerdere ritten",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            label = { Text("Strategie") }
                        )
                        ExposedDropdownMenu(
                            expanded = strategyExpanded,
                            onDismissRequest = { strategyExpanded = false }
                        ) {
                            strategyOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        odometerCorrectionStrategy = option.first
                                        strategyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notities over deze auto (Optioneel)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            }
            if (existingVehicle != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.bluetooth_carkit), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.pair_bluetooth_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (linkedDevices.isNotEmpty()) {
                            linkedDevices.forEach { device ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = device.deviceName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeBluetoothDevice(device) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.unpair),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { requestBluetoothAndShowDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.pair_new_device))
                        }
                    }
                }
            }
        }
    }

    if (showBluetoothDialog && existingVehicle != null) {
        BluetoothPairDialog(
            viewModel = viewModel,
            onDismiss = { showBluetoothDialog = false },
            onDeviceSelected = { mac, deviceName ->
                viewModel.addBluetoothDeviceToVehicle(mac, deviceName, existingVehicle.id)
                showBluetoothDialog = false
            }
        )
    }

    if (showOdometerCorrectionDialog && existingVehicle != null) {
        AlertDialog(
            onDismissRequest = { showOdometerCorrectionDialog = false },
            title = { Text("Km-stand corrigeren") },
            text = {
                OutlinedTextField(
                    value = odometerCorrectionInput,
                    onValueChange = { odometerCorrectionInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Werkelijke km-stand") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val corrected = odometerCorrectionInput.toIntOrNull() ?: existingVehicle.odometerCurrent
                    viewModel.confirmOdometerCheck(existingVehicle, corrected)
                    odometerCurrent = corrected.toString()
                    showOdometerCorrectionDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOdometerCorrectionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
