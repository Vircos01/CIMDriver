package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.SavedAddress
import java.text.Normalizer
import java.util.Locale
import android.location.Location

object AddressMatching {
    fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace("&", " en ")
            .replace("[^a-z0-9]".toRegex(), "")
    }

    fun matches(input: String?, candidate: String?): Boolean {
        val normalizedInput = normalize(input)
        return normalizedInput.isNotEmpty() && normalizedInput == normalize(candidate)
    }

    fun findSavedAddress(input: String?, addresses: List<SavedAddress>): SavedAddress? {
        return addresses.firstOrNull { address ->
            matches(input, address.label) || matches(input, address.address)
        }
    }

    fun findSavedAddressByProximity(
        lat: Double,
        lng: Double,
        input: String,
        addresses: List<SavedAddress>,
        maxDistanceMeters: Float = 250f
    ): String {
        val exactMatch = findSavedAddress(input, addresses)
        if (exactMatch != null) return exactMatch.address

        var closestAddress: SavedAddress? = null
        var minDistance = Float.MAX_VALUE

        val results = FloatArray(1)
        for (address in addresses) {
            if (address.latitude != null && address.longitude != null) {
                Location.distanceBetween(lat, lng, address.latitude, address.longitude, results)
                val distance = results[0]
                if (distance < maxDistanceMeters && distance < minDistance) {
                    minDistance = distance
                    closestAddress = address
                }
            }
        }

        return closestAddress?.address ?: input
    }
}
