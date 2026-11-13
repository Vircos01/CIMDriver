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

        if let matchingRule = matchingRule(
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
        return matchingRule(
            tripType: tripType,
            startAddressType: startAddressType,
            endAddressType: endAddressType,
            startAddress: startAddress,
            endAddress: endAddress,
            rules: rules
        )?.tripType ?? tripType
    }

    static func matchingRule(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        startAddress: String?,
        endAddress: String?,
        rules: [ClassificationRule]
    ) -> ClassificationRule? {
        let matchedRules = rules.filter { rule in
            guard rule.isEnabled else { return false }
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
            if countA != countB { return countA < countB }
            return a.orderIndex > b.orderIndex
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

    struct ConfidenceAssessment {
        let category: TripCategory
        let confidenceScore: Int // 0 - 100
        let reasoning: String
        var isHighConfidence: Bool { confidenceScore >= 85 }
    }

    static func assessConfidence(
        tripType: String?,
        startAddressType: String? = nil,
        endAddressType: String? = nil,
        startAddress: String? = nil,
        endAddress: String? = nil,
        rules: [ClassificationRule] = [],
        timestamp: Date? = nil,
        appSettings: AppSettings? = nil,
        isBluetoothTriggered: Bool = false,
        defaultCategory: TripCategory = .privateTrip
    ) -> ConfidenceAssessment {
        let matchedRule = matchingRule(
            tripType: tripType,
            startAddressType: startAddressType,
            endAddressType: endAddressType,
            startAddress: startAddress,
            endAddress: endAddress,
            rules: rules
        )
        let isCommutePair = (startAddressType == "THUIS" && endAddressType == "WERK") ||
            (startAddressType == "WERK" && endAddressType == "THUIS")
        let isCustomer = startAddressType == "KLANT" || endAddressType == "KLANT"
        let outsideHours = timestamp != nil ? isOutsideWorkHours(timestamp: timestamp!, appSettings: appSettings) : false

        var score = 50
        let category: TripCategory
        let reasoning: String

        if let rule = matchedRule {
            switch rule.category {
            case "BUSINESS": category = .business
            case "COMMUTE": category = .commute
            case "PRIVATE": category = .privateTrip
            default: category = defaultCategory
            }
            let isExact = !(rule.startAddress?.isEmpty ?? true) && !(rule.endAddress?.isEmpty ?? true)
            score = isExact ? 96 : 88
            reasoning = isExact ? "Vaste route-regel matched exact" : "Classificatieregel matched adrestypes"
        } else if isCommutePair || tripType == "COMMUTE" || tripType == dynamicsHomeToWork {
            category = .commute
            score = isBluetoothTriggered ? 92 : 88
            reasoning = "Herkend als reguliere woon-werk route"
        } else if isCustomer {
            category = .business
            score = 85
            reasoning = "Adres is gemarkeerd als klantlocatie"
        } else if outsideHours {
            category = .privateTrip
            score = 80
            reasoning = "Rit viel buiten geconfigureerde werktijden"
        } else if let type = tripType, businessTripTypes.contains(type) {
            category = .business
            score = 75
            reasoning = "Rit bevat zakelijk rittype"
        } else {
            category = defaultCategory
            score = 55
            reasoning = "Standaard categorie toegepast bij gebrek aan specifieke regels"
        }

        return ConfidenceAssessment(
            category: category,
            confidenceScore: min(100, max(0, score)),
            reasoning: reasoning
        )
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
