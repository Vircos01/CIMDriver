import Foundation
import SwiftData

@Model
final class HoursTarget {
    @Attribute(.unique) var id: UUID
    var name: String
    var clientId: UUID?
    var projectCodeId: UUID?
    var targetType: String
    var targetHours: Double
    var targetRevenue: Double
    var year: Int
    var color: String
    var isActive: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        clientId: UUID? = nil,
        projectCodeId: UUID? = nil,
        targetType: String = "HOURS",
        targetHours: Double = 0,
        targetRevenue: Double = 0,
        year: Int = Calendar.current.component(.year, from: Date()),
        color: String = "#4CAF50",
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.clientId = clientId
        self.projectCodeId = projectCodeId
        self.targetType = targetType
        self.targetHours = max(0, targetHours)
        self.targetRevenue = max(0, targetRevenue)
        self.year = year
        self.color = color
        self.isActive = isActive
        self.createdAt = createdAt
    }
}