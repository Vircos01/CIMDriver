package com.cimdriver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import com.cimdriver.app.ui.theme.CIMDriverGreen
import com.cimdriver.app.ui.theme.CIMDriverNavy
import com.cimdriver.app.ui.theme.CIMDriverWhite
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import com.cimdriver.app.util.FixType
import com.cimdriver.app.util.GapType
import com.cimdriver.app.util.OdometerGap
import com.cimdriver.app.util.OdometerGapAnalyzer.effectiveOdometerEnd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerGapReviewScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val vehicle = vehicles.find { it.id == vehicleId }
    val resolutions by viewModel.getResolutionsForVehicle(vehicleId).collectAsState(initial = emptyList())

    var detectedGaps by remember { mutableStateOf<List<OdometerGap>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var selectedGapForResolution by remember { mutableStateOf<OdometerGap?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Openstaand, 1 = Opgelost / Historie
    var batchFixing by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }

    fun refreshGaps() {
        isScanning = true
        viewModel.scanVehicleGaps(vehicleId) { gaps ->
            detectedGaps = gaps
            isScanning = false
        }
    }

    LaunchedEffect(vehicleId, resolutions.size) {
        refreshGaps()
    }

    val totalGapKm = remember(detectedGaps) {
        detectedGaps.sumOf { abs(it.gapKm) }
    }
    val smallGapsCount = remember(detectedGaps) {
        detectedGaps.count { abs(it.gapKm) <= 5 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = vehicle?.let { "Gaten in ${it.name}" } ?: "Gaten in registratie",
                        color = CIMDriverWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Terug",
                            tint = CIMDriverWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshGaps() }, enabled = !isScanning) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Opnieuw scannen",
                            tint = CIMDriverWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CIMDriverNavy
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Openstaand")
                            if (detectedGaps.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("${detectedGaps.size}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Historie / Opgelost")
                            if (resolutions.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${resolutions.size}")
                                }
                            }
                        }
                    }
                )
            }

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CIMDriverGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Km-keten controleren op gaten...")
                    }
                }
            } else if (selectedTab == 0) {
                // Open gaps view
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (detectedGaps.isEmpty())
                                    CIMDriverGreen.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (detectedGaps.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (detectedGaps.isEmpty()) CIMDriverGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (detectedGaps.isEmpty())
                                                "Km-registratie is 100% sluitend!"
                                            else
                                                "${detectedGaps.size} gaten gevonden ($totalGapKm km)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (detectedGaps.isEmpty()) CIMDriverGreen else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = if (detectedGaps.isEmpty())
                                                "Alle ritten sluiten naadloos op elkaar aan. Voldoet aan Belastingdienst eisen."
                                            else
                                                "De beginstand van ritten wijkt af van de vorige eindstand.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (detectedGaps.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }

                                if (smallGapsCount > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            batchFixing = true
                                            viewModel.batchResolveSmallGaps(vehicleId, maxGapKm = 5) { resolved ->
                                                batchFixing = false
                                                snackbarMessage = "$resolved kleine gaten automatisch gecorrigeerd!"
                                                refreshGaps()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !batchFixing,
                                        colors = ButtonDefaults.buttonColors(containerColor = CIMDriverGreen)
                                    ) {
                                        if (batchFixing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Bezig met corrigeren...")
                                        } else {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Los alle kleine gaten op (< 5 km) ($smallGapsCount)")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (detectedGaps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = CIMDriverGreen,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Geen afwijkingen aangetroffen",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "De tellerstanden sluiten onafgebroken op elkaar aan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(detectedGaps, key = { it.id }) { gap ->
                            GapItemCard(
                                gap = gap,
                                dateFormat = dateFormat,
                                onResolve = { selectedGapForResolution = gap }
                            )
                        }
                    }
                }
            } else {
                // Resolution history view
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (resolutions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Geen opgeloste gaten in de historie",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(resolutions, key = { it.id }) { resolution ->
                            ResolutionHistoryCard(
                                resolution = resolution,
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to resolve a selected gap
    if (selectedGapForResolution != null) {
        ResolveGapDialog(
            gap = selectedGapForResolution!!,
            onDismiss = { selectedGapForResolution = null },
            onApply = { fixType, note ->
                viewModel.resolveGap(
                    gap = selectedGapForResolution!!,
                    fixType = fixType,
                    note = note,
                    onComplete = {
                        selectedGapForResolution = null
                        refreshGaps()
                    }
                )
            }
        )
    }
}

@Composable
fun GapItemCard(
    gap: OdometerGap,
    dateFormat: SimpleDateFormat,
    onResolve: () -> Unit
) {
    val isMissing = gap.gapKm > 0
    val badgeColor = when (gap.gapType) {
        GapType.MISSING_KM -> MaterialTheme.colorScheme.error
        GapType.MISSING_TRIP -> Color(0xFFC2410C)
        GapType.OVERLAPPING_KM -> Color(0xFFD97706)
        GapType.ODOMETER_RESET -> Color(0xFF7C3AED)
        GapType.FIRST_TRIP_GAP -> Color(0xFF0284C7)
        GapType.CURRENT_STAND_GAP -> Color(0xFFD97706)
    }

    val typeLabel = when (gap.gapType) {
        GapType.MISSING_KM -> "Ontbrekende kilometers"
        GapType.MISSING_TRIP -> "Mogelijk vergeten rit"
        GapType.OVERLAPPING_KM -> "Overlappende kilometers"
        GapType.ODOMETER_RESET -> "Tellerstand gereset"
        GapType.FIRST_TRIP_GAP -> "Startstand afwijking"
        GapType.CURRENT_STAND_GAP -> "Verschil met actuele teller"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${if (gap.gapKm > 0) "+${gap.gapKm}" else "${gap.gapKm}"} km",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Range info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${dateFormat.format(Date(gap.gapTimeStart))} - ${dateFormat.format(Date(gap.gapTimeEnd))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stand info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stand springt van ${gap.expectedOdometer} km naar ${gap.actualOdometer} km",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Context trips
            if (gap.previousTrip != null || gap.nextTrip != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (gap.previousTrip != null) {
                            Text(
                                text = "Vorige rit (#${gap.previousTrip.id}): ${gap.previousTrip.startAddress ?: "Onbekend"} -> ${gap.previousTrip.endAddress ?: "Onbekend"} (${gap.previousTrip.distanceMeters / 1000} km, eindstand ${gap.previousTrip.effectiveOdometerEnd()} km)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (gap.nextTrip != null) {
                            if (gap.previousTrip != null) Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Volgende rit (#${gap.nextTrip.id}): ${gap.nextTrip.startAddress ?: "Onbekend"} -> ${gap.nextTrip.endAddress ?: "Onbekend"} (${gap.nextTrip.distanceMeters / 1000} km, beginstand ${gap.nextTrip.odometerStart} km)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onResolve,
                    colors = ButtonDefaults.buttonColors(containerColor = CIMDriverGreen)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oplossen")
                }
            }
        }
    }
}

@Composable
fun ResolutionHistoryCard(
    resolution: OdometerGapResolution,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (resolution.resolution) {
                        "CREATE_PRIVATE_TRIP" -> "Privérit aangemaakt"
                        "DISTRIBUTE_OVER_TRIPS" -> "Verdeeld over ritten"
                        "ADJUST_SINGLE_TRIP" -> "Rit aangepast"
                        "MARK_AS_EXPLAINED" -> "Verklaard (Belastingdienst)"
                        else -> "Genegeerd"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CIMDriverGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = resolution.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = CIMDriverGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gat van ${resolution.gapKm} km (${dateFormat.format(Date(resolution.gapStartTimestamp))})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!resolution.resolutionNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toelichting: ${resolution.resolutionNote}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveGapDialog(
    gap: OdometerGap,
    onDismiss: () -> Unit,
    onApply: (FixType, String?) -> Unit
) {
    var selectedFix by remember { mutableStateOf(gap.suggestedFixes.firstOrNull()?.type ?: FixType.CREATE_PRIVATE_TRIP) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gat oplossen (${if (gap.gapKm > 0) "+${gap.gapKm}" else "${gap.gapKm}"} km)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Kies hoe dit gat sluitend gemaakt moet worden voor de administratie:",
                    style = MaterialTheme.typography.bodyMedium
                )

                gap.suggestedFixes.forEach { fix ->
                    val isSelected = selectedFix == fix.type
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFix = fix.type }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CIMDriverGreen else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CIMDriverGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedFix = fix.type },
                                colors = RadioButtonDefaults.colors(selectedColor = CIMDriverGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = fix.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = fix.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Toelichting voor Belastingdienst (optioneel)") },
                    placeholder = { Text("Bijv. APK, privékilometers, garage...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedFix, note.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(containerColor = CIMDriverGreen)
            ) {
                Text("Toepassen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren")
            }
        }
    )
}
