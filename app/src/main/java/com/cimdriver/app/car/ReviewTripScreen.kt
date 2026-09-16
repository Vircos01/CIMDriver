package com.cimdriver.app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.lifecycle.lifecycleScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Trip
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.cimdriver.app.R

class ReviewTripScreen(carContext: CarContext, private val tripId: Long) : Screen(carContext) {

    private val db = AppDatabase.getDatabase(carContext)
    private var trip: Trip? = null
    private var classificationRules = emptyList<com.cimdriver.app.data.local.entity.ClassificationRule>()

    init {
        lifecycleScope.launch {
            val t = db.tripDao().getTripById(tripId)
            trip = t
            classificationRules = db.classificationRuleDao().getAllRules().first()
            invalidate()
        }
    }

    override fun onGetTemplate(): androidx.car.app.model.Template {
        val currentTrip = trip ?: return MessageTemplate.Builder(carContext.getString(R.string.trip_loading))
            .setTitle(carContext.getString(R.string.rate))
            .setHeaderAction(Action.BACK)
            .build()
            
        if (currentTrip.status != "TO_REVIEW") {
            return MessageTemplate.Builder(carContext.getString(R.string.trip_already_rated))
                .setTitle(carContext.getString(R.string.rated))
                .setHeaderAction(Action.BACK)
                .addAction(Action.Builder().setTitle(carContext.getString(R.string.back)).setOnClickListener { screenManager.pop() }.build())
                .build()
        }

        val tripTypes = classificationRules.mapNotNull { it.tripType }.distinct().ifEmpty {
            listOf("Home To Work", "Business Meeting", "Customer Visit", "Customer Billable", "Commissioned By CIMSOLUTIONS", "Exam Course", "Car Maintenance", "PERSONAL")
        }
        val itemListBuilder = androidx.car.app.model.ItemList.Builder()
        tripTypes.forEach { tripType ->
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(tripType)
                    .setOnClickListener { updateTripType(tripType) }
                    .build()
            )
        }


        return androidx.car.app.model.ListTemplate.Builder()
            .setTitle("Beoordeel: ${currentTrip.startAddress?.take(10) ?: "?"}..")
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .build()
    }

    private fun updateTripType(type: String) {
        val currentTrip = trip ?: return
        lifecycleScope.launch {
            db.tripDao().updateTrip(
                currentTrip.copy(
                    tripType = type,
                    status = "APPROVED"
                )
            )
            screenManager.pop()
        }
    }
}
