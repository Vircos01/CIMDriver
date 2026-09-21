package com.cimdriver.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.util.AddressMatching
import com.cimdriver.app.util.TripCategory
import com.cimdriver.app.util.TripClassification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripItem(
    trip: Trip,
    savedAddresses: List<com.cimdriver.app.data.local.entity.SavedAddress> = emptyList(),
    classificationSettings: com.cimdriver.app.data.local.entity.Settings = com.cimdriver.app.data.local.entity.Settings(),
    classificationRules: List<com.cimdriver.app.data.local.entity.ClassificationRule> = emptyList(),
    onQuickReview: (Trip, String) -> Unit = { _, _ -> },
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDelete: (Trip) -> Unit
) {
    val locale = Locale.getDefault()
    var showReviewTypeMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
    val startString = dateFormat.format(Date(trip.startTime))
    val endString = trip.endTime?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.trip_in_progress)
    
    val durationText = trip.endTime?.let { end ->
        val diffMinutes = (end - trip.startTime) / (1000 * 60)
        val hours = diffMinutes / 60
        val mins = diffMinutes % 60
        if (hours > 0) "${hours}u ${mins}m" else "${mins}m"
    } ?: "..."

    val distanceKm = String.format(locale, "%.1f km", trip.distanceMeters / 1000.0)

    val homeStr = stringResource(R.string.home)
    val workStr = stringResource(R.string.work)
    val customerStr = stringResource(R.string.customer)

    fun addressTypeFor(address: String?): String? {
        val savedAddress = AddressMatching.findSavedAddress(address, savedAddresses) ?: return null

        return savedAddress.addressType ?: when {
            savedAddress.isHomeLocation -> homeStr
            savedAddress.isWorkLocation -> workStr
            savedAddress.isCustomerLocation -> customerStr
            else -> null
        }
    }

    val startAddressType = addressTypeFor(trip.startAddress)
    val endAddressType = addressTypeFor(trip.endAddress)
    val displayTripCategory = when (TripClassification.classify(
        tripType = trip.tripType,
        startAddressType = startAddressType,
        endAddressType = endAddressType,
        defaultCategory = if (classificationSettings.classificationDefault == "BUSINESS") TripCategory.BUSINESS else TripCategory.PRIVATE,
        homeWorkAsCommute = classificationSettings.classifyHomeWorkAsCommute,
        customerAsBusiness = classificationSettings.classifyCustomerAsBusiness,
        rules = classificationRules,
        timestamp = trip.startTime,
        workDaysStr = classificationSettings.workDays,
        workStartTime = classificationSettings.workStartTime,
        workEndTime = classificationSettings.workEndTime
    )) {
        TripCategory.COMMUTE -> "Woon-werk"
        TripCategory.BUSINESS -> "Zakelijk"
        TripCategory.PRIVATE -> "Privé"
        null -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (trip.status == "TO_REVIEW") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$startString - $endString", style = MaterialTheme.typography.titleSmall)
                    if (trip.status == "TO_REVIEW") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = stringResource(R.string.to_be_rated),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val displayStartAddress = trip.startAddress?.let { addr ->
                    AddressMatching.findSavedAddress(addr, savedAddresses)?.label ?: addr
                } ?: stringResource(R.string.unknown)
                val displayEndAddress = trip.endAddress?.let { addr ->
                    AddressMatching.findSavedAddress(addr, savedAddresses)?.label ?: addr
                } ?: "..."
                Text(text = stringResource(R.string.trip_from, displayStartAddress), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.trip_to, displayEndAddress), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.trip_distance, distanceKm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = stringResource(R.string.trip_duration, durationText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (displayTripCategory != null) {
                        Text(text = displayTripCategory, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    val odoStartStr = String.format(locale, "%,d", trip.odometerStart).replace(',', '.')
                    val odoEndStr = trip.odometerEnd?.let { String.format(locale, "%,d", it).replace(',', '.') } ?: "..."
                    Text(
                        text = "📍 $odoStartStr km → $odoEndStr km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trip.status == "TO_REVIEW") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onQuickReview(trip, TripClassification.DYNAMICS_HOME_TO_WORK) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { Text("Woon-werk", maxLines = 1) }
                        OutlinedButton(
                            onClick = { onQuickReview(trip, TripClassification.DYNAMICS_BUSINESS_MEETING) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { Text("Zakelijk", maxLines = 1) }
                        OutlinedButton(
                            onClick = { onQuickReview(trip, TripClassification.DYNAMICS_PERSONAL) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { Text("Privé", maxLines = 1) }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showReviewTypeMenu = true }) {
                            Text("Meer opties")
                        }
                        DropdownMenu(
                            expanded = showReviewTypeMenu,
                            onDismissRequest = { showReviewTypeMenu = false }
                        ) {
                            classificationRules.mapNotNull { it.tripType }
                                .distinct()
                                .forEach { tripType ->
                                    DropdownMenuItem(
                                        text = { Text(tripType) },
                                        onClick = {
                                            onQuickReview(trip, tripType)
                                            showReviewTypeMenu = false
                                        }
                                    )
                                }
                        }
                    }
                }
                if (trip.isManual) {
                    Text(text = stringResource(R.string.trip_manual_added), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
