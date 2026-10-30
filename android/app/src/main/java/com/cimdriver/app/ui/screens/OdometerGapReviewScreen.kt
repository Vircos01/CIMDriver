package com.cimdriver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.cimdriver.app.util.OdometerRecalculator
import com.cimdriver.app.util.RecalculationPreview
import com.cimdriver.app.util.TripDistance
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
    val snackbarHostState = remember { SnackbarHostState() }

    var isRecalculatingPreview by remember { mutableStateOf(false) }
    var recalculationPreview by remember { mutableStateOf<RecalculationPreview?>(null) }
    var isApplyingRecalculation by remember { mutableStateOf(false) }

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

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val totalGapKm = remember(detectedGaps) {
        detectedGaps.sumOf { abs(it.gapKm) }
    }
    val smallGapsCount = remember(detectedGaps) {
        detectedGaps.count { abs(it.gapKm) <= 5 }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(
                        onClick = {
                            isRecalculatingPreview = true
                            viewModel.previewRecalculateOdometerChain(vehicleId) { preview ->
                                isRecalculatingPreview = false
                                recalculationPreview = preview
                            }
                        },
                        enabled = !isScanning && !isRecalculatingPreview && !isApplyingRecalculation
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Tellerstanden herberekenen",
                            tint = CIMDriverWhite
                        )
                    }
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

                    // Recalculate odometers card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CIMDriverGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Calculate,
                                                contentDescription = null,
                                                tint = CIMDriverGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "KMs opnieuw doorberekenen",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Herbereken alle tellerstanden chronologisch op basis van ritten en km-controles.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isRecalculatingPreview = true
                                        viewModel.previewRecalculateOdometerChain(vehicleId) { preview ->
                                            isRecalculatingPreview = false
                                            recalculationPreview = preview
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isRecalculatingPreview && !isApplyingRecalculation,
                                    colors = ButtonDefaults.buttonColors(containerColor = CIMDriverNavy)
                                ) {
                                    if (isRecalculatingPreview) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Bezig met berekenen...")
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tellerstanden herberekenen")
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
            onApply = { fixType, note, customStart, customEnd, tripType ->
                viewModel.resolveGap(
                    gap = selectedGapForResolution!!,
                    fixType = fixType,
                    note = note,
                    customStartAddress = customStart,
                    customEndAddress = customEnd,
                    tripType = tripType,
                    onComplete = {
                        selectedGapForResolution = null
                        refreshGaps()
                    }
                )
            }
        )
    }

    // Dialog to preview and confirm recalculation of odometer chain
    if (recalculationPreview != null) {
        RecalculateConfirmationDialog(
            preview = recalculationPreview!!,
            dateFormat = dateFormat,
            isApplying = isApplyingRecalculation,
            onDismiss = { recalculationPreview = null },
            onConfirm = {
                val previewToApply = recalculationPreview!!
                isApplyingRecalculation = true
                viewModel.applyRecalculateOdometerChain(vehicleId, previewToApply) { count ->
                    isApplyingRecalculation = false
                    recalculationPreview = null
                    snackbarMessage = "$count ritten succesvol herberekend!"
                    refreshGaps()
                }
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
                            val prevKm = TripDistance.metersToOdometerKilometers(gap.previousTrip.distanceMeters)
                            Text(
                                text = "Vorige rit (#${gap.previousTrip.id}): ${gap.previousTrip.startAddress ?: "Onbekend"} -> ${gap.previousTrip.endAddress ?: "Onbekend"} ($prevKm km, eindstand ${gap.previousTrip.effectiveOdometerEnd()} km)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (gap.nextTrip != null) {
                            if (gap.previousTrip != null) Spacer(modifier = Modifier.height(4.dp))
                            val nextKm = TripDistance.metersToOdometerKilometers(gap.nextTrip.distanceMeters)
                            Text(
                                text = "Volgende rit (#${gap.nextTrip.id}): ${gap.nextTrip.startAddress ?: "Onbekend"} -> ${gap.nextTrip.endAddress ?: "Onbekend"} ($nextKm km, beginstand ${gap.nextTrip.odometerStart} km)",
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
    onApply: (FixType, String?, String?, String?, String) -> Unit
) {
    var selectedFix by remember { mutableStateOf(gap.suggestedFixes.firstOrNull()?.type ?: FixType.CREATE_PRIVATE_TRIP) }
    var tripType by remember { mutableStateOf("PERSONAL") }
    var note by remember { mutableStateOf("") }

    val initialStart = gap.previousTrip?.endAddress ?: "Onbekend vertrekpunt"
    val isSameLocation = gap.previousTrip?.endAddress == gap.nextTrip?.startAddress
    val initialEnd = if (isSameLocation && !initialStart.startsWith("Onbekend")) "$initialStart (retour)" else gap.nextTrip?.startAddress ?: initialStart

    var startAddress by remember { mutableStateOf(initialStart) }
    var endAddress by remember { mutableStateOf(initialEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gat oplossen (${if (gap.gapKm > 0) "+${gap.gapKm}" else "${gap.gapKm}"} km)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Stand: ${gap.expectedOdometer} ➔ ${gap.actualOdometer} km (${if (gap.gapKm > 0) "+${gap.gapKm}" else "${gap.gapKm}"} km)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Afstand en tellerstanden worden exact sluitend vastgelegd op ${kotlin.math.abs(gap.gapKm)} km.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Kies de oplossing:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
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

                if (selectedFix == FixType.CREATE_PRIVATE_TRIP) {
                    Text(
                        text = "Ritdetails voor Belastingdienst:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tripType == "PERSONAL",
                            onClick = { tripType = "PERSONAL" },
                            label = { Text("Privé", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = tripType == "BUSINESS",
                            onClick = { tripType = "BUSINESS" },
                            label = { Text("Zakelijk", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = tripType == "COMMUTE",
                            onClick = { tripType = "COMMUTE" },
                            label = { Text("Woon-werk", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    OutlinedTextField(
                        value = startAddress,
                        onValueChange = { startAddress = it },
                        label = { Text("Vertreklocatie") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endAddress,
                        onValueChange = { endAddress = it },
                        label = { Text("Bestemming") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Snelle keuze voor korte rit (${gap.gapKm} km):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                endAddress = "Supermarkt / Boodschappen"
                                note = "Boodschappen"
                                tripType = "PERSONAL"
                            },
                            label = { Text("Boodschappen", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                endAddress = "Klant / Afspraak"
                                note = "Klantbezoek"
                                tripType = "BUSINESS"
                            },
                            label = { Text("Klantbezoek", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                endAddress = "Tankstation / Wasstraat"
                                note = "Tanken / wassen"
                            },
                            label = { Text("Tanken", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (selectedFix == FixType.SYNC_VEHICLE_ODOMETER) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Actuele tellerstand gelijkzetten",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "De actuele tellerstand van het voertuig (${gap.actualOdometer} km) wordt direct gelijkgezet aan ${gap.expectedOdometer} km (de eindstand van de laatste rit). Er worden geen ritten toegevoegd of verwijderd.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedFix == FixType.MARK_AS_EXPLAINED) {
                    Text(
                        text = "Veelvoorkomende verklaringen:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("APK / Keuring", "Garage / Onderhoud", "Proefrit monteur").forEach { preset ->
                            SuggestionChip(
                                onClick = { note = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Toelichting / omschrijving (optioneel)") },
                    placeholder = { Text("Bijv. Boodschappen, sportclub, APK...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        selectedFix,
                        note.takeIf { it.isNotBlank() },
                        startAddress.takeIf { it.isNotBlank() },
                        endAddress.takeIf { it.isNotBlank() },
                        tripType
                    )
                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecalculateConfirmationDialog(
    preview: RecalculationPreview,
    dateFormat: SimpleDateFormat,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isApplying) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = CIMDriverGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tellerstanden doorberekenen")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "De tellerstanden van alle ritten worden chronologisch doorgerekend op basis van de gereden afstanden, beginnend bij ${preview.initialOdometer} km.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totaal aantal ritten:", style = MaterialTheme.typography.bodySmall)
                            Text("${preview.totalTrips}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ritten die worden aangepast:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${preview.modifiedTripsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (preview.modifiedTripsCount > 0) CIMDriverGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Km-controles / ijkpunten:", style = MaterialTheme.typography.bodySmall)
                            Text("${preview.preservedChecksCount} behouden", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gaten na doorberekening:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${preview.remainingGaps.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (preview.remainingGaps.isEmpty()) CIMDriverGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gaten bij km-controles of actuele tellerstand blijven behouden. Je kunt deze daarna per stuk via 'Oplossen' verklaren of sluitend maken.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (preview.modifiedTrips.isNotEmpty()) {
                    Text(
                        text = "Voorbeeld van aanpassingen (${preview.modifiedTrips.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    preview.modifiedTrips.take(4).forEach { detail ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Rit #${detail.tripId} (${detail.distanceKm} km)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stand: ${detail.oldStart} - ${detail.oldEnd} ➔ ${detail.newStart} - ${detail.newEnd} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CIMDriverGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (preview.modifiedTrips.size > 4) {
                        Text(
                            text = "... en nog ${preview.modifiedTrips.size - 4} ritten",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Alle ritten hebben al de juiste tellerstand. Er zijn geen wijzigingen nodig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CIMDriverGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isApplying && preview.modifiedTripsCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CIMDriverGreen)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bezig met opslaan...")
                } else {
                    Text("Nu herberekenen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isApplying
            ) {
                Text("Annuleren")
            }
        }
    )
}
