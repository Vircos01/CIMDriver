import SwiftUI
import SwiftData
import UserNotifications
import BackgroundTasks

final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        TrackingManager.shared.handleNotificationAction(identifier: response.actionIdentifier)
    }
}

@main
struct CIMDriverApp: App {
    private let notificationDelegate = NotificationDelegate()
    @Environment(\.scenePhase) private var scenePhase
    @State private var trackingManager = TrackingManager.shared
    
    @MainActor
    var sharedModelContainer: ModelContainer = ModelContainerSetup.shared
    
    init() {
        UNUserNotificationCenter.current().delegate = notificationDelegate
        TrackingManager.shared.modelContext = sharedModelContainer.mainContext
        TrackingManager.shared.setup(modelContext: sharedModelContainer.mainContext)
        registerBackgroundTasks()
        
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, error in
            if let error = error {
                print("Error requesting notification auth: \(error)")
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(trackingManager)
        }
        .modelContainer(sharedModelContainer)
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .background {
                scheduleAppRefresh()
            }
        }
    }
    
    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.cimdriver.refresh", using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handleAppRefresh(task: refreshTask)
        }
    }
    
    private func handleAppRefresh(task: BGAppRefreshTask) {
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 1
        
        task.expirationHandler = {
            queue.cancelAllOperations()
        }
        
        queue.addOperation {
            Task { @MainActor in
                TrackingManager.shared.setup(modelContext: sharedModelContainer.mainContext)
                task.setTaskCompleted(success: true)
            }
        }
    }

    private func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.cimdriver.refresh")
        request.earliestBeginDate = Calendar.current.date(byAdding: .hour, value: 24, to: Date())
        try? BGTaskScheduler.shared.submit(request)
    }
}

struct ModelContainerSetup {
    @MainActor
    static let shared: ModelContainer = {
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
        
        let fileManager = FileManager.default
        let groupURL = fileManager.containerURL(forSecurityApplicationGroupIdentifier: "group.com.cimdriver.app")
        
        let modelConfiguration: ModelConfiguration
        if let url = groupURL?.appendingPathComponent("CIMDriver.sqlite") {
            modelConfiguration = ModelConfiguration(schema: schema, url: url)
        } else {
            modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        }

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()
}
