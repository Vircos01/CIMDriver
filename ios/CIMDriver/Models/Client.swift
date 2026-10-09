import Foundation
import SwiftData

@Model
final class Client {
    @Attribute(.unique) var id: UUID
    var name: String
    var color: String
    var isActive: Bool
    var createdAt: Date

    init(id: UUID = UUID(), name: String, color: String = "#1976D2", isActive: Bool = true, createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.color = color
        self.isActive = isActive
        self.createdAt = createdAt
    }
}