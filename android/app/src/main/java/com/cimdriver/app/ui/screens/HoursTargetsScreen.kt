package com.cimdriver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.HoursTarget
import com.cimdriver.app.ui.viewmodel.ClientProjectViewModel
import com.cimdriver.app.ui.viewmodel.HoursTargetViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoursTargetsScreen(
    onBack: () -> Unit,
    viewModel: HoursTargetViewModel = hiltViewModel(),
    clientProjectViewModel: ClientProjectViewModel = hiltViewModel()
) {
    val targetProgresses by viewModel.targetProgresses.collectAsState()
    val clients by clientProjectViewModel.clients.collectAsState()
    val projectCodes by clientProjectViewModel.projectCodes.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var targetToEdit by remember { mutableStateOf<HoursTarget?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Uren Targets $currentYear", color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug", tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = com.cimdriver.app.ui.theme.CIMDriverGreen,
                contentColor = com.cimdriver.app.ui.theme.CIMDriverWhite
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Target Toevoegen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (targetProgresses.isEmpty()) {
                item {
                    Text(
                        "Je hebt nog geen uren targets voor dit jaar ingesteld.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            
            items(targetProgresses, key = { it.targetDetails.target.id }) { progress ->
                val target = progress.targetDetails.target
                val clientName = progress.targetDetails.clientName
                val projectCodeStr = progress.targetDetails.projectCodeStr
                
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
                            Text(target.name, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { targetToEdit = target }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Bewerk", modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        val subtitleParts = mutableListOf<String>()
                        if (clientName != null) subtitleParts.add(clientName)
                        if (projectCodeStr != null) subtitleParts.add(projectCodeStr)
                        if (subtitleParts.isEmpty()) subtitleParts.add("Alle uren")
                        
                        Text(subtitleParts.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${String.format("%.1f", progress.accumulatedHours)} / ${target.targetHours} uur")
                            Text("${String.format("%.1f", progress.percentage * 100)}%")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val defaultColor = MaterialTheme.colorScheme.primary
                        // Parse color or fallback to primary
                        val barColor = try { Color(android.graphics.Color.parseColor(target.color)) } catch (e: Exception) { defaultColor }
                        
                        LinearProgressIndicator(
                            progress = { progress.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (progress.isBehind) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Achter op schema: ${String.format("%.1f", progress.diffHours)} uur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            } else {
                                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = com.cimdriver.app.ui.theme.CIMDriverGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Op schema", style = MaterialTheme.typography.bodySmall, color = com.cimdriver.app.ui.theme.CIMDriverGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || targetToEdit != null) {
        val isEdit = targetToEdit != null
        var name by remember { mutableStateOf(targetToEdit?.name ?: "") }
        var targetHoursStr by remember { mutableStateOf(targetToEdit?.targetHours?.toString() ?: "1600.0") }
        var selectedClientId by remember { mutableStateOf(targetToEdit?.clientId) }
        var selectedProjectId by remember { mutableStateOf(targetToEdit?.projectCodeId) }
        
        // Simple color options
        val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336")
        var selectedColor by remember { mutableStateOf(targetToEdit?.color ?: colors[0]) }

        var clientExpanded by remember { mutableStateOf(false) }
        var projectExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                targetToEdit = null
            },
            title = { Text(if (isEdit) "Target Bewerken" else "Nieuw Target") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Naam target") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetHoursStr,
                        onValueChange = { targetHoursStr = it },
                        label = { Text("Doeluren") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Client Dropdown
                    ExposedDropdownMenuBox(
                        expanded = clientExpanded,
                        onExpandedChange = { clientExpanded = !clientExpanded }
                    ) {
                        OutlinedTextField(
                            value = clients.find { it.id == selectedClientId }?.name ?: "Alle klanten (geen filter)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Klant Filter") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = clientExpanded,
                            onDismissRequest = { clientExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alle klanten (geen filter)") },
                                onClick = { 
                                    selectedClientId = null
                                    selectedProjectId = null
                                    clientExpanded = false
                                }
                            )
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name) },
                                    onClick = { 
                                        selectedClientId = client.id
                                        selectedProjectId = null
                                        clientExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Project Dropdown
                    val availableProjects = if (selectedClientId != null) projectCodes.filter { it.clientId == selectedClientId } else projectCodes
                    
                    ExposedDropdownMenuBox(
                        expanded = projectExpanded,
                        onExpandedChange = { projectExpanded = !projectExpanded }
                    ) {
                        OutlinedTextField(
                            value = availableProjects.find { it.id == selectedProjectId }?.code ?: "Alle projecten (geen filter)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project Filter") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = projectExpanded,
                            onDismissRequest = { projectExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alle projecten (geen filter)") },
                                onClick = { 
                                    selectedProjectId = null
                                    projectExpanded = false
                                }
                            )
                            availableProjects.forEach { pc ->
                                DropdownMenuItem(
                                    text = { Text(pc.code) },
                                    onClick = { 
                                        selectedProjectId = pc.id
                                        selectedClientId = pc.clientId ?: selectedClientId
                                        projectExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kleur", style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.forEach { colorHex ->
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == colorHex) 2.dp else 0.dp,
                                        color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hours = targetHoursStr.toDoubleOrNull()
                        if (name.isNotBlank() && hours != null) {
                            if (isEdit) {
                                viewModel.updateTarget(targetToEdit!!.copy(
                                    name = name,
                                    targetHours = hours,
                                    clientId = selectedClientId,
                                    projectCodeId = selectedProjectId,
                                    color = selectedColor
                                ))
                            } else {
                                viewModel.addTarget(
                                    HoursTarget(
                                        name = name,
                                        targetHours = hours,
                                        year = currentYear,
                                        clientId = selectedClientId,
                                        projectCodeId = selectedProjectId,
                                        color = selectedColor
                                    )
                                )
                            }
                            showAddDialog = false
                            targetToEdit = null
                        }
                    }
                ) { Text("Opslaan") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    targetToEdit = null
                }) { Text("Annuleren") }
            }
        )
    }
}
