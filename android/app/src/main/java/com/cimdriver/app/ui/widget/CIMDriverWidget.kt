package com.cimdriver.app.ui.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cimdriver.app.MainActivity
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.domain.DashboardStatsCalculator
import com.cimdriver.app.service.TrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CIMDriverWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context)
        
        provideContent {
            GlanceTheme {
                WidgetContent(context, data)
            }
        }
    }

    private suspend fun loadData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val cal = Calendar.getInstance()
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            
            val endOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val allTrips = db.tripDao().getAllTripsSync()
            val activeTrip = allTrips.find { it.status == "ACTIVE" }
            val monthTrips = allTrips.filter { it.startTime in startOfMonth..endOfMonth }
            
            val vehicles = db.vehicleDao().getAllVehiclesSync()
            val addresses = db.savedAddressDao().getAllSavedAddressesSync()
            val rules = db.classificationRuleDao().getAllRulesSync()
            val settings = db.settingsDao().getSettingsSync()
            
            val stats = DashboardStatsCalculator.calculateDashboardStats(
                tripList = monthTrips,
                timeFilter = com.cimdriver.app.ui.viewmodel.TimeFilter.MONTH,
                selectedVehicleId = null,
                vehicleList = vehicles,
                rules = rules,
                addresses = addresses,
                settings = settings
            )
            
            val defaultVehicle = vehicles.find { it.isDefault } ?: vehicles.firstOrNull()

            WidgetData(
                zakelijkKm = stats.zakelijkKm,
                priveKm = stats.priveKm,
                woonWerkKm = stats.woonWerkKm,
                activeTrip = activeTrip,
                vehicleName = defaultVehicle?.let { "${it.make} ${it.model}".trim().ifBlank { it.name } }
            )
        } catch (e: Exception) {
            WidgetData(0.0, 0.0, 0.0, null, null)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (data.activeTrip != null) {
                ActiveTripContent(data.activeTrip)
            } else {
                IdleMonthContent(data)
            }
        }
    }

    @Composable
    private fun ActiveTripContent(trip: Trip) {
        val isPrivate = trip.tripType.equals("PRIVATE", ignoreCase = true) || trip.tripType.equals("PERSONAL", ignoreCase = true)
        val typeLabel = if (isPrivate) "Privé" else "Zakelijk"
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(trip.startTime))
        val distKmStr = String.format(Locale.getDefault(), "%.1f km", trip.distanceMeters / 1000.0)

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚗 Rit actief: $typeLabel",
                style = TextStyle(
                    color = if (isPrivate) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = distKmStr,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "Gestart om $timeStr • ${trip.startAddress?.split(",")?.firstOrNull() ?: "Onderweg"}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                text = if (isPrivate) "Naar Zakelijk" else "Naar Privé",
                onClick = actionRunCallback<ToggleTripTypeActionCallback>(),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Button(
                text = "Stop Rit",
                onClick = actionRunCallback<StopTripActionCallback>(),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }

    @Composable
    private fun IdleMonthContent(data: WidgetData) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deze Maand",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            if (data.vehicleName != null) {
                Text(
                    text = data.vehicleName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MetricColumn(title = "Zakelijk", value = data.zakelijkKm, color = GlanceTheme.colors.primary, modifier = GlanceModifier.defaultWeight())
            MetricColumn(title = "Privé", value = data.priveKm, color = GlanceTheme.colors.error, modifier = GlanceModifier.defaultWeight())
            MetricColumn(title = "Woon-Werk", value = data.woonWerkKm, color = GlanceTheme.colors.tertiary, modifier = GlanceModifier.defaultWeight())
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                text = "▶️ Zakelijk",
                onClick = actionRunCallback<StartBusinessTripActionCallback>(),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Button(
                text = "▶️ Privé",
                onClick = actionRunCallback<StartPrivateTripActionCallback>(),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }

    @Composable
    private fun MetricColumn(title: String, value: Double, color: androidx.glance.unit.ColorProvider, modifier: GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.0f", value),
                style = TextStyle(color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

data class WidgetData(
    val zakelijkKm: Double,
    val priveKm: Double,
    val woonWerkKm: Double,
    val activeTrip: Trip? = null,
    val vehicleName: String? = null
)

class StartBusinessTripActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        startWidgetTrip(context, "Business Meeting")
        CIMDriverWidget().update(context, glanceId)
    }
}

class StartPrivateTripActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        startWidgetTrip(context, "PRIVATE")
        CIMDriverWidget().update(context, glanceId)
    }
}

class StopTripActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = "STOP_TRACKING"
        }
        ContextCompat.startForegroundService(context, intent)
        CIMDriverWidget().update(context, glanceId)
    }
}

class ToggleTripTypeActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_TOGGLE_TRIP_TYPE
        }
        ContextCompat.startForegroundService(context, intent)
        CIMDriverWidget().update(context, glanceId)
    }
}

private suspend fun startWidgetTrip(context: Context, tripType: String) = withContext(Dispatchers.IO) {
    try {
        val db = AppDatabase.getDatabase(context)
        val vehicles = db.vehicleDao().getAllVehiclesSync()
        val vehicle = vehicles.find { it.isDefault } ?: vehicles.firstOrNull() ?: return@withContext
        val activeTrip = db.tripDao().getActiveTripForVehicle(vehicle.id)
        val tripId = if (activeTrip != null) {
            activeTrip.id
        } else {
            val prevTrip = db.tripDao().getLatestTripForVehicle(vehicle.id)
            val initialStartAddress = if (prevTrip != null && !prevTrip.endAddress.isNullOrBlank() && prevTrip.status != "ACTIVE") {
                prevTrip.endAddress
            } else {
                "Locatie ophalen..."
            }
            db.tripDao().insertTrip(
                Trip(
                    vehicleId = vehicle.id,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    startAddress = initialStartAddress,
                    endAddress = null,
                    distanceMeters = 0,
                    tripType = tripType,
                    note = null,
                    status = "ACTIVE",
                    isManual = true,
                    odometerStart = vehicle.odometerCurrent,
                    odometerEnd = null
                )
            )
        }
        val intent = Intent(context, TrackingService::class.java).apply {
            action = "START_TRACKING"
            putExtra("TRIP_ID", tripId)
        }
        ContextCompat.startForegroundService(context, intent)
    } catch (e: Exception) {
        Log.e("CIMDriverWidget", "Error starting widget trip", e)
    }
}
