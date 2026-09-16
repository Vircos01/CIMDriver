package com.cimdriver.app.ui.screens

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import com.cimdriver.app.ui.viewmodel.TripDetailViewModel
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

val osmStyleJson = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "&copy; OpenStreetMap Contributors"
    }
  },
  "layers": [
    {
      "id": "osm",
      "type": "raster",
      "source": "osm",
      "minzoom": 0,
      "maxzoom": 22
    }
  ]
}
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun TripDetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    onEditTrip: (Long) -> Unit,
    onDuplicateTrip: (Long, Boolean) -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val locale = currentAppLocale()
    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    val trip by viewModel.trip.collectAsState()
    val points by viewModel.points.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.trip_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    var showRecalculateConfirm by remember { mutableStateOf(false) }

                    if (showRecalculateConfirm) {
                        AlertDialog(
                            onDismissRequest = { showRecalculateConfirm = false },
                            title = { Text(stringResource(R.string.recalculate_single_trip_title)) },
                            text = { Text(stringResource(R.string.recalculate_single_trip_desc)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.recalculateOdometers()
                                    showRecalculateConfirm = false
                                }) {
                                    Text(stringResource(R.string.recalculate), color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRecalculateConfirm = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }

                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                expanded = false
                                onEditTrip(tripId)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            onClick = {
                                expanded = false
                                onDuplicateTrip(tripId, false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.create_return_trip)) },
                            onClick = {
                                expanded = false
                                onDuplicateTrip(tripId, true)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recalculate_single_trip_title)) },
                            onClick = {
                                expanded = false
                                showRecalculateConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.cimdriver.app.ui.theme.CIMDriverNavy,
                    titleContentColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                    navigationIconContentColor = com.cimdriver.app.ui.theme.CIMDriverWhite,
                    actionIconContentColor = com.cimdriver.app.ui.theme.CIMDriverWhite
                )
            )
        }
    ) { padding ->
        if (trip == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
                    val startStr = dateFormat.format(Date(trip!!.startTime))
                    val endStr = trip!!.endTime?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.in_progress)
                    val durationText = trip!!.endTime?.let { end ->
                        val diffMinutes = (end - trip!!.startTime) / (1000 * 60)
                        val hours = diffMinutes / 60
                        val mins = diffMinutes % 60
                        if (hours > 0) "${hours}u ${mins}m" else "${mins}m"
                    } ?: "..."
                    val dist = String.format(locale, "%.1f km", trip!!.distanceMeters / 1000.0)

                    Text(stringResource(R.string.date_range, startStr, endStr), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.from_address, trip!!.startAddress ?: stringResource(R.string.unknown)))
                    Text(stringResource(R.string.to_address, trip!!.endAddress ?: stringResource(R.string.unknown)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.distance_format, dist), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.duration_format, durationText), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(stringResource(R.string.type_format, trip!!.tripType ?: stringResource(R.string.unknown)))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val odoStartStr = String.format(locale, "%,d", trip!!.odometerStart).replace(',', '.')
                    val odoEndStr = trip!!.odometerEnd?.let { String.format(locale, "%,d", it).replace(',', '.') } ?: "..."
                    Text(
                        text = stringResource(R.string.odometer_range_format, odoStartStr, odoEndStr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!trip!!.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.note_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(trip!!.note ?: "")
                    }
                }
            }

            if (trip!!.status == "TO_REVIEW") {
                val tripOptions = listOf("Home To Work", "Business Meeting", "Customer Visit", "Customer Billable", "Commissioned By CIMSOLUTIONS", "Exam Course", "Car Maintenance")
                var selectedType by remember(trip!!.tripType) { 
                    mutableStateOf(if (tripOptions.contains(trip!!.tripType)) trip!!.tripType else "Home To Work") 
                }
                var typeExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.rate_this_trip), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                label = { Text(stringResource(R.string.trip_type_label)) }
                            )
                            DropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                tripOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            selectedType = selectionOption
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.saveReview(selectedType) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MapLibreMapView(points = points)
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
fun MapLibreMapView(points: List<com.cimdriver.app.data.local.entity.LocationPoint>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            map.getMapAsync { mapboxMap ->
                mapboxMap.setStyle(Style.Builder().fromJson(osmStyleJson)) { _ ->
                    if (points.isNotEmpty()) {
                        mapboxMap.clear()
                        val latLngs = points.map { LatLng(it.latitude, it.longitude) }

                        mapboxMap.addPolyline(PolylineOptions().addAll(latLngs).color(Color.BLUE).width(5f))
                        mapboxMap.addMarker(MarkerOptions().position(latLngs.first()).title(context.getString(R.string.start)))

                        if (latLngs.size > 1) {
                            mapboxMap.addMarker(MarkerOptions().position(latLngs.last()).title(context.getString(R.string.end)))
                        }

                        val bounds = LatLngBounds.Builder().includes(latLngs).build()
                        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
                    }
                }
            }
        }
    )
}
