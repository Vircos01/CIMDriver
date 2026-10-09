package com.cimdriver.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.R
import com.cimdriver.app.ui.viewmodel.FuelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelScreen(
    onOpenDrawer: () -> Unit = {},
    onAddFuelClick: () -> Unit,
    onEditFuelClick: (Long) -> Unit,
    viewModel: FuelViewModel = hiltViewModel()
) {
    val fillUps by viewModel.fillUps.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    Scaffold(
        topBar = { 
            CimDriverTopAppBar(
                onOpenDrawer = onOpenDrawer,
                title = { Text(stringResource(R.string.fuel), color = com.cimdriver.app.ui.theme.CIMDriverWhite) }
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFuelClick) {
                Icon(Icons.Default.Add, contentDescription = "Toevoegen")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (vehicles.isNotEmpty() && fillUps.isNotEmpty()) {
                // Show stats for the first vehicle for now, or total
                val selectedVehicle = vehicles.first()
                val stats = viewModel.getStatsForVehicle(selectedVehicle.id, fillUps)
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Statistieken (${selectedVehicle.name})", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Totale kosten", style = MaterialTheme.typography.bodySmall)
                                Text("€${String.format("%.2f", stats.totalCost)}", style = MaterialTheme.typography.titleLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val isEV = selectedVehicle.engineType == "EV"
                                Text("Gemiddeld verbruik", style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.1f", stats.averageConsumption)} ${if (isEV) "kWh" else "L"}/100km", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            if (fillUps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.LocalGasStation,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Geen tankbeurten gevonden.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val drafts = fillUps.filter { it.status == "DRAFT" }
                val history = fillUps.filter { it.status != "DRAFT" }.sortedByDescending { it.dateTimestamp }

                LazyColumn {
                    if (drafts.isNotEmpty()) {
                        item {
                            Text(
                                "Te beoordelen tankbeurten",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(drafts) { fillUp ->
                            FuelCard(fillUp, vehicles) { onEditFuelClick(fillUp.id) }
                        }
                    }

                    if (history.isNotEmpty()) {
                        item {
                            Text(
                                "Historie",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(history) { fillUp ->
                            FuelCard(fillUp, vehicles) { onEditFuelClick(fillUp.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FuelCard(fillUp: com.cimdriver.app.data.local.entity.FuelFillUp, vehicles: List<com.cimdriver.app.data.local.entity.Vehicle>, onClick: () -> Unit) {
    val vehicle = vehicles.find { it.id == fillUp.vehicleId }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val isEV = vehicle?.engineType == "EV"
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(fillUp.dateTimestamp)),
                    style = MaterialTheme.typography.titleMedium
                )
                if (fillUp.status == "DRAFT") {
                    Icon(Icons.Default.Add, contentDescription = "Concept", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Voertuig: ${vehicle?.name ?: "Onbekend"}")
            Text("${if (isEV) "Geladen (kWh)" else "Getankt (Liters)"}: ${String.format("%.2f", fillUp.liters)}")
            Text("Totale kosten: €${String.format("%.2f", fillUp.totalCost)}")
            Text("Prijs per ${if (isEV) "kWh" else "liter"}: €${String.format("%.2f", fillUp.pricePerLiter)}")
            Text("Kilometerstand: ${fillUp.odometer} km")
            if (!fillUp.stationName.isNullOrBlank()) {
                Text("Locatie: ${fillUp.stationName}")
            }
        }
    }
}
