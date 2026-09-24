package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.viewmodel.TripsViewModel
import com.cimdriver.app.util.AddressMatching
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R
import com.cimdriver.app.ui.components.DateTimeSelectionCard
import com.cimdriver.app.ui.components.VehicleSelectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    tripId: Long? = null,
    copyFromId: Long? = null,
    isReturn: Boolean = false,
    onBack: () -> Unit,
    viewModel: TripsViewModel = hiltViewModel(),
    addressBookViewModel: com.cimdriver.app.ui.viewmodel.AddressBookViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val allTrips by viewModel.trips.collectAsState()
    val savedAddresses by addressBookViewModel.addresses.collectAsState()
    val existingTrip = allTrips.find { it.id == tripId }
    val copyTrip = allTrips.find { it.id == copyFromId }
    val refTrip = existingTrip ?: copyTrip

    var selectedVehicle by remember(refTrip) { mutableStateOf<Vehicle?>(null) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    var startAddress by remember(refTrip, isReturn) { 
        mutableStateOf(existingTrip?.startAddress ?: if (isReturn) copyTrip?.endAddress ?: "" else copyTrip?.startAddress ?: "") 
    }
    var endAddress by remember(refTrip, isReturn) { 
        mutableStateOf(existingTrip?.endAddress ?: if (isReturn) copyTrip?.startAddress ?: "" else copyTrip?.endAddress ?: "") 
    }
    var distance by remember(refTrip) { 
        mutableStateOf(existingTrip?.let { String.format(Locale.getDefault(), "%.1f", it.distanceMeters / 1000.0) } ?: copyTrip?.let { String.format(Locale.getDefault(), "%.1f", it.distanceMeters / 1000.0) } ?: "") 
    }
    val classificationRules by viewModel.classificationRules.collectAsState()
    val tripOptions = remember(classificationRules) {
        classificationRules.mapNotNull { it.tripType }.distinct().ifEmpty { 
            listOf("Home To Work", "Business Meeting", "Customer Visit", "Customer Billable", "Commissioned By CIMSOLUTIONS", "Exam Course", "Car Maintenance") 
        }
    }
    var tripType by remember(refTrip, tripOptions) { 
        mutableStateOf(if (refTrip?.tripType != null && tripOptions.contains(refTrip.tripType)) refTrip.tripType else "Home To Work") 
    }
    var typeExpanded by remember { mutableStateOf(false) }
    var note by remember(refTrip) { mutableStateOf(existingTrip?.note ?: copyTrip?.note ?: "") }
    var newOdometer by remember(refTrip) { 
        mutableStateOf(refTrip?.odometerEnd?.toString() ?: "") 
    }
    
    val startOdometer = remember(refTrip, selectedVehicle) {
        refTrip?.odometerStart ?: selectedVehicle?.odometerCurrent ?: 0
    }

    // Datum & Tijd
    val calendar = remember { Calendar.getInstance() }
    // Use refTrip so both edits and copies inherit the original time
    var selectedDateMillis by remember(refTrip) { mutableStateOf(refTrip?.startTime ?: System.currentTimeMillis()) }
    var selectedHour by remember(refTrip) { 
        if (refTrip != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = refTrip.startTime }
            mutableStateOf(cal.get(Calendar.HOUR_OF_DAY))
        } else {
            mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY))
        }
    }
    var selectedMinute by remember(refTrip) { 
        if (refTrip != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = refTrip.startTime }
            mutableStateOf(cal.get(Calendar.MINUTE))
        } else {
            mutableStateOf(calendar.get(Calendar.MINUTE))
        }
    }
    
    var durationMinutes by remember(refTrip) { 
        if (refTrip != null) {
            val end = refTrip.endTime ?: (refTrip.startTime)
            mutableStateOf(((end - refTrip.startTime) / (60 * 1000)).toString())
        } else {
            mutableStateOf("")
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("nl-NL"))
    val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))

    // Set initial vehicle
    LaunchedEffect(vehicles, refTrip) {
        if (selectedVehicle == null && vehicles.isNotEmpty()) {
            if (refTrip != null) {
                selectedVehicle = vehicles.find { it.id == refTrip.vehicleId }
            } else {
                selectedVehicle = vehicles.find { it.isDefault } ?: vehicles.first()
            }
        }
    }

    val frequentTrips = remember(allTrips) {
        allTrips
            .filter { !it.startAddress.isNullOrBlank() && !it.endAddress.isNullOrBlank() }
            .groupBy { Pair(it.startAddress, it.endAddress) }
            .map { it.value.first() to it.value.size }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    var initialLoad by remember { mutableStateOf(true) }
    var isCalculating by remember { mutableStateOf(false) }

    // (Removed flawed LaunchedEffect for auto-calculating distance)

    LaunchedEffect(startAddress, endAddress, savedAddresses) {
        if (initialLoad && refTrip != null) {
            initialLoad = false
            return@LaunchedEffect
        }
        initialLoad = false
        
        val actualStartLoc = AddressMatching.findSavedAddress(startAddress, savedAddresses)
        val actualEndLoc = AddressMatching.findSavedAddress(endAddress, savedAddresses)
        val endType = actualEndLoc?.defaultTripType
        val startType = actualStartLoc?.defaultTripType
        if (!endType.isNullOrBlank()) {
            tripType = endType
        } else if (!startType.isNullOrBlank()) {
            tripType = startType
        }

        if (newOdometer.isNotBlank()) {
            return@LaunchedEffect // Skip auto-calculate from addresses if odometer is filled
        }
        if (startAddress.length > 3 && endAddress.length > 3) {
            isCalculating = true
            kotlinx.coroutines.delay(1200) // debounce
            val actualStart = actualStartLoc?.address ?: startAddress
            val actualEnd = actualEndLoc?.address ?: endAddress
            val distAndDur = viewModel.calculateDistance(actualStart, actualEnd)
            if (distAndDur != null) {
                val distKm = distAndDur.first / 1000.0
                distance = String.format(Locale.getDefault(), "%.1f", distKm)
                if (startOdometer > 0) {
                    newOdometer = (startOdometer + distKm.toInt()).toString()
                }
                if (distAndDur.second > 0) {
                    val durationMs = (distAndDur.second * 1000).toLong()
                    durationMinutes = (durationMs / (60 * 1000)).toString()
                }
            }
            isCalculating = false
        } else {
            isCalculating = false
        }
    }

    fun getSelectedTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateMillis
        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(Calendar.MINUTE, selectedMinute)
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }
    
    fun getSelectedEndTimestamp(): Long {
        val duration = durationMinutes.toLongOrNull() ?: 0L
        return getSelectedTimestamp() + (duration * 60 * 1000)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (tripId == null) stringResource(R.string.new_trip) else stringResource(R.string.edit_trip), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val dist = distance.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val type = tripType
                            val actualStart = AddressMatching.findSavedAddress(startAddress, savedAddresses)?.address ?: startAddress
                            val actualEnd = AddressMatching.findSavedAddress(endAddress, savedAddresses)?.address ?: endAddress
                            
                            if (tripId == null) {
                                viewModel.addManualTrip(
                                    vehicleId = selectedVehicle?.id,
                                    startAddress = actualStart,
                                    endAddress = actualEnd,
                                    distanceKm = dist,
                                    type = type,
                                    startTime = getSelectedTimestamp(),
                                    endTime = getSelectedEndTimestamp(),
                                    note = note.takeIf { it.isNotBlank() }
                                )
                            } else {
                                viewModel.updateManualTrip(
                                    tripId = tripId,
                                    vehicleId = selectedVehicle?.id,
                                    startAddress = actualStart,
                                    endAddress = actualEnd,
                                    distanceKm = dist,
                                    type = type,
                                    startTime = getSelectedTimestamp(),
                                    endTime = getSelectedEndTimestamp(),
                                    note = note.takeIf { it.isNotBlank() }
                                )
                            }
                            onBack()
                        },
                        enabled = startAddress.isNotBlank() && endAddress.isNotBlank() && !isCalculating
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save), tint = if (startAddress.isNotBlank() && endAddress.isNotBlank() && !isCalculating) com.cimdriver.app.ui.theme.CIMDriverWhite else com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.5f))
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
            if (tripId == null && frequentTrips.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.frequent_trips), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(frequentTrips) { trip ->
                            androidx.compose.material3.AssistChip(
                                onClick = {
                                    startAddress = trip.startAddress ?: ""
                                    endAddress = trip.endAddress ?: ""
                                    tripType = trip.tripType
                                },
                                label = { Text("${trip.startAddress?.split(",")?.first()} ➔ ${trip.endAddress?.split(",")?.first()}") },
                                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            // === Datum & Tijd ===
            DateTimeSelectionCard(
                selectedDateMillis = selectedDateMillis,
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                onShowDatePicker = { showDatePicker = true },
                onShowTimePicker = { showTimePicker = true },
                dateFormat = dateFormat,
                timeFormat = timeFormat
            )

            // === Auto ===
            VehicleSelectionCard(
                selectedVehicle = selectedVehicle,
                vehicles = vehicles,
                vehicleExpanded = vehicleExpanded,
                onVehicleExpandedChange = { vehicleExpanded = it },
                onVehicleSelected = { selectedVehicle = it }
            )

            // === Route ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.route), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    var startAddressBookExpanded by remember { mutableStateOf(false) }
                    val isStartUnknown = startAddress.isNotBlank() && savedAddresses.none { it.address == startAddress }

                    AutoCompleteAddressField(
                        value = startAddress,
                        onValueChange = { startAddress = it },
                        label = stringResource(R.string.from_hint),
                        onSearchAddress = { query -> viewModel.searchAddress(query) },
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { startAddressBookExpanded = true }) {
                                    Icon(Icons.Filled.Contacts, contentDescription = stringResource(R.string.address_book))
                                }
                                DropdownMenu(expanded = startAddressBookExpanded, onDismissRequest = { startAddressBookExpanded = false }) {
                                    if (savedAddresses.isEmpty()) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.no_saved_addresses)) }, onClick = { startAddressBookExpanded = false })
                                    }
                                    savedAddresses.forEach { addr ->
                                        DropdownMenuItem(
                                            text = { Text(addr.label) },
                                            onClick = {
                                                startAddress = addr.address
                                                startAddressBookExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        supportingText = if (isStartUnknown) {
                            {
                                TextButton(
                                    onClick = {
                                        addressBookViewModel.addAddress(
                                            label = startAddress.split(",").firstOrNull() ?: startAddress,
                                            address = startAddress,
                                            isWorkLocation = false,
                                            isHomeLocation = false,
                                            isCustomerLocation = false,
                                            defaultTripType = null,
                                            projectCode = null,
                                            notes = null
                                        )
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.save_in_address_book))
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var endAddressBookExpanded by remember { mutableStateOf(false) }
                    val isEndUnknown = endAddress.isNotBlank() && savedAddresses.none { it.address == endAddress }

                    AutoCompleteAddressField(
                        value = endAddress,
                        onValueChange = { endAddress = it },
                        label = stringResource(R.string.to_hint),
                        onSearchAddress = { query -> viewModel.searchAddress(query) },
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { endAddressBookExpanded = true }) {
                                    Icon(Icons.Filled.Contacts, contentDescription = stringResource(R.string.address_book))
                                }
                                DropdownMenu(expanded = endAddressBookExpanded, onDismissRequest = { endAddressBookExpanded = false }) {
                                    if (savedAddresses.isEmpty()) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.no_saved_addresses)) }, onClick = { endAddressBookExpanded = false })
                                    }
                                    savedAddresses.forEach { addr ->
                                        DropdownMenuItem(
                                            text = { Text(addr.label) },
                                            onClick = {
                                                endAddress = addr.address
                                                endAddressBookExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        supportingText = if (isEndUnknown) {
                            {
                                TextButton(
                                    onClick = {
                                        addressBookViewModel.addAddress(
                                            label = endAddress.split(",").firstOrNull() ?: endAddress,
                                            address = endAddress,
                                            isWorkLocation = false,
                                            isHomeLocation = false,
                                            isCustomerLocation = false,
                                            defaultTripType = null,
                                            projectCode = null,
                                            notes = null
                                        )
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.save_in_address_book))
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newOdometer,
                        onValueChange = { 
                            newOdometer = it
                            val newOdoVal = it.toIntOrNull()
                            if (newOdoVal != null && startOdometer > 0) {
                                val diff = newOdoVal - startOdometer
                                if (diff >= 0) {
                                    distance = String.format(Locale.getDefault(), "%.1f", diff.toDouble())
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.end_odometer_optional)) },
                        supportingText = { Text(stringResource(R.string.start_odometer, startOdometer.toString())) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = distance,
                        onValueChange = { 
                            distance = it
                            val distKm = it.replace(",", ".").toDoubleOrNull()
                            if (distKm != null && startOdometer > 0) {
                                newOdometer = (startOdometer + distKm.toInt()).toString()
                            }
                        },
                        label = { Text(stringResource(R.string.distance_in_km)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isCalculating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text(stringResource(R.string.duration_min)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isCalculating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    )
                }
            }

            // === Type ===
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trip_type), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = tripType,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            label = { Text(stringResource(R.string.trip_type)) }
                        )
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            tripOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        tripType = selectionOption
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.note_optional)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            }

        }
    }

    // Date Picker Dialog
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

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.select_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
