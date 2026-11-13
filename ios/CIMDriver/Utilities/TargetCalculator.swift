import Foundation

struct TargetProgressSnapshot {
    let accumulatedHours: Double
    let accumulatedRevenue: Double
    let accumulatedValue: Double
    let effectiveTarget: Double
    let percentage: Double
    let isBehind: Bool
    let proRataFactor: Double
    let remainingWorkDays: Int
    let requiredPacePerDay: Double
    let guidanceMessage: String

    init(
        accumulatedHours: Double,
        accumulatedRevenue: Double,
        accumulatedValue: Double,
        effectiveTarget: Double,
        percentage: Double,
        isBehind: Bool,
        proRataFactor: Double,
        remainingWorkDays: Int = 0,
        requiredPacePerDay: Double = 0.0,
        guidanceMessage: String = ""
    ) {
        self.accumulatedHours = accumulatedHours
        self.accumulatedRevenue = accumulatedRevenue
        self.accumulatedValue = accumulatedValue
        self.effectiveTarget = effectiveTarget
        self.percentage = percentage
        self.isBehind = isBehind
        self.proRataFactor = proRataFactor
        self.remainingWorkDays = remainingWorkDays
        self.requiredPacePerDay = requiredPacePerDay
        self.guidanceMessage = guidanceMessage
    }
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

        let currentYear = calendar.component(.year, from: now)
        var remainingWorkDays = 0
        if target.year == currentYear {
            var tempDate = calendar.date(byAdding: .day, value: 1, to: now) ?? now
            while calendar.component(.year, from: tempDate) == target.year {
                let weekday = calendar.component(.weekday, from: tempDate)
                if weekday >= 2 && weekday <= 6 { // Mon - Fri
                    remainingWorkDays += 1
                }
                guard let next = calendar.date(byAdding: .day, value: 1, to: tempDate) else { break }
                tempDate = next
            }
        } else if target.year > currentYear {
            var comp = DateComponents()
            comp.year = target.year
            comp.month = 1
            comp.day = 1
            if var tempDate = calendar.date(from: comp) {
                while calendar.component(.year, from: tempDate) == target.year {
                    let weekday = calendar.component(.weekday, from: tempDate)
                    if weekday >= 2 && weekday <= 6 {
                        remainingWorkDays += 1
                    }
                    guard let next = calendar.date(byAdding: .day, value: 1, to: tempDate) else { break }
                    tempDate = next
                }
            }
        }

        let isRevenue = target.targetType == "REVENUE"
        let remainingValue = max(0, effectiveTarget - accumulatedValue)
        let requiredPacePerDay = remainingWorkDays > 0 ? remainingValue / Double(remainingWorkDays) : 0.0

        let guidanceMessage: String
        if effectiveTarget <= 0 {
            guidanceMessage = ""
        } else if accumulatedValue >= effectiveTarget {
            let surplus = accumulatedValue - effectiveTarget
            if isRevenue {
                guidanceMessage = String(format: "Doel behaald! (+€ %.0f)", surplus)
            } else {
                guidanceMessage = String(format: "Doel behaald! (+%.1f uur)", surplus)
            }
        } else if target.year < currentYear {
            if isRevenue {
                guidanceMessage = String(format: "Jaar afgesloten: € %.0f tekort", remainingValue)
            } else {
                guidanceMessage = String(format: "Jaar afgesloten: %.1f uur tekort", remainingValue)
            }
        } else if remainingWorkDays > 0 {
            if isRevenue {
                guidanceMessage = String(format: "Nog € %.0f nodig over %d werkdagen (€ %.0f/dag)", remainingValue, remainingWorkDays, requiredPacePerDay)
            } else {
                guidanceMessage = String(format: "Nog %.1f uur nodig over %d werkdagen (tempo: %.1f u/dag)", remainingValue, remainingWorkDays, requiredPacePerDay)
            }
        } else {
            if isRevenue {
                guidanceMessage = String(format: "Nog € %.0f nodig", remainingValue)
            } else {
                guidanceMessage = String(format: "Nog %.1f uur nodig", remainingValue)
            }
        }

        return TargetProgressSnapshot(
            accumulatedHours: accumulatedHours,
            accumulatedRevenue: accumulatedRevenue,
            accumulatedValue: accumulatedValue,
            effectiveTarget: effectiveTarget,
            percentage: percentage,
            isBehind: isBehind,
            proRataFactor: factor,
            remainingWorkDays: remainingWorkDays,
            requiredPacePerDay: requiredPacePerDay,
            guidanceMessage: guidanceMessage
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