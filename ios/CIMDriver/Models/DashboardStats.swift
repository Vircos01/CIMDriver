import Foundation

struct DashboardStats {
    var zakelijkKm: Double = 0
    var priveKm: Double = 0
    var woonWerkKm: Double = 0
    var zakelijkMin: Int = 0
    var priveMin: Int = 0
    var woonWerkMin: Int = 0
    var prevZakelijkKm: Double = 0
    var prevPriveKm: Double = 0
    var prevWoonWerkKm: Double = 0
    var ytdPriveKm: Double = 0
}

enum TimeFilter: String, CaseIterable, Identifiable {
    case week = "Week"
    case month = "Maand"
    case year = "Jaar"
    
    var id: String { self.rawValue }
}

struct DistanceChartItem: Identifiable {
    var id = UUID()
    var label: String
    var zakelijkKm: Double
    var priveKm: Double
    var woonWerkKm: Double
    var totalKm: Double
}

struct CategoryBreakdownItem: Identifiable {
    var id = UUID()
    var categoryName: String
    var distanceKm: Double
    var percentage: Double
}

enum TripStatus: String {
    case active = "ACTIVE"
    case done = "DONE"
    case merged = "MERGED"
    case toReview = "TO_REVIEW"
}
