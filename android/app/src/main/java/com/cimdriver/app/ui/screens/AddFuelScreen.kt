package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.data.local.entity.FuelFillUp
import com.cimdriver.app.ui.viewmodel.FuelViewModel
import com.cimdriver.app.ui.components.VehicleSelectionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelScreen(
    fillUpId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: FuelViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsState()
    
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.id ?: 0L) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    val isEV = selectedVehicle?.engineType == "EV" || selectedVehicle?.engineType == "PHEV"
    
    var litersStr by remember { mutableStateOf("") }
    var pricePerLiterStr by remember { mutableStateOf("") }
    var totalCostStr by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    var stationName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var existingFillUp by remember { mutableStateOf<FuelFillUp?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(fillUpId) {
        if (fillUpId != null) {
            val fillUp = viewModel.getFillUpById(fillUpId)
            if (fillUp != null) {
                existingFillUp = fillUp
                selectedVehicleId = fillUp.vehicleId
                if (fillUp.liters > 0) litersStr = fillUp.liters.toString()
                if (fillUp.pricePerLiter > 0) pricePerLiterStr = fillUp.pricePerLiter.toString()
                if (fillUp.totalCost > 0) totalCostStr = fillUp.totalCost.toString()
                odometerStr = fillUp.odometer.toString()
                stationName = fillUp.stationName ?: fillUp.address ?: ""
                notes = fillUp.notes ?: ""
                latitude = fillUp.latitude
                longitude = fillUp.longitude
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (fillUpId == null) "Tankbeurt Toevoegen" else "Tankbeurt Bewerken", color = com.cimdriver.app.ui.theme.CIMDriverWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug", tint = com.cimdriver.app.ui.theme.CIMDriverWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val liters = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val pricePerLiter = pricePerLiterStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val totalCost = totalCostStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val odometer = odometerStr.toIntOrNull() ?: 0

                            if (selectedVehicleId != 0L) {
                                val fillUp = FuelFillUp(
                                    vehicleId = selectedVehicleId,
                                    dateTimestamp = System.currentTimeMillis(),
                                    liters = liters,
                                    pricePerLiter = pricePerLiter,
                                    totalCost = totalCost,
                                    odometer = odometer,
                                    stationName = stationName.takeIf { it.isNotBlank() },
                                    address = stationName.takeIf { it.isNotBlank() },
                                    latitude = latitude,
                                    longitude = longitude,
                                    notes = notes,
                                    status = "COMPLETED"
                                )
                                if (existingFillUp != null) {
                                    viewModel.updateFillUp(fillUp.copy(id = existingFillUp!!.id, dateTimestamp = existingFillUp!!.dateTimestamp))
                                } else {
                                    viewModel.insertFillUp(fillUp)
                                }
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Opslaan",
                            tint = com.cimdriver.app.ui.theme.CIMDriverWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }

            VehicleSelectionCard(
                selectedVehicle = selectedVehicle,
                vehicles = vehicles,
                vehicleExpanded = expanded,
                onVehicleExpandedChange = { expanded = it },
                onVehicleSelected = { selectedVehicleId = it.id }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { litersStr = it },
                        label = { Text(if (isEV) "kWh" else "Liters") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pricePerLiterStr,
                        onValueChange = { pricePerLiterStr = it },
                        label = { Text(if (isEV) "Prijs per kWh (€)" else "Prijs per liter (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = totalCostStr,
                        onValueChange = { totalCostStr = it },
                        label = { Text("Totale kosten (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it },
                        label = { Text("Kilometerstand (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Locatie", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    AutoCompleteAddressField(
                value = stationName,
                onValueChange = { 
                    stationName = it
                    latitude = null
                    longitude = null
                    coroutineScope.launch {
                        val coords = viewModel.getCoordinatesForAddress(it)
                        if (coords != null) {
                            latitude = coords.first
                            longitude = coords.second
                        }
                    }
                },
                label = if (isEV) "Locatie laadpaal" else "Tankstation Locatie",
                onSearchAddress = { query -> viewModel.searchAddress(query) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val lat = latitude
            val lon = longitude
            if (lat != null && lon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    SinglePointMapView(lat, lon)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
                }
            }

            if (existingFillUp != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteFillUp(existingFillUp!!)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Verwijderen")
                }
            }
        }
    }
}
