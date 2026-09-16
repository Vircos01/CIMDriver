package com.cimdriver.app.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.service.TrackingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

class MainCarScreen(carContext: CarContext) : Screen(carContext) {

    private val db = com.cimdriver.app.data.local.AppDatabase.getDatabase(carContext)
    private var activeTrip: com.cimdriver.app.data.local.entity.Trip? = null

    init {
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                db.vehicleDao().getAllVehicles(),
                db.tripDao().getAllTrips()
            ) { vehicles, trips ->
                val activeVehicleId = CarUtil.getContextVehicleId(carContext, db, vehicles)
                if (activeVehicleId != null) {
                    val filteredTrips = trips.filter { it.vehicleId == activeVehicleId }
                    filteredTrips.find { it.status == "ACTIVE" } ?: trips.find { it.status == "ACTIVE" }
                } else {
                    trips.find { it.status == "ACTIVE" }
                }
            }.collect { active ->
                activeTrip = active
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val currentActiveTrip = activeTrip
        
        if (currentActiveTrip != null) {
            val paneBuilder = Pane.Builder()
                .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_location)).setTint(CarColor.GREEN).build())
                .addRow(Row.Builder()
                    .setTitle(carContext.getString(R.string.trip_current))
                    .addText(carContext.getString(R.string.trip_from, currentActiveTrip.startAddress ?: carContext.getString(R.string.trip_location_unknown)))
                    .build()
                )
                .addRow(Row.Builder()
                    .setTitle(carContext.getString(R.string.start_time))
                    .addText(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentActiveTrip.startTime)))
                    .build()
                )
                .addAction(Action.Builder()
                    .setTitle(carContext.getString(R.string.stop_trip))
                    .setOnClickListener {
                        val intent = android.content.Intent(carContext, com.cimdriver.app.service.TrackingService::class.java).apply {
                            action = "STOP_TRACKING"
                        }
                        androidx.core.content.ContextCompat.startForegroundService(carContext, intent)
                    }
                    .build()
                )

            return PaneTemplate.Builder(paneBuilder.build())
                .setTitle(carContext.getString(R.string.trip_registering))
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        val itemListBuilder = ItemList.Builder()
        
        // Hoofdmenu opties
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.my_trips))
                .addText(carContext.getString(R.string.review_recent_trips))
                .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_history)).build())
                .setOnClickListener { screenManager.push(RecentTripsCarScreen(carContext)) }
                .build()
        )
        
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.start_new_trip))
                .addText(carContext.getString(R.string.start_manual_trip_hint))
                .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_add)).build())
                .setOnClickListener { screenManager.push(StartTripScreen(carContext)) }
                .build()
        )
        
        itemListBuilder.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.address_book))
                .addText(carContext.getString(R.string.nav_to_saved_address))
                .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_address)).build())
                .setOnClickListener { screenManager.push(AddressBookCarScreen(carContext)) }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_main_menu))
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemListBuilder.build())
            .build()
    }
}
