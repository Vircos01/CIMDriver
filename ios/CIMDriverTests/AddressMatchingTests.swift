import XCTest
@testable import CIMDriver
import SwiftData

final class AddressMatchingTests: XCTestCase {

    func testNormalization() {
        // Test basic lowercase and diacritics
        XCTAssertEqual(AddressMatching.normalize("Kerksträät"), "kerkstraat")
        XCTAssertEqual(AddressMatching.normalize("  Teststraat 1  "), "teststraat1")
        XCTAssertEqual(AddressMatching.normalize("A & B"), "aenb")
        XCTAssertEqual(AddressMatching.normalize("Station-Plein"), "stationplein")
    }
    
    func testBaseNormalization() {
        // Test ignoring house letters
        XCTAssertEqual(AddressMatching.normalizeBase("Kerkstraat 1a"), "kerkstraat1")
        XCTAssertEqual(AddressMatching.normalizeBase("Kerkstraat 1b"), "kerkstraat1")
        XCTAssertEqual(AddressMatching.normalizeBase("Kerkstraat 123c"), "kerkstraat123")
        XCTAssertEqual(AddressMatching.normalizeBase("Kerkstraat 1"), "kerkstraat1")
        
        // This shouldn't strip anything if it doesn't match the pattern
        XCTAssertEqual(AddressMatching.normalizeBase("Kerkstraat"), "kerkstraat")
    }

    func testStringMatching() {
        XCTAssertTrue(AddressMatching.matches("Kerkstraat 1", candidate: "Kerkstraat 1"))
        XCTAssertTrue(AddressMatching.matches("Kerkstraat 1 ", candidate: "kerkstraat 1"))
        XCTAssertFalse(AddressMatching.matches("Kerkstraat 1a", candidate: "Kerkstraat 1b")) // strict match fails
        
        XCTAssertTrue(AddressMatching.matchesBase("Kerkstraat 1a", candidate: "Kerkstraat 1b")) // base match passes
    }
    
    func testProximityMatching() {
        let savedAddress = SavedAddress(
            label: "Thuis",
            address: "Kerkstraat 1",
            latitude: 52.370216,
            longitude: 4.895168
        )
        
        // Exact same location
        let match1 = AddressMatching.findBestMatch(
            lat: 52.370216,
            lon: 4.895168,
            input: nil,
            addresses: [savedAddress],
            maxDistanceMeters: 200.0
        )
        XCTAssertNotNil(match1)
        XCTAssertEqual(match1?.reason, .coordinate)
        
        // A little bit further, but within 200m
        let match2 = AddressMatching.findBestMatch(
            lat: 52.3705, // slightly off
            lon: 4.8955,
            input: nil,
            addresses: [savedAddress],
            maxDistanceMeters: 200.0
        )
        XCTAssertNotNil(match2)
        XCTAssertEqual(match2?.reason, .coordinate)
        
        // Too far (> 200m)
        let match3 = AddressMatching.findBestMatch(
            lat: 52.3800, // much further
            lon: 4.9000,
            input: nil,
            addresses: [savedAddress],
            maxDistanceMeters: 200.0
        )
        XCTAssertNil(match3)
    }
}
