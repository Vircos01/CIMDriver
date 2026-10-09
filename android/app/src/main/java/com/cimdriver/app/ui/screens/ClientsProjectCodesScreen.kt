package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.Client
import com.cimdriver.app.data.local.entity.ProjectCode
import com.cimdriver.app.ui.viewmodel.ClientProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsProjectCodesScreen(
    onBack: () -> Unit,
    viewModel: ClientProjectViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsState()
    val projectCodes by viewModel.projectCodes.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    
    var showAddProjectDialogForClient by remember { mutableStateOf<Long?>(null) }
    var projectToEdit by remember { mutableStateOf<ProjectCode?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Klanten & Projectcodes", color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
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
        floatingActionButton = {}
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Toon gearchiveerde items", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = showArchived,
                        onCheckedChange = { viewModel.toggleShowArchived() }
                    )
                }
            }

            items(clients, key = { it.id }) { client ->
                val clientProjects = projectCodes.filter { it.clientId == client.id }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(client.name, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { clientToEdit = client }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Bewerk", modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        if (clientProjects.isEmpty()) {
                            Text("Geen projectcodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            clientProjects.forEach { project ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            project.code, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (project.isActive) null else TextDecoration.LineThrough,
                                            color = if (project.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        if (!project.description.isNullOrBlank()) {
                                            Text(
                                                project.description, 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = if (project.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { projectToEdit = project }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Bewerk", modifier = Modifier.size(16.dp))
                                        }
                                        if (project.isActive) {
                                            IconButton(onClick = {
                                                viewModel.deleteProjectCode(project) { success, message ->
                                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Verwijder", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        } else {
                                            IconButton(onClick = {
                                                viewModel.restoreProjectCode(project)
                                                android.widget.Toast.makeText(context, "Projectcode hersteld", android.widget.Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Filled.Refresh, contentDescription = "Herstel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        TextButton(onClick = { showAddProjectDialogForClient = client.id }) {
                            Text("+ Projectcode toevoegen")
                        }
                    }
                }
            }
            
            // "No Client" project codes
            val noClientProjects = projectCodes.filter { it.clientId == null }
            if (noClientProjects.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Zonder klant", style = MaterialTheme.typography.titleMedium)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            noClientProjects.forEach { project ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            project.code, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (project.isActive) null else TextDecoration.LineThrough,
                                            color = if (project.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        if (!project.description.isNullOrBlank()) {
                                            Text(
                                                project.description, 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = if (project.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { projectToEdit = project }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Bewerk", modifier = Modifier.size(16.dp))
                                        }
                                        if (project.isActive) {
                                            IconButton(onClick = {
                                                viewModel.deleteProjectCode(project) { success, message ->
                                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Verwijder", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        } else {
                                            IconButton(onClick = {
                                                viewModel.restoreProjectCode(project)
                                                android.widget.Toast.makeText(context, "Projectcode hersteld", android.widget.Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Filled.Refresh, contentDescription = "Herstel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = { showAddProjectDialogForClient = -1L }) { // -1 for null
                                Text("+ Projectcode toevoegen")
                            }
                        }
                    }
                }
            } else if (clients.isNotEmpty()) {
                item {
                     TextButton(onClick = { showAddProjectDialogForClient = -1L }) {
                        Text("+ Vrije projectcode toevoegen")
                     }
                }
            }
            
            if (clients.isEmpty() && noClientProjects.isEmpty()) {
                item {
                    Text(
                        "Je hebt nog geen klanten of projectcodes toegevoegd. Gebruik de knop hieronder om te beginnen.",
                        modifier = Modifier.padding(top = 32.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showAddClientDialog || clientToEdit != null) {
        val isEdit = clientToEdit != null
        var name by remember { mutableStateOf(clientToEdit?.name ?: "") }
        
        AlertDialog(
            onDismissRequest = { 
                showAddClientDialog = false
                clientToEdit = null
            },
            title = { Text(if (isEdit) "Klant Bewerken" else "Nieuwe Klant") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Klantnaam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            if (isEdit) {
                                viewModel.updateClient(clientToEdit!!.copy(name = name))
                            } else {
                                viewModel.addClient(name = name)
                            }
                            showAddClientDialog = false
                            clientToEdit = null
                        }
                    }
                ) { Text("Opslaan") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddClientDialog = false
                    clientToEdit = null
                }) { Text("Annuleren") }
            }
        )
    }

    if (showAddProjectDialogForClient != null || projectToEdit != null) {
        val isEdit = projectToEdit != null
        var code by remember { mutableStateOf(projectToEdit?.code ?: "") }
        var description by remember { mutableStateOf(projectToEdit?.description ?: "") }
        var isBillable by remember { mutableStateOf(projectToEdit?.isBillable ?: true) }
        
        val actualClientId = if (isEdit) {
            projectToEdit?.clientId
        } else {
            if (showAddProjectDialogForClient == -1L) null else showAddProjectDialogForClient
        }
        
        AlertDialog(
            onDismissRequest = { 
                showAddProjectDialogForClient = null
                projectToEdit = null
            },
            title = { Text(if (isEdit) "Projectcode Bewerken" else "Nieuwe Projectcode") },
            text = {
                Column {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Projectcode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Omschrijving (optioneel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isBillable,
                            onCheckedChange = { isBillable = it }
                        )
                        Text("Declarabel", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (code.isNotBlank()) {
                            if (isEdit) {
                                viewModel.updateProjectCode(projectToEdit!!.copy(
                                    code = code,
                                    description = description.takeIf { it.isNotBlank() },
                                    isBillable = isBillable
                                ))
                            } else {
                                viewModel.addProjectCode(
                                    code = code,
                                    description = description.takeIf { it.isNotBlank() },
                                    clientId = actualClientId,
                                    isBillable = isBillable
                                )
                            }
                            showAddProjectDialogForClient = null
                            projectToEdit = null
                        }
                    }
                ) { Text("Opslaan") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddProjectDialogForClient = null
                    projectToEdit = null
                }) { Text("Annuleren") }
            }
        )
    }
}
