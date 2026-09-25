import Foundation
import CoreLocation
import SwiftData

final class RecoveryManager {
    private let staleTripThreshold: TimeInterval = 30 * 60

    enum RecoveryAction {
        case keepActive
        case finalize
    }

    func recoverIfNeeded(context: ModelContext, trip: Trip) -> RecoveryAction {
        if trip.locationPoints.isEmpty {
            trip.recoveryState = "RECOVERED_NO_POINTS"
            return .keepActive
        }

        let lastTimestamp = trip.locationPoints.max(by: { $0.timestamp < $1.timestamp })?.timestamp ?? trip.startTime
        if Date().timeIntervalSince(lastTimestamp) > staleTripThreshold {
            trip.recoveryState = "RECOVERED_STALE"
            return .finalize
        }

        trip.recoveryState = "RECOVERED_ACTIVE"
        return .keepActive
    }

    func finalizeRecoveredTripIfNeeded(context: ModelContext, trip: Trip, fallbackLocation: CLLocation? = nil) {
        guard trip.status == "ACTIVE" else { return }

        if let fallbackLocation {
            let point = LocationPoint(
                trip: trip,
                latitude: fallbackLocation.coordinate.latitude,
                longitude: fallbackLocation.coordinate.longitude,
                altitude: fallbackLocation.altitude,
                speed: fallbackLocation.speed,
                accuracy: fallbackLocation.horizontalAccuracy,
                timestamp: fallbackLocation.timestamp
            )
            context.insert(point)
            trip.locationPoints.append(point)
        }

        trip.recoveryState = "RECOVERED_FINALIZED"
        trip.status = "TO_REVIEW"
        trip.endTime = trip.locationPoints.max(by: { $0.timestamp < $1.timestamp })?.timestamp ?? Date()
    }
}
