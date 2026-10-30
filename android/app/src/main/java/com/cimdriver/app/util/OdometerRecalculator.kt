package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.OdometerCheck
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.util.OdometerGapAnalyzer.effectiveEndTime
import com.cimdriver.app.util.OdometerGapAnalyzer.effectiveOdometerEnd

data class TripChangeDetail(
    val tripId: Long,
    val startTime: Long,
    val startAddress: String?,
    val endAddress: String?,
    val oldStart: Int,
    val newStart: Int,
    val oldEnd: Int,
    val newEnd: Int,
    val distanceKm: Int
)

data class RecalculationPreview(
    val vehicleId: Long,
    val initialOdometer: Int,
    val totalTrips: Int,
    val modifiedTripsCount: Int,
    val modifiedTrips: List<TripChangeDetail>,
    val preservedChecksCount: Int,
    val preservedCheckpoints: List<OdometerCheck>,
    val remainingGaps: List<OdometerGap>,
    val oldCurrentOdometer: Int,
    val newCurrentOdometer: Int,
    val recalculatedTrips: List<Trip>
)

object OdometerRecalculator {

    /**
     * Recalculates the odometer readings (start and end) for all trips chronologically,
     * using the vehicle's starting odometer, the individual trip distances, and any
     * confirmed OdometerChecks as physical checkpoint anchors.
     *
     * Checkpoint anchors (OdometerChecks) are preserved: if a check has a reading that
     * differs from the accumulated trip distance, the check sets the new odometer baseline
     * for subsequent trips, leaving a gap before the check that can be reviewed and resolved.
     */
    fun preview(
        vehicle: Vehicle,
        trips: List<Trip>,
        odometerChecks: List<OdometerCheck> = emptyList(),
        resolutions: List<OdometerGapResolution> = emptyList()
    ): RecalculationPreview {
        val sortedTrips = trips.filter { it.vehicleId == vehicle.id }
            .sortedWith(compareBy({ it.startTime }, { it.id }))

        val validChecks = odometerChecks
            .filter { it.vehicleId == vehicle.id && it.status != "CANCELLED" }
            .sortedBy { it.timestamp }

        val explainedResolutions = resolutions
            .filter { it.vehicleId == vehicle.id && it.resolution == "MARK_AS_EXPLAINED" && it.status == "RESOLVED" }
            .sortedBy { it.gapStartTimestamp }

        var runningOdometer = vehicle.odometerStart
        var lastProcessedTime = vehicle.inServiceDate ?: Long.MIN_VALUE
        val preservedCheckpoints = mutableListOf<OdometerCheck>()

        // 1. Any odometer checks prior to the first trip?
        if (sortedTrips.isNotEmpty()) {
            val firstTripStart = sortedTrips.first().startTime
            val checksBeforeFirst = validChecks.filter { it.timestamp <= firstTripStart }
            if (checksBeforeFirst.isNotEmpty()) {
                val latestBefore = checksBeforeFirst.maxByOrNull { it.timestamp }!!
                runningOdometer = latestBefore.correctedOdometer
                lastProcessedTime = latestBefore.timestamp
                preservedCheckpoints.addAll(checksBeforeFirst)
            }
        }

        val recalculatedTrips = mutableListOf<Trip>()
        val changedDetails = mutableListOf<TripChangeDetail>()

        // 2. Iterate through trips
        for (trip in sortedTrips) {
            // Check if any OdometerCheck occurred between the previous event and this trip's start
            val checksInWindow = validChecks.filter { it.timestamp > lastProcessedTime && it.timestamp <= trip.startTime }
            if (checksInWindow.isNotEmpty()) {
                val latestCheck = checksInWindow.maxByOrNull { it.timestamp }!!
                runningOdometer = latestCheck.correctedOdometer
                lastProcessedTime = latestCheck.timestamp
                preservedCheckpoints.addAll(checksInWindow)
            } else {
                // Also check if an explained gap occurred in this window without a formal check
                val expInWindow = explainedResolutions.filter {
                    it.gapStartTimestamp >= lastProcessedTime && it.gapEndTimestamp <= trip.startTime
                }
                if (expInWindow.isNotEmpty()) {
                    runningOdometer += expInWindow.sumOf { it.gapKm }
                }
            }

            val distanceKm = TripDistance.metersToOdometerKilometers(trip.distanceMeters)
            val newStart = runningOdometer
            val newEnd = newStart + distanceKm

            val oldEnd = trip.effectiveOdometerEnd()
            if (trip.odometerStart != newStart || oldEnd != newEnd) {
                changedDetails.add(
                    TripChangeDetail(
                        tripId = trip.id,
                        startTime = trip.startTime,
                        startAddress = trip.startAddress,
                        endAddress = trip.endAddress,
                        oldStart = trip.odometerStart,
                        newStart = newStart,
                        oldEnd = oldEnd,
                        newEnd = newEnd,
                        distanceKm = distanceKm
                    )
                )
            }

            val updatedTrip = trip.copy(
                odometerStart = newStart,
                odometerEnd = newEnd
            )
            recalculatedTrips.add(updatedTrip)

            runningOdometer = newEnd
            lastProcessedTime = trip.effectiveEndTime()
        }

        // 3. Post-trip checks and current vehicle odometer
        val checksAfterLast = validChecks.filter { it.timestamp > lastProcessedTime }
        if (checksAfterLast.isNotEmpty()) {
            preservedCheckpoints.addAll(checksAfterLast)
        }

        val newCurrentOdometer = if (checksAfterLast.isNotEmpty()) {
            checksAfterLast.maxByOrNull { it.timestamp }!!.correctedOdometer
        } else {
            // Keep vehicle.odometerCurrent if it represents a higher/later physical reading,
            // or advance it to runningOdometer if trips exceed the previous odometerCurrent
            maxOf(vehicle.odometerCurrent, runningOdometer)
        }

        // 4. Analyze remaining gaps with recalculated trips
        val simulatedVehicle = vehicle.copy(odometerCurrent = newCurrentOdometer)
        val remainingGaps = OdometerGapAnalyzer.analyze(simulatedVehicle, recalculatedTrips)

        return RecalculationPreview(
            vehicleId = vehicle.id,
            initialOdometer = vehicle.odometerStart,
            totalTrips = sortedTrips.size,
            modifiedTripsCount = changedDetails.size,
            modifiedTrips = changedDetails,
            preservedChecksCount = preservedCheckpoints.distinctBy { it.id }.size,
            preservedCheckpoints = preservedCheckpoints.distinctBy { it.id },
            remainingGaps = remainingGaps,
            oldCurrentOdometer = vehicle.odometerCurrent,
            newCurrentOdometer = newCurrentOdometer,
            recalculatedTrips = recalculatedTrips
        )
    }
}

