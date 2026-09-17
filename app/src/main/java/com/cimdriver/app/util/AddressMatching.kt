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

    fun normalizeBase(value: String?): String {
        val normalized = normalize(value)
        val match = Regex("^([a-z]+\\d+)[a-z]*$").find(normalized)
        return match?.groupValues?.get(1) ?: normalized
    }

    fun matchesBase(input: String?, candidate: String?): Boolean {
        val normalizedInput = normalizeBase(input)
        return normalizedInput.isNotEmpty() && normalizedInput == normalizeBase(candidate)
    }

    fun findSavedAddress(input: String?, addresses: List<SavedAddress>): SavedAddress? {
        return addresses.firstOrNull { address ->
            matches(input, address.label) || matches(input, address.address) ||
            matchesBase(input, address.label) || matchesBase(input, address.address)
        }
    }

    enum class MatchReason {
        EXACT_TEXT,
        COORDINATE
    }

    data class AddressMatch(
        val address: SavedAddress,
        val distanceMeters: Float,
        val reason: MatchReason
    )

    fun findBestMatch(
        lat: Double?,
        lon: Double?,
        input: String?,
        addresses: List<SavedAddress>,
        maxDistanceMeters: Float = 200f
    ): AddressMatch? {
        val exactMatch = findSavedAddress(input, addresses)
        if (exactMatch != null) {
            return AddressMatch(exactMatch, 0f, MatchReason.EXACT_TEXT)
        }

        if (lat == null || lon == null) return null

        var bestMatch: SavedAddress? = null
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (address in addresses) {
            val addrLat = address.latitude ?: continue
            val addrLon = address.longitude ?: continue

            Location.distanceBetween(lat, lon, addrLat, addrLon, results)
            val distance = results[0]
            if (distance <= maxDistanceMeters && distance < minDistance) {
                minDistance = distance
                bestMatch = address
            }
        }

        return bestMatch?.let { AddressMatch(it, minDistance, MatchReason.COORDINATE) }
    }
}
