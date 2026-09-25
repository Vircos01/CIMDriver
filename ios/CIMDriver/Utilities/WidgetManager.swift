import Foundation
import SwiftData
import WidgetKit

struct WidgetManager {
    static func updateWidget(context: ModelContext) {
        let trips = (try? context.fetch(FetchDescriptor<Trip>())) ?? []
        let vehicles = (try? context.fetch(FetchDescriptor<Vehicle>())) ?? []
        let addresses = (try? context.fetch(FetchDescriptor<SavedAddress>())) ?? []
        let rules = (try? context.fetch(FetchDescriptor<ClassificationRule>())) ?? []

        let activeTrip = trips.first(where: { $0.status == "ACTIVE" })
        let stats = DashboardStatsCalculator.calculateDashboardStats(
            tripList: trips,
            timeFilter: .month,
            selectedVehicleId: nil,
            vehicleList: vehicles,
            rules: rules,
            addresses: addresses
        )

        if let defaults = UserDefaults(suiteName: "group.com.cimdriver.app") {
            defaults.set(trips.count, forKey: "widget_tripCount")
            defaults.set(stats.zakelijkKm, forKey: "widget_month_business_km")
            defaults.set(stats.priveKm, forKey: "widget_month_private_km")
            defaults.set(stats.woonWerkKm, forKey: "widget_month_commute_km")

            defaults.set(activeTrip != nil, forKey: "widget_active_trip_is_active")
            defaults.set(activeTrip?.startTime, forKey: "widget_active_trip_start_time")
            defaults.set(activeTrip?.distanceMeters ?? 0, forKey: "widget_active_trip_distance_meters")
            defaults.set(activeTrip?.vehicle?.name, forKey: "widget_active_trip_vehicle_name")
        }
        
        WidgetCenter.shared.reloadAllTimelines()
    }
}
