package com.cimdriver.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Vehicle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun DateTimeSelectionCard(
    selectedDateMillis: Long,
    selectedHour: Int,
    selectedMinute: Int,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.date_and_time), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onShowDatePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dateFormat.format(Date(selectedDateMillis)))
                }
                OutlinedButton(
                    onClick = onShowTimePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, selectedHour); set(Calendar.MINUTE, selectedMinute) }
                    Text("${stringResource(R.string.start)}: ${timeFormat.format(cal.time)}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionCard(
    selectedVehicle: Vehicle?,
    vehicles: List<Vehicle>,
    vehicleExpanded: Boolean,
    onVehicleExpandedChange: (Boolean) -> Unit,
    onVehicleSelected: (Vehicle) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.vehicle_label), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { onVehicleExpandedChange(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedVehicle?.name ?: stringResource(R.string.no_vehicle_selected))
                }
                DropdownMenu(expanded = vehicleExpanded, onDismissRequest = { onVehicleExpandedChange(false) }) {
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text("${vehicle.name} - ${vehicle.licensePlate}") },
                            onClick = {
                                onVehicleSelected(vehicle)
                                onVehicleExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
