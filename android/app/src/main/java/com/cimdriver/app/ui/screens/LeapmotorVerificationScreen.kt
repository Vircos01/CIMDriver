package com.cimdriver.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.components.core.CimDriverTopBar
import com.cimdriver.app.ui.components.core.TopBarNavType
import com.cimdriver.app.ui.theme.CIMDriverGreen
import com.cimdriver.app.ui.theme.CIMDriverOrange
import com.cimdriver.app.ui.theme.CIMDriverRed
import com.cimdriver.app.ui.theme.CIMDriverWhite
import com.cimdriver.app.ui.viewmodel.LeapmotorExecutedAction
import com.cimdriver.app.ui.viewmodel.LeapmotorVerificationViewModel
import com.cimdriver.app.util.LeapmotorActionType
import com.cimdriver.app.util.LeapmotorMatchItem
import com.cimdriver.app.util.LeapmotorMatchStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeapmotorVerificationScreen(
    vehicleId: Long? = null,
    initialImageUri: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: LeapmotorVerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val report by viewModel.report.collectAsState()
    val message by viewModel.message.collectAsState()
    val executedActions by viewModel.executedActions.collectAsState()
    val previouslyVerifiedTrips by viewModel.previouslyVerifiedTrips.collectAsState()

    var showTextInputDialog by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf("") }
    var showVehiclePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUri(uri)
        }
    }

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            viewModel.selectVehicleById(vehicleId)
        }
    }

    LaunchedEffect(initialImageUri) {
        if (!initialImageUri.isNullOrBlank()) {
            val uri = Uri.parse(initialImageUri)
            viewModel.processImageUri(uri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CimDriverTopBar(
                title = "Leapmotor Boordcomputer Sync",
                navType = TopBarNavType.BACK,
                onNavClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showTextInputDialog = true }) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "Tekst invoeren",
                            tint = CIMDriverWhite
                        )
                    }
                    if (vehicles.size > 1) {
                        Box {
                            IconButton(onClick = { showVehiclePicker = true }) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = "Voertuig kiezen",
                                    tint = CIMDriverWhite
                                )
                            }
                            DropdownMenu(
                                expanded = showVehiclePicker,
                                onDismissRequest = { showVehiclePicker = false }
                            ) {
                                vehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text("${v.name} (${v.licensePlate})") },
                                        onClick = {
                                            viewModel.selectVehicle(v)
                                            showVehiclePicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle & Instructions Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ElectricCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedVehicle?.let { "${it.name} (${it.licensePlate})" } ?: "Leapmotor B10",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Maak een screenshot van het weekoverzicht in de Leapmotor app (onder Energieverbruik per gereden afstand > Ritgegevens) en kies deze hieronder om ritten en ritduur slim te matchen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Screenshot analyseren...")
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecteer Leapmotor Screenshot")
                            }
                        }
                    }
                }
            }

            // Summary Card (if report available)
            if (report != null) {
                val rep = report!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Resultaat Boordcomputer Verificatie",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SummaryChip(
                                    count = rep.items.size,
                                    label = "Totaal",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SummaryChip(
                                    count = rep.exactMatchesCount,
                                    label = "In sync",
                                    color = CIMDriverGreen
                                )
                                SummaryChip(
                                    count = rep.discrepancyCount,
                                    label = "Afwijking",
                                    color = CIMDriverOrange
                                )
                                SummaryChip(
                                    count = rep.missingCount,
                                    label = "Gemist",
                                    color = CIMDriverRed
                                )
                            }

                            // Detected Odometer Checkpoint
                            if (rep.detectedOdometerKm != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Gedetecteerde tellerstand: ${rep.detectedOdometerKm} km",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Gevonden in kop van Leapmotor overzicht",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.confirmOdometerCheckpoint(rep.detectedOdometerKm) }
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bevestig ijkpunt", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // Batch Apply Button
                            val actionableCount = rep.items.count {
                                it.status != LeapmotorMatchStatus.EXACT_MATCH &&
                                it.suggestedAction.type != LeapmotorActionType.NONE
                            }
                            if (actionableCount > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.applyAllFixes() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pas alle $actionableCount correcties in 1x toe")
                                }
                            }
                        }
                    }
                }

                // Dedicated Executed Actions Section (Audit Log of What Changed)
                if (executedActions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CIMDriverGreen.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CIMDriverGreen.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CIMDriverGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Uitgevoerde acties in deze controle (${executedActions.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CIMDriverGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                executedActions.forEachIndexed { idx, act ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = CIMDriverGreen.copy(alpha = 0.2f)
                                        )
                                    }
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "✅ ${act.actionTitle}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = act.tripSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = act.actionDetail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section title
                item {
                    Text(
                        text = "Ritten per boordcomputer (${rep.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // List of matched items
                items(rep.items, key = { it.id }) { item ->
                    val tripKey = "${item.lmTrip.dateText}_${item.lmTrip.startTimeText}"
                    val executedForThis = executedActions.find { it.tripKey == tripKey }
                    LeapmotorMatchCard(
                        item = item,
                        executedAction = executedForThis,
                        onApplyFix = { viewModel.applyFix(item) }
                    )
                }
            }

            // Previously Verified Trips Section (Historical log)
            if (previouslyVerifiedTrips.isNotEmpty()) {
                item {
                    var showHistory by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHistory = !showHistory },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reeds geverifieerde ritten in administratie (${previouslyVerifiedTrips.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            if (showHistory) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                                previouslyVerifiedTrips.take(15).forEach { vTrip ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = "🚗 ${dateFormat.format(Date(vTrip.startTime))} • ${(vTrip.distanceMeters / 1000.0).let { String.format(Locale.getDefault(), "%.1f", it) }} km",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${vTrip.startAddress ?: "Start"} → ${vTrip.endAddress ?: "Bestemming"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = vTrip.note ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CIMDriverGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Text Input Dialog (fallback)
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Plak tekst uit Leapmotor app") },
            text = {
                Column {
                    Text(
                        text = "Kopieer de tekst van de ritten en plak deze hier:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("2026/11/19 18:58-19:18\n2026/11/19 18:13-18:29...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showTextInputDialog = false
                    viewModel.processRawText(pastedText)
                }) {
                    Text("Verwerken")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun LeapmotorMatchCard(
    item: LeapmotorMatchItem,
    executedAction: LeapmotorExecutedAction? = null,
    onApplyFix: () -> Unit
) {
    val lm = item.lmTrip
    val cim = item.matchingTrip

    val (statusColor, statusLabel, statusIcon) = when (item.status) {
        LeapmotorMatchStatus.EXACT_MATCH -> Triple(CIMDriverGreen, "In sync", Icons.Default.CheckCircle)
        LeapmotorMatchStatus.EXCESS_STOP_IDLE -> Triple(CIMDriverOrange, "Stilstand na stop", Icons.Default.HourglassBottom)
        LeapmotorMatchStatus.DELAYED_START -> Triple(CIMDriverOrange, "Vertraagde start", Icons.Default.Timelapse)
        LeapmotorMatchStatus.SHIFTED_TIME -> Triple(CIMDriverOrange, "Tijd verschoven", Icons.Default.Schedule)
        LeapmotorMatchStatus.DURATION_MISMATCH -> Triple(CIMDriverOrange, "Duur afwijking", Icons.Default.Warning)
        LeapmotorMatchStatus.MERGED_TRIP_CANDIDATE -> Triple(Color(0xFF9C27B0), "Samengevoegd", Icons.AutoMirrored.Filled.CallSplit)
        LeapmotorMatchStatus.SPLIT_TRIP_CANDIDATE -> Triple(Color(0xFF2196F3), "Onderbroken", Icons.AutoMirrored.Filled.MergeType)
        LeapmotorMatchStatus.MISSING_IN_CIMDRIVER -> Triple(CIMDriverRed, "Gemiste rit", Icons.Default.ErrorOutline)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // If an action was executed on this trip during this session, display prominent confirmation banner
            if (executedAction != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CIMDriverGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CIMDriverGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CIMDriverGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Actie uitgevoerd: ${executedAction.actionTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CIMDriverGreen
                            )
                            Text(
                                text = executedAction.actionDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Header: Date, Leapmotor time, status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${lm.dateText} • ${lm.startTimeText} - ${lm.endTimeText}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Boordcomputerduur: ${lm.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expanded Leapmotor telemetry details (if available)
            if (lm.distanceKm != null || lm.maxSpeedKmh != null || lm.energyKwh != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lm.distanceKm != null) {
                        TelemetryBadge(text = "${lm.distanceKm} km")
                    }
                    if (lm.maxSpeedKmh != null) {
                        TelemetryBadge(text = "Max ${lm.maxSpeedKmh} km/h")
                    }
                    if (lm.energyKwh != null) {
                        TelemetryBadge(text = "${lm.energyKwh} kWh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Explanation & GPS details
            Text(
                text = item.explanation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (cim != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val cimDur = ((cim.effectiveEndTime() - cim.startTime) / 60000L).toInt().coerceAtLeast(1)
                Text(
                    text = "CIMDriver GPS: ${cim.startAddress ?: "Start"} → ${cim.endAddress ?: "Bestemming"} (${(cim.distanceMeters / 1000.0).let { String.format(Locale.getDefault(), "%.1f", it) }} km, $cimDur min)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Suggested Action Button
            if (item.suggestedAction.type != LeapmotorActionType.NONE &&
                item.suggestedAction.type != LeapmotorActionType.CONFIRM_VERIFIED
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onApplyFix,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.status == LeapmotorMatchStatus.MISSING_IN_CIMDRIVER) {
                            CIMDriverRed
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        if (item.status == LeapmotorMatchStatus.MISSING_IN_CIMDRIVER) Icons.Default.Add else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.suggestedAction.title)
                }
            } else if (item.status == LeapmotorMatchStatus.EXACT_MATCH && executedAction == null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onApplyFix,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = CIMDriverGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Geverifieerd in administratie vastleggen", color = CIMDriverGreen)
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun com.cimdriver.app.data.local.entity.Trip.effectiveEndTime(): Long {
    return endTime ?: (startTime + (distanceMeters / 15).coerceAtLeast(60) * 1000L)
}
