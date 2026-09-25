import Foundation
import SwiftData

@Model
final class Trip {
    @Attribute(.unique) var id: UUID
    var vehicle: Vehicle?
    var startTime: Date
    var endTime: Date?
    var startAddress: String?
    var endAddress: String?
    var distanceMeters: Int
    var tripType: String // "BUSINESS", "Home To Work", "Customer Visit", etc.
    var note: String?
    var status: String // "ACTIVE", "TO_REVIEW", "DONE", "MERGED"
    var isManual: Bool
    var odometerStart: Int
    var odometerEnd: Int?
    var expectedDistanceMeters: Int?
    var projectCode: String?
    var recoveryState: String?
    @Relationship(deleteRule: .cascade, inverse: \LocationPoint.trip)
    var locationPoints: [LocationPoint] = []

    init(id: UUID = UUID(), vehicle: Vehicle? = nil, startTime: Date = Date(), endTime: Date? = nil, startAddress: String? = nil, endAddress: String? = nil, distanceMeters: Int = 0, tripType: String = "PRIVATE", note: String? = nil, status: String = "ACTIVE", isManual: Bool = false, odometerStart: Int, odometerEnd: Int? = nil, expectedDistanceMeters: Int? = nil, projectCode: String? = nil, recoveryState: String? = nil) {
        self.id = id
        self.vehicle = vehicle
        self.startTime = startTime
        self.endTime = endTime
        self.startAddress = startAddress
        self.endAddress = endAddress
        self.distanceMeters = distanceMeters
        self.tripType = tripType
        self.note = note
        self.status = status
        self.isManual = isManual
        self.odometerStart = odometerStart
        self.odometerEnd = odometerEnd
        self.expectedDistanceMeters = expectedDistanceMeters
        self.projectCode = projectCode
        self.recoveryState = recoveryState
    }
}
