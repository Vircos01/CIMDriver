import Foundation

struct TargetProgressSnapshot {
    let accumulatedHours: Double
    let accumulatedRevenue: Double
    let accumulatedValue: Double
    let effectiveTarget: Double
    let percentage: Double
    let isBehind: Bool
    let proRataFactor: Double
}

enum TargetCalculator {
    static func proRataFactor(startDate: Date?, targetYear: Int, calendar: Calendar = .current) -> Double {
        guard let startDate else { return 1 }

        let startYear = calendar.component(.year, from: startDate)
        guard startYear == targetYear else { return startYear < targetYear ? 1 : 0 }

        let startMonth = calendar.component(.month, from: startDate)
        return Double(13 - startMonth) / 12
    }

    static func effectiveAnnualTarget(_ annualTarget: Double, startDate: Date?, targetYear: Int, calendar: Calendar = .current) -> Double {
        max(0, annualTarget) * proRataFactor(startDate: startDate, targetYear: targetYear, calendar: calendar)
    }

    static func progress(
        for target: HoursTarget,
        workDays: [WorkDay],
        projectCodes: [ProjectCode],
        employmentStartDate: Date?,
        now: Date = Date(),
        calendar: Calendar = .current
    ) -> TargetProgressSnapshot {
        var accumulatedHours = 0.0
        var accumulatedRevenue = 0.0

        for workDay in workDays where ["APPROVED", "DONE"].contains(workDay.status) {
            guard calendar.component(.year, from: workDay.date) == target.year else { continue }
            let project = projectCodes.first { $0.code == workDay.projectCode }
            if let projectCodeId = target.projectCodeId, project?.id != projectCodeId { continue }
            if let clientId = target.clientId, project?.clientId != clientId { continue }

            let start = workDay.roundedArrivalTime ?? workDay.arrivalTime
            let end = workDay.roundedDepartureTime ?? workDay.departureTime ?? workDay.lastArrivalTime ?? start
            let workedHours = max(0, end.timeIntervalSince(start) / 3600 - Double(workDay.breakMinutes) / 60)
            accumulatedHours += workedHours
            accumulatedRevenue += workedHours * max(0, project?.hourlyRate ?? 0)
        }

        let factor = proRataFactor(startDate: employmentStartDate, targetYear: target.year, calendar: calendar)
        let annualValue = target.targetType == "REVENUE" ? target.targetRevenue : target.targetHours
        let effectiveTarget = max(0, annualValue) * factor
        let accumulatedValue = target.targetType == "REVENUE" ? accumulatedRevenue : accumulatedHours
        let percentage = effectiveTarget > 0 ? accumulatedValue / effectiveTarget : 0
        let elapsedRatio = elapsedRatio(for: target.year, startDate: employmentStartDate, now: now, calendar: calendar)
        let isBehind = elapsedRatio > 0 && elapsedRatio < 1 && accumulatedValue < effectiveTarget * elapsedRatio

        return TargetProgressSnapshot(
            accumulatedHours: accumulatedHours,
            accumulatedRevenue: accumulatedRevenue,
            accumulatedValue: accumulatedValue,
            effectiveTarget: effectiveTarget,
            percentage: percentage,
            isBehind: isBehind,
            proRataFactor: factor
        )
    }

    private static func elapsedRatio(for year: Int, startDate: Date?, now: Date, calendar: Calendar) -> Double {
        let currentYear = calendar.component(.year, from: now)
        guard year == currentYear else { return year < currentYear ? 1 : 0 }

        let firstMonth: Int
        if let startDate, calendar.component(.year, from: startDate) == year {
            firstMonth = calendar.component(.month, from: startDate)
        } else {
            firstMonth = 1
        }
        let currentMonth = calendar.component(.month, from: now)
        let elapsedMonths = min(max(currentMonth - firstMonth + 1, 0), 12 - firstMonth + 1)
        let totalMonths = 12 - firstMonth + 1
        return totalMonths > 0 ? Double(elapsedMonths) / Double(totalMonths) : 0
    }
}