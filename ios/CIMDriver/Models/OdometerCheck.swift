import Foundation
import SwiftData

@Model
class OdometerCheck {
    var id: UUID
    var vehicleId: UUID
    var timestamp: Date
    var registeredOdometer: Int
    var correctedOdometer: Int
    var photoData: Data?
    
    init(id: UUID = UUID(), vehicleId: UUID, timestamp: Date = Date(), registeredOdometer: Int, correctedOdometer: Int, photoData: Data? = nil) {
        self.id = id
        self.vehicleId = vehicleId
        self.timestamp = timestamp
        self.registeredOdometer = registeredOdometer
        self.correctedOdometer = correctedOdometer
        self.photoData = photoData
    }
}
