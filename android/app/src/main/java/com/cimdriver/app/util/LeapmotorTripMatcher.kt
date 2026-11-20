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
    val candidateTripIds: List<Long> = emptyList(),
    val newStartTime: Long? = null,
    val newEndTime: Long? = null,
    val estimatedDistanceMeters: Int? = null
)

data class LeapmotorMatchItem(
    val id: String,
    val lmTrip: LeapmotorTelematicsTrip,
    val matchingTrip: Trip? = null,
    val candidateTrips: List<Trip> = emptyList(),
    val status: LeapmotorMatchStatus,
    val isAlreadyVerified: Boolean = false,
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
     * Strictly tests whether a CIMDriver trip belongs to a specific Leapmotor telematics trip.
     * Prevents distant trips (e.g. 10-15 minutes later) from being falsely grouped together.
     */
    private fun isCandidateForLmTrip(cim: Trip, lm: LeapmotorTelematicsTrip): Boolean {
        val cimStart = cim.startTime
        val cimEnd = cim.effectiveEndTime()
        val lmStart = lm.startTimestamp
        val lmEnd = lm.endTimestamp

        // A trip that ended more than 3 minutes before LM trip started cannot be part of it
        if (cimEnd < lmStart - 3 * 60 * 1000L) return false

        // A trip that started more than 3 minutes after LM trip ended cannot be part of it
        if (cimStart > lmEnd + 3 * 60 * 1000L) return false

        val overlapStart = maxOf(cimStart, lmStart)
        val overlapEnd = minOf(cimEnd, lmEnd)
        val overlapMs = maxOf(0L, overlapEnd - overlapStart)

        val startDeltaMs = abs(cimStart - lmStart)
        val endDeltaMs = abs(cimEnd - lmEnd)

        // 1. Definite overlap of at least 30s
        if (overlapMs >= 30 * 1000L) return true

        // 2. Both start and end timestamps are very close (<= 3 min)
        if (startDeltaMs <= 3 * 60 * 1000L && endDeltaMs <= 3 * 60 * 1000L) return true

        // 3. Excess idle stop (car turned off, GPS ran up to 25 min longer)
        if (startDeltaMs <= 3 * 60 * 1000L && cimEnd >= lmEnd && (cimEnd - lmEnd) <= 25 * 60 * 1000L) return true

        // 4. Delayed GPS start (car started, GPS started within 15 min, both ended on time)
        if (endDeltaMs <= 3 * 60 * 1000L && cimStart >= lmStart && (cimStart - lmStart) <= 15 * 60 * 1000L) return true

        return false
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

            // Find candidates strictly within and overlapping this LM trip window
            val candidates = sortedCim.filter { isCandidateForLmTrip(it, lm) }

            if (candidates.isEmpty()) {
                // Completely missing trip in CIMDriver
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
                        candidateTrips = emptyList(),
                        status = LeapmotorMatchStatus.MISSING_IN_CIMDRIVER,
                        isAlreadyVerified = false,
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

                val isAlreadyVerified = cim.note?.contains("Leapmotor", ignoreCase = true) == true

                when {
                    // Exact match: both within 2 min, OR already verified and within 3 min
                    (abs(startDelta) <= 2 && abs(endDelta) <= 2) || (isAlreadyVerified && abs(startDelta) <= 3 && abs(endDelta) <= 3) -> {
                        items.add(
                            LeapmotorMatchItem(
                                id = "exact_${cim.id}_${lm.startTimeText}",
                                lmTrip = lm,
                                matchingTrip = cim,
                                candidateTrips = candidates,
                                status = LeapmotorMatchStatus.EXACT_MATCH,
                                isAlreadyVerified = isAlreadyVerified,
                                startDeltaMinutes = startDelta,
                                endDeltaMinutes = endDelta,
                                durationDeltaMinutes = durDelta,
                                explanation = if (isAlreadyVerified) {
                                    "Volledig in sync en reeds geverifieerd met boordcomputer (duur: $lmDur min)."
                                } else {
                                    "Volledig in sync met boordcomputer (duur: $lmDur min)."
                                },
                                suggestedAction = if (isAlreadyVerified) {
                                    LeapmotorSuggestedAction(
                                        type = LeapmotorActionType.NONE,
                                        title = "Reeds geverifieerd",
                                        description = "Boordcomputer en GPS bevestigen elkaar. Reeds vastgelegd in administratie.",
                                        targetTripId = cim.id
                                    )
                                } else {
                                    LeapmotorSuggestedAction(
                                        type = LeapmotorActionType.CONFIRM_VERIFIED,
                                        title = "Markeer als geverifieerd",
                                        description = "Boordcomputer en GPS bevestigen elkaar.",
                                        targetTripId = cim.id,
                                        newStartTime = lmStart,
                                        newEndTime = lmEnd
                                    )
                                }
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
                                candidateTrips = candidates,
                                status = LeapmotorMatchStatus.EXCESS_STOP_IDLE,
                                isAlreadyVerified = isAlreadyVerified,
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
                                candidateTrips = candidates,
                                status = LeapmotorMatchStatus.DELAYED_START,
                                isAlreadyVerified = isAlreadyVerified,
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
                                candidateTrips = candidates,
                                status = LeapmotorMatchStatus.SHIFTED_TIME,
                                isAlreadyVerified = isAlreadyVerified,
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
                                candidateTrips = candidates,
                                status = LeapmotorMatchStatus.DURATION_MISMATCH,
                                isAlreadyVerified = isAlreadyVerified,
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
                // Multiple CIMDriver trips cover this single LM trip (duplicates or fragments)
                val primaryTrip = candidates.first()
                items.add(
                    LeapmotorMatchItem(
                        id = "split_${lm.dateText}_${lm.startTimeText}",
                        lmTrip = lm,
                        matchingTrip = primaryTrip,
                        candidateTrips = candidates,
                        status = LeapmotorMatchStatus.SPLIT_TRIP_CANDIDATE,
                        isAlreadyVerified = false,
                        startDeltaMinutes = 0,
                        endDeltaMinutes = 0,
                        durationDeltaMinutes = candidates.sumOf { it.durationMinutes() } - lmDur,
                        explanation = "Leapmotor toont 1 aaneengesloten rit ($lmDur min), maar CIMDriver heeft ${candidates.size} ritten (duplicaten of fragmenten) geregistreerd.",
                        suggestedAction = LeapmotorSuggestedAction(
                            type = LeapmotorActionType.MERGE_TRIPS,
                            title = "Voeg ${candidates.size} ritten samen & ruim duplicaten op",
                            description = "Samenvoegen tot 1 rit van ${lm.startTimeText} tot ${lm.endTimeText} en overbodige ritten wissen.",
                            targetTripId = primaryTrip.id,
                            candidateTripIds = candidates.map { it.id },
                            newStartTime = lmStart,
                            newEndTime = lmEnd,
                            estimatedDistanceMeters = lm.distanceKm?.let { (it * 1000.0).roundToInt() }
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
