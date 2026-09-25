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


import androidx.navigation.NavController
import com.cimdriver.app.ui.navigation.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage

@Composable
fun SettingsListItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit = {},
    navController: NavController,
    viewModel: com.cimdriver.app.ui.viewmodel.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    val themeMode = initialSettings.themeMode

    Scaffold(
        topBar = { 
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = { Text(stringResource(R.string.settings), color = com.cimdriver.app.ui.theme.CIMDriverWhite) }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Algemeen
            SettingsSectionHeader(title = "Algemeen")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.appearance), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
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

            // App Gedrag
            SettingsSectionHeader(title = "App Gedrag")
            SettingsListItem(
                title = "Rit & Classificatie",
                icon = Icons.Filled.Settings,
                onClick = { navController.navigate(TripClassificationSettingsRoute) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsListItem(
                title = "Tracking & Privacy",
                icon = Icons.Filled.Security,
                onClick = { navController.navigate(TrackingPrivacySettingsRoute) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsListItem(
                title = "Notificaties",
                icon = Icons.Filled.Notifications,
                onClick = { navController.navigate(NotificationSettingsRoute) }
            )

            // Werkuren
            SettingsSectionHeader(title = "Werkuren")
            SettingsListItem(
                title = "Werkdagen & Tijden",
                icon = Icons.Filled.DateRange,
                onClick = { navController.navigate(WorkDaysEditorRoute) }
            )

            // Diagnostiek & Systeem
            SettingsSectionHeader(title = "Diagnostiek & Systeem")
            SettingsListItem(
                title = "Data Beheer & Backup",
                icon = Icons.Filled.Storage,
                onClick = { navController.navigate(DataManagementRoute) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SettingsListItem(
                title = "App Diagnostiek",
                icon = Icons.Filled.Build,
                onClick = { navController.navigate(DiagnosticsRoute) }
            )

            // Over
            SettingsSectionHeader(title = "Over")
            SettingsListItem(
                title = "Over CIMDriver",
                icon = Icons.Filled.Info,
                onClick = { navController.navigate(AboutRoute) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}



