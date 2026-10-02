package com.cimdriver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cimdriver.app.data.local.entity.Trip
import java.util.*

@Composable
fun TripsCalendarView(
    trips: List<Trip>,
    selectedYear: Int,
    onDayClick: (List<Trip>) -> Unit
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { 
        if (get(Calendar.YEAR) != selectedYear) {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, Calendar.JANUARY)
        }
    }.get(Calendar.MONTH)) }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, currentMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    // Group trips by day of month
    val tripsByDay = remember(trips, currentMonth, selectedYear) {
        val map = mutableMapOf<Int, MutableList<Trip>>()
        val tripCal = Calendar.getInstance()
        trips.forEach { trip ->
            tripCal.timeInMillis = trip.startTime
            if (tripCal.get(Calendar.YEAR) == selectedYear && tripCal.get(Calendar.MONTH) == currentMonth) {
                val day = tripCal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(trip)
            }
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (currentMonth > 0) currentMonth-- }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorige maand")
            }
            
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            Text(
                text = "$monthName $selectedYear",
                style = MaterialTheme.typography.titleLarge
            )
            
            IconButton(onClick = { if (currentMonth < 11) currentMonth++ }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Volgende maand")
            }
        }

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val totalCells = daysInMonth + offset
        val rows = (totalCells + 6) / 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalCells) { index ->
                if (index < offset) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val day = index - offset + 1
                    val dayTrips = tripsByDay[day] ?: emptyList()
                    CalendarDayCell(day = day, trips = dayTrips, onClick = { onDayClick(dayTrips) })
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: Int, trips: List<Trip>, onClick: () -> Unit) {
    val hasBusiness = trips.any { it.tripType == "BUSINESS" || it.tripType == "Customer Visit" }
    val hasCommute = trips.any { it.tripType == "COMMUTE" || it.tripType == "Home To Work" }
    val hasPrivate = trips.any { it.tripType == "PRIVATE" || it.tripType == "PERSONAL" }

    val bgColor = when {
        hasBusiness && hasPrivate -> MaterialTheme.colorScheme.tertiaryContainer // Mix
        hasBusiness -> MaterialTheme.colorScheme.primaryContainer
        hasCommute -> MaterialTheme.colorScheme.secondaryContainer
        hasPrivate -> Color(0xFFE8F5E9) // Light green for private
        else -> Color.Transparent
    }

    val textColor = when {
        hasBusiness && hasPrivate -> MaterialTheme.colorScheme.onTertiaryContainer
        hasBusiness -> MaterialTheme.colorScheme.onPrimaryContainer
        hasCommute -> MaterialTheme.colorScheme.onSecondaryContainer
        hasPrivate -> Color(0xFF1B5E20)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = trips.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            if (trips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.Center) {
                    trips.take(3).forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .padding(horizontal = 1.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}
