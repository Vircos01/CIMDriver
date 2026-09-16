package com.cimdriver.app.service

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeocoderService(private val context: Context) {
    
    private val geocoder = Geocoder(context, java.util.Locale.forLanguageTag("nl-NL"))

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.withTimeoutOrNull(3000) {
                    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : android.location.Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                if (continuation.isActive) continuation.resume(formatAddress(addresses.firstOrNull()))
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(addresses?.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e("GeocoderService", "Geocoding failed", e)
            null
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        
        // Try to construct a nice address like "Kerkstraat 1, Amsterdam"
        val thoroughfare = address.thoroughfare // Street name
        val subThoroughfare = address.subThoroughfare // House number
        val locality = address.locality // City
        
        return if (thoroughfare != null && locality != null) {
            if (subThoroughfare != null) {
                "$thoroughfare $subThoroughfare, $locality"
            } else {
                "$thoroughfare, $locality"
            }
        } else if (locality != null) {
            locality
        } else {
            // Fallback to the first formatted address line
            address.getAddressLine(0)
        }
    }

    suspend fun searchAddress(query: String): List<String> {
        if (query.length < 3) return emptyList()
        return try {
            var results: List<String> = emptyList()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                results = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocationName(query, 5, object : android.location.Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.mapNotNull { formatAddress(it) }.distinct())
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(emptyList())
                            }
                        })
                    }
                } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5)
                addresses?.mapNotNull { formatAddress(it) }?.distinct() ?: emptyList<String>()
            }
            
            if (results.isEmpty()) {
                results = searchNominatimAddress(query)
            }
            results
        } catch (e: Exception) {
            Log.e("GeocoderService", "Address search failed", e)
            searchNominatimAddress(query)
        }
    }

    suspend fun getOsrmDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Pair<Float, Float>? {
        return withContext(Dispatchers.IO) {
            try {
                val coords = String.format(Locale.US, "%f,%f;%f,%f", startLon, startLat, endLon, endLat)
                val urlString = "https://router.project-osrm.org/route/v1/driving/$coords?overview=false"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CIMDriver/1.0 (Android)")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val distance = firstRoute.optDouble("distance", -1.0)
                        val duration = firstRoute.optDouble("duration", -1.0)
                        if (distance >= 0) {
                            return@withContext Pair(distance.toFloat(), if (duration >= 0) duration.toFloat() else 0f)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("GeocoderService", "OSRM distance fetch failed", e)
                null
            }
        }
    }

    suspend fun calculateDistance(startAddress: String, endAddress: String): Pair<Float, Float>? {
        if (startAddress.isBlank() || endAddress.isBlank()) return null
        return try {
            val startCoords = getCoordinatesForAddress(startAddress)
            val endCoords = getCoordinatesForAddress(endAddress)
            
            if (startCoords != null && endCoords != null) {
                // Try to get driving distance from OSRM
                val drivingDistance = getOsrmDistance(
                    startCoords.first, startCoords.second,
                    endCoords.first, endCoords.second
                )
                
                if (drivingDistance != null) {
                    return drivingDistance
                }
                
                // Fallback to straight line distance
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    startCoords.first, startCoords.second,
                    endCoords.first, endCoords.second,
                    results
                )
                Pair(results[0], 0f) // No duration for straight line fallback
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GeocoderService", "Distance calculation failed", e)
            null
        }
    }

    suspend fun getCoordinatesForAddress(address: String): Pair<Double, Double>? {
        if (address.isBlank()) return null
        return try {
            var coords: Pair<Double, Double>? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                coords = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(address, 1, object : android.location.Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                val location = addresses.firstOrNull()
                                if (location != null) {
                                    if (cont.isActive) cont.resume(Pair(location.latitude, location.longitude))
                                } else {
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)
                val location = addresses?.firstOrNull()
                if (location != null) {
                    coords = Pair(location.latitude, location.longitude)
                }
            }
            if (coords == null) {
                coords = getNominatimCoordinates(address)
            }
            coords
        } catch (e: Exception) {
            Log.e("GeocoderService", "Get coordinates failed", e)
            getNominatimCoordinates(address)
        }
    }

    suspend fun getOsrmRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): List<Pair<Double, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                // OSRM expects longitude,latitude. Use Locale.US to ensure dot decimals.
                val coords = String.format(Locale.US, "%f,%f;%f,%f", startLon, startLat, endLon, endLat)
                val urlString = "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson"
                Log.d("GeocoderService", "Fetching OSRM route: $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CIMDriver/1.0 (Android)")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("GeocoderService", "OSRM Response Code: $responseCode")
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d("GeocoderService", "OSRM Response Length: ${response.length}")
                    val jsonObject = JSONObject(response)
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val geometry = firstRoute.optJSONObject("geometry")
                        if (geometry != null) {
                            val coordinates = geometry.optJSONArray("coordinates")
                            if (coordinates != null) {
                                val routePoints = mutableListOf<Pair<Double, Double>>()
                                for (i in 0 until coordinates.length()) {
                                    val point = coordinates.getJSONArray(i)
                                    // GeoJSON is [longitude, latitude]
                                    val lon = point.getDouble(0)
                                    val lat = point.getDouble(1)
                                    routePoints.add(Pair(lat, lon))
                                }
                                Log.d("GeocoderService", "Parsed ${routePoints.size} route points")
                                return@withContext routePoints
                            } else {
                                Log.e("GeocoderService", "coordinates is null")
                            }
                        } else {
                            Log.e("GeocoderService", "geometry is null")
                        }
                    } else {
                        Log.e("GeocoderService", "routes array is empty or null")
                    }
                } else {
                    Log.e("GeocoderService", "OSRM Response Code was not 200: $responseCode")
                }
                emptyList()
            } catch (e: Exception) {
                Log.e("GeocoderService", "OSRM route fetch failed", e)
                try {
                    val logFile = java.io.File(context.cacheDir, "osrm_error.log")
                    logFile.writeText("Error: ${e.message}\n${e.stackTraceToString()}")
                } catch (ignored: Exception) {}
                emptyList()
            }
        }
    }

    private suspend fun getNominatimCoordinates(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
                val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedAddress&format=json&limit=1"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CIMDriver/1.0 (Android)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.optString("lat").toDoubleOrNull()
                        val lon = firstResult.optString("lon").toDoubleOrNull()
                        if (lat != null && lon != null) {
                            return@withContext Pair(lat, lon)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("GeocoderService", "Nominatim fetch failed", e)
                null
            }
        }
    }

    private suspend fun searchNominatimAddress(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "CIMDriver/1.0 (Android)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)
                    val results = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val result = jsonArray.getJSONObject(i)
                        val addressDetails = result.optJSONObject("address")
                        
                        val road = addressDetails?.optString("road")
                        val houseNumber = addressDetails?.optString("house_number")
                        val city = addressDetails?.optString("city")?.takeIf { it.isNotBlank() } ?: 
                                   addressDetails?.optString("town")?.takeIf { it.isNotBlank() } ?: 
                                   addressDetails?.optString("village")
                        
                        if (!road.isNullOrEmpty() && !city.isNullOrEmpty()) {
                            if (!houseNumber.isNullOrEmpty()) {
                                results.add("$road $houseNumber, $city")
                            } else {
                                results.add("$road, $city")
                            }
                        } else {
                            val display = result.optString("display_name")
                            if (display.isNotBlank()) {
                                results.add(display.split(",").take(2).joinToString(",").trim())
                            }
                        }
                    }
                    return@withContext results.distinct()
                }
                emptyList()
            } catch (e: Exception) {
                Log.e("GeocoderService", "Nominatim search failed", e)
                emptyList()
            }
        }
    }
}
