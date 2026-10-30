package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GapType {
    MISSING_KM,        // odometerEnd[n] < odometerStart[n+1]
    OVERLAPPING_KM,    // odometerEnd[n] > odometerStart[n+1]
    MISSING_TRIP,      // Long time span without registered trip with missing km
    ODOMETER_RESET,    // Stand dropped drastically
    FIRST_TRIP_GAP,    // First trip start != vehicle start
    CURRENT_STAND_GAP  // Latest trip end != vehicle current
}

enum class FixType {
    CREATE_PRIVATE_TRIP,   // Create private trip for missing km
    DISTRIBUTE_OVER_TRIPS, // Distribute gap proportionally
    ADJUST_SINGLE_TRIP,    // Adjust adjacent trip
    MARK_AS_EXPLAINED,     // Mark with explanation (e.g. maintenance/garage)
    IGNORE                 // Deliberately ignore
}

data class TripChange(
    val tripId: Long,
    val oldOdoStart: Int,
    val newOdoStart: Int,
    val oldOdoEnd: Int,
    val newOdoEnd: Int,
    val oldDistanceKm: Int,
    val newDistanceKm: Int
)

data class SuggestedFix(
    val type: FixType,
    val title: String,
    val description: String,
    val previewChanges: List<TripChange> = emptyList()
)

data class OdometerGap(
    val id: String,
    val vehicleId: Long,
    val gapType: GapType,
    val previousTrip: Trip?,
    val nextTrip: Trip?,
    val expectedOdometer: Int,
    val actualOdometer: Int,
    val gapKm: Int,
    val gapTimeStart: Long,
    val gapTimeEnd: Long,
    val suggestedFixes: List<SuggestedFix> = emptyList()
)

object OdometerGapAnalyzer {

    fun Trip.effectiveOdometerEnd(): Int {
        return odometerEnd ?: (odometerStart + (distanceMeters / 1000.0).roundToInt())
    }

    fun Trip.effectiveEndTime(): Long {
        return endTime ?: (startTime + (distanceMeters / 15).coerceAtLeast(60) * 1000L)
    }

    /**
     * Scans the chronological chain of trips for a vehicle and finds all discrepancies/gaps.
     */
    fun analyze(vehicle: Vehicle, trips: List<Trip>): List<OdometerGap> {
        val sortedTrips = trips.sortedBy { it.startTime }
        val gaps = mutableListOf<OdometerGap>()

        if (sortedTrips.isEmpty()) {
            if (vehicle.odometerCurrent != vehicle.odometerStart) {
                val gapKm = vehicle.odometerCurrent - vehicle.odometerStart
                gaps.add(
                    OdometerGap(
                        id = "gap_no_trips_${vehicle.id}",
                        vehicleId = vehicle.id,
                        gapType = GapType.CURRENT_STAND_GAP,
                        previousTrip = null,
                        nextTrip = null,
                        expectedOdometer = vehicle.odometerStart,
                        actualOdometer = vehicle.odometerCurrent,
                        gapKm = gapKm,
                        gapTimeStart = vehicle.inServiceDate ?: System.currentTimeMillis(),
                        gapTimeEnd = System.currentTimeMillis(),
                        suggestedFixes = generateFixes(
                            gapType = GapType.CURRENT_STAND_GAP,
                            gapKm = gapKm,
                            prevTrip = null,
                            nextTrip = null,
                            contextTrips = emptyList()
                        )
                    )
                )
            }
            return gaps
        }

        // 1. Check start of first trip against vehicle.odometerStart
        val firstTrip = sortedTrips.first()
        if (firstTrip.odometerStart != vehicle.odometerStart) {
            val gapKm = firstTrip.odometerStart - vehicle.odometerStart
            gaps.add(
                OdometerGap(
                    id = "gap_first_${firstTrip.id}",
                    vehicleId = vehicle.id,
                    gapType = GapType.FIRST_TRIP_GAP,
                    previousTrip = null,
                    nextTrip = firstTrip,
                    expectedOdometer = vehicle.odometerStart,
                    actualOdometer = firstTrip.odometerStart,
                    gapKm = gapKm,
                    gapTimeStart = vehicle.inServiceDate ?: firstTrip.startTime - 86400000L,
                    gapTimeEnd = firstTrip.startTime,
                    suggestedFixes = generateFixes(
                        gapType = GapType.FIRST_TRIP_GAP,
                        gapKm = gapKm,
                        prevTrip = null,
                        nextTrip = firstTrip,
                        contextTrips = listOf(firstTrip)
                    )
                )
            )
        }

        // 2. Check gaps between consecutive trips
        for (i in 0 until sortedTrips.size - 1) {
            val prev = sortedTrips[i]
            val next = sortedTrips[i + 1]
            val prevEnd = prev.effectiveOdometerEnd()
            val prevEndTime = prev.effectiveEndTime()
            val diff = next.odometerStart - prevEnd

            if (diff != 0) {
                val gapType = when {
                    diff < -50 -> GapType.ODOMETER_RESET
                    diff < 0 -> GapType.OVERLAPPING_KM
                    (next.startTime - prevEndTime) > (48 * 3600 * 1000L) -> GapType.MISSING_TRIP
                    else -> GapType.MISSING_KM
                }

                // Recent trips for preview calculation
                val surroundingTrips = sortedTrips.subList(
                    maxOf(0, i - 2),
                    minOf(sortedTrips.size, i + 3)
                )

                gaps.add(
                    OdometerGap(
                        id = "gap_${prev.id}_${next.id}",
                        vehicleId = vehicle.id,
                        gapType = gapType,
                        previousTrip = prev,
                        nextTrip = next,
                        expectedOdometer = prevEnd,
                        actualOdometer = next.odometerStart,
                        gapKm = diff,
                        gapTimeStart = prevEndTime,
                        gapTimeEnd = next.startTime,
                        suggestedFixes = generateFixes(
                            gapType = gapType,
                            gapKm = diff,
                            prevTrip = prev,
                            nextTrip = next,
                            contextTrips = surroundingTrips
                        )
                    )
                )
            }
        }

        // 3. Check latest trip end against vehicle.odometerCurrent
        val lastTrip = sortedTrips.last()
        val lastTripEnd = lastTrip.effectiveOdometerEnd()
        val lastTripEndTime = lastTrip.effectiveEndTime()
        val currentDiff = vehicle.odometerCurrent - lastTripEnd
        if (currentDiff != 0) {
            val gapType = if (currentDiff > 0) GapType.MISSING_KM else GapType.OVERLAPPING_KM
            val surroundingTrips = sortedTrips.takeLast(minOf(5, sortedTrips.size))
            gaps.add(
                OdometerGap(
                    id = "gap_last_${lastTrip.id}",
                    vehicleId = vehicle.id,
                    gapType = GapType.CURRENT_STAND_GAP,
                    previousTrip = lastTrip,
                    nextTrip = null,
                    expectedOdometer = lastTripEnd,
                    actualOdometer = vehicle.odometerCurrent,
                    gapKm = currentDiff,
                    gapTimeStart = lastTripEndTime,
                    gapTimeEnd = System.currentTimeMillis(),
                    suggestedFixes = generateFixes(
                        gapType = gapType,
                        gapKm = currentDiff,
                        prevTrip = lastTrip,
                        nextTrip = null,
                        contextTrips = surroundingTrips
                    )
                )
            )
        }

        return gaps
    }

    /**
     * Checks if the trip chain for a vehicle is completely continuous and airtight.
     */
    fun isChainAirtight(vehicle: Vehicle, trips: List<Trip>): Boolean {
        return analyze(vehicle, trips).isEmpty()
    }

    private fun generateFixes(
        gapType: GapType,
        gapKm: Int,
        prevTrip: Trip?,
        nextTrip: Trip?,
        contextTrips: List<Trip>
    ): List<SuggestedFix> {
        val fixes = mutableListOf<SuggestedFix>()

        if (gapKm > 0) {
            // Missing km: option to create a private trip
            val fromAddr = prevTrip?.endAddress ?: "Onbekend vertrekpunt"
            val toAddr = nextTrip?.startAddress ?: "Onbekend eindpunt"
            fixes.add(
                SuggestedFix(
                    type = FixType.CREATE_PRIVATE_TRIP,
                    title = "Privérit aanmaken ($gapKm km)",
                    description = "Maak een sluitende privérit aan van $fromAddr naar $toAddr voor het ontbrekende verschil."
                )
            )

            // Option to distribute over context trips
            if (contextTrips.isNotEmpty()) {
                val preview = calculateDistributionPreview(contextTrips, gapKm)
                fixes.add(
                    SuggestedFix(
                        type = FixType.DISTRIBUTE_OVER_TRIPS,
                        title = "Verschil verdelen over ${contextTrips.size} ritten",
                        description = "Verhoog de afstand van aangrenzende ritten evenredig met in totaal $gapKm km.",
                        previewChanges = preview
                    )
                )
            }
        } else if (gapKm < 0) {
            // Overlapping km: adjust single trip or distribute
            if (prevTrip != null) {
                fixes.add(
                    SuggestedFix(
                        type = FixType.ADJUST_SINGLE_TRIP,
                        title = "Vorige rit inkorten (${abs(gapKm)} km)",
                        description = "Verlaag de eindstand van rit #${prevTrip.id} zodat deze aansluit op de volgende rit."
                    )
                )
            }
        }

        // Always available: Mark as explained for Dutch tax compliance
        fixes.add(
            SuggestedFix(
                type = FixType.MARK_AS_EXPLAINED,
                title = "Markeren met toelichting (Belastingdienst)",
                description = "Registreer een officiële verklaring voor het verschil (bijv. garagebezoek, APK, proefrit)."
            )
        )

        // Deliberately ignore
        fixes.add(
            SuggestedFix(
                type = FixType.IGNORE,
                title = "Negeren",
                description = "Negeer dit verschil bewust in de rapportages."
            )
        )

        return fixes
    }

    /**
     * Calculates proportional preview changes if a gap of [gapKm] is distributed over [trips].
     */
    fun calculateDistributionPreview(trips: List<Trip>, gapKm: Int): List<TripChange> {
        if (trips.isEmpty()) return emptyList()

        val totalDistKm = trips.sumOf { (it.distanceMeters / 1000.0) }
        if (totalDistKm <= 0.0) return emptyList()

        val ratio = (totalDistKm + gapKm) / totalDistKm
        val changes = mutableListOf<TripChange>()

        var currentStart = trips.first().odometerStart

        for (trip in trips) {
            val oldDist = (trip.distanceMeters / 1000.0).roundToInt()
            val newDist = maxOf(1, (oldDist * ratio).roundToInt())
            val newStart = currentStart
            val newEnd = newStart + newDist

            changes.add(
                TripChange(
                    tripId = trip.id,
                    oldOdoStart = trip.odometerStart,
                    newOdoStart = newStart,
                    oldOdoEnd = trip.effectiveOdometerEnd(),
                    newOdoEnd = newEnd,
                    oldDistanceKm = oldDist,
                    newDistanceKm = newDist
                )
            )

            currentStart = newEnd
        }

        return changes
    }
}
