import Foundation
import SwiftData

@Model
final class WorkDay {
    @Attribute(.unique) var id: UUID
    var date: Date // Start of day
    var firstDepartureTime: Date
    var arrivalTime: Date
    var departureTime: Date?
    var lastArrivalTime: Date?
    var roundedArrivalTime: Date?
    var roundedDepartureTime: Date?
    var breakMinutes: Int
    var workLocationLabel: String?
    var projectCode: String?
    var status: String // "IN_PROGRESS", "TO_REVIEW", "DONE"
    var note: String?

    init(id: UUID = UUID(), date: Date, firstDepartureTime: Date, arrivalTime: Date, departureTime: Date? = nil, lastArrivalTime: Date? = nil, roundedArrivalTime: Date? = nil, roundedDepartureTime: Date? = nil, breakMinutes: Int = 30, workLocationLabel: String? = nil, projectCode: String? = nil, status: String = "IN_PROGRESS", note: String? = nil) {
        self.id = id
        self.date = date
        self.firstDepartureTime = firstDepartureTime
        self.arrivalTime = arrivalTime
        self.departureTime = departureTime
        self.lastArrivalTime = lastArrivalTime
        self.roundedArrivalTime = roundedArrivalTime
        self.roundedDepartureTime = roundedDepartureTime
        self.breakMinutes = breakMinutes
        self.workLocationLabel = workLocationLabel
        self.projectCode = projectCode
        self.status = status
        self.note = note
    }
}
