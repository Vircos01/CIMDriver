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
        
        // 2. Woon-werk rit (eerder vandaag)
        let earlierToday = cal.date(byAdding: .hour, value: -2, to: now)!
        let tripCommute = Trip(
            vehicle: vehicle,
            startTime: earlierToday,
            endTime: earlierToday.addingTimeInterval(1800), // 30 min
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

    func testPrivateDetourAllocation() {
        let vehicle = Vehicle(name: "TestCar", licensePlate: "AB-123-C", make: "BMW", model: "i4", odometerStart: 0, odometerCurrent: 500)
        let now = Date()

        // Zakelijke rit van 50 km waarvan 5 km privé-omrijden
        let tripBusinessWithDetour = Trip(
            vehicle: vehicle,
            startTime: now,
            endTime: now.addingTimeInterval(3600),
            distanceMeters: 50000,
            tripType: "BUSINESS",
            status: "DONE",
            odometerStart: 0,
            privateDetourMeters: 5000
        )

        let stats = DashboardStatsCalculator.calculateDashboardStats(
            tripList: [tripBusinessWithDetour],
            timeFilter: .week,
            selectedVehicleId: vehicle.id,
            vehicleList: [vehicle],
            rules: [],
            addresses: []
        )

        XCTAssertEqual(stats.zakelijkKm, 45.0, "Business distance should deduct private detour")
        XCTAssertEqual(stats.priveKm, 5.0, "Private distance should receive private detour")
        XCTAssertEqual(stats.ytdPriveKm, 5.0, "YTD private distance should include private detour")
    }

    func testFiscalYearProjection() {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        // Mid-year test date (day ~182.5)
        let midYear = formatter.date(from: "2024-07-01")!
        let projection = DashboardStatsCalculator.calculateFiscalYearProjection(ytdPriveKm: 250.0, now: midYear)
        // 2024 is leap year (366 days), July 1 is day 183
        XCTAssertGreaterThan(projection, 490.0)
        XCTAssertLessThan(projection, 510.0)
    }

    func testCalculateProRataLimit() {
        let fullYearLimit = DashboardStatsCalculator.calculateProRataLimit(yearlyLimit: 500, inServiceDate: nil, endServiceDate: nil)
        XCTAssertEqual(fullYearLimit, 500)

        let cal = Calendar.current
        let currentYear = cal.component(.year, from: Date())
        var components = DateComponents()
        components.year = currentYear
        components.month = 7
        components.day = 1
        let midYear = cal.date(from: components)!

        let halfYearLimit = DashboardStatsCalculator.calculateProRataLimit(yearlyLimit: 500, inServiceDate: midYear, endServiceDate: nil)
        XCTAssertEqual(halfYearLimit, 250)
    }
}
