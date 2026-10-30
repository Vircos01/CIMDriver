import Foundation
import SwiftData

@Model
final class OdometerGapResolution {
    @Attribute(.unique) var id: UUID
    var vehicleId: UUID
    var detectedAt: Date
    var resolvedAt: Date?
    var gapType: String
    var gapStartTimestamp: Date
    var gapEndTimestamp: Date
    var expectedOdometer: Int
    var actualOdometer: Int
    var gapKm: Int
    var resolution: String // "CREATE_PRIVATE_TRIP", "ADJUST_SINGLE_TRIP", "MARK_AS_EXPLAINED", "SYNC_VEHICLE_ODOMETER", "IGNORE"
    var resolutionNote: String?
    var affectedTripIds: String?
    var status: String // "RESOLVED", "IGNORED", "OPEN"
    
    init(
        id: UUID = UUID(),
        vehicleId: UUID,
        detectedAt: Date = Date(),
        resolvedAt: Date? = Date(),
        gapType: String,
        gapStartTimestamp: Date,
        gapEndTimestamp: Date,
        expectedOdometer: Int,
        actualOdometer: Int,
        gapKm: Int,
        resolution: String,
        resolutionNote: String? = nil,
        affectedTripIds: String? = nil,
        status: String = "RESOLVED"
    ) {
        self.id = id
        self.vehicleId = vehicleId
        self.detectedAt = detectedAt
        self.resolvedAt = resolvedAt
        self.gapType = gapType
        self.gapStartTimestamp = gapStartTimestamp
        self.gapEndTimestamp = gapEndTimestamp
        self.expectedOdometer = expectedOdometer
        self.actualOdometer = actualOdometer
        self.gapKm = gapKm
        self.resolution = resolution
        self.resolutionNote = resolutionNote
        self.affectedTripIds = affectedTripIds
        self.status = status
    }
}
