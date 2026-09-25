package com.cimdriver.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.WorkDay
import com.cimdriver.app.ui.viewmodel.WorkHoursViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkHoursScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: WorkHoursViewModel = hiltViewModel(),
    onAddWorkDay: () -> Unit = {},
    onEditWorkDay: (Long) -> Unit = {}
) {
    val locale = currentAppLocale()
    val workDays by viewModel.workDays.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val groupedWorkDays by viewModel.groupedAndFilteredWorkDays.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val availablePeriods by viewModel.availablePeriods.collectAsState()

    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }
    var workDayToDelete by remember { mutableStateOf<WorkDay?>(null) }

    var isSearchVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_ellipsis), color = com.cimdriver.app.ui.theme.CIMDriverWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                unfocusedTextColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                                cursorColor = com.cimdriver.app.ui.theme.CIMDriverWhite
                            )
                        )
                    }
                    if (!isSearchVisible) {
                        Text(stringResource(R.string.work_hours), color = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    } else {
                        IconButton(onClick = { 
                            isSearchVisible = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.export), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Deel (CSV)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_werkuren.csv", "text/csv"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportWorkHoursToCsv(context, uri, workDays)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deel (PDF)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_werkuren.pdf", "application/pdf"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportWorkHoursToPdf(context, uri, workDays)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWorkDay) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_work_day))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { viewModel.setViewMode("WEEK") },
                            selected = viewMode == "WEEK"
                        ) {
                            Text(stringResource(R.string.week))
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { viewModel.setViewMode("MONTH") },
                            selected = viewMode == "MONTH"
                        ) {
                            Text(stringResource(R.string.month))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    var periodExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { periodExpanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(selectedPeriod)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = periodExpanded,
                            onDismissRequest = { periodExpanded = false }
                        ) {
                            availablePeriods.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period) },
                                    onClick = {
                                        viewModel.setSelectedPeriod(period)
                                        periodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (groupedWorkDays.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isNotBlank()) {
                        Text(stringResource(R.string.no_work_hours_search_results, searchQuery))
                    } else {
                        Text(stringResource(R.string.no_work_hours))
                    }
                }
            } else {
                val dateFormat = SimpleDateFormat("EEEE d MMM", java.util.Locale.forLanguageTag("nl-NL"))
                val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))

                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    groupedWorkDays.forEach { (groupName, days) ->
                        item {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(days, key = { it.id }) { workDay ->
                        val isToReview = workDay.status == "TO_REVIEW"
                        val isInProgress = workDay.status == "IN_PROGRESS"
                        val effectiveStart = workDay.roundedArrivalTime ?: workDay.arrivalTime
                        val effectiveEnd = workDay.roundedDepartureTime ?: workDay.departureTime ?: workDay.lastArrivalTime ?: workDay.arrivalTime
                        val effectiveBreakMinutes = viewModel.effectiveBreakMinutes(workDay)
                        val totalMillis = viewModel.netDurationMillis(workDay)
                        val totalHours = totalMillis / 3600000f

                        com.cimdriver.app.ui.components.SwipeToDeleteContainer(
                            modifier = Modifier.animateItem(),
                            onDelete = { workDayToDelete = workDay },
                            backgroundPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                                    onEditWorkDay(workDay.id)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isToReview) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateFormat.format(Date(workDay.date)), style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isToReview) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = stringResource(R.string.to_be_rated),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        if (isInProgress) {
                                            Text(
                                                text = stringResource(R.string.in_progress), 
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primaryContainer, 
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Text(stringResource(R.string.total_time_hours, String.format(locale, "%.1f", totalHours)), style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                                
                                
                                val exactStart = timeFormat.format(Date(workDay.arrivalTime))
                                val exactEnd = workDay.departureTime?.let { timeFormat.format(Date(it)) } ?: workDay.lastArrivalTime?.let { timeFormat.format(Date(it)) } ?: exactStart
                                val roundedStart = workDay.roundedArrivalTime?.let { timeFormat.format(Date(it)) } ?: exactStart
                                val roundedEnd = workDay.roundedDepartureTime?.let { timeFormat.format(Date(it)) } ?: exactEnd
                                
                                if (isInProgress) {
                                    Text(stringResource(R.string.worked_hours_from, exactStart), style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text(stringResource(R.string.worked_hours_range, roundedStart, roundedEnd), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.exact_times_range, exactStart, exactEnd), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(stringResource(R.string.break_minutes_label, effectiveBreakMinutes.toString()), style = MaterialTheme.typography.bodyMedium)
                                if (workDay.workLocationLabel != null) {
                                    Text(stringResource(R.string.location_label, workDay.workLocationLabel), style = MaterialTheme.typography.bodySmall)
                                }
                                if (workDay.projectCode != null) {
                                    Text(stringResource(R.string.project_label, workDay.projectCode), style = MaterialTheme.typography.bodySmall)
                                }
                                
                                if (isToReview) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(onClick = {
                                            viewModel.updateWorkDay(
                                                id = workDay.id,
                                                date = workDay.date,
                                                firstDepartureTime = workDay.firstDepartureTime,
                                                arrivalTime = workDay.arrivalTime,
                                                departureTime = workDay.departureTime,
                                                lastArrivalTime = workDay.lastArrivalTime,
                                                roundedArrivalTime = workDay.roundedArrivalTime,
                                                roundedDepartureTime = workDay.roundedDepartureTime,
                                                breakMinutes = workDay.breakMinutes,
                                                workLocationLabel = workDay.workLocationLabel,
                                                projectCode = workDay.projectCode,
                                                status = "APPROVED"
                                            )
                                        }) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.approve))
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    if (workDayToDelete != null) {
        AlertDialog(
            onDismissRequest = { workDayToDelete = null },
            title = { Text(stringResource(R.string.delete_work_day)) },
            text = { Text(stringResource(R.string.delete_work_day_confirmation)) },
            confirmButton = {
                Button(onClick = {
                    workDayToDelete?.let { viewModel.deleteWorkDay(it) }
                    workDayToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { workDayToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
}


