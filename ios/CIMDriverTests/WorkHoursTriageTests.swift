import XCTest
@testable import CIMDriver

final class WorkHoursTriageTests: XCTestCase {

    func testCompleteWorkDay_hasHighConfidence() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!

        var components = DateComponents()
        components.year = 2026
        components.month = 5
        components.day = 11 // Monday
        components.hour = 0
        let date = calendar.date(from: components)!

        let arrival = date.addingTimeInterval(8.5 * 3600) // 08:30
        let departure = date.addingTimeInterval(17 * 3600) // 17:00

        let workDay = WorkDay(
            date: date,
            firstDepartureTime: arrival,
            arrivalTime: arrival,
            departureTime: departure,
            lastArrivalTime: departure,
            breakMinutes: 30,
            status: "DONE"
        )

        let suggestion = WorkHoursTriage.evaluateWorkDay(
            workDay: workDay,
            now: date.addingTimeInterval(2 * 86400),
            calendar: calendar
        )

        XCTAssertTrue(suggestion.isHighConfidence)
        XCTAssertFalse(suggestion.requiresAttention)
        XCTAssertTrue(suggestion.statusText.contains("Vrijwel zeker compleet"))
        XCTAssertEqual(suggestion.suggestedBreakMinutes, 30)
    }

    func testMissingDeparture_suggestsFromReturnTrip() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!

        var components = DateComponents()
        components.year = 2026
        components.month = 5
        components.day = 11 // Monday
        let date = calendar.date(from: components)!

        let arrival = date.addingTimeInterval(8.5 * 3600) // 08:30

        let workDay = WorkDay(
            date: date,
            firstDepartureTime: arrival,
            arrivalTime: arrival,
            departureTime: nil,
            lastArrivalTime: nil,
            breakMinutes: 30,
            status: "IN_PROGRESS"
        )

        let returnTrip = Trip(
            startTime: date.addingTimeInterval(17.25 * 3600), // 17:15
            endTime: date.addingTimeInterval(17.75 * 3600),
            startAddress: "Kantoor, Utrecht",
            endAddress: "Thuis, Amsterdam",
            distanceMeters: 35000,
            tripType: "COMMUTE",
            odometerStart: 10000
        )

        let suggestion = WorkHoursTriage.evaluateWorkDay(
            workDay: workDay,
            tripsOnDay: [returnTrip],
            now: date.addingTimeInterval(2 * 86400),
            calendar: calendar
        )

        XCTAssertTrue(suggestion.requiresAttention)
        XCTAssertEqual(suggestion.suggestedDepartureTime, returnTrip.startTime)
        XCTAssertTrue(suggestion.statusText.contains("Eindtijd ontbreekt"))
    }

    func testLongDayWithoutBreak_suggestsArboBreak() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!

        var components = DateComponents()
        components.year = 2026
        components.month = 5
        components.day = 11
        let date = calendar.date(from: components)!

        let arrival = date.addingTimeInterval(8 * 3600) // 08:00
        let departure = date.addingTimeInterval(17 * 3600) // 17:00 (9 hours)

        let workDay = WorkDay(
            date: date,
            firstDepartureTime: arrival,
            arrivalTime: arrival,
            departureTime: departure,
            lastArrivalTime: departure,
            breakMinutes: 0,
            status: "TO_REVIEW"
        )

        let suggestion = WorkHoursTriage.evaluateWorkDay(
            workDay: workDay,
            now: date.addingTimeInterval(86400),
            calendar: calendar
        )

        XCTAssertTrue(suggestion.requiresAttention)
        XCTAssertEqual(suggestion.suggestedBreakMinutes, 45) // Arbowet > 8h requires 45 min
        XCTAssertTrue(suggestion.statusText.contains("zonder pauze"))
    }
}
