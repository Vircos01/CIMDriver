import SwiftData
import SwiftUI

@MainActor
class PreviewContainer {
    static let shared = PreviewContainer()
    
    let container: ModelContainer
    
    init() {
        let schema = Schema([
            Vehicle.self,
            BluetoothDevice.self,
            Trip.self,
            LocationPoint.self,
            SavedAddress.self,
            WorkDay.self,
            ClassificationRule.self,
            AppSettings.self
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        
        do {
            container = try ModelContainer(for: schema, configurations: [modelConfiguration])
            insertSampleData()
        } catch {
            fatalError("Could not create ModelContainer for preview: \(error)")
        }
    }
    
    private func insertSampleData() {
        let context = container.mainContext
        
        // Sample Trips
        let trip1 = Trip(startTime: Date().addingTimeInterval(-86400), status: "TO_REVIEW", odometerStart: 120500)
        trip1.endTime = Date().addingTimeInterval(-82800) // 1 hour later
        trip1.startAddress = "Kerkstraat 1, Amsterdam"
        trip1.endAddress = "Science Park 400, Amsterdam"
        trip1.distanceMeters = 8500
        trip1.tripType = "COMMUTE"
        
        let trip2 = Trip(startTime: Date().addingTimeInterval(-3600), status: "ACTIVE", odometerStart: 120508)
        trip2.startAddress = "Science Park 400, Amsterdam"
        
        context.insert(trip1)
        context.insert(trip2)
        
        try? context.save()
    }
}
