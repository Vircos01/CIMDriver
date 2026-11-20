package com.cimdriver.app.ui.screens

import com.cimdriver.app.domain.DashboardStatsCalculator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import com.cimdriver.app.ui.theme.CIMDriverGreen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.LocalGasStation
import com.cimdriver.app.ui.components.DistanceStackedChart
import com.cimdriver.app.ui.components.WorkHoursBarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cimdriver.app.ui.components.DashboardPermissionsSection
import com.cimdriver.app.ui.components.core.CimDriverTopBar
import com.cimdriver.app.ui.components.core.TopBarNavType
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


@Composable
fun DashboardScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: TripsViewModel = hiltViewModel(),
    workHoursViewModel: com.cimdriver.app.ui.viewmodel.WorkHoursViewModel = hiltViewModel(),
    settingsViewModel: com.cimdriver.app.ui.viewmodel.SettingsViewModel = hiltViewModel(),
    updateViewModel: com.cimdriver.app.ui.viewmodel.UpdateViewModel = hiltViewModel(),
    hoursTargetViewModel: com.cimdriver.app.ui.viewmodel.HoursTargetViewModel = hiltViewModel(),
    vehiclesViewModel: com.cimdriver.app.ui.viewmodel.VehiclesViewModel = hiltViewModel(),
    fuelViewModel: com.cimdriver.app.ui.viewmodel.FuelViewModel = hiltViewModel(),
    onNavigateToTrips: () -> Unit = {},
    onNavigateToWorkHours: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToGaps: (Long) -> Unit = {},
    onNavigateToFuelDraft: (Long) -> Unit = {},
    onNavigateToHoursTargets: () -> Unit = {},
    onNavigateToEditVehicle: (Long) -> Unit = {}
) {
    val locale = currentAppLocale()
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

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundLocation = isGranted
    }

    val uiState by viewModel.dashboardUiState.collectAsState()
    val stats = uiState.dashboardStats
    val timeFilter = uiState.dashboardTimeFilter
    val reviewCount = uiState.tripsToReviewCount
    val workDayReviewCount by workHoursViewModel.workDaysToReviewCount.collectAsState()
    val workHoursStats by workHoursViewModel.currentMonthWorkHours.collectAsState()
    val vehicles = uiState.vehicles
    val distanceChartData = uiState.dashboardDistanceChartData
    val categoryBreakdown = uiState.dashboardCategoryBreakdown
    val workHoursChartData by workHoursViewModel.dashboardWorkHoursChartData.collectAsState()
    val trackingStatus by TrackingStatusStore.status.collectAsState()
    val openGapsCount by vehiclesViewModel.openGapsCount.collectAsState(initial = 0)
    val defaultVehicle = remember(vehicles) { vehicles.find { it.isDefault } ?: vehicles.firstOrNull() }
    val settings by settingsViewModel.settings.collectAsState()
    var showTrackingStatusDialog by remember { mutableStateOf(false) }

    val updateInfo by updateViewModel.updateInfo.collectAsState()
    val allTrips by viewModel.trips.collectAsState()
    val targetProgresses by hoursTargetViewModel.targetProgresses.collectAsState()
    val allFillUps by fuelViewModel.fillUps.collectAsState()
    val pendingFuelDrafts = remember(allFillUps) { allFillUps.filter { it.status == "DRAFT" } }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }

    val reportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val period = when (timeFilter) {
                TimeFilter.WEEK -> context.getString(R.string.period_this_week)
                TimeFilter.MONTH -> context.getString(R.string.period_this_month)
                TimeFilter.YEAR -> context.getString(R.string.period_this_year)
            }
            com.cimdriver.app.util.ExportUtil.exportSummaryCsv(context, uri, period, stats, workHoursStats)
        }
    }
    val reportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val period = when (timeFilter) {
                TimeFilter.WEEK -> context.getString(R.string.period_this_week)
                TimeFilter.MONTH -> context.getString(R.string.period_this_month)
                TimeFilter.YEAR -> context.getString(R.string.period_this_year)
            }
            com.cimdriver.app.util.ExportUtil.exportSummaryPdf(context, uri, period, stats, workHoursStats)
        }
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

    val allGranted = hasFineLocation && hasBackgroundLocation && hasNotificationPermission && isIgnoringBattery && hasBluetoothPermission
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val trackingIconColor = when {
        trackingStatus.isActive -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        allGranted -> androidx.compose.ui.graphics.Color(0xFF1976D2)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            CimDriverTopBar(
                title = stringResource(R.string.dashboard),
                navType = TopBarNavType.MENU,
                onNavClick = onOpenDrawer,
                actions = {
                    VehicleSelectorTopBarAction(
                        vehicles = vehicles,
                        activeVehicleId = uiState.globalSelectedVehicleId ?: vehicles.firstOrNull()?.id,
                        onVehicleSelected = { viewModel.setGlobalSelectedVehicleId(it) }
                    )
                    IconButton(onClick = { showTrackingStatusDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.tracking_status),
                            tint = trackingIconColor
                        )
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (updateInfo.isUpdateAvailable && updateInfo.latestVersionCode > (settings?.skippedUpdateVersionCode ?: 0)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nieuwe versie beschikbaar: ${updateInfo.latestVersionName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateInfo.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { settingsViewModel.skipUpdate(updateInfo.latestVersionCode) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Overslaan")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo.downloadUrl))
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Update Nu")
                            }
                        }
                    }
                }
            }

            if (allGranted) {
                // Systeem- & Batterijgezondheid monitor
                com.cimdriver.app.ui.components.HealthIssueBanner(
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Dagelijkse Uitzonderingen Inbox (Triage met 1-tap acceptatie & confidence)
                val reviewCandidates by viewModel.tripsToReviewCandidates.collectAsState()
                val workDayReviewCandidates by workHoursViewModel.workDayReviewCandidates.collectAsState()
                
                val calendar = remember { java.util.Calendar.getInstance() }
                val dayOfMonth = remember { calendar.get(java.util.Calendar.DAY_OF_MONTH) }
                val currentMonthKey = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(calendar.time) }
                val closingPrefs = remember { context.getSharedPreferences("monthly_closing_prefs", Context.MODE_PRIVATE) }
                var isClosingBannerDismissed by remember {
                    mutableStateOf(closingPrefs.getString("banner_dismissed_month", null) == currentMonthKey)
                }

                if (dayOfMonth <= 7 && !isClosingBannerDismissed) {
                    val prevCal = remember {
                        java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
                    }
                    val prevMonthName = remember {
                        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.forLanguageTag("nl-NL")).format(prevCal.time)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.forLanguageTag("nl-NL")) else it.toString() }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Fiscale Maandafsluiting gereed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        closingPrefs.edit().putString("banner_dismissed_month", currentMonthKey).apply()
                                        isClosingBannerDismissed = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Sluiten",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "De rittenadministratie van $prevMonthName staat klaar voor controle en export naar je boekhouder of salarisadministratie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    com.cimdriver.app.service.MonthlyClosingStore.requestClosing(prevMonthName)
                                    onNavigateToTrips()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Controleren & Delen ($prevMonthName)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                com.cimdriver.app.ui.components.DailyInboxCard(
                    reviewCandidates = reviewCandidates,
                    workDayReviewCount = workDayReviewCount,
                    workDayReviewCandidates = workDayReviewCandidates,
                    openGapsCount = openGapsCount,
                    pendingFuelDrafts = pendingFuelDrafts,
                    vehicles = vehicles,
                    onQuickApproveTrip = { candidate ->
                        viewModel.quickApproveTrip(candidate)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Rit goedgekeurd",
                                actionLabel = "Ongedaan maken",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.undoLastApproval()
                            }
                        }
                    },
                    onQuickApproveWorkDay = { candidate ->
                        workHoursViewModel.quickApproveWorkDay(candidate)
                        coroutineScope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Werkdag goedgekeurd",
                                actionLabel = "Ongedaan maken",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                workHoursViewModel.undoLastApproval()
                            }
                        }
                    },
                    onApproveAllHighConfidence = {
                        val highConfTripsCount = reviewCandidates.count { it.assessment.isHighConfidence }
                        val highConfWorkDaysCount = workDayReviewCandidates.count { it.suggestion.isHighConfidence }
                        viewModel.approveAllHighConfidenceTrips(reviewCandidates)
                        workHoursViewModel.approveAllHighConfidenceWorkDays(workDayReviewCandidates)
                        coroutineScope.launch {
                            val totalApproved = highConfTripsCount + highConfWorkDaysCount
                            val res = snackbarHostState.showSnackbar(
                                message = "$totalApproved items goedgekeurd",
                                actionLabel = "Ongedaan maken",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                viewModel.undoLastApproval()
                                workHoursViewModel.undoLastApproval()
                            }
                        }
                    },
                    onNavigateToTrips = onNavigateToTrips,
                    onNavigateToWorkHours = onNavigateToWorkHours,
                    onNavigateToGaps = { defaultVehicle?.let { onNavigateToGaps(it.id) } },
                    onNavigateToFuelDraft = onNavigateToFuelDraft
                )

                if (defaultVehicle != null && defaultVehicle.lastOdometerCheckTimestamp > 0) {
                    val daysSinceCheck = ((System.currentTimeMillis() - defaultVehicle.lastOdometerCheckTimestamp) / (1000 * 60 * 60 * 24)).toInt()
                    if (daysSinceCheck >= 30) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Km-controle aanbevolen", style = MaterialTheme.typography.titleMedium, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Laatste km-controle van ${defaultVehicle.name} was $daysSinceCheck dagen geleden. Voer een controle uit om de keten sluitend te houden.", textAlign = TextAlign.Center, color = Color(0xFF92400E))
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { com.cimdriver.app.service.OdometerCheckStore.requestCheck(defaultVehicle.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD97706),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Km-stand controleren")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                val activeVehicleId = uiState.globalSelectedVehicleId ?: vehicles.firstOrNull()?.id
                val activeVehicle = vehicles.find { it.id == activeVehicleId }

                if (activeVehicle?.showPrivateKmWarning == true) {
                    val limit = DashboardStatsCalculator.calculateCombinedProRataLimit(
                        vehicle = activeVehicle,
                        allVehicles = vehicles,
                        yearlyLimit = activeVehicle.privateKmYearlyLimit
                    )
                    val current = stats.ytdPriveKm.toFloat()
                    val progress = (current / limit.toFloat()).coerceIn(0f, 1f)
                    
                    val progressColor = when {
                        progress == 0f -> androidx.compose.ui.graphics.Color.Transparent
                        progress >= 1f -> MaterialTheme.colorScheme.error
                        progress >= 0.8f -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.DirectionsCar,
                                        contentDescription = "Privé",
                                        tint = if (progress >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Privégebruik (YTD)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f / %d km", current, limit),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            val predecessors = remember(activeVehicle, vehicles) {
                                DashboardStatsCalculator.getPredecessorVehicles(activeVehicle, vehicles)
                            }
                            if (predecessors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gezamenlijke fiscale norm (privé km van ${predecessors.joinToString(", ") { "${it.make} ${it.model}" }} overgedragen).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (activeVehicle.inServiceDate != null || activeVehicle.endServiceDate != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Aangepaste grens (pro rata).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (stats.projectedYearEndPriveKm > 0.0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val isProjectedOver = stats.projectedYearEndPriveKm > limit
                                val projKm = String.format(Locale.getDefault(), "%.0f km", stats.projectedYearEndPriveKm)
                                Text(
                                    text = if (isProjectedOver) "⚠️ Verwachte eindstand 31 dec: $projKm (dreigt grens te overschrijden!)"
                                           else "✨ Verwachte eindstand 31 dec: $projKm (veilig binnen de grens)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isProjectedOver) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { 
                                viewModel.setDashboardTimeFilter(TimeFilter.WEEK)
                                workHoursViewModel.setDashboardTimeFilter(TimeFilter.WEEK)
                            },
                            selected = timeFilter == TimeFilter.WEEK
                        ) { Text(stringResource(R.string.week)) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { 
                                viewModel.setDashboardTimeFilter(TimeFilter.MONTH)
                                workHoursViewModel.setDashboardTimeFilter(TimeFilter.MONTH)
                            },
                            selected = timeFilter == TimeFilter.MONTH
                        ) { Text(stringResource(R.string.month)) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { 
                                viewModel.setDashboardTimeFilter(TimeFilter.YEAR)
                                workHoursViewModel.setDashboardTimeFilter(TimeFilter.YEAR)
                            },
                            selected = timeFilter == TimeFilter.YEAR
                        ) { Text(stringResource(R.string.year)) }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val filterText = when(timeFilter) {
                    TimeFilter.WEEK -> context.getString(R.string.period_this_week)
                    TimeFilter.MONTH -> context.getString(R.string.period_this_month)
                    TimeFilter.YEAR -> context.getString(R.string.period_this_year)
                }
                Text(text = stringResource(R.string.stats_period, filterText), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.business), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = String.format(locale, "%.1f km", stats.zakelijkKm), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            
                            val zakelijkTrend = if (stats.prevZakelijkKm > 0) ((stats.zakelijkKm - stats.prevZakelijkKm) / stats.prevZakelijkKm) * 100 else 0.0
                            if (stats.prevZakelijkKm > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (zakelijkTrend >= 0) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (zakelijkTrend >= 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = String.format(locale, "%.1f%%", Math.abs(zakelijkTrend)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (zakelijkTrend >= 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                    )
                                }
                            }
                            
                            if (stats.woonWerkKm > 0.0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Commute,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFF0288D1),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = String.format(locale, "%.1f km", stats.woonWerkKm), style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF0288D1))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val compensationRate = settings?.businessCompensation ?: 0.23f
                            val vergoeding = (stats.zakelijkKm + stats.woonWerkKm) * compensationRate
                            Text(text = String.format(locale, "€ %.2f", vergoeding), style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFF2E7D32))
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            val zakH = stats.zakelijkMin / 60
                            val zakM = stats.zakelijkMin % 60
                            val wwH = stats.woonWerkMin / 60
                            val wwM = stats.woonWerkMin % 60
                            
                            Text(text = if (zakH > 0) "${zakH}u ${zakM}m" else "${zakM}m", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (stats.woonWerkMin > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Commute,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFF0288D1),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (wwH > 0) "${wwH}u ${wwM}m" else "${wwM}m", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF0288D1))
                                }
                            }
                        }
                    }
                    Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.private_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = String.format(locale, "%.1f km", stats.priveKm), style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color(0xFFF57C00))
                            
                            val priveTrend = if (stats.prevPriveKm > 0) ((stats.priveKm - stats.prevPriveKm) / stats.prevPriveKm) * 100 else 0.0
                            if (stats.prevPriveKm > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (priveTrend >= 0) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (priveTrend >= 0) androidx.compose.ui.graphics.Color(0xFFD32F2F) else androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = String.format(locale, "%.1f%%", Math.abs(priveTrend)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (priveTrend >= 0) androidx.compose.ui.graphics.Color(0xFFD32F2F) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            val privH = stats.priveMin / 60
                            val privM = stats.priveMin % 60
                            
                            // To align better with the left card, we can use spacer if there's no w/w
                            if (stats.woonWerkKm > 0.0 || stats.prevZakelijkKm > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            Text(text = if (privH > 0) "${privH}u ${privM}m" else "${privM}m", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }


                DistanceStackedChart(data = distanceChartData)
                
                if (categoryBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Rittypes (Zakelijk & Woon-werk)", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.cimdriver.app.ui.components.CategoryPieChart(data = categoryBreakdown)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.worked_hours), style = MaterialTheme.typography.titleMedium)
                        Text(text = String.format(locale, "%.1f uur", workHoursStats), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    WorkHoursBarChart(data = workHoursChartData)
                }
                
                if (targetProgresses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHoursTargets() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Targets", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onNavigateToHoursTargets) {
                            Text("Beheren", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val defaultColor = MaterialTheme.colorScheme.primary
                    targetProgresses.forEach { progress ->
                        val target = progress.targetDetails.target
                        val barColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(target.color)) } catch (e: Exception) { defaultColor }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { onNavigateToHoursTargets() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(target.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${String.format("%.1f", progress.percentage * 100)}%", style = MaterialTheme.typography.titleMedium, color = barColor)
                                }
                                
                                val subtitleParts = mutableListOf<String>()
                                progress.targetDetails.clientName?.let { subtitleParts.add(it) }
                                progress.targetDetails.projectCodeStr?.let { subtitleParts.add(it) }
                                if (subtitleParts.isEmpty()) subtitleParts.add("Alle uren")
                                
                                Text(subtitleParts.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                LinearProgressIndicator(
                                    progress = { progress.percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (target.targetType == "REVENUE") {
                                        Text(String.format(locale, "€ %,.2f", progress.accumulatedRevenue), style = MaterialTheme.typography.bodyMedium)
                                        Text("Doel: " + String.format(locale, "€ %,.2f", progress.effectiveTargetValue), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text(String.format(locale, "%,.1f uur", progress.accumulatedHours), style = MaterialTheme.typography.bodyMedium)
                                        Text("Doel: " + String.format(locale, "%,.1f uur", progress.effectiveTargetValue), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                
                val activeVehicles = remember(vehicles) { vehicles.filter { it.isActive } }
                if (activeVehicles.size >= 2) {
                    Spacer(modifier = Modifier.height(24.dp))
                    MultiVehicleFleetCard(
                        vehicles = activeVehicles,
                        allTrips = allTrips,
                        locale = locale,
                        onEditVehicle = onNavigateToEditVehicle
                    )
                } else if (activeVehicles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(R.string.odometer_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    activeVehicles.forEach { vehicle ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "${vehicle.make} ${vehicle.model}", style = MaterialTheme.typography.titleMedium)
                                    Text(text = vehicle.licensePlate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(locale, "%,d km", vehicle.odometerCurrent).replace(',', '.'), 
                                        style = MaterialTheme.typography.titleLarge, 
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (vehicle.lastOdometerCheckTimestamp > 0) {
                                        val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy", locale).format(java.util.Date(vehicle.lastOdometerCheckTimestamp))
                                        Text(
                                            text = stringResource(R.string.odometer_last_checked, dateStr), 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                DashboardPermissionsSection(
                    hasFineLocation = hasFineLocation,
                    hasBackgroundLocation = hasBackgroundLocation,
                    hasNotificationPermission = hasNotificationPermission,
                    hasBluetoothPermission = hasBluetoothPermission,
                    isIgnoringBattery = isIgnoringBattery,
                    locationPermissionLauncher = locationPermissionLauncher,
                    notificationLauncher = notificationLauncher,
                    backgroundLocationLauncher = backgroundLocationLauncher,
                    bluetoothLauncher = bluetoothLauncher,
                    batteryLauncher = batteryLauncher
                )
            }
        }
    }

    if (showTrackingStatusDialog) {
        val gpsAccuracy = trackingStatus.gpsAccuracyMeters
        AlertDialog(
            onDismissRequest = { showTrackingStatusDialog = false },
            title = { Text(if (trackingStatus.isActive) stringResource(R.string.tracking_active) else if (allGranted) stringResource(R.string.tracking_ready) else stringResource(R.string.tracking_not_ready)) },
            text = {
                Column {
                    Text(trackingStatus.message)
                    if (trackingStatus.isActive && gpsAccuracy != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.gps_accuracy, gpsAccuracy.toInt().toString()))
                    }
                    if (!trackingStatus.isActive && allGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.tracking_standby_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!allGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.grant_permissions_desc))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackingStatusDialog = false }) { Text(stringResource(R.string.close)) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTrackingStatusDialog = false
                    onNavigateToDiagnostics()
                }) { Text(stringResource(R.string.system_diagnostics)) }
            }
        )
    }

}

@Composable
private fun MultiVehicleFleetCard(
    vehicles: List<com.cimdriver.app.data.local.entity.Vehicle>,
    allTrips: List<com.cimdriver.app.data.local.entity.Trip>,
    locale: java.util.Locale,
    onEditVehicle: (Long) -> Unit = {}
) {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    val startOfMonth = calendar.timeInMillis

    calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
    val startOfYear = calendar.timeInMillis

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wagenpark Overzicht",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${vehicles.size} voertuigen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            vehicles.forEachIndexed { index, vehicle ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }

                val monthTrips = remember(allTrips, vehicle.id, startOfMonth) {
                    allTrips.filter { it.vehicleId == vehicle.id && it.startTime >= startOfMonth }
                }
                val monthKm = remember(monthTrips) {
                    monthTrips.sumOf { it.distanceMeters } / 1000.0
                }
                val yearPrivateTrips = remember(allTrips, vehicle.id, startOfYear) {
                    allTrips.filter {
                        it.vehicleId == vehicle.id && it.startTime >= startOfYear &&
                            (it.tripType == "PRIVATE" || it.tripType == com.cimdriver.app.util.TripClassification.DYNAMICS_PERSONAL)
                    }
                }
                val yearPrivateKm = remember(yearPrivateTrips) {
                    yearPrivateTrips.sumOf { it.distanceMeters + it.privateDetourMeters } / 1000.0
                }

                // Check trade-in relationships
                val successor = remember(vehicle, vehicles) { vehicles.find { it.replacesVehicleId == vehicle.id } }
                val predecessors = remember(vehicle, vehicles) { com.cimdriver.app.domain.DashboardStatsCalculator.getPredecessorVehicles(vehicle, vehicles) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditVehicle(vehicle.id) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${vehicle.make} ${vehicle.model}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFD54F),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                                ) {
                                    Text(
                                        text = vehicle.licensePlate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                if (vehicle.isDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Standaard",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (successor != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Ingeruild voor ${successor.make} ${successor.model}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (predecessors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "Vervangt ${predecessors.first().make} ${predecessors.first().model}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(locale, "%,d km", vehicle.odometerCurrent).replace(',', '.'),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${monthTrips.size} ritten (${String.format(locale, "%.0f", monthKm)} km) deze mnd",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (successor != null) {
                        // Vehicle was replaced / traded in: km are transferred to successor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Privé gereden dit jaar:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(locale, "%.1f", yearPrivateKm)} km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Overgedragen naar ${successor.make} ${successor.model} (${successor.licensePlate})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (predecessors.isNotEmpty()) {
                        // Successor vehicle: combine private km and joint limit
                        val predecessorPrivateKm = remember(predecessors, allTrips, startOfYear) {
                            val predIds = predecessors.map { it.id }.toSet()
                            allTrips.filter {
                                it.vehicleId in predIds && it.startTime >= startOfYear &&
                                    (it.tripType == "PRIVATE" || it.tripType == com.cimdriver.app.util.TripClassification.DYNAMICS_PERSONAL)
                            }.sumOf { it.distanceMeters + it.privateDetourMeters } / 1000.0
                        }
                        val totalCombinedPrivateKm = yearPrivateKm + predecessorPrivateKm
                        val combinedLimit = remember(vehicle, vehicles) {
                            com.cimdriver.app.domain.DashboardStatsCalculator.calculateCombinedProRataLimit(
                                vehicle = vehicle,
                                allVehicles = vehicles,
                                yearlyLimit = if (vehicle.privateKmYearlyLimit > 0) vehicle.privateKmYearlyLimit else 500
                            )
                        }
                        val maxStandardLimit = if (vehicle.privateKmYearlyLimit > 0) vehicle.privateKmYearlyLimit else 500
                        val isProRata = combinedLimit < maxStandardLimit
                        val privateProgress = if (combinedLimit > 0) (totalCombinedPrivateKm / combinedLimit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
                        val warningThreshold = combinedLimit * 0.8
                        val statusColor = when {
                            totalCombinedPrivateKm > combinedLimit -> Color(0xFFE53935)
                            totalCombinedPrivateKm >= warningThreshold -> Color(0xFFFFB300)
                            else -> CIMDriverGreen
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Privé dit jaar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isProRata) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(naar rato)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format(locale, "%.1f", totalCombinedPrivateKm)} / $combinedLimit km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        if (predecessorPrivateKm > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "(${String.format(locale, "%.1f", yearPrivateKm)} km eigen + ${String.format(locale, "%.1f", predecessorPrivateKm)} km ${predecessors.first().make})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { privateProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusHint = when {
                            totalCombinedPrivateKm > combinedLimit -> "Fiscaal bijtellingsrisico (overschreden)"
                            totalCombinedPrivateKm >= warningThreshold -> {
                                val remaining = (combinedLimit - totalCombinedPrivateKm).toInt().coerceAtLeast(0)
                                "Nog $remaining km tot fiscale bijtellingsgrens"
                            }
                            else -> if (isProRata) "Veilig binnen $combinedLimit km privé-norm (pro rata incl. inruil)" else "Veilig binnen $combinedLimit km privé-norm (incl. inruil)"
                        }
                        Text(
                            text = statusHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor.copy(alpha = 0.9f)
                        )
                    } else {
                        // Standalone vehicle (e.g. Ford Kuga)
                        val proRataLimit = remember(vehicle) {
                            com.cimdriver.app.domain.DashboardStatsCalculator.calculateProRataLimit(
                                yearlyLimit = if (vehicle.privateKmYearlyLimit > 0) vehicle.privateKmYearlyLimit else 500,
                                inServiceDate = vehicle.inServiceDate,
                                endServiceDate = vehicle.endServiceDate
                            )
                        }
                        val isProRata = vehicle.inServiceDate != null || vehicle.endServiceDate != null
                        val privateProgress = if (proRataLimit > 0) (yearPrivateKm / proRataLimit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
                        val warningThreshold = proRataLimit * 0.8
                        val statusColor = when {
                            yearPrivateKm > proRataLimit -> Color(0xFFE53935)
                            yearPrivateKm >= warningThreshold -> Color(0xFFFFB300)
                            else -> CIMDriverGreen
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Privé dit jaar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isProRata) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(naar rato)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format(locale, "%.1f", yearPrivateKm)} / $proRataLimit km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { privateProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusHint = when {
                            yearPrivateKm > proRataLimit -> "Fiscaal bijtellingsrisico (overschreden)"
                            yearPrivateKm >= warningThreshold -> {
                                val remaining = (proRataLimit - yearPrivateKm).toInt().coerceAtLeast(0)
                                "Nog $remaining km tot fiscale bijtellingsgrens"
                            }
                            else -> if (isProRata) "Veilig binnen $proRataLimit km privé-norm (pro rata)" else "Veilig binnen $proRataLimit km privé-norm"
                        }
                        Text(
                            text = statusHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
