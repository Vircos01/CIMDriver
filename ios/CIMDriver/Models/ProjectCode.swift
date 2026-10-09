import Foundation
import SwiftData

@Model
final class ProjectCode {
    @Attribute(.unique) var id: UUID
    @Attribute(.unique) var code: String
    var clientId: UUID?
    var details: String?
    var hourlyRate: Double
    var isBillable: Bool
    var isActive: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        code: String,
        clientId: UUID? = nil,
        details: String? = nil,
        hourlyRate: Double = 0,
        isBillable: Bool = true,
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.code = code
        self.clientId = clientId
        self.details = details
        self.hourlyRate = max(0, hourlyRate)
        self.isBillable = isBillable
        self.isActive = isActive
        self.createdAt = createdAt
    }
}