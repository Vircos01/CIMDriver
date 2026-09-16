package com.cimdriver.app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.screens.AutoCompleteAddressField
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AddManualTripDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSearchAddress: suspend (String) -> List<String>,
    onCalculateDistance: suspend (String, String) -> Float?,
    onSave: (Long?, String, String, Double, String) -> Unit
) {
    var selectedVehicle by remember { mutableStateOf(vehicles.find { it.isDefault } ?: vehicles.firstOrNull()) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    
    var startAddress by remember { mutableStateOf("") }
    var endAddress by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var tripType by remember { mutableStateOf("Zakelijk") }

    LaunchedEffect(startAddress, endAddress) {
        if (startAddress.length > 3 && endAddress.length > 3) {
            delay(1200) // debounce
            val dist = onCalculateDistance(startAddress, endAddress)
            if (dist != null) {
                distance = String.format(Locale.getDefault(), "%.1f", dist / 1000.0)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_trip)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Vehicle Dropdown
                Box {
                    OutlinedButton(onClick = { vehicleExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedVehicle?.name ?: stringResource(R.string.no_vehicle_selected))
                    }
                    DropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = { Text(vehicle.name) },
                                onClick = {
                                    selectedVehicle = vehicle
                                    vehicleExpanded = false
                                }
                            )
                        }
                    }
                }
                
                AutoCompleteAddressField(
                    value = startAddress,
                    onValueChange = { startAddress = it },
                    label = stringResource(R.string.from_hint),
                    onSearchAddress = onSearchAddress
                )
                
                AutoCompleteAddressField(
                    value = endAddress,
                    onValueChange = { endAddress = it },
                    label = stringResource(R.string.to_hint),
                    onSearchAddress = onSearchAddress
                )
                
                OutlinedTextField(value = distance, onValueChange = { distance = it }, label = { Text(stringResource(R.string.distance_in_km)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                
                // Trip Type Segmented Control Mock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = tripType == "Zakelijk", onClick = { tripType = "Zakelijk" }, label = { Text(stringResource(R.string.business)) })
                    FilterChip(selected = tripType == "Privé", onClick = { tripType = "Privé" }, label = { Text(stringResource(R.string.private_usage)) })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val dist = distance.replace(",", ".").toDoubleOrNull() ?: 0.0
                onSave(selectedVehicle?.id, startAddress, endAddress, dist, if (tripType == "Zakelijk") "BUSINESS" else "PERSONAL")
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun BluetoothPairDialog(
    viewModel: VehiclesViewModel,
    onDismiss: () -> Unit,
    onDeviceSelected: (macAddress: String, name: String) -> Unit
) {
    val devices = viewModel.getBondedBluetoothDevices()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_bluetooth_device)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.no_paired_devices))
            } else {
                LazyColumn {
                    items(devices) { device ->
                        TextButton(
                            onClick = { onDeviceSelected(device.first, device.second) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${device.second} (${device.first})", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
