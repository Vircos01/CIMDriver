import XCTest
@testable import CIMDriver

final class TargetCalculatorTests: XCTestCase {
    private var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        return calendar
    }

    func testSeptemberEmploymentStartUsesFourTwelfthsOfAnnualTarget() {
        let startDate = calendar.date(from: DateComponents(year: 2026, month: 9, day: 1))!

        XCTAssertEqual(TargetCalculator.proRataFactor(startDate: startDate, targetYear: 2026, calendar: calendar), 4.0 / 12.0, accuracy: 0.000001)
        XCTAssertEqual(TargetCalculator.effectiveAnnualTarget(60_000, startDate: startDate, targetYear: 2026, calendar: calendar), 20_000, accuracy: 0.01)
    }

    func testMissingEmploymentStartUsesFullAnnualTarget() {
        XCTAssertEqual(TargetCalculator.proRataFactor(startDate: nil, targetYear: 2026, calendar: calendar), 1)
    }

    func testEmploymentStartingAfterTargetYearHasNoTarget() {
        let startDate = calendar.date(from: DateComponents(year: 2027, month: 1, day: 1))!

        XCTAssertEqual(TargetCalculator.proRataFactor(startDate: startDate, targetYear: 2026, calendar: calendar), 0)
    }

    func testEmploymentStartingBeforeTargetYearUsesFullAnnualTarget() {
        let startDate = calendar.date(from: DateComponents(year: 2025, month: 9, day: 1))!

        XCTAssertEqual(TargetCalculator.proRataFactor(startDate: startDate, targetYear: 2026, calendar: calendar), 1)
    }

    func testRevenueProgressUsesFixedProjectRateAndNoStartDateDefaultsToFullTarget() {
        let projectId = UUID()
        let project = ProjectCode(id: projectId, code: "P-100", hourlyRate: 100)
        let workDay = WorkDay(
            date: calendar.date(from: DateComponents(year: 2026, month: 7, day: 1))!,
            firstDepartureTime: calendar.date(from: DateComponents(year: 2026, month: 7, day: 1, hour: 8))!,
            arrivalTime: calendar.date(from: DateComponents(year: 2026, month: 7, day: 1, hour: 9))!,
            departureTime: calendar.date(from: DateComponents(year: 2026, month: 7, day: 1, hour: 18))!,
            breakMinutes: 60,
            projectCode: "P-100",
            status: "APPROVED"
        )
        let target = HoursTarget(name: "Jaaromzet", projectCodeId: projectId, targetType: "REVENUE", targetRevenue: 60_000, year: 2026)

        let result = TargetCalculator.progress(
            for: target,
            workDays: [workDay],
            projectCodes: [project],
            employmentStartDate: nil,
            now: calendar.date(from: DateComponents(year: 2026, month: 10, day: 9))!,
            calendar: calendar
        )

        XCTAssertEqual(result.accumulatedHours, 8, accuracy: 0.001)
        XCTAssertEqual(result.accumulatedRevenue, 800, accuracy: 0.001)
        XCTAssertEqual(result.effectiveTarget, 60_000, accuracy: 0.001)
        XCTAssertEqual(result.percentage, 800.0 / 60_000.0, accuracy: 0.000001)
    }
}