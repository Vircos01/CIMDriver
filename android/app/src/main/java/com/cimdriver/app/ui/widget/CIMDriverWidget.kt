package com.cimdriver.app.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
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
import com.cimdriver.app.domain.DashboardStatsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class CIMDriverWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data in background
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
            
            WidgetData(
                zakelijkKm = stats.zakelijkKm,
                priveKm = stats.priveKm,
                woonWerkKm = stats.woonWerkKm
            )
        } catch (e: Exception) {
            WidgetData(0.0, 0.0, 0.0)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Deze Maand",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MetricColumn(title = "Zakelijk", value = data.zakelijkKm, color = GlanceTheme.colors.primary, modifier = GlanceModifier.defaultWeight())
                MetricColumn(title = "Privé", value = data.priveKm, color = GlanceTheme.colors.error, modifier = GlanceModifier.defaultWeight())
                MetricColumn(title = "Woon-Werk", value = data.woonWerkKm, color = GlanceTheme.colors.tertiary, modifier = GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun MetricColumn(title: String, value: Double, color: androidx.glance.unit.ColorProvider, modifier: GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = String.format("%.0f", value),
                style = TextStyle(color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

data class WidgetData(
    val zakelijkKm: Double,
    val priveKm: Double,
    val woonWerkKm: Double
)
