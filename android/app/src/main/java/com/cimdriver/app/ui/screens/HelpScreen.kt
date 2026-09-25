package com.cimdriver.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            com.cimdriver.app.ui.screens.CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = { Text(stringResource(R.string.help), color = com.cimdriver.app.ui.theme.CIMDriverWhite) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            HelpSection(
                icon = Icons.Filled.Dashboard,
                title = stringResource(R.string.help_dashboard_title),
                description = stringResource(R.string.help_dashboard_desc)
            )

            HelpSection(
                icon = Icons.Filled.Bluetooth,
                title = stringResource(R.string.help_bluetooth_title),
                description = stringResource(R.string.help_bluetooth_desc)
            )

            HelpSection(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.help_manual_title),
                description = stringResource(R.string.help_manual_desc)
            )

            HelpSection(
                icon = Icons.AutoMirrored.Filled.CallMerge,
                title = stringResource(R.string.help_merge_title),
                description = stringResource(R.string.help_merge_desc)
            )
            
            HelpSection(
                icon = Icons.Filled.DirectionsCar,
                title = stringResource(R.string.help_classification_title),
                description = stringResource(R.string.help_classification_desc)
            )

            HelpSection(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.help_hours_title),
                description = stringResource(R.string.help_hours_desc)
            )

            HelpSection(
                icon = Icons.Filled.Contacts,
                title = stringResource(R.string.help_addressbook_title),
                description = stringResource(R.string.help_addressbook_desc)
            )

            HelpSection(
                icon = Icons.Filled.DirectionsCar,
                title = stringResource(R.string.help_vehicles_title),
                description = stringResource(R.string.help_vehicles_desc)
            )
            
            HelpSection(
                icon = Icons.Filled.FileDownload,
                title = stringResource(R.string.help_export_title),
                description = stringResource(R.string.help_export_desc)
            )

            HelpSection(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.help_settings_title),
                description = stringResource(R.string.help_settings_desc)
            )

            HelpSection(
                icon = Icons.Filled.DirectionsCar,
                title = stringResource(R.string.help_android_auto_title),
                description = stringResource(R.string.help_android_auto_desc)
            )

            HelpSection(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.help_backup_title),
                description = stringResource(R.string.help_backup_desc)
            )
        }
    }
}

@Composable
fun HelpSection(icon: ImageVector, title: String, description: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Inklappen" else "Uitklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
