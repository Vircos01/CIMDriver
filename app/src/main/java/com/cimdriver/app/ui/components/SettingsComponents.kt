package com.cimdriver.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cimdriver.app.R
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun OdometerSettingsCard(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    intervalDaysStr: String,
    onIntervalDaysChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.odometer_settings), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.odometer_settings_desc), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isEnabled, onCheckedChange = onEnabledChange)
                Text(stringResource(R.string.enable_reminder))
            }
            
            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = intervalDaysStr,
                    onValueChange = onIntervalDaysChange,
                    label = { Text(stringResource(R.string.days_between_checks)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text(stringResource(R.string.days)) }
                )
            }
        }
    }
}

@Composable
fun TrackingSettingsCard(
    gracePeriodMinsStr: String,
    onGracePeriodChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.tracking_settings), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.tracking_settings_desc), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = gracePeriodMinsStr,
                onValueChange = onGracePeriodChange,
                label = { Text(stringResource(R.string.grace_period)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text(stringResource(R.string.minutes)) }
            )
        }
    }
}

@Composable
fun PrivacySettingsCard(
    settings: com.cimdriver.app.data.local.entity.Settings,
    onRetentionDaysChanged: (Int) -> Unit,
    onClearLocationHistory: () -> Unit,
    onResetAllData: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.privacy_location_data), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.gps_points_deleted_after), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(30 to "30 d", 90 to "90 d", 365 to "365 d", 0 to stringResource(R.string.never)).forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                        selected = settings.locationRetentionDays == option.first,
                        onClick = { onRetentionDaysChanged(option.first) }
                    ) { Text(option.second) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (settings.locationRetentionDays == 0) stringResource(R.string.gps_not_deleted) else stringResource(R.string.gps_deleted_periodically),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onClearLocationHistory, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clear_gps_history))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onResetAllData, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationSettingsCard(
    viewModel: SettingsViewModel,
    settings: com.cimdriver.app.data.local.entity.Settings,
    rules: List<com.cimdriver.app.data.local.entity.ClassificationRule>
) {
    var editingRule by remember { mutableStateOf<com.cimdriver.app.data.local.entity.ClassificationRule?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.trip_classification), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.trip_classification_desc), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.unknown_addresses), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    selected = settings.classificationDefault == "PRIVATE",
                    onClick = { viewModel.updateClassificationSettings("PRIVATE", settings.classifyHomeWorkAsCommute, settings.classifyCustomerAsBusiness) }
                ) { Text(stringResource(R.string.private_usage)) }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = settings.classificationDefault == "BUSINESS",
                    onClick = { viewModel.updateClassificationSettings("BUSINESS", settings.classifyHomeWorkAsCommute, settings.classifyCustomerAsBusiness) }
                ) { Text(stringResource(R.string.business)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_work_as_commute))
                    Text(stringResource(R.string.home_work_as_commute_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = settings.classifyHomeWorkAsCommute,
                    onCheckedChange = { enabled -> viewModel.updateClassificationSettings(settings.classificationDefault, enabled, settings.classifyCustomerAsBusiness) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.customer_as_business))
                    Text(stringResource(R.string.customer_as_business_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = settings.classifyCustomerAsBusiness,
                    onCheckedChange = { enabled -> viewModel.updateClassificationSettings(settings.classificationDefault, settings.classifyHomeWorkAsCommute, enabled) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val newRuleStr = stringResource(R.string.settings_new_rule)
                Text(stringResource(R.string.custom_rules), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = {
                    editingRule = com.cimdriver.app.data.local.entity.ClassificationRule(
                        name = newRuleStr,
                        category = "BUSINESS"
                    )
                }) { Text(stringResource(R.string.add)) }
            }
            if (rules.isEmpty()) {
                Text(stringResource(R.string.no_custom_rules), style = MaterialTheme.typography.bodySmall)
            } else {
                rules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, style = MaterialTheme.typography.bodyMedium)
                            val startStr = rule.startAddressType ?: rule.startAddress ?: "*"
                            val endStr = rule.endAddressType ?: rule.endAddress ?: "*"
                            Text(
                                "$startStr <-> $endStr | ${rule.tripType ?: "*"} | ${rule.category}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { editingRule = rule }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_rule))
                        }
                        IconButton(onClick = { viewModel.deleteClassificationRule(rule) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }

    editingRule?.let { rule ->
        var name by remember(rule) { mutableStateOf(rule.name) }
        var startType by remember(rule) { mutableStateOf(rule.startAddressType.orEmpty()) }
        var endType by remember(rule) { mutableStateOf(rule.endAddressType.orEmpty()) }
        var startAddr by remember(rule) { mutableStateOf(rule.startAddress.orEmpty()) }
        var endAddr by remember(rule) { mutableStateOf(rule.endAddress.orEmpty()) }
        var tripType by remember(rule) { mutableStateOf(rule.tripType.orEmpty()) }
        var category by remember(rule) { mutableStateOf(rule.category) }

        AlertDialog(
            onDismissRequest = { editingRule = null },
            title = { Text(if (rule.id == 0L) stringResource(R.string.add_rule) else stringResource(R.string.edit_rule)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name_label)) }, singleLine = true)
                    OutlinedTextField(startType, { startType = it }, label = { Text(stringResource(R.string.start_address_type_optional)) }, singleLine = true)
                    OutlinedTextField(endType, { endType = it }, label = { Text(stringResource(R.string.end_address_type_optional)) }, singleLine = true)
                    OutlinedTextField(startAddr, { startAddr = it }, label = { Text("Exact Start Adres (optioneel)") }, singleLine = true)
                    OutlinedTextField(endAddr, { endAddr = it }, label = { Text("Exact Eind Adres (optioneel)") }, singleLine = true)
                    OutlinedTextField(tripType, { tripType = it }, label = { Text(stringResource(R.string.dynamics_trip_type_optional)) }, singleLine = true)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("BUSINESS" to stringResource(R.string.business), "PRIVATE" to stringResource(R.string.private_usage), "COMMUTE" to "Woon-werk").forEachIndexed { index, option ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                selected = category == option.first,
                                onClick = { category = option.first }
                            ) { Text(option.second) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveClassificationRule(
                                rule.copy(
                                    name = name.trim(),
                                    startAddressType = startType.trim().ifBlank { null },
                                    endAddressType = endType.trim().ifBlank { null },
                                    startAddress = startAddr.trim().ifBlank { null },
                                    endAddress = endAddr.trim().ifBlank { null },
                                    tripType = tripType.trim().ifBlank { null },
                                    category = category
                                )
                            )
                        }
                        editingRule = null
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editingRule = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun PermissionsSettingsCard() {
    val context = LocalContext.current
    var hasFineLocation by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) 
    }
    var hasBackgroundLocation by remember { 
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true)
    }

    var hasBluetoothPermission by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true)
    }

    var hasNotificationPermission by remember { 
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val bluetoothSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundLocation = isGranted
    }

    var isIgnoringBattery by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName))
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isIgnoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.permissions), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.permissions_desc), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            DashboardPermissionsSection(
                hasFineLocation = hasFineLocation,
                hasBackgroundLocation = hasBackgroundLocation,
                hasNotificationPermission = hasNotificationPermission,
                hasBluetoothPermission = hasBluetoothPermission,
                isIgnoringBattery = isIgnoringBattery,
                locationPermissionLauncher = locationPermissionLauncher,
                notificationLauncher = notificationLauncher,
                backgroundLocationLauncher = backgroundLocationLauncher,
                bluetoothLauncher = bluetoothSettingsLauncher,
                batteryLauncher = batteryLauncher
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkHoursSettingsCard(
    daySettings: androidx.compose.runtime.snapshots.SnapshotStateList<com.cimdriver.app.util.DayWorkHours>,
    breakMinutesStr: String,
    onBreakMinutesChange: (String) -> Unit,
    toleranceMinutesStr: String,
    onToleranceMinutesChange: (String) -> Unit
) {
    var timePickerState by remember { mutableStateOf<Triple<Int, Boolean, Boolean>?>(null) } // dayIndex, isStart, showPicker

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.work_hours), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.work_hours_desc), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            daySettings.forEachIndexed { index, dayWorkHours ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dayWorkHours.isEnabled,
                        onCheckedChange = { checked ->
                            daySettings[index] = dayWorkHours.copy(isEnabled = checked)
                        }
                    )
                    Text(text = com.cimdriver.app.util.WorkHoursUtil.getDayName(dayWorkHours.dayOfWeek).take(2), modifier = Modifier.width(32.dp))
                    
                    if (dayWorkHours.isEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { timePickerState = Triple(index, true, true) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(dayWorkHours.startTime)
                        }
                        Text(" - ", modifier = Modifier.padding(horizontal = 4.dp))
                        OutlinedButton(
                            onClick = { timePickerState = Triple(index, false, true) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(dayWorkHours.endTime)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = breakMinutesStr,
                onValueChange = onBreakMinutesChange,
                label = { Text(stringResource(R.string.default_break_minutes)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = toleranceMinutesStr,
                onValueChange = onToleranceMinutesChange,
                label = { Text(stringResource(R.string.work_time_tolerance)) },
                supportingText = { Text(stringResource(R.string.work_time_tolerance_desc)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

        }
    }

    timePickerState?.let { (dayIndex, isStart, showPicker) ->
        if (showPicker) {
            val daySetting = daySettings[dayIndex]
            val timeStr = if (isStart) daySetting.startTime else daySetting.endTime
            val parts = timeStr.split(":")
            val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            
            val pickerState = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = true)
            
            AlertDialog(
                onDismissRequest = { timePickerState = null },
                title = { Text(stringResource(R.string.set_time)) },
                text = {
                    TimePicker(state = pickerState)
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newTime = String.format(Locale.getDefault(), "%02d:%02d", pickerState.hour, pickerState.minute)
                        if (isStart) {
                            daySettings[dayIndex] = daySetting.copy(startTime = newTime)
                        } else {
                            daySettings[dayIndex] = daySetting.copy(endTime = newTime)
                        }
                        timePickerState = null
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { timePickerState = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
