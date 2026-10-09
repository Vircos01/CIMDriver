package com.cimdriver.app.companion

import android.content.Context
import android.util.Log
import com.cimdriver.app.data.local.AppDatabase
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ZeppCompanionServer(
    private val context: Context,
    port: Int = 8765
) : NanoHTTPD(port) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        @Volatile
        private var instance: ZeppCompanionServer? = null

        fun startIfNeeded(context: Context): ZeppCompanionServer? {
            val current = instance
            if (current != null && current.isAlive) {
                return current
            }

            return try {
                val server = ZeppCompanionServer(context.applicationContext)
                server.start()
                instance = server
                Log.i("ZeppCompanionServer", "Started on http://0.0.0.0:${server.listeningPort}")
                server
            } catch (e: Exception) {
                Log.e("ZeppCompanionServer", "Failed to start server", e)
                null
            }
        }

        fun stopIfNeeded() {
            val current = instance
            if (current != null) {
                current.stop()
                instance = null
                Log.i("ZeppCompanionServer", "Stopped Zepp companion endpoint")
            }
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/zepp/summary", "/api/zepp/summary" -> {
                val summary = buildSummary()
                val payload = json.encodeToString(summary)
                newFixedLengthResponse(Response.Status.OK, "application/json", payload)
            }
            "/zepp/action", "/api/zepp/action" -> handleAction(session)
            "/health" -> newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private fun handleAction(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"ok\":false,\"message\":\"POST required\"}")
        }

        val body = session.inputStream.readBytes().toString(Charsets.UTF_8)
        val request = runCatching {
            if (body.isBlank()) ZeppCompanionActionRequest() else json.decodeFromString<ZeppCompanionActionRequest>(body)
        }.getOrElse {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"ok\":false,\"message\":\"Invalid action payload\"}")
        }

        val response = runBlocking {
            val database = AppDatabase.getDatabase(context)
            when (request.action.lowercase()) {
                "classify_trip" -> {
                    val normalizedType = when (request.tripType.lowercase()) {
                        "private", "personal" -> "PERSONAL"
                        "commute", "home_to_work", "woon_werk" -> "COMMUTE"
                        else -> "BUSINESS"
                    }

                    val trip = database.tripDao().getAllTripsSync()
                        .firstOrNull { it.status == "TO_REVIEW" || it.status == "ACTIVE" }

                    if (trip == null) {
                        ZeppCompanionActionResponse(false, "No trip pending for review")
                    } else {
                        database.tripDao().updateTrip(trip.copy(tripType = normalizedType, status = "DONE"))
                        ZeppCompanionActionResponse(true, "Trip saved as $normalizedType")
                    }
                }
                "approve_fuel" -> {
                    val draft = database.fuelFillUpDao().getAllFillUpsSync()
                        .firstOrNull { it.status == "DRAFT" }

                    if (draft == null) {
                        ZeppCompanionActionResponse(false, "No draft fuel entry pending")
                    } else {
                        database.fuelFillUpDao().updateFillUp(draft.copy(status = "COMPLETED"))
                        ZeppCompanionActionResponse(true, "Fuel entry approved")
                    }
                }
                else -> ZeppCompanionActionResponse(false, "Unknown action: ${request.action}")
            }
        }

        val payload = json.encodeToString(response)
        return newFixedLengthResponse(Response.Status.OK, "application/json", payload)
    }

    private fun buildSummary(): ZeppCompanionSummary = runBlocking {
        val database = AppDatabase.getDatabase(context)
        val allTrips = database.tripDao().getAllTripsSync()
        val activeTrip = allTrips.firstOrNull { it.status == "ACTIVE" } ?: allTrips.firstOrNull()
        val tripStatus = if (activeTrip != null && activeTrip.status == "ACTIVE") "driving" else "idle"
        val distanceKm = if (activeTrip != null) activeTrip.distanceMeters / 1000.0 else 0.0
        val vehicle = activeTrip?.vehicleId?.let { database.vehicleDao().getVehicleById(it) }
        val fuelStatus = when (vehicle?.engineType) {
            "EV" -> "charging"
            "PHEV" -> "tanking"
            else -> "ready"
        }

        ZeppCompanionSummary(
            tripStatus = tripStatus,
            distanceKm = distanceKm,
            fuelStatus = fuelStatus,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
