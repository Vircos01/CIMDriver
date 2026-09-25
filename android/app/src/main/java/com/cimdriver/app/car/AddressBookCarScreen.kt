package com.cimdriver.app.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.lifecycle.lifecycleScope
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.SavedAddress
import kotlinx.coroutines.launch
import com.cimdriver.app.R

class AddressBookCarScreen(carContext: CarContext) : Screen(carContext) {

    private val db = AppDatabase.getDatabase(carContext)
    private var addresses: List<SavedAddress> = emptyList()
    private var isLoading = true

    init {
        lifecycleScope.launch {
            db.savedAddressDao().getAllAddresses().collect { list ->
                addresses = list
                isLoading = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): androidx.car.app.model.Template {
        if (isLoading) {
            return MessageTemplate.Builder(carContext.getString(R.string.loading_address_book))
                .setTitle(carContext.getString(R.string.address_book))
                .setHeaderAction(Action.BACK)
                .build()
        }

        if (addresses.isEmpty()) {
            return MessageTemplate.Builder(carContext.getString(R.string.no_addresses_yet))
                .setTitle(carContext.getString(R.string.address_book))
                .setHeaderAction(Action.BACK)
                .build()
        }

        val itemListBuilder = ItemList.Builder()
        
        for (addr in addresses) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(addr.label)
                    .addText(addr.address)
                    .setImage(androidx.car.app.model.CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.cimdriver.app.R.drawable.ic_car_navigation)).setTint(androidx.car.app.model.CarColor.PRIMARY).build())
                    .setOnClickListener { startNavigation(addr.address) }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.address_book))
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .build()
    }

    private fun startNavigation(address: String) {
        val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(address)}")
        val intent = android.content.Intent(androidx.car.app.CarContext.ACTION_NAVIGATE, uri)
        
        try {
            carContext.startCarApp(intent)
            androidx.car.app.CarToast.makeText(carContext, "Navigeren naar ${address}", androidx.car.app.CarToast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            androidx.car.app.CarToast.makeText(carContext, "Kan navigatie niet starten (App niet gevonden?)", androidx.car.app.CarToast.LENGTH_LONG).show()
        }
    }
}
