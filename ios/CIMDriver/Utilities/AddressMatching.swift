import Foundation
import CoreLocation

enum AddressMatchReason {
    case exactText
    case coordinate
}

struct AddressMatch {
    let address: SavedAddress
    let distanceMeters: Float
    let reason: AddressMatchReason
}

enum AddressMatching {
    static func normalize(_ value: String?) -> String {
        guard let value = value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }
        
        let withoutAccents = value.folding(options: .diacriticInsensitive, locale: .current)
        
        return withoutAccents
            .lowercased()
            .replacingOccurrences(of: "&", with: " en ")
            .replacingOccurrences(of: "[^a-z0-9]", with: "", options: .regularExpression)
    }

    static func matches(_ input: String?, candidate: String?) -> Bool {
        let normalizedInput = normalize(input)
        return !normalizedInput.isEmpty && normalizedInput == normalize(candidate)
    }

    static func normalizeBase(_ value: String?) -> String {
        let normalized = normalize(value)
        if let regex = try? NSRegularExpression(pattern: "^([a-z]+\\d+)[a-z]*$"),
           let match = regex.firstMatch(in: normalized, range: NSRange(normalized.startIndex..., in: normalized)) {
            if let range = Range(match.range(at: 1), in: normalized) {
                return String(normalized[range])
            }
        }
        return normalized
    }

    static func matchesBase(_ input: String?, candidate: String?) -> Bool {
        let normalizedInput = normalizeBase(input)
        return !normalizedInput.isEmpty && normalizedInput == normalizeBase(candidate)
    }

    static func findSavedAddress(_ input: String?, addresses: [SavedAddress]) -> SavedAddress? {
        return addresses.first { address in
            matches(input, candidate: address.label) || matches(input, candidate: address.address) ||
            matchesBase(input, candidate: address.label) || matchesBase(input, candidate: address.address)
        }
    }

    static func findBestMatch(
        lat: Double?,
        lon: Double?,
        input: String?,
        addresses: [SavedAddress],
        maxDistanceMeters: Float = 200.0
    ) -> AddressMatch? {
        if let exactMatch = findSavedAddress(input, addresses: addresses) {
            return AddressMatch(address: exactMatch, distanceMeters: 0, reason: .exactText)
        }

        guard let lat = lat, let lon = lon else { return nil }
        let location = CLLocation(latitude: lat, longitude: lon)

        var bestMatch: SavedAddress? = nil
        var minDistance: Float = .greatestFiniteMagnitude

        for address in addresses {
            guard let addrLat = address.latitude, let addrLon = address.longitude else { continue }
            
            let addrLocation = CLLocation(latitude: addrLat, longitude: addrLon)
            let distance = Float(location.distance(from: addrLocation))
            
            if distance <= maxDistanceMeters && distance < minDistance {
                minDistance = distance
                bestMatch = address
            }
        }

        if let bestMatch = bestMatch {
            return AddressMatch(address: bestMatch, distanceMeters: minDistance, reason: .coordinate)
        }
        
        return nil
    }
}
