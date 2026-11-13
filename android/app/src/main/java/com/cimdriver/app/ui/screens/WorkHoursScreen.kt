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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.WorkDay
import com.cimdriver.app.ui.viewmodel.WorkHoursViewModel
import com.cimdriver.app.ui.components.core.CimDriverEmptyState
import androidx.compose.material.icons.filled.Schedule
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
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
    val workDaySuggestions by viewModel.workDaySuggestions.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportMenu by remember { mutableStateOf(false) }
    var workDayToDelete by remember { mutableStateOf<WorkDay?>(null) }

    var isSearchVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            if (groupedWorkDays.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Per periode:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                    onClick = {},
                                    enabled = false
                                )
                                groupedWorkDays.forEach { (groupName, days) ->
                                    val slug = groupName.replace(" ", "_").replace("-", "_")
                                    DropdownMenuItem(
                                        text = { Text("  $groupName (PDF)") },
                                        onClick = {
                                            showExportMenu = false
                                            com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                                context, "cimdriver_werkuren_${slug}.pdf", "application/pdf"
                                            ) { uri ->
                                                com.cimdriver.app.util.ExportUtil.exportWorkHoursToPdf(context, uri, days, title = groupName)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("  $groupName (CSV)") },
                                        onClick = {
                                            showExportMenu = false
                                            com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                                context, "cimdriver_werkuren_${slug}.csv", "text/csv"
                                            ) { uri ->
                                                com.cimdriver.app.util.ExportUtil.exportWorkHoursToCsv(context, uri, days)
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(if (groupedWorkDays.isNotEmpty()) "Alle werkdagen (PDF)" else "Deel (PDF)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_werkuren.pdf", "application/pdf"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportWorkHoursToPdf(context, uri, workDays)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (groupedWorkDays.isNotEmpty()) "Alle werkdagen (CSV)" else "Deel (CSV)") },
                                onClick = {
                                    showExportMenu = false
                                    com.cimdriver.app.util.ExportUtil.shareExportedFile(
                                        context, "cimdriver_werkuren.csv", "text/csv"
                                    ) { uri ->
                                        com.cimdriver.app.util.ExportUtil.exportWorkHoursToCsv(context, uri, workDays)
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
                    CimDriverEmptyState(
                        icon = Icons.Filled.Schedule,
                        title = if (searchQuery.isNotBlank()) "Geen werkuren gevonden" else stringResource(R.string.no_work_hours),
                        message = if (searchQuery.isNotBlank()) "Geen resultaten voor \"$searchQuery\"."
                                  else "Er zijn nog geen werkdagen geregistreerd voor deze periode.",
                        actionText = if (searchQuery.isBlank()) "Werkdag toevoegen" else null,
                        actionIcon = Icons.Filled.Add,
                        onActionClick = if (searchQuery.isBlank()) onAddWorkDay else null
                    )
                }
            } else {
                val dateFormat = SimpleDateFormat("EEEE d MMM", java.util.Locale.forLanguageTag("nl-NL"))
                val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))

                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    groupedWorkDays.forEach { (groupName, days) ->
                        item(key = "header_$groupName") {
                            WorkHoursHeaderItem(
                                groupName = groupName,
                                days = days,
                                context = context
                            )
                        }
                        items(days, key = { it.id }) { workDay ->
                        val isToReview = workDay.status == "TO_REVIEW"
                        val isApproved = workDay.status == "APPROVED" || workDay.status == "DONE"
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
                                    containerColor = if (isToReview) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
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
                                        } else if (isApproved) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Goedgekeurd",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(18.dp)
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

                                val suggestion = workDaySuggestions[workDay.id]
                                suggestion?.let { sug ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val badgeColor = when {
                                        isApproved -> Color(0xFF16A34A)
                                        isInProgress -> MaterialTheme.colorScheme.primary
                                        sug.isHighConfidence -> Color(0xFF16A34A)
                                        else -> Color(0xFFD97706)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isApproved || sug.isHighConfidence) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sug.statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                            color = badgeColor
                                        )
                                    }
                                }

                                if (isToReview && !isInProgress) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val sug = suggestion ?: com.cimdriver.app.util.WorkHoursTriage.evaluateWorkDay(workDay)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Slimme suggestie:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            val depTimeStr = sug.suggestedDepartureTime?.let { timeFormat.format(Date(it)) } ?: "17:00"
                                            Text(
                                                text = "Vertrek: $depTimeStr • Pauze: ${sug.suggestedBreakMinutes} min" +
                                                        (sug.suggestedProjectCode?.let { " • Project: $it" } ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Button(
                                                    onClick = {
                                                        viewModel.quickApproveWorkDay(com.cimdriver.app.util.WorkDayReviewCandidate(workDay, sug))
                                                        coroutineScope.launch {
                                                            val res = snackbarHostState.showSnackbar(
                                                                message = "Werkdag goedgekeurd",
                                                                actionLabel = "Ongedaan maken",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            if (res == SnackbarResult.ActionPerformed) {
                                                                viewModel.undoLastApproval()
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Akkoord met suggestie", style = MaterialTheme.typography.labelMedium)
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

@Composable
private fun WorkHoursHeaderItem(
    groupName: String,
    days: List<WorkDay>,
    context: Context
) {
    var showMenu by remember { mutableStateOf(false) }
    val slug = groupName.replace(" ", "_").replace("-", "_")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = groupName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Box {
            OutlinedButton(
                onClick = { showMenu = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Deel $groupName",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deel", style = MaterialTheme.typography.labelSmall)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Exporteer PDF ($groupName)") },
                    onClick = {
                        showMenu = false
                        com.cimdriver.app.util.ExportUtil.shareExportedFile(
                            context, "cimdriver_werkuren_${slug}.pdf", "application/pdf"
                        ) { uri ->
                            com.cimdriver.app.util.ExportUtil.exportWorkHoursToPdf(context, uri, days, title = groupName)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exporteer CSV ($groupName)") },
                    onClick = {
                        showMenu = false
                        com.cimdriver.app.util.ExportUtil.shareExportedFile(
                            context, "cimdriver_werkuren_${slug}.csv", "text/csv"
                        ) { uri ->
                            com.cimdriver.app.util.ExportUtil.exportWorkHoursToCsv(context, uri, days)
                        }
                    }
                )
            }
        }
    }
}


