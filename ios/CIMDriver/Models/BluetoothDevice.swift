import Foundation
import SwiftData

@Model
final class BluetoothDevice {
    @Attribute(.unique) var macAddress: String
    var deviceName: String
    var isAutoTrigger: Bool
    
    var vehicle: Vehicle?

    init(macAddress: String, deviceName: String, isAutoTrigger: Bool = true, vehicle: Vehicle? = nil) {
        self.macAddress = macAddress
        self.deviceName = deviceName
        self.isAutoTrigger = isAutoTrigger
        self.vehicle = vehicle
    }
}
