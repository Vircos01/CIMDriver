package com.cimdriver.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cimdriver.app.MainActivity
import com.cimdriver.app.R
import com.cimdriver.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CIMDriverWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val activeTrip = db.tripDao().getAllTripsSync().firstOrNull { it.status == "ACTIVE" }
                
                if (activeTrip != null) {
                    val durationMs = System.currentTimeMillis() - activeTrip.startTime
                    val min = (durationMs / 60000).toInt()
                    val km = String.format("%.1f", activeTrip.distanceMeters / 1000f)
                    
                    views.setTextViewText(R.id.widget_status, "Rit actief: $min min, $km km")
                } else {
                    views.setTextViewText(R.id.widget_status, "Geen actieve rit")
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
