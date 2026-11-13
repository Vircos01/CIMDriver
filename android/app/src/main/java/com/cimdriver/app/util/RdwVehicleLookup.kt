package com.cimdriver.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RdwVehicleData(
    val licensePlate: String,
    val make: String,
    val model: String,
    val year: String?,
    val engineType: String, // "ICE", "EV", "PHEV"
    val vehicleType: String? = null
)

object RdwVehicleLookup {

    private const val BASE_VEHICLE_URL = "https://opendata.rdw.nl/resource/m9d7-ebf2.json"
    private const val BASE_FUEL_URL = "https://opendata.rdw.nl/resource/8ys7-d773.json"

    fun cleanPlate(plate: String): String {
        return plate.replace("-", "").replace(" ", "").trim().uppercase(Locale.ROOT)
    }

    suspend fun lookup(licensePlate: String): Result<RdwVehicleData> = withContext(Dispatchers.IO) {
        val clean = cleanPlate(licensePlate)
        if (clean.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Kenteken is te kort"))
        }

        try {
            val vehicleUrl = URL("$BASE_VEHICLE_URL?kenteken=$clean")
            val conn = vehicleUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("RDW API HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            if (array.length() == 0) {
                return@withContext Result.failure(NoSuchElementException("Geen voertuig gevonden voor kenteken $clean"))
            }

            val item = array.getJSONObject(0)
            val rawMake = item.optString("merk", "")
            val rawModel = item.optString("handelsbenaming", "")
            val rawDate = item.optString("datum_eerste_toelating", "")
            val year = if (rawDate.length >= 4) rawDate.take(4) else null
            val vehicleType = item.optString("voertuigsoort").takeIf { it.isNotBlank() }

            val make = capitalizeWords(rawMake)
            val model = capitalizeWords(rawModel)

            val engineType = lookupEngineType(clean, rawModel)

            Result.success(
                RdwVehicleData(
                    licensePlate = clean,
                    make = make,
                    model = model,
                    year = year,
                    engineType = engineType,
                    vehicleType = vehicleType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun determineEngineTypeFromFuels(fuelDescriptions: List<String>, rawModel: String = ""): String {
        val lowerFuels = fuelDescriptions.map { it.lowercase(Locale.ROOT) }
        val hasElectric = lowerFuels.any { it.contains("elektr") }
        val hasFossil = lowerFuels.any { 
            it.contains("benzine") || it.contains("diesel") || it.contains("lpg") || it.contains("cng") || it.contains("alcohol") 
        }

        return when {
            hasElectric && hasFossil -> "PHEV"
            hasElectric && !hasFossil -> "EV"
            hasFossil -> "ICE"
            else -> {
                val upperModel = rawModel.uppercase(Locale.ROOT)
                when {
                    upperModel.contains("PHEV") || upperModel.contains("HYBRID") -> "PHEV"
                    upperModel.contains("EV") || upperModel.contains("ELECTRIC") || upperModel.startsWith("E-") -> "EV"
                    else -> "ICE"
                }
            }
        }
    }

    private fun lookupEngineType(cleanPlate: String, rawModel: String): String {
        return try {
            val fuelUrl = URL("$BASE_FUEL_URL?kenteken=$cleanPlate")
            val conn = fuelUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val fuels = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val fuel = obj.optString("brandstof_omschrijving", "")
                    if (fuel.isNotBlank()) fuels.add(fuel)
                }
                determineEngineTypeFromFuels(fuels, rawModel)
            } else {
                determineEngineTypeFromFuels(emptyList(), rawModel)
            }
        } catch (e: Exception) {
            determineEngineTypeFromFuels(emptyList(), rawModel)
        }
    }

    internal fun capitalizeWords(input: String): String {
        if (input.isBlank()) return input
        return input.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
    }
}
