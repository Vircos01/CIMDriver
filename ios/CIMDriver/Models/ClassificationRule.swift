import Foundation
import SwiftData

@Model
final class ClassificationRule {
    @Attribute(.unique) var id: UUID
    var name: String
    var startAddressType: String? // "THUIS", "WERK", "KLANT"
    var endAddressType: String?
    var startAddress: String?
    var endAddress: String?
    var tripType: String?
    var category: String // "BUSINESS", "PRIVATE", "COMMUTE"

    init(id: UUID = UUID(), name: String, startAddressType: String? = nil, endAddressType: String? = nil, startAddress: String? = nil, endAddress: String? = nil, tripType: String? = nil, category: String = "PRIVATE") {
        self.id = id
        self.name = name
        self.startAddressType = startAddressType
        self.endAddressType = endAddressType
        self.startAddress = startAddress
        self.endAddress = endAddress
        self.tripType = tripType
        self.category = category
    }
}
