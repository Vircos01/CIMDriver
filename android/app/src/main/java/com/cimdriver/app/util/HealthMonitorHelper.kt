package com.cimdriver.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class HealthIssue(
    val id: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val isCritical: Boolean,
    val createIntent: (Context) -> Intent
)

object HealthMonitorHelper {

    fun checkHealth(context: Context): List<HealthIssue> {
        val issues = mutableListOf<HealthIssue>()
        val packageName = context.packageName

        // 1. Batterijoptimalisatie check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                issues.add(
                    HealthIssue(
                        id = "battery_optimization",
                        title = "Batterijbesparing actief",
                        description = "Android kan ritten onderbreken om stroom te besparen. Schakel batterijoptimalisatie uit voor betrouwbare tracking.",
                        actionLabel = "Uitschakelen",
                        isCritical = true,
                        createIntent = { ctx ->
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${ctx.packageName}")
                            }
                        }
                    )
                )
            }
        }

        // 2. Achtergrondlocatie check (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBgLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBgLocation) {
                issues.add(
                    HealthIssue(
                        id = "background_location",
                        title = "Achtergrondlocatie beperkt",
                        description = "Locatie staat niet op 'Altijd toestaan'. Ritten worden mogelijk niet gestart als het scherm uit staat.",
                        actionLabel = "Toestaan",
                        isCritical = true,
                        createIntent = { ctx ->
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", ctx.packageName, null)
                            }
                        }
                    )
                )
            }
        }

        // 3. Notificaties check
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notificationsEnabled) {
            issues.add(
                HealthIssue(
                    id = "notifications_disabled",
                    title = "Meldingen uitgeschakeld",
                    description = "Zonder meldingen kan de achtergrondservice worden gestopt en mis je statusupdates van ritten.",
                    actionLabel = "Meldingen aanzetten",
                    isCritical = true,
                    createIntent = { ctx ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", ctx.packageName, null)
                            }
                        }
                    }
                )
            )
        }

        // 4. GPS / Locatieservices check
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        if (!isGpsEnabled) {
            issues.add(
                HealthIssue(
                    id = "gps_disabled",
                    title = "GPS staat uit",
                    description = "Locatiebepaling is uitgeschakeld op je toestel. Ritten kunnen niet worden opgenomen.",
                    actionLabel = "GPS aanzetten",
                    isCritical = true,
                    createIntent = { _ ->
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    }
                )
            )
        }

        // 5. Bluetooth Connect (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBtConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBtConnect) {
                issues.add(
                    HealthIssue(
                        id = "bluetooth_connect",
                        title = "Bluetooth carkit permissie ontbreekt",
                        description = "CIMDriver kan geen verbinding detecteren met je carkit om automatisch ritten te starten.",
                        actionLabel = "Instellingen openen",
                        isCritical = false,
                        createIntent = { ctx ->
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", ctx.packageName, null)
                            }
                        }
                    )
                )
            }
        }

        return issues
    }
}
