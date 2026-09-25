package com.cimdriver.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import com.cimdriver.app.ui.components.DistanceStackedChart
import com.cimdriver.app.ui.components.WorkHoursBarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.components.VehicleItem
import kotlinx.coroutines.launch
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.viewmodel.TripsViewModel
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import com.cimdriver.app.ui.viewmodel.TimeFilter
import com.cimdriver.app.util.TripCategory
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.util.AddressMatching
import com.cimdriver.app.service.TrackingStatusStore
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExposedDropdownMenuBox
import com.cimdriver.app.data.local.entity.BluetoothDevice as DbBluetoothDevice
import androidx.compose.foundation.combinedClickable
import android.os.PowerManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: VehiclesViewModel = hiltViewModel(),
    onAddVehicle: () -> Unit = {},
    onEditVehicle: (Long) -> Unit = {}
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToRecalculate by remember { mutableStateOf<Vehicle?>(null) }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = { CimDriverTopAppBar(
            onOpenDrawer = onOpenDrawer,
            title = { Text(stringResource(R.string.vehicles_title), color = com.cimdriver.app.ui.theme.CIMDriverWhite) }
        ) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddVehicle() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_vehicle))
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Text(stringResource(R.string.no_vehicles_added), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    com.cimdriver.app.ui.components.SwipeToDeleteContainer(
                        modifier = Modifier.animateItem(),
                        onDelete = {
                            coroutineScope.launch {
                                val canDelete = viewModel.canDeleteVehicle(vehicle.id)
                                if (canDelete) {
                                    vehicleToDelete = vehicle
                                } else {
                                    deleteErrorMsg = context.getString(R.string.cannot_delete_vehicle_linked_to_trips)
                                }
                            }
                        },
                        backgroundPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        VehicleItem(
                            vehicle = vehicle, 
                            onEdit = { onEditVehicle(it.id) },
                        onRecalculate = {
                            vehicleToRecalculate = vehicle
                        },
                        onSetDefault = {
                            viewModel.setAsDefaultVehicle(it)
                        }
                        )
                    }
                }
            }
        }
    }

    if (vehicleToDelete != null) {
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text(stringResource(R.string.delete_vehicle)) },
            text = { Text(stringResource(R.string.delete_vehicle_confirmation, vehicleToDelete?.name ?: "")) },
            confirmButton = {
                Button(onClick = {
                    vehicleToDelete?.let { viewModel.deleteVehicle(it) }
                    vehicleToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (deleteErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { deleteErrorMsg = null },
            title = { Text(stringResource(R.string.not_possible)) },
            text = { Text(deleteErrorMsg!!) },
            confirmButton = {
                Button(onClick = { deleteErrorMsg = null }) { Text(stringResource(R.string.ok_camel)) }
            }
        )
    }

    if (vehicleToRecalculate != null) {
        AlertDialog(
            onDismissRequest = { vehicleToRecalculate = null },
            title = { Text(stringResource(R.string.recalculate_odometer_title)) },
            text = { Text(stringResource(R.string.recalculate_odometer_desc, vehicleToRecalculate?.name ?: "")) },
            confirmButton = {
                Button(onClick = {
                    vehicleToRecalculate?.let { viewModel.recalculateOdometerHistory(it.id) }
                    vehicleToRecalculate = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text(stringResource(R.string.recalculate))
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToRecalculate = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

