package com.cimdriver.app.ui.screens

import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.viewmodel.WorkHoursViewModel
import com.cimdriver.app.ui.viewmodel.AddressBookViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkDayScreen(
    workDayId: Long? = null,
    onBack: () -> Unit,
    viewModel: WorkHoursViewModel = hiltViewModel(),
    addressBookViewModel: AddressBookViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val locale = currentAppLocale()
    val addresses by addressBookViewModel.addresses.collectAsState()
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val calendar = remember { Calendar.getInstance() }
    
    var depHour by remember { mutableStateOf(8) }
    var depMinute by remember { mutableStateOf(0) }
    
    var arrHour by remember { mutableStateOf(8) }
    var arrMinute by remember { mutableStateOf(30) }
    
    var depWorkHour by remember { mutableStateOf(17) }
    var depWorkMinute by remember { mutableStateOf(0) }
    
    var arrHomeHour by remember { mutableStateOf(17) }
    var arrHomeMinute by remember { mutableStateOf(30) }

    var breakMinutes by remember { mutableStateOf("30") }
    var workLocationLabel by remember { mutableStateOf("") }
    var projectCode by remember { mutableStateOf("") }
    var workLocationExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    
    var timePickerType by remember { mutableStateOf<String?>(null) } // "DEP", "ARR", "DEP_WORK", "ARR_HOME"

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", locale)

    val workDays by viewModel.workDays.collectAsState()
    var initialLoadDone by remember { mutableStateOf(false) }

    LaunchedEffect(workDayId, workDays) {
        if (!initialLoadDone && workDayId != null && workDays.isNotEmpty()) {
            val wd = workDays.find { it.id == workDayId }
            if (wd != null) {
                selectedDateMillis = wd.date
                
                val startCal = Calendar.getInstance().apply { timeInMillis = wd.firstDepartureTime }
                depHour = startCal.get(Calendar.HOUR_OF_DAY)
                depMinute = startCal.get(Calendar.MINUTE)
                
                val arrCal = Calendar.getInstance().apply { timeInMillis = wd.arrivalTime }
                arrHour = arrCal.get(Calendar.HOUR_OF_DAY)
                arrMinute = arrCal.get(Calendar.MINUTE)
                
                val depWorkCal = Calendar.getInstance().apply { timeInMillis = wd.departureTime ?: wd.lastArrivalTime ?: wd.arrivalTime }
                depWorkHour = depWorkCal.get(Calendar.HOUR_OF_DAY)
                depWorkMinute = depWorkCal.get(Calendar.MINUTE)
                
                val endCal = Calendar.getInstance().apply { timeInMillis = wd.lastArrivalTime ?: wd.arrivalTime }
                arrHomeHour = endCal.get(Calendar.HOUR_OF_DAY)
                arrHomeMinute = endCal.get(Calendar.MINUTE)
                
                breakMinutes = wd.breakMinutes.toString()
                workLocationLabel = wd.workLocationLabel ?: ""
                projectCode = wd.projectCode ?: ""
                initialLoadDone = true
            }
        }
    }

    fun getTime(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (workDayId == null) stringResource(R.string.new_work_day) else stringResource(R.string.edit_work_day), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (workDayId == null) {
                                viewModel.addManualWorkDay(
                                    date = selectedDateMillis,
                                    firstDepartureTime = getTime(depHour, depMinute),
                                    arrivalTime = getTime(arrHour, arrMinute),
                                    departureTime = getTime(depWorkHour, depWorkMinute),
                                    lastArrivalTime = getTime(arrHomeHour, arrHomeMinute),
                                    breakMinutes = breakMinutes.toIntOrNull() ?: 0,
                                    workLocationLabel = workLocationLabel.ifBlank { context.getString(R.string.manually_entered) },
                                    projectCode = projectCode.ifBlank { null }
                                )
                            } else {
                                viewModel.updateWorkDay(
                                    id = workDayId,
                                    date = selectedDateMillis,
                                    firstDepartureTime = getTime(depHour, depMinute),
                                    arrivalTime = getTime(arrHour, arrMinute),
                                    departureTime = getTime(depWorkHour, depWorkMinute),
                                    lastArrivalTime = getTime(arrHomeHour, arrHomeMinute),
                                    roundedArrivalTime = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(getTime(arrHour, arrMinute)),
                                    roundedDepartureTime = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(getTime(depWorkHour, depWorkMinute)),
                                    breakMinutes = breakMinutes.toIntOrNull() ?: 0,
                                    workLocationLabel = workLocationLabel.ifBlank { null },
                                    projectCode = projectCode.ifBlank { null },
                                    status = "APPROVED"
                                )
                            }
                            onBack()
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
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
            // === Datum ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.date), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(dateFormat.format(Date(selectedDateMillis)))
                    }
                }
            }

            // === Tijden ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.times), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { timePickerType = "DEP" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.home_departure, String.format(locale, "%02d:%02d", depHour, depMinute)), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        OutlinedButton(onClick = { timePickerType = "ARR" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.work_arrival, String.format(locale, "%02d:%02d", arrHour, arrMinute)), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { timePickerType = "DEP_WORK" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.work_departure, String.format(locale, "%02d:%02d", depWorkHour, depWorkMinute)), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        OutlinedButton(onClick = { timePickerType = "ARR_HOME" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.home_arrival, String.format(locale, "%02d:%02d", arrHomeHour, arrHomeMinute)), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            
            // === Samenvatting Werkuren ===
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.work_hours_summary), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val actualStartMs = getTime(arrHour, arrMinute)
                    val actualEndMs = getTime(depWorkHour, depWorkMinute)
                    val roundedStartMs = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(actualStartMs)
                    val roundedEndMs = com.cimdriver.app.util.TimeUtil.roundToNearestQuarterHour(actualEndMs)
                    
                    val timeFormat = SimpleDateFormat("HH:mm", locale)
                    val breakMins = breakMinutes.toIntOrNull() ?: 0
                    val totalMs = roundedEndMs - roundedStartMs - (breakMins * 60000L)
                    val totalHours = if (totalMs > 0) totalMs / 3600000f else 0f
                    
                    Text(stringResource(R.string.actual_hours, timeFormat.format(Date(actualStartMs)), timeFormat.format(Date(actualEndMs))), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.rounded_hours, timeFormat.format(Date(roundedStartMs)), timeFormat.format(Date(roundedEndMs))), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.break_minutes_label, breakMins.toString()), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.total_time_hours, String.format(locale, "%.2f", totalHours)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            // === Overig ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.other), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = workLocationLabel,
                            onValueChange = { 
                                workLocationLabel = it
                                workLocationExpanded = it.isNotBlank()
                            },
                            label = { Text(stringResource(R.string.work_location_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = workLocationExpanded,
                            onDismissRequest = { workLocationExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f),
                            properties = PopupProperties(focusable = false)
                        ) {
                            val filteredAddresses = addresses.filter {
                                it.label.contains(workLocationLabel, ignoreCase = true) ||
                                it.address.contains(workLocationLabel, ignoreCase = true)
                            }
                            if (filteredAddresses.isEmpty()) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.no_results)) }, onClick = { workLocationExpanded = false })
                            } else {
                                filteredAddresses.take(4).forEach { addr ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(addr.label)
                                                Text(addr.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            workLocationLabel = addr.label
                                            if (addr.projectCode != null) {
                                                projectCode = addr.projectCode
                                            }
                                            workLocationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectCode,
                        onValueChange = { projectCode = it },
                        label = { Text(stringResource(R.string.project_code_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = breakMinutes,
                        onValueChange = { breakMinutes = it },
                        label = { Text(stringResource(R.string.break_minutes_input)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (timePickerType != null) {
        val (initHour, initMin) = when(timePickerType) {
            "DEP" -> Pair(depHour, depMinute)
            "ARR" -> Pair(arrHour, arrMinute)
            "DEP_WORK" -> Pair(depWorkHour, depWorkMinute)
            "ARR_HOME" -> Pair(arrHomeHour, arrHomeMinute)
            else -> Pair(8, 0)
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { timePickerType = null },
            title = { Text(stringResource(R.string.select_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    when(timePickerType) {
                        "DEP" -> { depHour = timePickerState.hour; depMinute = timePickerState.minute }
                        "ARR" -> { arrHour = timePickerState.hour; arrMinute = timePickerState.minute }
                        "DEP_WORK" -> { depWorkHour = timePickerState.hour; depWorkMinute = timePickerState.minute }
                        "ARR_HOME" -> { arrHomeHour = timePickerState.hour; arrHomeMinute = timePickerState.minute }
                    }
                    timePickerType = null
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerType = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
