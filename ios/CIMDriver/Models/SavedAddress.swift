import Foundation
import SwiftData

@Model
final class SavedAddress {
    @Attribute(.unique) var id: UUID
    var label: String
    var address: String
    var isWorkLocation: Bool
    var isHomeLocation: Bool
    var isCustomerLocation: Bool
    var defaultTripType: String?
    var projectCode: String?
    var addressType: String? // "THUIS", "WERK", "KLANT"
    var latitude: Double?
    var longitude: Double?

    init(id: UUID = UUID(), label: String, address: String, isWorkLocation: Bool = false, isHomeLocation: Bool = false, isCustomerLocation: Bool = false, defaultTripType: String? = nil, projectCode: String? = nil, addressType: String? = nil, latitude: Double? = nil, longitude: Double? = nil) {
        self.id = id
        self.label = label
        self.address = address
        self.isWorkLocation = isWorkLocation
        self.isHomeLocation = isHomeLocation
        self.isCustomerLocation = isCustomerLocation
        self.defaultTripType = defaultTripType
        self.projectCode = projectCode
        self.addressType = addressType
        self.latitude = latitude
        self.longitude = longitude
    }
}
