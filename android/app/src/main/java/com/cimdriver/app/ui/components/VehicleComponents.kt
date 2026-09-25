package com.cimdriver.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Vehicle

@Composable
fun VehicleItem(
    vehicle: Vehicle, 
    onEdit: (Vehicle) -> Unit,
    onRecalculate: (Vehicle) -> Unit = {},
    onSetDefault: (Vehicle) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEdit(vehicle) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = vehicle.name, style = MaterialTheme.typography.titleMedium)
                    if (vehicle.isDefault) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.vehicle_default), tint = com.cimdriver.app.ui.theme.CIMDriverGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = "${vehicle.make} ${vehicle.model} - ${vehicle.licensePlate}", style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.vehicle_odometer, vehicle.odometerCurrent.toString()), style = MaterialTheme.typography.bodySmall)
            }

            Row {
                IconButton(onClick = { onSetDefault(vehicle) }) {
                    Icon(
                        if (vehicle.isDefault) Icons.Filled.Star else Icons.Outlined.Star, 
                        contentDescription = stringResource(R.string.vehicle_set_default), 
                        tint = if (vehicle.isDefault) com.cimdriver.app.ui.theme.CIMDriverGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onRecalculate(vehicle) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Herbereken", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
