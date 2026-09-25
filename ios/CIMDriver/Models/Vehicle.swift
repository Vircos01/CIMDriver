import Foundation
import SwiftData

@Model
final class Vehicle {
    @Attribute(.unique) var id: UUID
    var name: String
    var licensePlate: String
    var make: String
    var model: String
    var year: String?
    var odometerStart: Int
    var odometerCurrent: Int
    var privateKmYearlyLimit: Int
    var showPrivateKmWarning: Bool
    var usageType: String // "MIXED", "BUSINESS_ONLY", "PRIVATE_ONLY"
    var lastOdometerCheckTimestamp: Date?
    var isDefault: Bool
    var odometerCorrectionStrategy: String // "DISTRIBUTE", "CREATE_TRIP", "LEAVE_GAP"

    @Relationship(deleteRule: .cascade) var bluetoothDevices: [BluetoothDevice] = []
    @Relationship(deleteRule: .nullify) var trips: [Trip] = []

    init(id: UUID = UUID(), name: String, licensePlate: String, make: String, model: String, year: String? = nil, odometerStart: Int, odometerCurrent: Int, privateKmYearlyLimit: Int = 500, showPrivateKmWarning: Bool = true, usageType: String = "MIXED", lastOdometerCheckTimestamp: Date? = nil, isDefault: Bool = false, odometerCorrectionStrategy: String = "DISTRIBUTE") {
        self.id = id
        self.name = name
        self.licensePlate = licensePlate
        self.make = make
        self.model = model
        self.year = year
        self.odometerStart = odometerStart
        self.odometerCurrent = odometerCurrent
        self.privateKmYearlyLimit = privateKmYearlyLimit
        self.showPrivateKmWarning = showPrivateKmWarning
        self.usageType = usageType
        self.lastOdometerCheckTimestamp = lastOdometerCheckTimestamp
        self.isDefault = isDefault
        self.odometerCorrectionStrategy = odometerCorrectionStrategy
    }
}
