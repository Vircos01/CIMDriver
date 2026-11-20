package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import kotlin.math.abs
import kotlin.math.roundToInt

enum class LeapmotorMatchStatus {
    EXACT_MATCH,          // Times & duration match within <= 2 min (Green)
    EXCESS_STOP_IDLE,     // Car turned off, but CIMDriver kept running >= 3 min (Yellow)
    DELAYED_START,        // Car started, but CIMDriver began tracking >= 3 min later (Yellow)
    SHIFTED_TIME,         // Duration matches, but time window shifted (Yellow)
    DURATION_MISMATCH,    // Duration discrepancy >= 3 min (Orange)
    MERGED_TRIP_CANDIDATE,// 1 CIMDriver trip spans multiple Leapmotor trips (Purple)
    SPLIT_TRIP_CANDIDATE, // Multiple CIMDriver trips inside 1 Leapmotor trip (Blue)
    MISSING_IN_CIMDRIVER  // Recorded by Leapmotor, but completely missing in CIMDriver (Red)
}

enum class LeapmotorActionType {
    NONE,
    CONFIRM_VERIFIED,
    TRIM_END_TIME,
    EXTEND_START_TIME,
    ALIGN_TIMES,
    ADD_MISSING_TRIP,
    SPLIT_TRIP,
    MERGE_TRIPS
}

data class LeapmotorSuggestedAction(
    val type: LeapmotorActionType,
    val title: String,
    val description: String,
    val targetTripId: Long? = null,
    val newStartTime: Long? = null,
    val newEndTime: Long? = null,
    val estimatedDistanceMeters: Int? = null
)

data class LeapmotorMatchItem(
    val id: String,
    val lmTrip: LeapmotorTelematicsTrip,
    val matchingTrip: Trip? = null,
    val status: LeapmotorMatchStatus,
    val startDeltaMinutes: Int = 0,     // Positive: CIMDriver started later
    val endDeltaMinutes: Int = 0,       // Positive: CIMDriver ended later
    val durationDeltaMinutes: Int = 0,  // Positive: CIMDriver duration longer
    val explanation: String,
    val suggestedAction: LeapmotorSuggestedAction
)

data class LeapmotorComparisonReport(
    val items: List<LeapmotorMatchItem>,
    val exactMatchesCount: Int,
    val discrepancyCount: Int,
    val missingCount: Int,
    val detectedOdometerKm: Int?,
    val vehicle: Vehicle
)

object LeapmotorTripMatcher {

    private fun Trip.effectiveEndTime(): Long {
        return endTime ?: (startTime + (distanceMeters / 15).coerceAtLeast(60) * 1000L)
    }

    private fun Trip.durationMinutes(): Int {
        val durationMs = effectiveEndTime() - startTime
        return (durationMs / 60000L).toInt().coerceAtLeast(1)
    }

    /**
     * Matches extracted Leapmotor telematics trips against CIMDriver registered trips.
     */
    fun match(
        lmTrips: List<LeapmotorTelematicsTrip>,
        cimTrips: List<Trip>,
        vehicle: Vehicle,
        detectedOdometerKm: Int? = null
    ): LeapmotorComparisonReport {
        val sortedLm = lmTrips.sortedBy { it.startTimestamp }
        val sortedCim = cimTrips.sortedBy { it.startTime }

        val items = mutableListOf<LeapmotorMatchItem>()

        // Track how many LM trips match each CIM trip (for merged trip detection)
        val cimTripLmMatches = mutableMapOf<Long, MutableList<LeapmotorTelematicsTrip>>()

        for (lm in sortedLm) {
            val lmStart = lm.startTimestamp
            val lmEnd = lm.endTimestamp
            val lmDur = lm.durationMinutes

            // Find overlapping or near CIMDriver trips
            val candidates = sortedCim.filter { cim ->
                val cimStart = cim.startTime
                val cimEnd = cim.effectiveEndTime()

                val overlaps = cimStart < lmEnd && cimEnd > lmStart
                val nearStart = abs(cimStart - lmStart) <= 15 * 60 * 1000L
                val nearEnd = abs(cimEnd - lmEnd) <= 15 * 60 * 1000L

                overlaps || nearStart || nearEnd
            }

            if (candidates.isEmpty()) {
                // Completely missing trip in CIMDriver
                val prevCim = sortedCim.lastOrNull { it.effectiveEndTime() <= lmStart }
                val nextCim = sortedCim.firstOrNull { it.startTime >= lmEnd }

                val estDistanceM = when {
                    lm.distanceKm != null -> (lm.distanceKm * 1000.0).roundToInt()
                    lmDur <= 2 -> 500 // short repositioning
                    lmDur <= 5 -> (lmDur * 500) // ~30 km/h city
                    else -> (lmDur * 800) // ~50 km/h mixed
                }

                items.add(
                    LeapmotorMatchItem(
                        id = "missing_${lm.dateText}_${lm.startTimeText}",
                        lmTrip = lm,
                        matchingTrip = null,
                        status = LeapmotorMatchStatus.MISSING_IN_CIMDRIVER,
                        startDeltaMinutes = 0,
                        endDeltaMinutes = 0,
                        durationDeltaMinutes = -lmDur,
                        explanation = "Niet geregistreerd in CIMDriver. Korte rit van $lmDur min gemist (${lm.startTimeText} - ${lm.endTimeText}).",
                        suggestedAction = LeapmotorSuggestedAction(
                            type = LeapmotorActionType.ADD_MISSING_TRIP,
                            title = "Voeg gemiste rit toe ($lmDur min)",
                            description = "Maak rit aan van ${lm.startTimeText} tot ${lm.endTimeText} (${(estDistanceM / 1000.0)} km).",
                            newStartTime = lmStart,
                            newEndTime = lmEnd,
                            estimatedDistanceMeters = estDistanceM
                        )
                    )
                )
            } else if (candidates.size == 1) {
                val cim = candidates.first()
                cimTripLmMatches.getOrPut(cim.id) { mutableListOf() }.add(lm)

                val cimStart = cim.startTime
                val cimEnd = cim.effectiveEndTime()
                val cimDur = cim.durationMinutes()

                val startDelta = ((cimStart - lmStart) / 60000L).toInt()
                val endDelta = ((cimEnd - lmEnd) / 60000L).toInt()
                val durDelta = cimDur - lmDur

                when {
                    // Exact match: both within 2 min
                    abs(startDelta) <= 2 && abs(endDelta) <= 2 -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "exact_${cim.id}_${lm.startTimeText}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                status = LeapmotorMatchStatus.EXACT_MATCH,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = "Volledig in sync met boordcomputer (duur: $lmDur min).",
                                suggestedAction = LeapmotorSuggestedAction(
                                    type = LeapmotorActionType.CONFIRM_VERIFIED,
                                    title = "Markeer als geverifieerd",
                                    description = "Boordcomputer en GPS bevestigen elkaar.",
                                    targetTripId = cim.id,
                                    newStartTime = lmStart,
                                    newEndTime = lmEnd
                                )
                            )
                        )
                    }

                    // Excess stop idle: ended >= 3 min late, start was on time
                    endDelta >= 3 && abs(startDelta) <= 2 -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "excess_stop_${cim.id}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                status = LeapmotorMatchStatus.EXCESS_STOP_IDLE,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = "CIMDriver liep $endDelta min door na uitschakelen motor. Ritduur is $cimDur min ipv $lmDur min.",
                                suggestedAction = LeapmotorSuggestedAction(
                                    type = LeapmotorActionType.TRIM_END_TIME,
                                    title = "Snijd $endDelta min stilstand af",
                                    description = "Pas eindtijd aan naar ${lm.endTimeText} (motorstop).",
                                    targetTripId = cim.id,
                                    newEndTime = lmEnd
                                )
                            )
                        )
                    }

                    // Delayed start: started >= 3 min late, stop was on time
                    startDelta >= 3 && abs(endDelta) <= 2 -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "delayed_start_${cim.id}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                status = LeapmotorMatchStatus.DELAYED_START,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = "CIMDriver startte $startDelta min later dan motorstart. Ritduur is $cimDur min ipv $lmDur min.",
                                suggestedAction = LeapmotorSuggestedAction(
                                    type = LeapmotorActionType.EXTEND_START_TIME,
                                    title = "Herstel vertrektijd naar ${lm.startTimeText}",
                                    description = "Starttijd aanpassen naar ${lm.startTimeText} (motorstart).",
                                    targetTripId = cim.id,
                                    newStartTime = lmStart
                                )
                            )
                        )
                    }

                    // Shifted time: duration matches, but window shifted
                    abs(durDelta) <= 2 -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "shifted_${cim.id}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                status = LeapmotorMatchStatus.SHIFTED_TIME,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = "Duur klopt ($lmDur min), maar tijdsvenster verschoven (start Δ ${startDelta}m, stop Δ ${endDelta}m).",
                                suggestedAction = LeapmotorSuggestedAction(
                                    type = LeapmotorActionType.ALIGN_TIMES,
                                    title = "Synchroniseer met boordcomputertijden",
                                    description = "Pas starttijd aan naar ${lm.startTimeText} en eindtijd naar ${lm.endTimeText}.",
                                    targetTripId = cim.id,
                                    newStartTime = lmStart,
                                    newEndTime = lmEnd
                                )
                            )
                        )
                    }

                    // General duration mismatch
                    else -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "mismatch_${cim.id}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                status = LeapmotorMatchStatus.DURATION_MISMATCH,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = "Afwijking in ritduur: CIMDriver meet $cimDur min, Leapmotor $lmDur min (verschil: ${if (durDelta > 0) "+$durDelta" else "$durDelta"} min).",
                                suggestedAction = LeapmotorSuggestedAction(
                                    type = LeapmotorActionType.ALIGN_TIMES,
                                    title = "Synchroniseer tijden naar ${lm.startTimeText} - ${lm.endTimeText}",
                                    description = "Boordcomputertijden overnemen.",
                                    targetTripId = cim.id,
                                    newStartTime = lmStart,
                                    newEndTime = lmEnd
                                )
                            )
                        )
                    }
                }
            } else {
                // Multiple CIMDriver trips cover this single LM trip
                items.add(
                    LeapmotorMatchItem(
                        id = "split_${lm.dateText}_${lm.startTimeText}",
                        lmTrip = lm,
                        matchingTrip = candidates.first(),
                        status = LeapmotorMatchStatus.SPLIT_TRIP_CANDIDATE,
                        startDeltaMinutes = 0,
                        endDeltaMinutes = 0,
                        durationDeltaMinutes = candidates.sumOf { it.durationMinutes() } - lmDur,
                        explanation = "Leapmotor toont 1 aaneengesloten rit ($lmDur min), maar CIMDriver heeft ${candidates.size} losse ritten geregistreerd.",
                        suggestedAction = LeapmotorSuggestedAction(
                            type = LeapmotorActionType.MERGE_TRIPS,
                            title = "Voeg ${candidates.size} ritten samen",
                            description = "Samenvoegen tot 1 aaneengesloten rit van ${lm.startTimeText} tot ${lm.endTimeText}.",
                            targetTripId = candidates.first().id,
                            newStartTime = lmStart,
                            newEndTime = lmEnd
                        )
                    )
                )
            }
        }

        // Post-process: check for merged trip candidates (where 1 CIMDriver trip matched multiple LM trips)
        val finalItems = items.map { item ->
            val cim = item.matchingTrip
            if (cim != null && (cimTripLmMatches[cim.id]?.size ?: 0) > 1 && item.status != LeapmotorMatchStatus.SPLIT_TRIP_CANDIDATE) {
                val lmList = cimTripLmMatches[cim.id]!!
                item.copy(
                    status = LeapmotorMatchStatus.MERGED_TRIP_CANDIDATE,
                    explanation = "CIMDriver heeft ritten samengevoegd (${cim.durationMinutes()} min). Boordcomputer toont ${lmList.size} aparte ritten met motorstop.",
                    suggestedAction = LeapmotorSuggestedAction(
                        type = LeapmotorActionType.SPLIT_TRIP,
                        title = "Splits in ${lmList.size} ritten",
                        description = "Rit splitsen conform boordcomputer ${item.lmTrip.startTimeText} - ${item.lmTrip.endTimeText}.",
                        targetTripId = cim.id,
                        newStartTime = item.lmTrip.startTimestamp,
                        newEndTime = item.lmTrip.endTimestamp
                    )
                )
            } else {
                item
            }
        }

        val exactCount = finalItems.count { it.status == LeapmotorMatchStatus.EXACT_MATCH }
        val missingCount = finalItems.count { it.status == LeapmotorMatchStatus.MISSING_IN_CIMDRIVER }
        val discrepancyCount = finalItems.size - exactCount - missingCount

        return LeapmotorComparisonReport(
            items = finalItems,
            exactMatchesCount = exactCount,
            discrepancyCount = discrepancyCount,
            missingCount = missingCount,
            detectedOdometerKm = detectedOdometerKm,
            vehicle = vehicle
        )
    }
}
