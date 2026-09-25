import Foundation

enum TripCategory: String, Codable {
    case business = "BUSINESS"
    case privateTrip = "PRIVATE"
    case commute = "COMMUTE"
}

enum TripClassification {
    static let dynamicsHomeToWork = "Home To Work"
    static let dynamicsBusinessMeeting = "Business Meeting"
    static let dynamicsPersonal = "PERSONAL"

    private static let businessTripTypes: Set<String> = [
        "BUSINESS",
        "Business Meeting",
        "Customer Visit",
        "Customer Billable",
        "Commissioned By CIMSOLUTIONS",
        "Exam Course",
        "Car Maintenance"
    ]

    static func classify(
        tripType: String?,
        startAddressType: String? = nil,
        endAddressType: String? = nil,
        startAddress: String? = nil,
        endAddress: String? = nil,
        defaultCategory: TripCategory = .privateTrip,
        homeWorkAsCommute: Bool = true,
        customerAsBusiness: Bool = true,
        rules: [ClassificationRule] = [],
        timestamp: Date? = nil,
        appSettings: AppSettings? = nil
    ) -> TripCategory {
        let isCommuteAddressPair = (startAddressType == "THUIS" && endAddressType == "WERK") ||
            (startAddressType == "WERK" && endAddressType == "THUIS")
            
        // Outside work hours check first (unless there's an explicit rule)
        let outsideHours = timestamp != nil ? isOutsideWorkHours(timestamp: timestamp!, appSettings: appSettings) : false

        if let matchingRule = findMatchingRule(
            tripType: tripType,
            startAddressType: startAddressType,
            endAddressType: endAddressType,
            startAddress: startAddress,
            endAddress: endAddress,
            rules: rules
        ) {
            switch matchingRule.category {
            case "BUSINESS": return .business
            case "COMMUTE": return .commute
            case "PRIVATE": return .privateTrip
            default: return defaultCategory
            }
        }

        if homeWorkAsCommute && (isCommuteAddressPair || tripType == "COMMUTE" || tripType == dynamicsHomeToWork) {
            return .commute
        }
        if customerAsBusiness && (startAddressType == "KLANT" || endAddressType == "KLANT") {
            return .business
        }
        if let type = tripType, businessTripTypes.contains(type) {
            return .business
        }
        if tripType == "PRIVATE" || tripType == dynamicsPersonal {
            return .privateTrip
        }
        
        if outsideHours {
            return .privateTrip
        }
        
        return defaultCategory
    }

    static func resolveTripType(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        startAddress: String?,
        endAddress: String?,
        rules: [ClassificationRule]
    ) -> String? {
        return findMatchingRule(
            tripType: tripType,
            startAddressType: startAddressType,
            endAddressType: endAddressType,
            startAddress: startAddress,
            endAddress: endAddress,
            rules: rules
        )?.tripType ?? tripType
    }

    private static func findMatchingRule(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        startAddress: String?,
        endAddress: String?,
        rules: [ClassificationRule]
    ) -> ClassificationRule? {
        let matchedRules = rules.filter { rule in
            let tripMatches = (rule.tripType?.isEmpty ?? true) || rule.tripType?.caseInsensitiveCompare(tripType ?? "") == .orderedSame
            
            let typeDirectionForward = ((rule.startAddressType?.isEmpty ?? true) || rule.startAddressType?.caseInsensitiveCompare(startAddressType ?? "") == .orderedSame) &&
                ((rule.endAddressType?.isEmpty ?? true) || rule.endAddressType?.caseInsensitiveCompare(endAddressType ?? "") == .orderedSame)
                
            let typeDirectionReverse = ((rule.startAddressType?.isEmpty ?? true) || rule.startAddressType?.caseInsensitiveCompare(endAddressType ?? "") == .orderedSame) &&
                ((rule.endAddressType?.isEmpty ?? true) || rule.endAddressType?.caseInsensitiveCompare(startAddressType ?? "") == .orderedSame)

            let exactDirectionForward = (!(rule.startAddress?.isEmpty ?? true) && rule.startAddress?.caseInsensitiveCompare(startAddress ?? "") == .orderedSame) &&
                (!(rule.endAddress?.isEmpty ?? true) && rule.endAddress?.caseInsensitiveCompare(endAddress ?? "") == .orderedSame)
                
            let exactDirectionReverse = (!(rule.startAddress?.isEmpty ?? true) && rule.startAddress?.caseInsensitiveCompare(endAddress ?? "") == .orderedSame) &&
                (!(rule.endAddress?.isEmpty ?? true) && rule.endAddress?.caseInsensitiveCompare(startAddress ?? "") == .orderedSame)
                
            let matchesTypes = typeDirectionForward || typeDirectionReverse
            let matchesExact = exactDirectionForward || exactDirectionReverse
            
            let usesExactAddresses = !(rule.startAddress?.isEmpty ?? true) && !(rule.endAddress?.isEmpty ?? true)
            
            return tripMatches && (usesExactAddresses ? matchesExact : matchesTypes)
        }
        
        return matchedRules.max { a, b in
            let countA = [a.startAddressType, a.endAddressType, a.tripType, a.startAddress, a.endAddress].compactMap { $0 }.filter { !$0.isEmpty }.count
            let countB = [b.startAddressType, b.endAddressType, b.tripType, b.startAddress, b.endAddress].compactMap { $0 }.filter { !$0.isEmpty }.count
            return countA < countB
        }
    }

    static func isOutsideWorkHours(timestamp: Date, appSettings: AppSettings?) -> Bool {
        guard let settings = appSettings else { return false }
        
        let calendar = Calendar.current
        let calDayOfWeek = calendar.component(.weekday, from: timestamp)
        let ourDayOfWeek = calDayOfWeek == 1 ? 7 : calDayOfWeek - 1
        
        let daysStr = settings.workDays
        let daysMap = daysStr.split(separator: ";").compactMap { String($0) }
        
        var isWorkDay = false
        var startMins = 8 * 60
        var endMins = 17 * 60
        
        for dayItem in daysMap {
            let parts = dayItem.split(separator: "=")
            if parts.count == 2, let dayNum = Int(parts[0]), dayNum == ourDayOfWeek {
                isWorkDay = true
                let times = parts[1].split(separator: "-")
                if times.count == 2 {
                    let startParts = times[0].split(separator: ":")
                    let endParts = times[1].split(separator: ":")
                    if startParts.count == 2, let h1 = Int(startParts[0]), let m1 = Int(startParts[1]) {
                        startMins = h1 * 60 + m1
                    }
                    if endParts.count == 2, let h2 = Int(endParts[0]), let m2 = Int(endParts[1]) {
                        endMins = h2 * 60 + m2
                    }
                }
                break
            }
        }
        
        if !isWorkDay {
            return true
        }
        
        let currentHour = calendar.component(.hour, from: timestamp)
        let currentMin = calendar.component(.minute, from: timestamp)
        let currentMins = currentHour * 60 + currentMin
        
        if currentMins < startMins || currentMins > endMins {
            return true
        }
        
        return false
    }
}
import Foundation
import SwiftData

struct PatternDetector {
    /// Detects if the current trip type has been manually selected 3 or more times for this specific route.
    /// If so, returns a proposed `ClassificationRule`.
    @MainActor
    static func detectRuleSuggestion(
        for trip: Trip,
        context: ModelContext,
        allTrips: [Trip]
    ) -> ClassificationRule? {
        guard !(trip.startAddress ?? "").isEmpty, !(trip.endAddress ?? "").isEmpty else { return nil }
        
        let startMatch = AddressMatching.normalizeBase(trip.startAddress ?? "")
        let endMatch = AddressMatching.normalizeBase(trip.endAddress ?? "")
        
        let sameRouteTrips = allTrips.filter { t in
            t.vehicle?.id == trip.vehicle?.id &&
            AddressMatching.normalizeBase(t.startAddress ?? "") == startMatch &&
            AddressMatching.normalizeBase(t.endAddress ?? "") == endMatch &&
            t.tripType == trip.tripType
        }
        
        if sameRouteTrips.count >= 3 {
            // Suggest a rule
            let category: String
            switch TripClassification.classify(tripType: trip.tripType) {
            case .business: category = "BUSINESS"
            case .commute: category = "COMMUTE"
            case .privateTrip: category = "PRIVATE"
            }
            
            return ClassificationRule(
                name: "Auto-suggestie: \(startMatch) -> \(endMatch)",
                startAddress: trip.startAddress ?? "",
                endAddress: trip.endAddress ?? "",
                tripType: trip.tripType,
                category: category
            )
        }
        
        return nil
    }
}
