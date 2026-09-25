package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cimdriver.app.service.TrackingStatusStore
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cimdriver.app.ui.viewmodel.DiagnosticsViewModel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val trackingStatus by viewModel.trackingStatus.collectAsState()
    val systemState by viewModel.systemState.collectAsState()

    // Reload system state when screen is shown
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadSystemDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.system_diagnostics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Systeem & App Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App & Systeem Info", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("App Versie:", style = MaterialTheme.typography.labelMedium)
                    Text("CIMDriver v${systemState.appVersion}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Batterij Optimalisatie:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (systemState.isIgnoringBatteryOptimizations) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Correct (Uitgeschakeld)", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Waarschuwing! (Ingebaard)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Tracking Service
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tracking Service", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(stringResource(R.string.current_state), style = MaterialTheme.typography.labelMedium)
                    Text(trackingStatus.state.name, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Bericht:", style = MaterialTheme.typography.labelMedium)
                    Text(trackingStatus.message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (trackingStatus.tripId != null) {
                        Text(stringResource(R.string.active_trip_id), style = MaterialTheme.typography.labelMedium)
                        Text(trackingStatus.tripId.toString(), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (trackingStatus.gpsAccuracyMeters != null) {
                        Text("GPS Nauwkeurigheid:", style = MaterialTheme.typography.labelMedium)
                        Text("${trackingStatus.gpsAccuracyMeters}m", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (trackingStatus.errorCause != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.last_error), style = MaterialTheme.typography.labelMedium)
                        Text(trackingStatus.errorCause!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Actieve Werkdag
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Actieve Werkdag", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (systemState.activeWorkDay != null) {
                        Text("Status:", style = MaterialTheme.typography.labelMedium)
                        Text("Ingelogd (Actief)", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Kantoor / Locatie:", style = MaterialTheme.typography.labelMedium)
                        Text(systemState.activeWorkDay!!.workLocationLabel ?: "Onbekend", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Aankomsttijd:", style = MaterialTheme.typography.labelMedium)
                        Text(systemState.formattedWorkDayStart, style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text("Status:", style = MaterialTheme.typography.labelMedium)
                        Text("Geen actieve werkdag", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Boot Recovery
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Boot Recovery & Failsafe", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (systemState.activeTripIdInRecovery != null) {
                        Text("Status:", style = MaterialTheme.typography.labelMedium)
                        Text("Trip in wachtrij voor herstel", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Rit ID:", style = MaterialTheme.typography.labelMedium)
                        Text(systemState.activeTripIdInRecovery.toString(), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Herstelpogingen:", style = MaterialTheme.typography.labelMedium)
                        Text(systemState.recoveryAttempts.toString(), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text("Status:", style = MaterialTheme.typography.labelMedium)
                        Text("Alles normaal (geen herstel nodig)", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
