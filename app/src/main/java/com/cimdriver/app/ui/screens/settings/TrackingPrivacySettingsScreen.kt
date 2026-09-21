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
import com.cimdriver.app.ui.components.TrackingSettingsCard
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import com.cimdriver.app.ui.components.PrivacySettingsCard
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingPrivacySettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    
    var gracePeriodMinsStr by remember(initialSettings.gracePeriodSec) { 
        mutableStateOf((initialSettings.gracePeriodSec / 60).toString()) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_and_privacy), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val graceSec = (gracePeriodMinsStr.toIntOrNull() ?: 3) * 60
                        viewModel.updateGracePeriod(graceSec)
                        
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
            TrackingSettingsCard(
                gracePeriodMinsStr = gracePeriodMinsStr,
                onGracePeriodChange = { gracePeriodMinsStr = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // We only show retention days here, as wipe history / delete all is moved to DataManagement
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.privacy), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_desc), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.retention_period), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            selected = initialSettings.locationRetentionDays == 30,
                            onClick = { viewModel.updateLocationRetentionDays(30) }
                        ) { Text(stringResource(R.string.retention_30_days)) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            selected = initialSettings.locationRetentionDays == 90,
                            onClick = { viewModel.updateLocationRetentionDays(90) }
                        ) { Text(stringResource(R.string.retention_90_days)) }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            selected = initialSettings.locationRetentionDays == 365,
                            onClick = { viewModel.updateLocationRetentionDays(365) }
                        ) { Text(stringResource(R.string.retention_1_year)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            com.cimdriver.app.ui.components.PermissionsSettingsCard()
        }
    }
}
