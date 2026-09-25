import Foundation
import CoreBluetooth
import OSLog

protocol BluetoothServiceDelegate: AnyObject {
    func didConnectToVehicleDevice()
    func didDisconnectFromVehicleDevice()
}

@Observable
class BluetoothService: NSObject {
    private let logger = Logger(subsystem: "com.cimdriver.app", category: "BluetoothService")
    
    private var centralManager: CBCentralManager!
    weak var delegate: BluetoothServiceDelegate?
    
    // In-memory state for UI
    var isPoweredOn: Bool = false
    var connectedDevices: [UUID] = []
    
    override init() {
        super.init()
        let options: [String: Any] = [
            CBCentralManagerOptionRestoreIdentifierKey: "com.cimsolutions.CIMDriver.Bluetooth"
        ]
        centralManager = CBCentralManager(delegate: self, queue: nil, options: options)
    }
    
    // API for the UI to save a device as a "Carkit"
    func getSavedDeviceUUIDs() -> [UUID] {
        let strings = UserDefaults.standard.stringArray(forKey: "savedCarkitUUIDs") ?? []
        return strings.compactMap { UUID(uuidString: $0) }
    }
    
    func saveDeviceUUID(_ uuid: UUID) {
        var uuids = getSavedDeviceUUIDs()
        if !uuids.contains(uuid) {
            uuids.append(uuid)
            UserDefaults.standard.set(uuids.map { $0.uuidString }, forKey: "savedCarkitUUIDs")
        }
        reconnectToSavedDevices()
    }
    
    func removeDeviceUUID(_ uuid: UUID) {
        var uuids = getSavedDeviceUUIDs()
        uuids.removeAll { $0 == uuid }
        UserDefaults.standard.set(uuids.map { $0.uuidString }, forKey: "savedCarkitUUIDs")
    }
    
    // Try to retrieve and connect to saved devices (for background recovery)
    func reconnectToSavedDevices() {
        guard centralManager.state == .poweredOn else { return }
        
        let savedUUIDs = getSavedDeviceUUIDs()
        guard !savedUUIDs.isEmpty else { return }
        
        let peripherals = centralManager.retrievePeripherals(withIdentifiers: savedUUIDs)
        for peripheral in peripherals {
            if peripheral.state == .disconnected {
                logger.info("Attempting to connect to saved peripheral: \(peripheral.identifier)")
                centralManager.connect(peripheral, options: nil)
            } else if peripheral.state == .connected {
                if !connectedDevices.contains(peripheral.identifier) {
                    connectedDevices.append(peripheral.identifier)
                }
            }
        }
    }
    
    func scanForPeripherals() {
        if centralManager.state == .poweredOn {
            // We just trigger retrieval instead of full scanning, since iOS restricts background scanning
            reconnectToSavedDevices()
        }
    }
}

extension BluetoothService: CBCentralManagerDelegate {
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        isPoweredOn = (central.state == .poweredOn)
        logger.info("Bluetooth state updated: \(central.state.rawValue)")
        
        if central.state == .poweredOn {
            reconnectToSavedDevices()
        }
    }
    
    // Called when iOS wakes up the app in the background for Bluetooth events
    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
        logger.info("Bluetooth state restored by iOS")
        if let peripherals = dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral] {
            for peripheral in peripherals {
                // If it's one of our saved carkits, we want to maintain the connection
                if getSavedDeviceUUIDs().contains(peripheral.identifier) {
                    centralManager.connect(peripheral, options: nil)
                    if peripheral.state == .connected {
                        if !connectedDevices.contains(peripheral.identifier) {
                            connectedDevices.append(peripheral.identifier)
                            delegate?.didConnectToVehicleDevice()
                        }
                    }
                }
            }
        }
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        logger.info("Connected to peripheral: \(peripheral.identifier)")
        if !connectedDevices.contains(peripheral.identifier) {
            connectedDevices.append(peripheral.identifier)
        }
        
        if getSavedDeviceUUIDs().contains(peripheral.identifier) {
            delegate?.didConnectToVehicleDevice()
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        logger.info("Disconnected from peripheral: \(peripheral.identifier), error: \(String(describing: error))")
        connectedDevices.removeAll { $0 == peripheral.identifier }
        
        if getSavedDeviceUUIDs().contains(peripheral.identifier) {
            delegate?.didDisconnectFromVehicleDevice()
            // Immediately try to reconnect so we get notified next time they get in the car
            centralManager.connect(peripheral, options: nil)
        }
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        logger.error("Failed to connect to peripheral: \(peripheral.identifier)")
        // Retry connection
        if getSavedDeviceUUIDs().contains(peripheral.identifier) {
            centralManager.connect(peripheral, options: nil)
        }
    }
}
