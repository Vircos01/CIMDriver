package com.cimdriver.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
import com.cimdriver.app.ui.components.ClassificationSettingsCard
import com.cimdriver.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripClassificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val classificationRules by viewModel.classificationRules.collectAsState()
    
    val initialSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
    var businessCompensationStr by remember(initialSettings.businessCompensation) { 
        mutableStateOf(initialSettings.businessCompensation.toString()) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_classification), color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val comp = businessCompensationStr.replace(',', '.').toFloatOrNull() ?: 0.23f
                        viewModel.updateBusinessCompensation(comp)
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
            ClassificationSettingsCard(viewModel, initialSettings, classificationRules)
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Zakelijke vergoeding", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Vergoeding per km in euro's (bijv. 0.23)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = businessCompensationStr,
                        onValueChange = { businessCompensationStr = it },
                        label = { Text("Vergoeding per km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
