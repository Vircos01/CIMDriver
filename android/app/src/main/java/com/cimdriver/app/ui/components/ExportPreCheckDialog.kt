package com.cimdriver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.util.GapType
import com.cimdriver.app.util.OdometerGapAnalyzer
import kotlin.math.abs

@Composable
fun ExportPreCheckDialog(
    title: String,
    trips: List<Trip>,
    vehicles: List<Vehicle>,
    onConfirmExport: () -> Unit,
    onDismiss: () -> Unit,
    onQuickApproveAll: (() -> Unit)? = null
) {
    val unreviewed = trips.filter { it.status == "TO_REVIEW" }
    val missingOdometer = trips.filter { it.odometerEnd == null }
    val deviatingWithoutNote = trips.filter { t ->
        val exp = t.expectedDistanceMeters
        val dist = t.distanceMeters
        exp != null && abs(dist - exp) > 2000 && abs(dist - exp) > (exp * 0.1) && t.note.isNullOrBlank()
    }

    val vehicleGaps = vehicles.flatMap { v ->
        val vTrips = trips.filter { it.vehicleId == v.id }
        if (vTrips.isNotEmpty()) OdometerGapAnalyzer.analyze(v, vTrips) else emptyList()
    }.filter { it.gapType != GapType.CURRENT_STAND_GAP && it.gapKm != 0 }

    val hasWarnings = unreviewed.isNotEmpty() || missingOdometer.isNotEmpty() || vehicleGaps.isNotEmpty() || deviatingWithoutNote.isNotEmpty()
    val isPerfect = !hasWarnings

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "📋 Fiscale Exportcontrole",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Toetsing van de geselecteerde ritten aan de eisen van de Belastingdienst (art. 3.17 Wet IB 2001):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Kilometerstanden
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (vehicleGaps.isEmpty() && missingOdometer.isEmpty())
                            Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (vehicleGaps.isEmpty() && missingOdometer.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (vehicleGaps.isEmpty() && missingOdometer.isEmpty()) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Kilometerstanden",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1B)
                            )
                            val text = when {
                                vehicleGaps.isNotEmpty() -> "${vehicleGaps.size} onderbreking(en) / gaten (${vehicleGaps.sumOf { abs(it.gapKm) }} km niet verklaard)"
                                missingOdometer.isNotEmpty() -> "${missingOdometer.size} rit(ten) missen een eindtellerstand"
                                else -> "100% sluitend — Geen onderbroken standen"
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }

                // 2. Classificatie
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (unreviewed.isEmpty()) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (unreviewed.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (unreviewed.isEmpty()) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Rittenclassificatie",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1B)
                                )
                                Text(
                                    text = if (unreviewed.isEmpty()) "Alle ${trips.size} ritten geclassificeerd"
                                    else "${unreviewed.size} rit(ten) staan nog op 'Te beoordelen'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF424242)
                                )
                            }
                        }
                        if (unreviewed.isNotEmpty() && onQuickApproveAll != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onQuickApproveAll,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text("✓ Keur alle ${unreviewed.size} ritten direct goed", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 3. Fiscale Toelichtingen & Afwijkingen
                if (deviatingWithoutNote.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Routeafwijkingen",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1B)
                                )
                                Text(
                                    text = "${deviatingWithoutNote.size} rit(ten) wijken meer dan 10% af zonder notitie",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF424242)
                                )
                            }
                        }
                    }
                }

                // Status Banner
                Surface(
                    color = if (isPerfect) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPerfect) "🏆 100% Fiscaal Sluitend. Voldoet volledig aan de fiscale rapportage-eisen van de Belastingdienst."
                        else "⚠️ Let op: Dit rapport bevat openstaande punten die bij controle door fiscus of werkgever tot vragen kunnen leiden.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPerfect) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirmExport()
                }
            ) {
                Text(if (isPerfect) "Exporteer Rapport" else "Toch exporteren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Eerst afronden")
            }
        }
    )
}

