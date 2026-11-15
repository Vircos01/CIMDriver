package com.cimdriver.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import com.cimdriver.app.ui.components.core.CimDriverTopBar
import com.cimdriver.app.ui.components.core.TopBarNavType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showRestartDialog by remember { mutableStateOf(false) }
    var showClearLocationDialog by remember { mutableStateOf(false) }
    var showResetAllDataDialog by remember { mutableStateOf(false) }

    val (latestDbBackup, latestCsvExport) = remember {
        val documentsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
        val backupDir = java.io.File(documentsDir, "CIMDriver/Backups")
        if (backupDir.exists()) {
            val files = backupDir.listFiles()
            val latestZip = files?.filter { it.extension.equals("zip", ignoreCase = true) }?.maxByOrNull { it.lastModified() }
            val latestCsv = files?.filter { it.extension.equals("csv", ignoreCase = true) }?.maxByOrNull { it.lastModified() }
            Pair(latestZip, latestCsv)
        } else Pair(null, null)
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            com.cimdriver.app.util.BackupUtil.createBackup(context, uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = com.cimdriver.app.util.BackupUtil.restoreBackup(context, uri)
            if (success) {
                showRestartDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            CimDriverTopBar(
                title = stringResource(R.string.data_management),
                navType = TopBarNavType.BACK,
                onNavClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Status automatische back-up & export
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.status_backup_and_exports),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Database back-up sectie (.zip)
                    Text(
                        text = stringResource(R.string.latest_db_backup),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (latestDbBackup != null) {
                        val fmt = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale.getDefault())
                        val sizeKb = (latestDbBackup.length() / 1024).coerceAtLeast(1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.backup_validated_restorable),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.backup_db_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tijdstip: ${fmt.format(java.util.Date(latestDbBackup.lastModified()))} (${sizeKb} KB)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Bestand: ${latestDbBackup.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_db_backup_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Periodieke CSV export sectie (.csv)
                    Text(
                        text = stringResource(R.string.latest_csv_export),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (latestCsvExport != null) {
                        val fmt = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale.getDefault())
                        val sizeKb = (latestCsvExport.length() / 1024).coerceAtLeast(1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.csv_export_readonly_badge),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.csv_export_readonly_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tijdstip: ${fmt.format(java.util.Date(latestCsvExport.lastModified()))} (${sizeKb} KB)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Bestand: ${latestCsvExport.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_csv_export_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Back-up & Herstel", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { backupLauncher.launch("cimdriver_backup.zip") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.create_backup))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(text = stringResource(R.string.restore_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.restore_backup), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Gegevens Wissen", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Waarschuwing: Gegevens wissen kan niet ongedaan worden gemaakt.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { showClearLocationDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.clear_location_history_title))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { showResetAllDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.reset_all_data_title))
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restore_completed)) },
            text = { Text(stringResource(R.string.backup_restored_msg)) },
            confirmButton = {
                Button(onClick = { kotlin.system.exitProcess(0) }) {
                    Text(stringResource(R.string.understood_close))
                }
            }
        )
    }

    if (showClearLocationDialog) {
        AlertDialog(
            onDismissRequest = { showClearLocationDialog = false },
            title = { Text(stringResource(R.string.clear_location_history_title)) },
            text = { Text(stringResource(R.string.clear_location_history_desc)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLocationHistory()
                    showClearLocationDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLocationDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showResetAllDataDialog) {
        var resetConfirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetAllDataDialog = false },
            title = { Text(stringResource(R.string.reset_all_data_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.reset_all_data_desc))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetConfirmation,
                        onValueChange = { resetConfirmation = it },
                        label = { Text(stringResource(R.string.type_delete_all)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = resetConfirmation == stringResource(R.string.type_delete_all).removePrefix("Typ "),
                    onClick = {
                        viewModel.resetAllData()
                        showResetAllDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
