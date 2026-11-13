import XCTest
@testable import CIMDriver

final class RdwVehicleLookupTests: XCTestCase {

    func testDetermineEngineType() {
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["Elektriciteit"]), "EV")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["elektriciteit"]), "EV")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["Benzine", "Elektriciteit"]), "PHEV")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["Hybride"]), "PHEV")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["Benzine"]), "ICE")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["Diesel"]), "ICE")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: ["LPG"]), "ICE")
        XCTAssertEqual(RdwVehicleLookup.determineEngineType(from: []), "ICE")
    }
}
