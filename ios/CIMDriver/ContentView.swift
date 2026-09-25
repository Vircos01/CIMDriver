import SwiftUI
import SwiftData

enum AppTab: Int, Hashable {
    case dashboard = 0
    case trips = 1
    case workHours = 2
    case settings = 3
}

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(TrackingManager.self) private var trackingManager
    @Query private var settingsList: [AppSettings]
    
    private var appSettings: AppSettings? { settingsList.first }
    
    private var activeColorScheme: ColorScheme? {
        guard let theme = appSettings?.themeMode else { return nil }
        switch theme {
        case "LIGHT": return .light
        case "DARK": return .dark
        default: return nil
        }
    }
    
    @State private var selectedTab: AppTab = .dashboard
    @State private var dashboardNavigationID = UUID()
    @State private var tripsNavigationID = UUID()
    @State private var workHoursNavigationID = UUID()
    @State private var settingsNavigationID = UUID()
    
    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView(selectedTab: dashboardTabBinding)
                .id(dashboardNavigationID)
                .tabItem {
                    Label("Dashboard", systemImage: "gauge.with.dots.needle.bottom.50percent")
                }
                .tag(AppTab.dashboard)
            
            TripsView()
                .id(tripsNavigationID)
                .tabItem {
                    Label("Ritten", systemImage: "car.fill")
                }
                .tag(AppTab.trips)
            
            WorkHoursView()
                .id(workHoursNavigationID)
                .tabItem {
                    Label("Uren", systemImage: "clock.fill")
                }
                .tag(AppTab.workHours)
            
            SettingsView()
                .id(settingsNavigationID)
                .tabItem {
                    Label("Instellingen", systemImage: "gearshape.fill")
                }
                .tag(AppTab.settings)
        }
        .tint(.cimGreen)
        .onAppear {
            trackingManager.setup(modelContext: modelContext)
        }
        .onChange(of: selectedTab) { _, newTab in
            resetNavigationState(for: newTab)
        }
        .preferredColorScheme(activeColorScheme)
    }
    
    private var dashboardTabBinding: Binding<Int> {
        Binding(
            get: { selectedTab.rawValue },
            set: { selectedTab = AppTab(rawValue: $0) ?? .dashboard }
        )
    }
    
    private func resetNavigationState(for tab: AppTab) {
        switch tab {
        case .dashboard:
            dashboardNavigationID = UUID()
        case .trips:
            tripsNavigationID = UUID()
        case .workHours:
            workHoursNavigationID = UUID()
        case .settings:
            settingsNavigationID = UUID()
        }
    }
}
