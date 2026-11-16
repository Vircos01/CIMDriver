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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
import com.cimdriver.app.ui.components.core.CimDriverTopBar
import com.cimdriver.app.ui.components.core.TopBarNavType
import com.cimdriver.app.ui.components.WorkHoursSettingsCard
import com.cimdriver.app.ui.viewmodel.SettingsViewModel
import com.cimdriver.app.util.WorkHoursUtil
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDaysEditorScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    
    val daySettings = remember(initialSettings.workDays) {
        val parsed = WorkHoursUtil.parseWorkHoursString(
            initialSettings.workDays, 
            initialSettings.workStartTime, 
            initialSettings.workEndTime
        )
        androidx.compose.runtime.mutableStateListOf(*parsed.toTypedArray())
    }
    
    var breakMinutesStr by remember(initialSettings.defaultBreakMinutes) { mutableStateOf(initialSettings.defaultBreakMinutes.toString()) }
    var toleranceMinutesStr by remember(initialSettings.workHoursToleranceMinutes) { mutableStateOf(initialSettings.workHoursToleranceMinutes.toString()) }
    var wifiWorkHoursEnabled by remember(initialSettings.wifiWorkHoursEnabled) { mutableStateOf(initialSettings.wifiWorkHoursEnabled) }
    var wifiBufferMinutesStr by remember(initialSettings.wifiDisconnectBufferMinutes) { mutableStateOf(initialSettings.wifiDisconnectBufferMinutes.toString()) }

    Scaffold(
        topBar = {
            CimDriverTopBar(
                title = stringResource(R.string.work_hours),
                navType = TopBarNavType.BACK,
                onNavClick = onBack,
                actions = {
                    IconButton(onClick = {
                        val serializedDays = WorkHoursUtil.serializeWorkHours(daySettings)
                        val breakMins = breakMinutesStr.toIntOrNull() ?: 30
                        val toleranceMins = toleranceMinutesStr.toIntOrNull() ?: 30
                        viewModel.updateWorkingHours(
                            initialSettings.workStartTime, 
                            initialSettings.workEndTime, 
                            serializedDays, 
                            breakMins, 
                            toleranceMins
                        )
                        viewModel.updateWifiWorkHoursSettings(
                            wifiWorkHoursEnabled,
                            wifiBufferMinutesStr.toIntOrNull() ?: 15
                        )
                        Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(Icons.Filled.Check, stringResource(R.string.save), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                }
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
            WorkHoursSettingsCard(
                daySettings = daySettings,
                breakMinutesStr = breakMinutesStr,
                onBreakMinutesChange = { breakMinutesStr = it },
                toleranceMinutesStr = toleranceMinutesStr,
                onToleranceMinutesChange = { toleranceMinutesStr = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kantoor-Wi-Fi registratie",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Voor fiets-, loop- of OV-forensen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = wifiWorkHoursEnabled,
                            onCheckedChange = { wifiWorkHoursEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Registreert werkuren automatisch wanneer je verbinding maakt met het Wi-Fi-netwerk van een werklocatie (ingesteld in het Adresboek). Er wordt géén autorit geregistreerd. Pauzes (binnen en buiten kantoor) worden automatisch verwerkt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (wifiWorkHoursEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = wifiBufferMinutesStr,
                            onValueChange = { wifiBufferMinutesStr = it },
                            label = { Text("Buffer bij verbreken (minuten)") },
                            supportingText = { Text("Wachttijd voordat een werkdag wordt afgerond na verlies van Wi-Fi verbinding (bijv. tijdens lunchwandeling). Standaard: 15 min.") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
    }
}
