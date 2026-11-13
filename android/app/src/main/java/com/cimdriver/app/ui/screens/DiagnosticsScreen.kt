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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.cimdriver.app.ui.components.core.CimDriverTopBar
import com.cimdriver.app.ui.components.core.TopBarNavType
import com.cimdriver.app.util.HealthMonitorHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val trackingStatus by viewModel.trackingStatus.collectAsState()
    val systemState by viewModel.systemState.collectAsState()
    var healthIssues by remember { mutableStateOf(HealthMonitorHelper.checkHealth(context)) }

    // Reload system state when screen is shown
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadSystemDiagnostics()
        healthIssues = HealthMonitorHelper.checkHealth(context)
    }

    Scaffold(
        topBar = {
            CimDriverTopBar(
                title = stringResource(R.string.system_diagnostics),
                navType = TopBarNavType.BACK,
                onNavClick = onBack
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
            // Menselijke Betrouwbaarheidsstatus
            val isOptimal = healthIssues.isEmpty()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOptimal) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOptimal) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isOptimal) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOptimal) "Tracking Betrouwbaarheid: Optimaal"
                            else "Tracking Betrouwbaarheid: Aandacht Vereist (${healthIssues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOptimal) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isOptimal) "Alle benodigde achtergrondsysteemdiensten, Doze-mode uitzonderingen, GPS- en Bluetooth-permissies functioneren perfect. Ritten worden betrouwbaar geregistreerd."
                        else "Eén of meer systeeminstellingen kunnen automatische registratie beperken of ritten voortijdig stoppen:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                    if (healthIssues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        healthIssues.forEach { issue ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(issue.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                                        Text(issue.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF555555))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(issue.createIntent(context))
                                            } catch (e: Exception) {
                                                android.util.Log.e("DiagnosticsScreen", "Error launching health intent", e)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(issue.actionLabel, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

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
