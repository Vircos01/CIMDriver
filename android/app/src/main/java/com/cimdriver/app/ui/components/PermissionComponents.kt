package com.cimdriver.app.ui.components

import android.os.Build
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cimdriver.app.R

enum class PermissionStatus {
    Granted, Requestable, NeedsRationale, Blocked
}

data class PermissionUiState(
    val status: PermissionStatus,
    val title: String,
    val description: String,
    val importance: String
)

class PermissionStateWrapper(
    val status: PermissionStatus,
    val markRequested: () -> Unit,
    val launchSettings: () -> Unit
)

@Composable
fun rememberPermissionState(permission: String): PermissionStateWrapper {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val prefs = remember { context.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    var shouldShowRationale by remember { mutableStateOf(activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false) }
    var hasBeenRequested by remember { mutableStateOf(prefs.getBoolean("req_$permission", false)) }
    
    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                shouldShowRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
                hasBeenRequested = prefs.getBoolean("req_$permission", false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val status = when {
        isGranted -> PermissionStatus.Granted
        shouldShowRationale -> PermissionStatus.NeedsRationale
        hasBeenRequested -> PermissionStatus.Blocked
        else -> PermissionStatus.Requestable
    }
    
    val markRequested = {
        prefs.edit().putBoolean("req_$permission", true).apply()
        hasBeenRequested = true
    }
    
    val launchSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    return PermissionStateWrapper(status, markRequested, launchSettings)
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PermissionChecklistItem(
    uiState: PermissionUiState,
    isEnabled: Boolean = true,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }

    if (showRationale) {
        PermissionRationaleDialog(
            title = uiState.title,
            description = uiState.description,
            onConfirm = {
                showRationale = false
                onRequest()
            },
            onDismiss = { showRationale = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.status == PermissionStatus.Granted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.status == PermissionStatus.Granted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (uiState.status == PermissionStatus.Granted) "✅ " else "❌ ", style = MaterialTheme.typography.titleMedium)
                    Text(text = uiState.title, style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.importance.isNotEmpty()) {
                    Text(
                        text = uiState.importance, 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = uiState.description, style = MaterialTheme.typography.bodySmall)
            }
            if (uiState.status != PermissionStatus.Granted) {
                Button(
                    onClick = {
                        when (uiState.status) {
                            PermissionStatus.Requestable -> onRequest()
                            PermissionStatus.NeedsRationale -> showRationale = true
                            PermissionStatus.Blocked -> onOpenSettings()
                            PermissionStatus.Granted -> {}
                        }
                    }, 
                    enabled = isEnabled
                ) {
                    Text(
                        when (uiState.status) {
                            PermissionStatus.NeedsRationale -> "Waarom nodig?"
                            PermissionStatus.Blocked -> "Open instellingen"
                            else -> stringResource(R.string.allow)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardPermissionsSection(
    hasFineLocation: Boolean,
    hasBackgroundLocation: Boolean,
    hasNotificationPermission: Boolean,
    hasBluetoothPermission: Boolean,
    isIgnoringBattery: Boolean,
    locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    notificationLauncher: ManagedActivityResultLauncher<String, Boolean>,
    backgroundLocationLauncher: ManagedActivityResultLauncher<String, Boolean>,
    bluetoothLauncher: ManagedActivityResultLauncher<String, Boolean>,
    batteryLauncher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>
) {
    Text(text = stringResource(R.string.permissions_needed_desc), textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))

    // 1. Fine Location
    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    PermissionChecklistItem(
        uiState = PermissionUiState(
            status = if (hasFineLocation) PermissionStatus.Granted else fineLocationState.status,
            title = stringResource(R.string.permission_location_title),
            description = stringResource(R.string.permission_location_desc),
            importance = "Vereist"
        ),
        onRequest = {
            fineLocationState.markRequested()
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        },
        onOpenSettings = fineLocationState.launchSettings
    )
    
    Spacer(modifier = Modifier.height(8.dp))

    // 2. Notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        PermissionChecklistItem(
            uiState = PermissionUiState(
                status = if (hasNotificationPermission) PermissionStatus.Granted else notificationState.status,
                title = stringResource(R.string.permission_notifications_title),
                description = stringResource(R.string.permission_notifications_desc),
                importance = "Voor betrouwbaarheid"
            ),
            onRequest = {
                notificationState.markRequested()
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onOpenSettings = notificationState.launchSettings
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // 3. Background Location
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val bgLocationState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        PermissionChecklistItem(
            uiState = PermissionUiState(
                status = if (hasBackgroundLocation) PermissionStatus.Granted else bgLocationState.status,
                title = stringResource(R.string.permission_background_title),
                description = stringResource(R.string.permission_background_desc),
                importance = "Aanbevolen voor automatische ritten"
            ),
            isEnabled = hasFineLocation,
            onRequest = {
                bgLocationState.markRequested()
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            },
            onOpenSettings = bgLocationState.launchSettings
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // 4. Bluetooth
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val bluetoothState = rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
        PermissionChecklistItem(
            uiState = PermissionUiState(
                status = if (hasBluetoothPermission) PermissionStatus.Granted else bluetoothState.status,
                title = stringResource(R.string.permission_bluetooth_title),
                description = stringResource(R.string.permission_bluetooth_desc),
                importance = "Aanbevolen voor bluetooth auto-start"
            ),
            onRequest = {
                bluetoothState.markRequested()
                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            },
            onOpenSettings = bluetoothState.launchSettings
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // 5. Battery Optimization (no runtime permission state machine, so we mock it)
    val context = LocalContext.current
    PermissionChecklistItem(
        uiState = PermissionUiState(
            status = if (isIgnoringBattery) PermissionStatus.Granted else PermissionStatus.Requestable,
            title = stringResource(R.string.permission_battery_title),
            description = stringResource(R.string.permission_battery_desc),
            importance = "Voor betrouwbaarheid"
        ),
        onRequest = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            batteryLauncher.launch(intent)
        },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            batteryLauncher.launch(intent)
        }
    )
}
