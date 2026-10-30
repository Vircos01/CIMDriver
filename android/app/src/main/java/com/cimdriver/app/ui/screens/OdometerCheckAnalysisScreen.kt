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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.theme.CIMDriverGreen
import com.cimdriver.app.ui.theme.CIMDriverNavy
import com.cimdriver.app.ui.theme.CIMDriverWhite
import com.cimdriver.app.ui.viewmodel.VehiclesViewModel
import com.cimdriver.app.util.OdometerGapAnalyzer
import com.cimdriver.app.util.OdometerGapAnalyzer.effectiveOdometerEnd
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class CheckResolutionStrategy(val title: String, val description: String) {
    CREATE_TRIP(
        title = "Sluitende privérit toevoegen",
        description = "Maakt een rit aan tussen de ritten om de keten sluitend te maken voor de Belastingdienst."
    ),
    DISTRIBUTE(
        title = "Verschil evenredig verdelen",
        description = "Verhoogt of verlaagt de geregistreerde kilometers van de afgelopen ritten evenredig."
    ),
    GAP_WITH_REASON(
        title = "Als verklaard gat vastleggen",
        description = "Legt het verschil vast met een officiële toelichting (bijv. garage, proefrit, APK)."
    ),
    MANUAL(
        title = "Later handmatig oplossen",
        description = "Slaat de controle op als 'in afwachting' en voegt een openstaand gat toe aan de lijst."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerCheckAnalysisScreen(
    vehicleId: Long,
    enteredOdometer: Int,
    photoPath: String? = null,
    onNavigateBack: () -> Unit,
    onConfirmed: () -> Unit,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val vehicle = vehicles.find { it.id == vehicleId }
    val trips by viewModel.getTripsForVehicle(vehicleId).collectAsState(initial = emptyList())

    val sortedTrips = remember(trips) { trips.sortedByDescending { it.startTime } }
    val recentTrips = remember(sortedTrips) { sortedTrips.take(5) }

    val registeredOdo = vehicle?.odometerCurrent ?: enteredOdometer
    val diff = enteredOdometer - registeredOdo

    var selectedStrategy by remember(diff) {
        mutableStateOf(
            if (diff > 0) CheckResolutionStrategy.CREATE_TRIP
            else if (diff < 0) CheckResolutionStrategy.DISTRIBUTE
            else CheckResolutionStrategy.GAP_WITH_REASON
        )
    }

    var reasonText by remember { mutableStateOf("") }
    var tripStartAddress by remember(recentTrips) {
        mutableStateOf(recentTrips.firstOrNull()?.endAddress ?: "Laatste bekende locatie")
    }
    var tripEndAddress by remember(tripStartAddress) {
        mutableStateOf(tripStartAddress)
    }

    var isSaving by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Km-controle Analyse",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CIMDriverNavy
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuleren")
                    }
                    Button(
                        onClick = {
                            if (vehicle != null && !isSaving) {
                                isSaving = true
                                val strategyName = when (selectedStrategy) {
                                    CheckResolutionStrategy.CREATE_TRIP -> "CREATE_TRIP"
                                    CheckResolutionStrategy.DISTRIBUTE -> "DISTRIBUTE"
                                    CheckResolutionStrategy.GAP_WITH_REASON -> "GAP_WITH_REASON"
                                    CheckResolutionStrategy.MANUAL -> "MANUAL"
                                }
                                val effectiveReason = if (selectedStrategy == CheckResolutionStrategy.CREATE_TRIP && tripStartAddress.isNotBlank()) {
                                    val prefix = "Rit: $tripStartAddress -> $tripEndAddress"
                                    if (reasonText.isNotBlank()) "$prefix ($reasonText)" else prefix
                                } else {
                                    reasonText
                                }

                                viewModel.confirmOdometerCheckWithDetails(
                                    vehicle = vehicle,
                                    correctedOdometer = enteredOdometer,
                                    photoPath = photoPath,
                                    strategy = strategyName,
                                    reason = effectiveReason,
                                    onComplete = {
                                        isSaving = false
                                        onConfirmed()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && (selectedStrategy != CheckResolutionStrategy.GAP_WITH_REASON || reasonText.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = CIMDriverGreen)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Vastleggen & opslaan", color = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (vehicle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle & Deviation Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = vehicle.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${vehicle.make} ${vehicle.model} - ${vehicle.licensePlate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (photoPath != null && File(photoPath).exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(photoPath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto tellerstand",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Comparison row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Geregistreerd",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatKm(registeredOdo)} km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Ingevoerd",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatKm(enteredOdometer)} km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Difference chip
                        val badgeBg = if (diff == 0) CIMDriverGreen.copy(alpha = 0.15f)
                        else if (abs(diff) <= 5) Color(0xFFFFA000).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)

                        val badgeColor = if (diff == 0) CIMDriverGreen
                        else if (abs(diff) <= 5) Color(0xFFD97706)
                        else MaterialTheme.colorScheme.error

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(badgeBg, RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (diff == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (diff == 0) "Stand klopt exact met de administratie"
                                    else "Afwijking geconstateerd: ${if (diff > 0) "+$diff" else "$diff"} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
            }

            // Strategy selection
            item {
                Text(
                    text = "Kies gewenste oplossing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(CheckResolutionStrategy.values()) { strategy ->
                val isSelected = selectedStrategy == strategy
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedStrategy = strategy }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CIMDriverGreen else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CIMDriverGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedStrategy = strategy },
                            colors = RadioButtonDefaults.colors(selectedColor = CIMDriverGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strategy.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strategy.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Custom configuration depending on selected strategy
            if (selectedStrategy == CheckResolutionStrategy.CREATE_TRIP && diff > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Details ontbrekende rit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tripStartAddress,
                                onValueChange = { tripStartAddress = it },
                                label = { Text("Vertrekadres") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tripEndAddress,
                                onValueChange = { tripEndAddress = it },
                                label = { Text("Aankomstadres") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Reason / Audit trail note
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedStrategy == CheckResolutionStrategy.GAP_WITH_REASON)
                                "Toelichting afwijking (verplicht voor Belastingdienst)"
                            else "Toelichting / notitie (optioneel)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            placeholder = { Text("Bijv. APK keuring, garagebezoek, proefrit...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            }

            // Live Preview of Changes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview van wijzigingen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        when (selectedStrategy) {
                            CheckResolutionStrategy.CREATE_TRIP -> {
                                if (diff > 0) {
                                    Text(
                                        text = "• Er wordt 1 nieuwe privérit aangemaakt van $diff km ($tripStartAddress naar $tripEndAddress).\n• Tellerstand van de rit loopt van $registeredOdo naar $enteredOdometer km.\n• Voertuigtellerstand wordt bijgewerkt naar $enteredOdometer km.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "• Afwijking is negatief ($diff km). Een rit toevoegen is niet mogelijk; kies 'Verschil verdelen' of 'Als verklaard vastleggen'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            CheckResolutionStrategy.DISTRIBUTE -> {
                                val count = recentTrips.size
                                Text(
                                    text = "• Het verschil van ${if (diff > 0) "+$diff" else "$diff"} km wordt evenredig verdeeld over de $count meest recente ritten.\n• Begin- en eindstanden van deze ritten worden sluitend herberekend.\n• Voertuigtellerstand wordt bijgewerkt naar $enteredOdometer km.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            CheckResolutionStrategy.GAP_WITH_REASON -> {
                                Text(
                                    text = "• Er worden geen ritten gewijzigd.\n• In het integriteitsrapport wordt een officiële verklaring van ${if (diff > 0) "+$diff" else "$diff"} km opgenomen met reden: '${reasonText.ifBlank { "..." }}'.\n• Voertuigtellerstand wordt bijgewerkt naar $enteredOdometer km.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            CheckResolutionStrategy.MANUAL -> {
                                Text(
                                    text = "• Tellerstand wordt bijgewerkt naar $enteredOdometer km.\n• Dit verschil (${if (diff > 0) "+$diff" else "$diff"} km) blijft openstaan in het overzicht 'Gaten in registratie' om later nader te onderzoeken.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Recent Trips Context (Last 5 trips)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recente ritten ter referentie (${recentTrips.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (recentTrips.isEmpty()) {
                item {
                    Text(
                        text = "Geen eerdere ritten gevonden voor dit voertuig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentTrips) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateFormat.format(Date(trip.startTime)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${trip.distanceMeters / 1000} km",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CIMDriverGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${trip.startAddress ?: "Onbekend"} -> ${trip.endAddress ?: "Onbekend"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stand: ${trip.odometerStart} km -> ${trip.effectiveOdometerEnd()} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKm(km: Int): String {
    return String.format(Locale.US, "%,d", km).replace(',', '.')
}
