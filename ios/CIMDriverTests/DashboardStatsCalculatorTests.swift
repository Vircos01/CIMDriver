import XCTest
@testable import CIMDriver

final class DashboardStatsCalculatorTests: XCTestCase {

    func testYTDAndCategoryAggregation() {
        let vehicle = Vehicle(name: "TestCar", licensePlate: "AB-123-C", make: "BMW", model: "i4", odometerStart: 0, odometerCurrent: 500)
        let addressWerk = SavedAddress(label: "Werk", address: "Kantoor", addressType: "WERK")
        
        let now = Date()
        let cal = Calendar.current
        
        // 1. Zakelijke rit (vandaag)
        let tripBusiness = Trip(
            vehicle: vehicle,
            startTime: now,
            endTime: now.addingTimeInterval(3600), // 1 uur
            distanceMeters: 50000,
            tripType: "BUSINESS",
            status: "DONE",
            odometerStart: 0
        )
        
        // 2. Woon-werk rit (gisteren)
        let yesterday = cal.date(byAdding: .day, value: -1, to: now)!
        let tripCommute = Trip(
            vehicle: vehicle,
            startTime: yesterday,
            endTime: yesterday.addingTimeInterval(1800), // 30 min
            endAddress: "Kantoor",
            distanceMeters: 25000,
            tripType: "COMMUTE",
            status: "DONE",
            odometerStart: 50000
        )
        
        // 3. Prive rit (vandaag)
        let tripPrivate = Trip(
            vehicle: vehicle,
            startTime: now,
            endTime: now.addingTimeInterval(1800), // 30 min
            distanceMeters: 10000,
            tripType: "PRIVATE",
            status: "DONE",
            odometerStart: 75000
        )
        
        // Calculate stats for current week
        let stats = DashboardStatsCalculator.calculateDashboardStats(
            tripList: [tripBusiness, tripCommute, tripPrivate],
            timeFilter: .week,
            selectedVehicleId: vehicle.id,
            vehicleList: [vehicle],
            rules: [],
            addresses: [addressWerk]
        )
        
        // Assert Zakelijk
        XCTAssertEqual(stats.zakelijkKm, 50.0, "Zakelijk should be 50km")
        XCTAssertEqual(stats.zakelijkMin, 60, "Zakelijk duration should be 60 min")
        
        // Assert Commute (Woon-Werk)
        XCTAssertEqual(stats.woonWerkKm, 25.0, "Woon-werk should be 25km")
        XCTAssertEqual(stats.woonWerkMin, 30, "Woon-werk duration should be 30 min")
        
        // Assert Private
        XCTAssertEqual(stats.priveKm, 10.0, "Private should be 10km")
        XCTAssertEqual(stats.priveMin, 30, "Private duration should be 30 min")
        
        // Assert YTD Private
        XCTAssertEqual(stats.ytdPriveKm, 10.0, "Year-to-date private should be 10km")
    }
}
