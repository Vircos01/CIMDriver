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
import com.cimdriver.app.ui.components.TrackingSettingsCard
import com.cimdriver.app.ui.components.OdometerSettingsCard
import com.cimdriver.app.ui.components.WorkHoursSettingsCard
import com.cimdriver.app.ui.components.ClassificationSettingsCard
import com.cimdriver.app.ui.components.PrivacySettingsCard
import com.cimdriver.app.ui.components.PermissionsSettingsCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
fun SettingsScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    viewModel: com.cimdriver.app.ui.viewmodel.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    var showRestartDialog by remember { mutableStateOf(false) }
    var showClearLocationDialog by remember { mutableStateOf(false) }
    var showResetAllDataDialog by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            com.cimdriver.app.util.BackupUtil.createBackup(context, uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = com.cimdriver.app.util.BackupUtil.restoreBackup(context, uri)
            if (success) {
                showRestartDialog = true
            }
        }
    }

    val settings by viewModel.settings.collectAsState()
    val classificationRules by viewModel.classificationRules.collectAsState()
    
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    val themeMode = initialSettings.themeMode

    // Hoisted states for Tracking
    var gracePeriodMinsStr by remember(initialSettings.gracePeriodSec) { mutableStateOf((initialSettings.gracePeriodSec / 60).toString()) }

    // Hoisted states for Odometer
    var odometerReminder by remember(initialSettings.odometerReminder) { mutableStateOf(initialSettings.odometerReminder) }
    var odometerIntervalStr by remember(initialSettings.odometerReminderIntervalDays) { mutableStateOf(initialSettings.odometerReminderIntervalDays.toString()) }
    

    // Hoisted states for WorkHours
    val daySettings = remember(initialSettings.workDays) {
        val parsed = com.cimdriver.app.util.WorkHoursUtil.parseWorkHoursString(
            initialSettings.workDays, 
            initialSettings.workStartTime, 
            initialSettings.workEndTime
        )
        androidx.compose.runtime.mutableStateListOf(*parsed.toTypedArray())
    }
    var breakMinutesStr by remember(initialSettings.defaultBreakMinutes) { mutableStateOf(initialSettings.defaultBreakMinutes.toString()) }
    var toleranceMinutesStr by remember(initialSettings.workHoursToleranceMinutes) { mutableStateOf(initialSettings.workHoursToleranceMinutes.toString()) }

    Scaffold(
        topBar = { CimDriverTopAppBar(
            onOpenDrawer = onOpenDrawer,
            title = { Text(stringResource(R.string.settings), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
            actions = {
                IconButton(onClick = {
                    val graceSec = (gracePeriodMinsStr.toIntOrNull() ?: 3) * 60
                    viewModel.updateGracePeriod(graceSec)

                    val odoDays = odometerIntervalStr.toIntOrNull() ?: 30
                    viewModel.updateOdometerReminder(odometerReminder, odoDays)

                    val serializedDays = com.cimdriver.app.util.WorkHoursUtil.serializeWorkHours(daySettings)
                    val breakMins = breakMinutesStr.toIntOrNull() ?: 30
                    val toleranceMins = toleranceMinutesStr.toIntOrNull() ?: 30
                    viewModel.updateWorkingHours(initialSettings.workStartTime, initialSettings.workEndTime, serializedDays, breakMins, toleranceMins)
                    
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_saved), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Check, stringResource(R.string.save), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                }
            }
        ) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.appearance_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { viewModel.updateThemeMode("LIGHT") },
                            selected = themeMode == "LIGHT"
                        ) {
                            Text(stringResource(R.string.light))
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { viewModel.updateThemeMode("DARK") },
                            selected = themeMode == "DARK"
                        ) {
                            Text(stringResource(R.string.dark))
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { viewModel.updateThemeMode("SYSTEM") },
                            selected = themeMode == "SYSTEM"
                        ) {
                            Text(stringResource(R.string.system))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TrackingSettingsCard(
                gracePeriodMinsStr = gracePeriodMinsStr,
                onGracePeriodChange = { gracePeriodMinsStr = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            WorkHoursSettingsCard(
                daySettings = daySettings,
                breakMinutesStr = breakMinutesStr,
                onBreakMinutesChange = { breakMinutesStr = it },
                toleranceMinutesStr = toleranceMinutesStr,
                onToleranceMinutesChange = { toleranceMinutesStr = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            ClassificationSettingsCard(viewModel, initialSettings, classificationRules)
            Spacer(modifier = Modifier.height(16.dp))

            OdometerSettingsCard(
                isEnabled = odometerReminder,
                onEnabledChange = { odometerReminder = it },
                intervalDaysStr = odometerIntervalStr,
                onIntervalDaysChange = { odometerIntervalStr = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            PrivacySettingsCard(
                settings = initialSettings,
                onRetentionDaysChanged = viewModel::updateLocationRetentionDays,
                onClearLocationHistory = { showClearLocationDialog = true },
                onResetAllData = { showResetAllDataDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))

            PermissionsSettingsCard()

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToDiagnostics
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.system_diagnostics), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.system_diagnostics_desc), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.data_management), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { backupLauncher.launch("cimdriver_backup.zip") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.create_backup))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(text = stringResource(R.string.restore_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.restore_backup), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restore_completed)) },
            text = { Text(stringResource(R.string.backup_restored_msg)) },
            confirmButton = {
                Button(onClick = { kotlin.system.exitProcess(0) }) {
                    Text(stringResource(R.string.understood_close))
                }
            }
        )
    }

    if (showClearLocationDialog) {
        AlertDialog(
            onDismissRequest = { showClearLocationDialog = false },
            title = { Text(stringResource(R.string.clear_location_history_title)) },
            text = { Text(stringResource(R.string.clear_location_history_desc)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLocationHistory()
                    showClearLocationDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLocationDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showResetAllDataDialog) {
        var resetConfirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetAllDataDialog = false },
            title = { Text(stringResource(R.string.reset_all_data_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.reset_all_data_desc))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetConfirmation,
                        onValueChange = { resetConfirmation = it },
                        label = { Text(stringResource(R.string.type_delete_all)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = resetConfirmation == stringResource(R.string.type_delete_all).removePrefix("Typ "),
                    onClick = {
                    viewModel.resetAllData()
                    showResetAllDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}



