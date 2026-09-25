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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.work_hours), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
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
            WorkHoursSettingsCard(
                daySettings = daySettings,
                breakMinutesStr = breakMinutesStr,
                onBreakMinutesChange = { breakMinutesStr = it },
                toleranceMinutesStr = toleranceMinutesStr,
                onToleranceMinutesChange = { toleranceMinutesStr = it }
            )
        }
    }
}
