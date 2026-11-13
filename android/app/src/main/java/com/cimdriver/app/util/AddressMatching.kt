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

    fun computeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toFloat()
    }

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
        var bestPriority = -1

        for (address in addresses) {
            val addrLat = address.latitude ?: continue
            val addrLon = address.longitude ?: continue

            val distance = computeDistanceMeters(lat, lon, addrLat, addrLon)
            if (distance <= maxDistanceMeters) {
                val priority = when {
                    address.isHomeLocation -> 2
                    address.isWorkLocation -> 1
                    else -> 0
                }
                
                if (priority > bestPriority || (priority == bestPriority && distance < minDistance)) {
                    bestPriority = priority
                    minDistance = distance
                    bestMatch = address
                }
            }
        }

        return bestMatch?.let { AddressMatch(it, minDistance, MatchReason.COORDINATE) }
    }

    /**
     * Resolves a departure location when exiting a parking garage or starting with delayed GPS fix.
     * Checks if the first acquired GPS point is within [maxExitRadiusMeters] (default 1500m) of the
     * location where the vehicle was last parked or a known saved address.
     */
    fun findParkingGarageDepartureMatch(
        firstGpsLat: Double?,
        firstGpsLon: Double?,
        parkedLat: Double?,
        parkedLon: Double?,
        parkedAddressLabel: String?,
        addresses: List<SavedAddress>,
        maxExitRadiusMeters: Float = 1500f
    ): AddressMatch? {
        val parkedSavedAddress = findSavedAddress(parkedAddressLabel, addresses)
        
        // If no GPS fix was acquired, use the parked address if available
        if (firstGpsLat == null || firstGpsLon == null) {
            return parkedSavedAddress?.let { AddressMatch(it, 0f, MatchReason.EXACT_TEXT) }
        }

        // Check if first GPS fix is within the garage exit radius of the last parked position
        val targetParkedLat = parkedSavedAddress?.latitude ?: parkedLat
        val targetParkedLon = parkedSavedAddress?.longitude ?: parkedLon

        if (targetParkedLat != null && targetParkedLon != null) {
            val distance = computeDistanceMeters(firstGpsLat, firstGpsLon, targetParkedLat, targetParkedLon)
            if (distance <= maxExitRadiusMeters) {
                if (parkedSavedAddress != null) {
                    return AddressMatch(parkedSavedAddress, distance, MatchReason.COORDINATE)
                } else if (!parkedAddressLabel.isNullOrBlank()) {
                    val fallbackAddress = SavedAddress(
                        label = parkedAddressLabel,
                        address = parkedAddressLabel,
                        latitude = targetParkedLat,
                        longitude = targetParkedLon
                    )
                    return AddressMatch(fallbackAddress, distance, MatchReason.COORDINATE)
                }
            }
        }

        // Check if any saved address (e.g. Work, Home) is within maxExitRadiusMeters
        return findBestMatch(firstGpsLat, firstGpsLon, null, addresses, maxDistanceMeters = maxExitRadiusMeters)
    }
}
