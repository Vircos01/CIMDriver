import XCTest
@testable import CIMDriver

final class WorkHoursNormalizerTests: XCTestCase {

    func testToleranceRounding() {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        
        // Let's say the work day starts at "08:00"
        let dayStart = formatter.date(from: "2023-10-16 00:00:00")!
        
        // 1. Exact match
        let actualExact = formatter.date(from: "2023-10-16 08:00:00")!
        let normalizedExact = WorkHoursNormalizer.normalize(
            actualTime: actualExact,
            dayStart: dayStart,
            configuredTime: "08:00",
            toleranceMinutes: 90
        )
        XCTAssertEqual(normalizedExact, actualExact, "Exact time should remain the same")
        
        // 2. Within tolerance (e.g., arrived at 07:53, configured is 08:00)
        let actualEarly = formatter.date(from: "2023-10-16 07:53:00")!
        let expectedRounded = formatter.date(from: "2023-10-16 08:00:00")!
        
        let normalizedEarly = WorkHoursNormalizer.normalize(
            actualTime: actualEarly,
            dayStart: dayStart,
            configuredTime: "08:00",
            toleranceMinutes: 90
        )
        XCTAssertEqual(normalizedEarly, expectedRounded, "Time within tolerance should be rounded to configured time")
        
        // 3. Outside tolerance (e.g., arrived at 06:15, configured is 08:00)
        let actualVeryEarly = formatter.date(from: "2023-10-16 06:15:00")!
        let normalizedVeryEarly = WorkHoursNormalizer.normalize(
            actualTime: actualVeryEarly,
            dayStart: dayStart,
            configuredTime: "08:00",
            toleranceMinutes: 90
        )
        XCTAssertEqual(normalizedVeryEarly, actualVeryEarly, "Time outside tolerance should NOT be rounded")
    }
}
