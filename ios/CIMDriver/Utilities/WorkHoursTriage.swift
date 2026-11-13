import Foundation
import SwiftData

struct WorkDaySuggestion: Equatable {
    let suggestedDepartureTime: Date?
    let suggestedBreakMinutes: Int
    let suggestedProjectCode: String?
    let confidenceScore: Int
    let statusText: String
    let isHighConfidence: Bool
    let requiresAttention: Bool
}

struct WorkDayReviewCandidate: Identifiable, Equatable {
    var id: PersistentIdentifier { workDay.persistentModelID }
    let workDay: WorkDay
    let suggestion: WorkDaySuggestion
}

enum WorkHoursTriage {
    static func evaluateWorkDay(
        workDay: WorkDay,
        tripsOnDay: [Trip] = [],
        settings: AppSettings? = nil,
        savedAddresses: [SavedAddress] = [],
        now: Date = Date(),
        calendar: Calendar = .current
    ) -> WorkDaySuggestion {
        let isToday = calendar.isDate(workDay.date, inSameDayAs: now)
        let actualDeparture = workDay.roundedDepartureTime ?? workDay.departureTime ?? workDay.lastArrivalTime
        let departureMissing = actualDeparture == nil

        // Roster / Basisrooster resolution
        let calDayOfWeek = calendar.component(.weekday, from: workDay.date)
        let ourDayOfWeek = calDayOfWeek == 1 ? 7 : calDayOfWeek - 1 // 1=Monday...7=Sunday
        var scheduledEndTime = "17:00"
        if let end = settings?.workEndTime {
            let fmt = DateFormatter()
            fmt.dateFormat = "HH:mm"
            scheduledEndTime = fmt.string(from: end)
        }
        var isOffDay = false
        if let settings {
            let daysMap = settings.workDays.split(separator: ";").compactMap { String($0) }
            var foundDay = false
            for dayItem in daysMap {
                let parts = dayItem.split(separator: "=")
                if parts.count == 2, let dayNum = Int(parts[0]), dayNum == ourDayOfWeek {
                    foundDay = true
                    let times = parts[1].split(separator: "-")
                    if times.count == 2 {
                        scheduledEndTime = String(times[1])
                    }
                    break
                }
            }
            if !foundDay && !settings.workDays.isEmpty {
                isOffDay = true
            }
        } else {
            isOffDay = (ourDayOfWeek == 6 || ourDayOfWeek == 7)
        }

        let suggestedDeparture: Date
        if let actualDeparture {
            suggestedDeparture = actualDeparture
        } else {
            // Find return trip home on that day
            let returnTrip = tripsOnDay.last { trip in
                let isCommute = trip.tripType == "COMMUTE" || trip.tripType == "Home To Work"
                let endsHome = trip.endAddress?.localizedCaseInsensitiveContains("thuis") == true ||
                    savedAddresses.contains { $0.isHomeLocation && $0.address.localizedCaseInsensitiveCompare(trip.endAddress ?? "") == .orderedSame }
                return (isCommute || endsHome) && trip.startTime > workDay.arrivalTime
            }

            if let returnTrip {
                suggestedDeparture = returnTrip.startTime
            } else {
                suggestedDeparture = WorkHoursNormalizer.normalize(
                    actualTime: workDay.arrivalTime.addingTimeInterval(8 * 3600),
                    dayStart: calendar.startOfDay(for: workDay.date),
                    configuredTime: scheduledEndTime
                )
            }
        }

        // Break time suggestion
        let start = workDay.roundedArrivalTime ?? workDay.arrivalTime
        let end = workDay.roundedDepartureTime ?? suggestedDeparture
        let grossDuration = max(0, end.timeIntervalSince(start))
        let grossHours = grossDuration / 3600.0

        let configuredBreak = settings?.breakMinutes ?? 30
        let standardBreak: Int
        if grossHours >= 8.0 {
            standardBreak = max(45, configuredBreak)
        } else if grossHours >= 4.5 {
            standardBreak = configuredBreak
        } else {
            standardBreak = 0
        }

        let suggestedBreak = workDay.breakMinutes > 0 ? workDay.breakMinutes : standardBreak
        let zeroBreakOnLongDay = grossHours >= 7.5 && workDay.breakMinutes == 0

        // Project code suggestion
        let existingProject = workDay.projectCode?.isEmpty == false ? workDay.projectCode : nil
        let tripWithProject = tripsOnDay.first { $0.projectCode?.isEmpty == false }
        let customerAddress = tripsOnDay.compactMap { trip in
            savedAddresses.first { addr in
                addr.isCustomerLocation && (
                    addr.address.localizedCaseInsensitiveCompare(trip.startAddress ?? "") == .orderedSame ||
                    addr.address.localizedCaseInsensitiveCompare(trip.endAddress ?? "") == .orderedSame
                )
            }
        }.first

        let suggestedProject = existingProject ?? tripWithProject?.projectCode ?? customerAddress?.projectCode

        var score = 95
        var statusText: String
        var requiresAttention = false

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"

        if departureMissing && isToday && now.timeIntervalSince(workDay.arrivalTime) < 9 * 3600 {
            score = 60
            statusText = "Werkdag momenteel actief (gestart \(timeFormatter.string(from: workDay.arrivalTime)))"
            requiresAttention = false
        } else if departureMissing {
            score = 55
            statusText = "Eindtijd ontbreekt (voorstel: \(timeFormatter.string(from: suggestedDeparture)))"
            requiresAttention = true
        } else if zeroBreakOnLongDay {
            score = 65
            statusText = String(format: "Lange werkdag (%.1fu) zonder pauze", grossHours)
            requiresAttention = true
        } else if grossHours < 4.0 && !departureMissing {
            score = 75
            statusText = String(format: "Korte werkdag (%.1f uur)", grossHours)
            requiresAttention = true
        } else if isOffDay {
            score = 70
            statusText = "Geregistreerd buiten reguliere werkdagen"
            requiresAttention = true
        } else {
            let netDuration = max(0, grossDuration - Double(suggestedBreak) * 60)
            let netHours = netDuration / 3600.0
            statusText = String(format: "Vrijwel zeker compleet (%.1f uur netto, %dm pauze)", netHours, suggestedBreak)
            requiresAttention = false
        }

        return WorkDaySuggestion(
            suggestedDepartureTime: suggestedDeparture,
            suggestedBreakMinutes: suggestedBreak,
            suggestedProjectCode: suggestedProject,
            confidenceScore: score,
            statusText: statusText,
            isHighConfidence: score >= 85,
            requiresAttention: requiresAttention || workDay.status == "TO_REVIEW"
        )
    }
}
