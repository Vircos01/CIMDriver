package com.cimdriver.app.ui.screens

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.cimdriver.app.ui.components.DistanceStackedChart
import com.cimdriver.app.ui.components.WorkHoursBarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cimdriver.app.ui.components.DashboardPermissionsSection
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
    onNavigateToTrips: () -> Unit = {},
    onNavigateToWorkHours: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {}
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
    val settings by settingsViewModel.settings.collectAsState()
    var showTrackingStatusDialog by remember { mutableStateOf(false) }

    val updateInfo by updateViewModel.updateInfo.collectAsState()

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
    val trackingIconColor = when {
        trackingStatus.isActive -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        allGranted -> androidx.compose.ui.graphics.Color(0xFF1976D2)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        topBar = { 
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = { Text(stringResource(R.string.dashboard), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
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
                if (reviewCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.action_required), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.trips_to_review, reviewCount), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToTrips,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                    contentColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(stringResource(R.string.view_trips))
                            }
                        }
                    }
                }

                if (workDayReviewCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.check_work_hours), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.work_days_to_review, workDayReviewCount), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToWorkHours,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Text(stringResource(R.string.view_work_hours))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                val activeVehicleId = uiState.globalSelectedVehicleId ?: vehicles.firstOrNull()?.id
                val activeVehicle = vehicles.find { it.id == activeVehicleId }

                if (activeVehicle?.showPrivateKmWarning == true) {
                    val limit = activeVehicle.privateKmYearlyLimit
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
                
                if (vehicles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(R.string.odometer_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    vehicles.filter { it.isActive }.forEach { vehicle ->
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
