import XCTest
@testable import CIMDriver
import SwiftData

final class TripClassificationTests: XCTestCase {

    func testTripClassificationHierarchy() {
        // Test hierarchy: Exact address rule wins > Type rule wins > Built-in logic > Default
        
        // 1. Exact address rule
        let ruleExact = ClassificationRule(
            name: "Exact Address",
            startAddress: "Kerkstraat 1",
            endAddress: "Station",
            category: "PRIVATE"
        )
        
        // 2. Type rule
        let ruleType = ClassificationRule(
            name: "Type Rule",
            startAddressType: "KLANT",
            endAddressType: "WERK",
            category: "COMMUTE"
        )
        
        // Test Exact Address matching
        let categoryExact = TripClassification.classify(
            tripType: nil,
            startAddress: "Kerkstraat 1",
            endAddress: "Station",
            rules: [ruleExact, ruleType]
        )
        XCTAssertEqual(categoryExact, .privateTrip, "Exact address rule should win and classify as PRIVATE")
        
        // Test Type rule matching
        let categoryType = TripClassification.classify(
            tripType: nil,
            startAddressType: "KLANT",
            endAddressType: "WERK",
            rules: [ruleType]
        )
        XCTAssertEqual(categoryType, .commute, "Type rule should classify as COMMUTE")
    }

    func testBuiltInLogic() {
        // Built-in logic: Thuis <-> Werk = Commute
        let categoryCommute = TripClassification.classify(
            tripType: nil,
            startAddressType: "THUIS",
            endAddressType: "WERK",
            homeWorkAsCommute: true
        )
        XCTAssertEqual(categoryCommute, .commute, "Thuis <-> Werk should be COMMUTE")
        
        let categoryCommuteReverse = TripClassification.classify(
            tripType: nil,
            startAddressType: "WERK",
            endAddressType: "THUIS",
            homeWorkAsCommute: true
        )
        XCTAssertEqual(categoryCommuteReverse, .commute, "Werk <-> Thuis should be COMMUTE")
        
        // Built-in logic: Klant = Business
        let categoryBusiness = TripClassification.classify(
            tripType: nil,
            startAddressType: "KLANT",
            customerAsBusiness: true
        )
        XCTAssertEqual(categoryBusiness, .business, "Klant location should be BUSINESS")
    }
    
    func testDynamicsTripTypes() {
        let categoryDynamicsBusiness = TripClassification.classify(
            tripType: "Customer Visit"
        )
        XCTAssertEqual(categoryDynamicsBusiness, .business, "Customer Visit should be BUSINESS")
        
        let categoryDynamicsPersonal = TripClassification.classify(
            tripType: "PERSONAL"
        )
        XCTAssertEqual(categoryDynamicsPersonal, .privateTrip, "PERSONAL should be PRIVATE")
    }
    
    func testOutsideWorkHours() {
        let appSettings = AppSettings(
            workDays: "1=08:00-17:00;2=08:00-17:00;3=08:00-17:00;4=08:00-17:00;5=08:00-17:00"
        )
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        formatter.timeZone = TimeZone.current
        
        // Assuming today is a Monday (or similar workday), we need a fixed date.
        // 2023-10-16 is a Monday.
        let mondayWorkHour = formatter.date(from: "2023-10-16 10:00:00")! // In work hours
        let mondayNight = formatter.date(from: "2023-10-16 20:00:00")! // Outside work hours
        let sunday = formatter.date(from: "2023-10-15 12:00:00")! // Weekend
        
        // Test in work hours
        let catInHours = TripClassification.classify(
            tripType: nil,
            timestamp: mondayWorkHour,
            appSettings: appSettings
        )
        XCTAssertEqual(catInHours, .privateTrip, "Default should be PRIVATE when no other rules match, even during work hours")
        
        // Test outside work hours
        let catOutsideHours = TripClassification.classify(
            tripType: nil,
            defaultCategory: .business, // Override default to see if outside work hours forces PRIVATE
            timestamp: mondayNight,
            appSettings: appSettings
        )
        XCTAssertEqual(catOutsideHours, .privateTrip, "Outside work hours should force PRIVATE")
        
        // Test weekend
        let catWeekend = TripClassification.classify(
            tripType: nil,
            defaultCategory: .business,
            timestamp: sunday,
            appSettings: appSettings
        )
        XCTAssertEqual(catWeekend, .privateTrip, "Weekend should force PRIVATE")
    }
}
