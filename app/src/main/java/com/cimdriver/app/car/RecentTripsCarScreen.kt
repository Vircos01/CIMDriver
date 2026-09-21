package com.cimdriver.app.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.util.TripCategory
import com.cimdriver.app.util.TripClassification
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.service.TrackingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cimdriver.app.R

class RecentTripsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val db = AppDatabase.getDatabase(carContext)
    private var allTrips: List<Trip> = emptyList()
    private var filteredTrips: List<Trip> = emptyList()
    private var activeTrip: Trip? = null
    private var showAllTrips = false
    private var classificationRules = emptyList<com.cimdriver.app.data.local.entity.ClassificationRule>()
    private var savedAddresses = emptyList<com.cimdriver.app.data.local.entity.SavedAddress>()

    init {
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                db.vehicleDao().getAllVehicles(),
                db.tripDao().getAllTrips(),
                db.classificationRuleDao().getAllRules(),
                db.savedAddressDao().getAllAddresses(),
                db.settingsDao().getSettings()
            ) { vehicles, trips, rules, addresses, settings ->
                val activeVehicleId = CarUtil.getContextVehicleId(carContext, db, vehicles)
                val currentSettings = settings ?: com.cimdriver.app.data.local.entity.Settings()
                if (activeVehicleId != null) {
                    val filtered = trips.filter { it.vehicleId == activeVehicleId }
                    val active = trips.find { it.status == "ACTIVE" }
                    listOf(trips, filtered, active ?: filtered.find { it.status == "ACTIVE" }, rules, addresses, currentSettings)
                } else {
                    listOf(trips, emptyList<Trip>(), trips.find { it.status == "ACTIVE" }, rules, addresses, currentSettings)
                }
            }.collect { values ->
                @Suppress("UNCHECKED_CAST")
                allTrips = values[0] as List<Trip>
                @Suppress("UNCHECKED_CAST")
                filteredTrips = values[1] as List<Trip>
                activeTrip = values[2] as Trip?
                @Suppress("UNCHECKED_CAST")
                classificationRules = values[3] as List<com.cimdriver.app.data.local.entity.ClassificationRule>
                @Suppress("UNCHECKED_CAST")
                savedAddresses = values[4] as List<com.cimdriver.app.data.local.entity.SavedAddress>
                appSettings = values[5] as com.cimdriver.app.data.local.entity.Settings
                invalidate() // Refresh the screen when data changes
            }
        }
    }

    private var appSettings: com.cimdriver.app.data.local.entity.Settings = com.cimdriver.app.data.local.entity.Settings()

    override fun onGetTemplate(): Template {
        val currentActiveTrip = activeTrip

        if (currentActiveTrip != null) {
            val paneBuilder = Pane.Builder()
                .addRow(Row.Builder()
                    .setTitle(carContext.getString(R.string.from))
                    .addText(currentActiveTrip.startAddress ?: "Locatie onbekend")
                    .build()
                )
                .addRow(Row.Builder()
                    .setTitle(carContext.getString(R.string.start_time))
                    .addText(formatTime(currentActiveTrip.startTime))
                    .build()
                )
                .addAction(Action.Builder()
                    .setTitle(carContext.getString(R.string.stop_trip))
                    .setOnClickListener {
                        val intent = Intent(carContext, TrackingService::class.java).apply {
                            action = "STOP_TRACKING"
                        }
                        androidx.core.content.ContextCompat.startForegroundService(carContext, intent)
                    }
                    .build()
                )

            return PaneTemplate.Builder(paneBuilder.build())
                .setTitle(carContext.getString(R.string.trip_registering))
                .setHeaderAction(Action.BACK)
                .build()
        } else {
            val listToShow = if (showAllTrips) allTrips else filteredTrips
            val recentTrips = listToShow.filter { it.status != "ACTIVE" }.take(4) // Max 6 items in list, leaving room for toggle

            val itemListBuilder = ItemList.Builder()

            // Toggle row at the top
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(if (showAllTrips) "Toon alleen actieve auto" else "Toon alle ritten (alle auto's)")
                    .setOnClickListener {
                        showAllTrips = !showAllTrips
                        invalidate()
                    }
                    .build()
            )

            if (recentTrips.isEmpty()) {
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(carContext.getString(R.string.no_recent_trips))
                        .build()
                )
            } else {
                for (trip in recentTrips) {
                    val afstandKm = String.format(Locale.getDefault(), "%.1f km", trip.distanceMeters / 1000f)
                    var iconRes = com.cimdriver.app.R.drawable.ic_car_person
                    var carColor = CarColor.BLUE
                    val category = TripClassification.classify(
                        tripType = trip.tripType,
                        startAddressType = addressType(trip.startAddress),
                        endAddressType = addressType(trip.endAddress),
                        defaultCategory = if (appSettings.classificationDefault == "BUSINESS") TripCategory.BUSINESS else TripCategory.PRIVATE,
                        homeWorkAsCommute = appSettings.classifyHomeWorkAsCommute,
                        customerAsBusiness = appSettings.classifyCustomerAsBusiness,
                        rules = classificationRules,
                        timestamp = trip.startTime,
                        workDaysStr = appSettings.workDays,
                        workStartTime = appSettings.workStartTime,
                        workEndTime = appSettings.workEndTime
                    )
                    
                    if (category == TripCategory.BUSINESS) {
                        iconRes = com.cimdriver.app.R.drawable.ic_car_business
                        carColor = CarColor.PRIMARY
                    } else if (category == TripCategory.COMMUTE) {
                        iconRes = com.cimdriver.app.R.drawable.ic_car_commute
                        carColor = CarColor.GREEN
                    }
                    val categoryStr = trip.tripType
                    
                    val titleSpan = android.text.SpannableString("${formatDate(trip.startTime)} - $afstandKm ($categoryStr)")
                    
                    val rowBuilder = Row.Builder()
                        .setTitle(titleSpan)
                        .addText("${trip.startAddress ?: "?"} \u2192 ${trip.endAddress ?: "?"}")
                        .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, iconRes)).setTint(carColor).build())
                        
                    if (trip.status == "TO_REVIEW") {
                        rowBuilder.setOnClickListener { screenManager.push(ReviewTripScreen(carContext, trip.id)) }
                    }
                    
                    itemListBuilder.addItem(rowBuilder.build())
                }
            }

            return ListTemplate.Builder()
                .setTitle(if (showAllTrips) "Alle Ritten" else "Recente Ritten")
                .setHeaderAction(Action.BACK)
                .setSingleList(itemListBuilder.build())
                .build()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun addressType(address: String?): String? {
        val saved = com.cimdriver.app.util.AddressMatching.findSavedAddress(address, savedAddresses) ?: return null
        return saved.addressType ?: when {
            saved.isHomeLocation -> "THUIS"
            saved.isWorkLocation -> "WERK"
            saved.isCustomerLocation -> "KLANT"
            else -> null
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
