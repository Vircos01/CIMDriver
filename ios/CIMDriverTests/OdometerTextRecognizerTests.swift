import XCTest
@testable import CIMDriver

final class OdometerTextRecognizerTests: XCTestCase {
    func testExtractsReadingWithThousandsSeparator() {
        XCTAssertEqual(OdometerTextRecognizer.reading(in: "ODO 123.456 km"), "123456")
    }

    func testIgnoresNumbersThatAreTooShortOrTooLong() {
        XCTAssertNil(OdometerTextRecognizer.reading(in: "Trip 12 km"))
        XCTAssertNil(OdometerTextRecognizer.reading(in: "VIN 1234567890123"))
    }

    func testSelectsLongestPlausibleReading() {
        XCTAssertEqual(OdometerTextRecognizer.reading(in: "Trip 845 km ODO 123456"), "123456")
    }
}