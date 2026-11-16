package com.cimdriver.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log

class WifiStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo: NetworkInfo? = androidx.core.content.IntentCompat.getParcelableExtra(
                intent,
                WifiManager.EXTRA_NETWORK_INFO,
                NetworkInfo::class.java
            )
            if (networkInfo?.isConnected == true) {
                val wifiInfo: WifiInfo? = androidx.core.content.IntentCompat.getParcelableExtra(
                    intent,
                    WifiManager.EXTRA_WIFI_INFO,
                    WifiInfo::class.java
                )
                val ssid = wifiInfo?.ssid ?: WifiWorkHoursManager.getCurrentWifiSsid(context)
                if (ssid != null) {
                    Log.d("WifiStateReceiver", "Connected to Wi-Fi: $ssid")
                    WifiWorkHoursManager.handleWifiConnected(context, ssid)
                }
            } else if (networkInfo?.isConnectedOrConnecting == false) {
                Log.d("WifiStateReceiver", "Disconnected from Wi-Fi")
                val ssid = WifiWorkHoursManager.getCurrentWifiSsid(context)
                WifiWorkHoursManager.handleWifiDisconnected(context, ssid)
            }
        }
    }
}

