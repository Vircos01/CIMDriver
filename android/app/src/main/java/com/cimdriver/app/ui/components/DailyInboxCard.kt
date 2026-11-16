package com.cimdriver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cimdriver.app.data.local.entity.FuelFillUp
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.ui.viewmodel.TripReviewCandidate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyInboxCard(
    reviewCandidates: List<TripReviewCandidate>,
    workDayReviewCount: Int,
    openGapsCount: Int,
    pendingFuelDrafts: List<FuelFillUp>,
    vehicles: List<Vehicle>,
    onQuickApproveTrip: (TripReviewCandidate) -> Unit,
    onApproveAllHighConfidence: () -> Unit,
    onNavigateToTrips: () -> Unit,
    onNavigateToWorkHours: () -> Unit,
    onNavigateToGaps: () -> Unit,
    onNavigateToFuelDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
    workDayReviewCandidates: List<com.cimdriver.app.util.WorkDayReviewCandidate> = emptyList(),
    onQuickApproveWorkDay: ((com.cimdriver.app.util.WorkDayReviewCandidate) -> Unit)? = null
) {
    val effectiveWorkDayCount = maxOf(workDayReviewCount, workDayReviewCandidates.size)
    val totalCount = reviewCandidates.size + effectiveWorkDayCount + openGapsCount + pendingFuelDrafts.size
    val highConfidenceTrips = remember(reviewCandidates) { reviewCandidates.filter { it.assessment.isHighConfidence } }
    val highConfidenceWorkDays = remember(workDayReviewCandidates) { workDayReviewCandidates.filter { it.suggestion.isHighConfidence } }
    val totalHighConfidence = highConfidenceTrips.size + highConfidenceWorkDays.size
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (totalCount > 0) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0FDF4)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (totalCount > 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFDCFCE7)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (totalCount > 0) Icons.Filled.Inbox else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (totalCount > 0) MaterialTheme.colorScheme.primary else Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (totalCount > 0) "Dagelijkse Inbox" else "Alles is bijgewerkt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCount > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF15803D)
                    )
                    Text(
                        text = if (totalCount > 0) "$totalCount openstaande actiepunten" else "Geen ritten, uren of tank-/laadsessies ter beoordeling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                if (totalCount > 0) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Inklappen" else "Uitklappen"
                        )
                    }
                }
            }

            if (isExpanded && totalCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Batch goedkeuren voor hoge betrouwbaarheid
                if (totalHighConfidence >= 2) {
                    Button(
                        onClick = onApproveAllHighConfidence,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alles met hoge zekerheid goedkeuren ($totalHighConfidence)")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 1. Ritten ter beoordeling (Confidence score + 1-tap accept)
                if (reviewCandidates.isNotEmpty()) {
                    Text(
                        text = "Ritten ter beoordeling (${reviewCandidates.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    reviewCandidates.take(3).forEach { candidate ->
                        InboxTripItem(
                            candidate = candidate,
                            onQuickApprove = { onQuickApproveTrip(candidate) },
                            onClick = onNavigateToTrips
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (reviewCandidates.size > 3) {
                        TextButton(
                            onClick = onNavigateToTrips,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Bekijk alle ${reviewCandidates.size} ritten →")
                        }
                    }
                }

                // 2. Openstaande werkdagen (Triage & 1-tap suggesties)
                if (workDayReviewCandidates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Werkuren ter controle (${workDayReviewCandidates.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    workDayReviewCandidates.take(3).forEach { candidate ->
                        InboxWorkDayItem(
                            candidate = candidate,
                            onQuickApprove = { onQuickApproveWorkDay?.invoke(candidate) },
                            onClick = onNavigateToWorkHours
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (workDayReviewCandidates.size > 3) {
                        TextButton(
                            onClick = onNavigateToWorkHours,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Bekijk alle ${workDayReviewCandidates.size} werkdagen →")
                        }
                    }
                } else if (workDayReviewCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InboxNoticeItem(
                        icon = Icons.Filled.Schedule,
                        title = "Controleer werkuren",
                        description = "$workDayReviewCount werkdagen wachten nog op bevestiging.",
                        actionLabel = "Bekijken",
                        onClick = onNavigateToWorkHours,
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                // 3. Kilometergaten
                if (openGapsCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InboxNoticeItem(
                        icon = Icons.Filled.Warning,
                        title = "Gaten in kilometerregistratie",
                        description = "$openGapsCount openstaande discrepanties in tellerstand.",
                        actionLabel = "Oplossen",
                        onClick = onNavigateToGaps,
                        accentColor = MaterialTheme.colorScheme.error
                    )
                }

                // 4. Tank-/Laadsessie concepten
                if (pendingFuelDrafts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val firstDraft = pendingFuelDrafts.first()
                    val vehicle = vehicles.find { it.id == firstDraft.vehicleId }
                    val isEV = vehicle?.engineType == "EV"
                    InboxNoticeItem(
                        icon = Icons.Filled.LocalGasStation,
                        title = if (isEV) "Laadsessie registreren" else "Tankbeurt registreren",
                        description = "Tussenstop bij ${firstDraft.stationName ?: if (isEV) "Laadpaal" else "Tankstation"}.",
                        actionLabel = "Invoeren",
                        onClick = { onNavigateToFuelDraft(firstDraft.id) },
                        accentColor = Color(0xFF0284C7)
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxTripItem(
    candidate: TripReviewCandidate,
    onQuickApprove: () -> Unit,
    onClick: () -> Unit
) {
    val trip = candidate.trip
    val assessment = candidate.assessment
    val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val startStr = timeFormat.format(Date(trip.startTime))
    val distanceKm = String.format(Locale.getDefault(), "%.1f km", trip.distanceMeters / 1000.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$startStr • $distanceKm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Confidence badge
                val badgeColor = if (assessment.isHighConfidence) Color(0xFF16A34A) else Color(0xFFD97706)
                Text(
                    text = "${assessment.confidenceScore}% zeker",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${trip.startAddress ?: "Start"} → ${trip.endAddress ?: "Eind"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voorstel: ${candidate.suggestedCategoryName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onQuickApprove,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Akkoord", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InboxWorkDayItem(
    candidate: com.cimdriver.app.util.WorkDayReviewCandidate,
    onQuickApprove: () -> Unit,
    onClick: () -> Unit
) {
    val workDay = candidate.workDay
    val suggestion = candidate.suggestion
    val dateFormat = SimpleDateFormat("EEEE d MMM", Locale.forLanguageTag("nl-NL"))
    val dateStr = dateFormat.format(Date(workDay.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (workDay.registrationSource == "WIFI") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📶 Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Confidence badge
                val badgeColor = if (suggestion.isHighConfidence) Color(0xFF16A34A) else Color(0xFFD97706)
                Text(
                    text = "${suggestion.confidenceScore}% zeker",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion.statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val detailText = buildString {
                    append("${suggestion.suggestedBreakMinutes}m pauze")
                    suggestion.suggestedProjectCode?.let { append(" • $it") }
                }
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onQuickApprove,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Akkoord", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InboxNoticeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

