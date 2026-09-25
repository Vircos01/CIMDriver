import SwiftUI
import CoreBluetooth
import SwiftData

struct VehicleBluetoothView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(TrackingManager.self) private var trackingManager
    @State private var scanner = BluetoothScanner()
    
    var vehicle: Vehicle

    
    var body: some View {
        List {
            Section(header: Text("Gekoppelde Carkits"), footer: Text("CIMDriver start automatisch een rit wanneer uw telefoon met deze apparaten verbindt.")) {
                
                if vehicle.bluetoothDevices.isEmpty {
                    Text("Geen carkit gekoppeld aan deze auto.")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(vehicle.bluetoothDevices) { device in
                        let uuid = UUID(uuidString: device.macAddress) ?? UUID()
                        HStack {
                            VStack(alignment: .leading) {
                                Text(device.deviceName)
                                    .font(.headline)
                                Text(device.macAddress)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if trackingManager.bluetoothService.connectedDevices.contains(uuid) {
                                Text("Verbonden")
                                    .font(.caption)
                                    .foregroundColor(.cimGreen)
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                trackingManager.bluetoothService.removeDeviceUUID(uuid)
                                modelContext.delete(device)
                                try? modelContext.save()
                            } label: {
                                Label("Verwijder", systemImage: "trash")
                            }
                        }

                    }
                }
            }
            
            Section(header: Text("Nieuwe apparaten in de buurt"), footer: Text("Zorg dat u in de auto zit en dat de auto aan staat.")) {
                if scanner.isScanning {
                    HStack {
                        ProgressView()
                            .padding(.trailing, 8)
                        Text("Zoeken naar apparaten...")
                    }
                }
                
                ForEach(scanner.discoveredPeripherals, id: \.peripheral.identifier) { item in
                    Button(action: {
                        trackingManager.bluetoothService.saveDeviceUUID(item.peripheral.identifier)
                        let newDevice = BluetoothDevice(
                            macAddress: item.peripheral.identifier.uuidString,
                            deviceName: item.peripheral.name ?? "Onbekend apparaat",
                            vehicle: vehicle
                        )
                        modelContext.insert(newDevice)
                        try? modelContext.save()
                        scanner.stopScanning()
                    }) {
                        HStack {
                            Text(item.peripheral.name ?? "Onbekend apparaat")
                                .foregroundColor(.primary)
                            Spacer()
                            Text(item.peripheral.identifier.uuidString.prefix(8) + "...")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("Bluetooth Carkit")
        .onAppear {
            scanner.startScanning()
        }
        .onDisappear {
            scanner.stopScanning()
        }
    }
}

// Helper class specifically for scanning new devices in the settings UI
@Observable
class BluetoothScanner: NSObject, CBCentralManagerDelegate {
    struct DiscoveredPeripheral {
        let peripheral: CBPeripheral
        let rssi: NSNumber
    }
    
    private var centralManager: CBCentralManager!
    var isScanning = false
    var discoveredPeripherals: [DiscoveredPeripheral] = []
    
    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }
    
    func startScanning() {
        if centralManager.state == .poweredOn {
            discoveredPeripherals.removeAll()
            centralManager.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: false])
            isScanning = true
        }
    }
    
    func stopScanning() {
        centralManager.stopScan()
        isScanning = false
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            startScanning()
        } else {
            stopScanning()
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        guard peripheral.name != nil else { return } // Alleen apparaten met een naam
        
        if !discoveredPeripherals.contains(where: { $0.peripheral.identifier == peripheral.identifier }) {
            discoveredPeripherals.append(DiscoveredPeripheral(peripheral: peripheral, rssi: RSSI))
            // Sort by signal strength
            discoveredPeripherals.sort { $0.rssi.intValue > $1.rssi.intValue }
        }
    }
}
