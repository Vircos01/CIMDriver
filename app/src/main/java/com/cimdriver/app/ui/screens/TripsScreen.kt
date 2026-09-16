package com.cimdriver.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import com.cimdriver.app.ui.components.DistanceStackedChart
import com.cimdriver.app.ui.components.WorkHoursBarChart
import com.cimdriver.app.ui.components.TripItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import androidx.compose.material3.ExperimentalMaterial3Api
import com.cimdriver.app.data.local.entity.BluetoothDevice as DbBluetoothDevice
import androidx.compose.foundation.combinedClickable
import android.os.PowerManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: TripsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    addressBookViewModel: com.cimdriver.app.ui.viewmodel.AddressBookViewModel = hiltViewModel(),
    onTripClick: (Trip) -> Unit = {},
    onAddTrip: () -> Unit = {}
) {
    val uiState by viewModel.tripsScreenUiState.collectAsState()
    
    val trips = uiState.filteredTrips
    val savedAddresses by addressBookViewModel.addresses.collectAsState()
    val classificationSettings = settingsViewModel.settings.collectAsState().value ?: com.cimdriver.app.data.local.entity.Settings()
    val classificationRules by settingsViewModel.classificationRules.collectAsState()
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var tripToClassify by remember { mutableStateOf<Trip?>(null) }
    
    val currentFilter = uiState.tripFilter
    val selectedYear = uiState.selectedYear
    val searchQuery = uiState.searchQuery
    val availableYears = uiState.availableYears
    val vehicles = uiState.vehicles
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTrips by remember { mutableStateOf(setOf<Trip>()) }
    val mergeSuggestion = uiState.mergeSuggestion
    val context = LocalContext.current

    var showExportMenu by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_ellipsis), color = com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                unfocusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                cursorColor = com.cimdriver.app.ui.theme.CIMDriverWhite
                            )
                        )
                    }
                    if (!isSearchVisible) {
                        Text(stringResource(R.string.trips), color = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    } else {
                        IconButton(onClick = { 
                            isSearchVisible = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    }
                    VehicleSelectorTopBarAction(
                        vehicles = vehicles,
                        activeVehicleId = uiState.globalSelectedVehicleId ?: vehicles.firstOrNull()?.id,
                        onVehicleSelected = { viewModel.setGlobalSelectedVehicleId(it) }
                    )
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.export), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Deel (CSV)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_export.csv", "text/csv"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportToCsv(context, uri, trips, savedAddresses, classificationRules)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deel (PDF)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_export.pdf", "application/pdf"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportToPdf(context, uri, trips, savedAddresses, classificationRules)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTrip() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_trip))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(modifier = Modifier.height(16.dp))

            // Action Bar for Merging & Batch Classify
            if (selectedTrips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${selectedTrips.size} geselecteerd", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var showBatchMenu by remember { mutableStateOf(false) }
                        
                        Box {
                            TextButton(onClick = { showBatchMenu = true }) {
                                Text(stringResource(R.string.batch_classify_as))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showBatchMenu,
                                onDismissRequest = { showBatchMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.business)) },
                                    onClick = {
                                        viewModel.batchUpdateTripTypes(selectedTrips.map { it.id }, "Customer Visit")
                                        selectedTrips = emptySet()
                                        showBatchMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.private_usage)) },
                                    onClick = {
                                        viewModel.batchUpdateTripTypes(selectedTrips.map { it.id }, "PERSONAL")
                                        selectedTrips = emptySet()
                                        showBatchMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.commute_short)) },
                                    onClick = {
                                        viewModel.batchUpdateTripTypes(selectedTrips.map { it.id }, "COMMUTE")
                                        selectedTrips = emptySet()
                                        showBatchMenu = false
                                    }
                                )
                            }
                        }
                        
                        if (selectedTrips.size == 2) {
                            IconButton(onClick = {
                                val list = selectedTrips.toList()
                                viewModel.mergeTrips(list[0], list[1])
                                selectedTrips = emptySet()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = stringResource(R.string.merge))
                            }
                        }
                        IconButton(onClick = { selectedTrips = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                }
            } else {
                // Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                            onClick = { viewModel.setTripFilter("ALLE") },
                            selected = currentFilter == "ALLE"
                        ) { Text(stringResource(R.string.all), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                            onClick = { viewModel.setTripFilter("BUSINESS") },
                            selected = currentFilter == "BUSINESS"
                        ) { Text(stringResource(R.string.business), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                            onClick = { viewModel.setTripFilter("COMMUTE") },
                            selected = currentFilter == "COMMUTE"
                        ) { Text(stringResource(R.string.commute_short), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                            onClick = { viewModel.setTripFilter("PRIVATE") },
                            selected = currentFilter == "PRIVATE"
                        ) { Text(stringResource(R.string.private_usage), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var showYearMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { showYearMenu = true },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("$selectedYear")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.select_year))
                            }
                            DropdownMenu(
                                expanded = showYearMenu,
                                onDismissRequest = { showYearMenu = false }
                            ) {
                                availableYears.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text("$year") },
                                        onClick = {
                                            viewModel.setSelectedYear(year)
                                            showYearMenu = false
                                        }
                                    )
                                }
                            }
                        }

                    }
                }
            }

            if (trips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            text = stringResource(R.string.no_trips_for_filter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (mergeSuggestion != null) {
                        val (older, newer) = mergeSuggestion
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.merge_trips_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL")) }
                                val olderStartTime = timeFormat.format(java.util.Date(older.startTime))
                                val olderEndTime = older.endTime?.let { timeFormat.format(java.util.Date(it)) } ?: "?"
                                val newerStartTime = timeFormat.format(java.util.Date(newer.startTime))
                                val newerEndTime = newer.endTime?.let { timeFormat.format(java.util.Date(it)) } ?: "?"

                                val olderText = "• $olderStartTime - $olderEndTime (${older.startAddress ?: "?"} ➔ ${older.endAddress ?: "?"})"
                                val newerText = "• $newerStartTime - $newerEndTime (${newer.startAddress ?: "?"} ➔ ${newer.endAddress ?: "?"})"

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.merge_trips_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(olderText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(newerText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { viewModel.rejectMerge(older, newer) }) {
                                        Text(stringResource(R.string.ignore))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { viewModel.acceptMerge(older, newer) }) {
                                        Text(stringResource(R.string.merge_action))
                                    }
                                }
                            }
                        }
                    }
                    
                    PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            delay(500)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(trips, key = { it.id }) { trip ->
                            val isSelected = selectedTrips.contains(trip)
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * .25f }
                            )

                            LaunchedEffect(dismissState.currentValue) {
                                when (dismissState.currentValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        tripToClassify = trip
                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        tripToDelete = trip
                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                    else -> {}
                                }
                            }

                            SwipeToDismissBox(
                                modifier = Modifier.animateItem(),
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color by androidx.compose.animation.animateColorAsState(
                                        when (direction) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                            else -> Color.Transparent
                                        }
                                    )
                                    val alignment = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }
                                    val icon = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        else -> Icons.Default.Delete
                                    }
                            
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp, horizontal = 16.dp)
                                            .background(color = color, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        if (direction != SwipeToDismissBoxValue.Settled) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            ) {
                                TripItem(
                                    trip = trip,
                                    savedAddresses = savedAddresses,
                                    classificationSettings = classificationSettings,
                                    classificationRules = classificationRules,
                                    onQuickReview = { reviewedTrip, tripType -> viewModel.reviewTrip(reviewedTrip, tripType) },
                                    isSelected = isSelected,
                                    onClick = { 
                                        if (selectedTrips.isNotEmpty()) {
                                            if (isSelected) selectedTrips = selectedTrips - trip else selectedTrips = selectedTrips + trip
                                        } else {
                                            onTripClick(trip)
                                        }
                                    },
                                    onLongClick = {
                                        if (isSelected) selectedTrips = selectedTrips - trip else selectedTrips = selectedTrips + trip
                                    },
                                    onDelete = { tripToDelete = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text(stringResource(R.string.delete_trip)) },
            text = { Text(stringResource(R.string.confirm_delete_trip_msg)) },
            confirmButton = {
                Button(onClick = {
                    tripToDelete?.let { viewModel.deleteTrip(it) }
                    tripToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (tripToClassify != null) {
        AlertDialog(
            onDismissRequest = { tripToClassify = null },
            title = { Text("Rit Classificeren") },
            text = {
                Column {
                    Text("Hoe wil je deze rit classificeren?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            tripToClassify?.let { viewModel.reviewTrip(it, "Customer Visit") }
                            tripToClassify = null
                        }
                    ) { Text(stringResource(R.string.business)) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            tripToClassify?.let { viewModel.reviewTrip(it, "COMMUTE") }
                            tripToClassify = null
                        }
                    ) { Text(stringResource(R.string.commute_short)) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            tripToClassify?.let { viewModel.reviewTrip(it, "PERSONAL") }
                            tripToClassify = null
                        }
                    ) { Text(stringResource(R.string.private_usage)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { tripToClassify = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

