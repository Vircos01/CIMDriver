import Foundation
import SwiftData

@Model
final class FuelFillUp {
    @Attribute(.unique) var id: UUID
    var vehicle: Vehicle?
    var date: Date
    var liters: Double
    var pricePerLiter: Double
    var totalCost: Double
    var odometer: Int
    var stationName: String?
    var notes: String?
    
    init(id: UUID = UUID(), vehicle: Vehicle? = nil, date: Date = Date(), liters: Double = 0, pricePerLiter: Double = 0, totalCost: Double = 0, odometer: Int = 0, stationName: String? = nil, notes: String? = nil) {
        self.id = id
        self.vehicle = vehicle
        self.date = date
        self.liters = liters
        self.pricePerLiter = pricePerLiter
        self.totalCost = totalCost
        self.odometer = odometer
        self.stationName = stationName
        self.notes = notes
    }
}
