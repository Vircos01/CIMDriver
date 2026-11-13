package com.cimdriver.app.car

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.entity.Vehicle

object CarUtil {
    fun getLocalizedTripType(tripType: String?): String {
        return when (tripType) {
            "Home To Work", "COMMUTE" -> "Woon-werk"
            "Business Meeting" -> "Zakelijk overleg"
            "Customer Visit" -> "Klantbezoek"
            "Customer Billable" -> "Klant declarabel"
            "Commissioned By CIMSOLUTIONS" -> "In opdracht CIMSOLUTIONS"
            "Exam Course" -> "Cursus / Examen"
            "Car Maintenance" -> "Auto-onderhoud"
            "PRIVATE", "PERSONAL" -> "Privé"
            "BUSINESS" -> "Zakelijk"
            null -> "Onbekend"
            else -> tripType
        }
    }

    suspend fun getContextVehicleId(context: Context, db: AppDatabase, vehicles: List<Vehicle>): Long? {
        try {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true // On older Android versions, we only need BLUETOOTH which is usually granted at install time
            }

            if (hasPermission) {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP) +
                        bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET)
                val connectedMacs = connectedDevices.map { it.address }

                if (connectedMacs.isNotEmpty()) {
                    val btDao = db.bluetoothDeviceDao()
                    for (mac in connectedMacs) {
                        val btDevice = btDao.getDeviceByMac(mac)
                        if (btDevice != null) {
                            return btDevice.vehicleId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CarUtil", "Error getting context vehicle id", e)
        }
        
        // Fallback to default vehicle
        return vehicles.find { it.isDefault }?.id ?: vehicles.firstOrNull()?.id
    }
}
