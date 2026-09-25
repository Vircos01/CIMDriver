package com.cimdriver.app.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.lifecycle.lifecycleScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.service.TrackingService
import kotlinx.coroutines.launch
import com.cimdriver.app.R

class StartTripScreen(carContext: CarContext) : Screen(carContext) {

    private val db = AppDatabase.getDatabase(carContext)
    private var vehicles: List<Vehicle> = emptyList()
    private var selectedVehicleId: Long? = null
    private var isLoading = true

    init {
        lifecycleScope.launch {
            db.vehicleDao().getAllVehicles().collect { allVehicles ->
                vehicles = allVehicles
                selectedVehicleId = CarUtil.getContextVehicleId(carContext, db, allVehicles) ?: allVehicles.firstOrNull()?.id
                isLoading = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): androidx.car.app.model.Template {
        if (isLoading) {
            return MessageTemplate.Builder(carContext.getString(R.string.loading))
                .setTitle(carContext.getString(R.string.start_trip))
                .setHeaderAction(Action.BACK)
                .build()
        }

        if (vehicles.isEmpty()) {
            return MessageTemplate.Builder(carContext.getString(R.string.add_vehicle_first))
                .setTitle(carContext.getString(R.string.no_vehicles))
                .setHeaderAction(Action.BACK)
                .build()
        }

        val itemListBuilder = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Home To Work")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_commute)).setTint(androidx.car.app.model.CarColor.GREEN).build())
                    .setOnClickListener { startTrip("Home To Work") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Business Meeting")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Business Meeting") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Customer Visit")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Customer Visit") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Customer Billable")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Customer Billable") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Commissioned By CIMSOLUTIONS")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Commissioned By CIMSOLUTIONS") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Exam Course")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Exam Course") }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Car Maintenance")
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_business)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startTrip("Car Maintenance") }
                    .build()
            )


        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.start_manual_trip))
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .build()
    }

    private fun startTrip(tripType: String) {
        val vehicleId = selectedVehicleId ?: return
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tripDao = db.tripDao()
                val vehicleDao = db.vehicleDao()
                val vehicle = vehicleDao.getVehicleById(vehicleId)
                val startOdo = vehicle?.odometerCurrent ?: 0

                val activeTrip = tripDao.getActiveTripForVehicle(vehicleId)
                val tripId = activeTrip?.id ?: tripDao.insertTrip(
                    com.cimdriver.app.data.local.entity.Trip(
                        vehicleId = vehicleId,
                        startTime = System.currentTimeMillis(),
                        endTime = null,
                        startAddress = "Locatie ophalen...",
                        endAddress = null,
                        distanceMeters = 0,
                        tripType = tripType,
                        note = null,
                        status = "ACTIVE",
                        isManual = true,
                        odometerStart = startOdo,
                        odometerEnd = null
                    )
                )

                val intent = Intent(carContext, TrackingService::class.java).apply {
                    action = "START_TRACKING"
                    putExtra("TRIP_ID", tripId)
                }
                
                // Use ContextCompat to safely start foreground service
                androidx.core.content.ContextCompat.startForegroundService(carContext, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        screenManager.pop()
    }
}
