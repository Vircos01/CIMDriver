import Foundation
import SwiftData

struct TripChangeDetail: Identifiable {
    var id: UUID { tripId }
    let tripId: UUID
    let startTime: Date
    let startAddress: String?
    let endAddress: String?
    let oldStart: Int
    let newStart: Int
    let oldEnd: Int
    let newEnd: Int
    let distanceKm: Int
}

struct TripRecalculationUpdate {
    let trip: Trip
    let newStart: Int
    let newEnd: Int
}

struct RecalculationPreview {
    let vehicleId: UUID
    let initialOdometer: Int
    let totalTrips: Int
    let modifiedTripsCount: Int
    let modifiedTrips: [TripChangeDetail]
    let preservedChecksCount: Int
    let preservedCheckpoints: [OdometerCheck]
    let remainingGaps: [OdometerGap]
    let oldCurrentOdometer: Int
    let newCurrentOdometer: Int
    let updates: [TripRecalculationUpdate]
}

struct OdometerRecalculator {
    
    /**
     * Recalculates the odometer readings (start and end) for all trips chronologically,
     * using the vehicle's starting odometer, the individual trip distances, and any
     * confirmed OdometerChecks as physical checkpoint anchors.
     */
    static func preview(
        vehicle: Vehicle,
        trips: [Trip],
        odometerChecks: [OdometerCheck] = [],
        resolutions: [OdometerGapResolution] = []
    ) -> RecalculationPreview {
        let vehicleTrips = trips.filter { $0.vehicle?.id == vehicle.id }
        let sortedTrips = vehicleTrips.sorted {
            if $0.startTime == $1.startTime {
                return $0.id.uuidString < $1.id.uuidString
            }
            return $0.startTime < $1.startTime
        }
        
        let validChecks = odometerChecks
            .filter { $0.vehicleId == vehicle.id }
            .sorted { $0.timestamp < $1.timestamp }
        
        let explainedResolutions = resolutions
            .filter { $0.vehicleId == vehicle.id && $0.resolution == "MARK_AS_EXPLAINED" && $0.status == "RESOLVED" }
            .sorted { $0.gapStartTimestamp < $1.gapStartTimestamp }
        
        var runningOdometer = vehicle.odometerStart
        var lastProcessedTime = vehicle.inServiceDate ?? Date.distantPast
        var preservedCheckpoints: [OdometerCheck] = []
        
        // 1. Any odometer checks prior to the first trip?
        if let firstTrip = sortedTrips.first {
            let checksBeforeFirst = validChecks.filter { $0.timestamp <= firstTrip.startTime }
            if let latestBefore = checksBeforeFirst.max(by: { $0.timestamp < $1.timestamp }) {
                runningOdometer = latestBefore.correctedOdometer
                lastProcessedTime = latestBefore.timestamp
                preservedCheckpoints.append(contentsOf: checksBeforeFirst)
            }
        }
        
        var changedDetails: [TripChangeDetail] = []
        var updates: [TripRecalculationUpdate] = []
        var simulatedTrips: [Trip] = []
        
        // 2. Iterate through trips chronologically
        for trip in sortedTrips {
            let checksInWindow = validChecks.filter { $0.timestamp > lastProcessedTime && $0.timestamp <= trip.startTime }
            if let latestCheck = checksInWindow.max(by: { $0.timestamp < $1.timestamp }) {
                runningOdometer = latestCheck.correctedOdometer
                lastProcessedTime = latestCheck.timestamp
                preservedCheckpoints.append(contentsOf: checksInWindow)
            } else {
                let expInWindow = explainedResolutions.filter {
                    $0.gapStartTimestamp >= lastProcessedTime && $0.gapEndTimestamp <= trip.startTime
                }
                if !expInWindow.isEmpty {
                    runningOdometer += expInWindow.reduce(0) { $0 + $1.gapKm }
                }
            }
            
            let distanceKm = max(0, Int(round(Double(trip.distanceMeters) / 1000.0)))
            let newStart = runningOdometer
            let newEnd = newStart + distanceKm
            let oldEnd = OdometerGapAnalyzer.effectiveOdometerEnd(trip: trip)
            
            if trip.odometerStart != newStart || oldEnd != newEnd {
                changedDetails.append(
                    TripChangeDetail(
                        tripId: trip.id,
                        startTime: trip.startTime,
                        startAddress: trip.startAddress,
                        endAddress: trip.endAddress,
                        oldStart: trip.odometerStart,
                        newStart: newStart,
                        oldEnd: oldEnd,
                        newEnd: newEnd,
                        distanceKm: distanceKm
                    )
                )
            }
            
            updates.append(TripRecalculationUpdate(trip: trip, newStart: newStart, newEnd: newEnd))
            
            let simTrip = Trip(
                id: trip.id,
                vehicle: vehicle,
                startTime: trip.startTime,
                endTime: trip.endTime,
                startAddress: trip.startAddress,
                endAddress: trip.endAddress,
                distanceMeters: trip.distanceMeters,
                tripType: trip.tripType,
                note: trip.note,
                status: trip.status,
                isManual: trip.isManual,
                odometerStart: newStart,
                odometerEnd: newEnd,
                expectedDistanceMeters: trip.expectedDistanceMeters,
                projectCode: trip.projectCode,
                recoveryState: trip.recoveryState
            )
            simulatedTrips.append(simTrip)
            
            runningOdometer = newEnd
            lastProcessedTime = OdometerGapAnalyzer.effectiveEndTime(trip: trip)
        }
        
        // 3. Post-trip checks and current vehicle odometer
        let checksAfterLast = validChecks.filter { $0.timestamp > lastProcessedTime }
        if !checksAfterLast.isEmpty {
            preservedCheckpoints.append(contentsOf: checksAfterLast)
        }
        
        let newCurrentOdometer: Int
        if let latestPostCheck = checksAfterLast.max(by: { $0.timestamp < $1.timestamp }) {
            newCurrentOdometer = latestPostCheck.correctedOdometer
        } else {
            newCurrentOdometer = max(vehicle.odometerCurrent, runningOdometer)
        }
        
        // 4. Analyze remaining gaps with recalculated trips
        let simVehicle = Vehicle(
            id: vehicle.id,
            name: vehicle.name,
            licensePlate: vehicle.licensePlate,
            make: vehicle.make,
            model: vehicle.model,
            odometerStart: vehicle.odometerStart,
            odometerCurrent: newCurrentOdometer,
            privateKmYearlyLimit: vehicle.privateKmYearlyLimit,
            inServiceDate: vehicle.inServiceDate
        )
        let remainingGaps = OdometerGapAnalyzer.analyze(vehicle: simVehicle, trips: simulatedTrips)
        
        let uniqueCheckpoints = Array(Dictionary(grouping: preservedCheckpoints, by: { $0.id }).values.compactMap { $0.first })
        
        return RecalculationPreview(
            vehicleId: vehicle.id,
            initialOdometer: vehicle.odometerStart,
            totalTrips: sortedTrips.count,
            modifiedTripsCount: changedDetails.count,
            modifiedTrips: changedDetails,
            preservedChecksCount: uniqueCheckpoints.count,
            preservedCheckpoints: uniqueCheckpoints,
            remainingGaps: remainingGaps,
            oldCurrentOdometer: vehicle.odometerCurrent,
            newCurrentOdometer: newCurrentOdometer,
            updates: updates
        )
    }
    
    /**
     * Applies the preview updates to the trips and vehicle in the model context.
     */
    static func apply(preview: RecalculationPreview, vehicle: Vehicle, in context: ModelContext) {
        for update in preview.updates {
            update.trip.odometerStart = update.newStart
            update.trip.odometerEnd = update.newEnd
        }
        vehicle.odometerCurrent = preview.newCurrentOdometer
        try? context.save()
    }
}

