package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cimdriver.app.R
import com.cimdriver.app.data.local.entity.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CimDriverTopAppBar(
    onOpenDrawer: () -> Unit,
    title: @Composable () -> Unit = { },
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { }
) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.open_menu),
                    tint = com.cimdriver.app.ui.theme.CIMDriverWhite
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy
        )
    )
}

@Composable
fun VehicleSelectorTopBarAction(
    vehicles: List<Vehicle>,
    activeVehicleId: Long?,
    onVehicleSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    if (vehicles.isNotEmpty()) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar, 
                    contentDescription = stringResource(R.string.select_vehicle),
                    tint = com.cimdriver.app.ui.theme.CIMDriverWhite
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                vehicles.forEach { v ->
                    val isSelected = v.id == activeVehicleId
                    DropdownMenuItem(
                        text = { Text("${v.name} - ${v.licensePlate}", fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else null) },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.selected),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        onClick = {
                            onVehicleSelected(v.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSearchAddress: suspend (String) -> List<String>,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    
    LaunchedEffect(value) {
        if (value.length >= 3 && !expanded) {
            val results = onSearchAddress(value)
            if (results.isNotEmpty() && !results.contains(value)) {
                suggestions = results
                expanded = true
            }
        } else {
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { }
    ) {
        @Suppress("DEPRECATION")
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = false
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = trailingIcon,
            supportingText = supportingText
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
