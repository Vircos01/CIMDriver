import XCTest
@testable import CIMDriver

final class ExportUtilTests: XCTestCase {

    func testGenerateTripsCSVWithPeriodTitle() {
        let trip = Trip(
            startTime: Date(),
            distanceMeters: 15000,
            odometerStart: 1000,
            odometerEnd: 1015
        )
        trip.startAddress = "Kerkstraat 1"
        trip.endAddress = "Markt 5"
        trip.tripType = "BUSINESS"
        trip.status = "APPROVED"

        let url = ExportUtil.generateTripsCSV(trips: [trip], periodTitle: "Oktober 2026")
        XCTAssertNotNil(url)
        XCTAssertTrue(url!.lastPathComponent.contains("Oktober_2026"))
        
        if let url = url, let content = try? String(contentsOf: url, encoding: .utf8) {
            XCTAssertTrue(content.contains("Kerkstraat 1"))
            XCTAssertTrue(content.contains("15.0"))
            XCTAssertTrue(content.contains("BUSINESS"))
        } else {
            XCTFail("Could not read exported CSV")
        }
    }

    func testGenerateTripsPDFWithPeriodTitle() {
        let trip = Trip(
            startTime: Date(),
            distanceMeters: 22000,
            odometerStart: 5000,
            odometerEnd: 5022
        )
        trip.startAddress = "Startweg 10"
        trip.endAddress = "Eindweg 20"
        trip.tripType = "COMMUTE"

        let url = ExportUtil.generateTripsPDF(trips: [trip], periodTitle: "September 2026")
        XCTAssertNotNil(url)
        XCTAssertTrue(url!.lastPathComponent.contains("September_2026"))
        XCTAssertTrue(url!.pathExtension.lowercased() == "pdf")
    }
}

