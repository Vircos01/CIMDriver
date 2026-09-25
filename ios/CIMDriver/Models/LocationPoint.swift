import Foundation
import SwiftData

@Model
final class LocationPoint {
    @Attribute(.unique) var id: UUID
    var trip: Trip?
    var latitude: Double
    var longitude: Double
    var altitude: Double
    var speed: Double
    var accuracy: Double
    var timestamp: Date

    init(id: UUID = UUID(), trip: Trip? = nil, latitude: Double, longitude: Double, altitude: Double, speed: Double, accuracy: Double, timestamp: Date = Date()) {
        self.id = id
        self.trip = trip
        self.latitude = latitude
        self.longitude = longitude
        self.altitude = altitude
        self.speed = speed
        self.accuracy = accuracy
        self.timestamp = timestamp
    }
}
