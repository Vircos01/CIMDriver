package com.cimdriver.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
import com.cimdriver.app.ui.components.OdometerSettingsCard
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    
    var odometerReminder by remember(initialSettings.odometerReminder) { mutableStateOf(initialSettings.odometerReminder) }
    var odometerIntervalStr by remember(initialSettings.odometerReminderIntervalDays) { mutableStateOf(initialSettings.odometerReminderIntervalDays.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val odoDays = odometerIntervalStr.toIntOrNull() ?: 30
                        viewModel.updateOdometerReminder(odometerReminder, odoDays)
                        
                        Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(Icons.Filled.Check, stringResource(R.string.save), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy)
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
            OdometerSettingsCard(
                isEnabled = odometerReminder,
                onEnabledChange = { odometerReminder = it },
                intervalDaysStr = odometerIntervalStr,
                onIntervalDaysChange = { odometerIntervalStr = it }
            )
        }
    }
}
