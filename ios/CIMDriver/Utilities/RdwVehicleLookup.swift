import Foundation

struct RdwVehicleData {
    let licensePlate: String
    let make: String
    let model: String
    let year: String?
    let engineType: String // "ICE", "EV", "PHEV"
    let vehicleType: String?
}

enum RdwVehicleLookup {
    private static let baseVehicleUrl = "https://opendata.rdw.nl/resource/m9d7-ebf2.json"
    private static let baseFuelUrl = "https://opendata.rdw.nl/resource/8ys7-d773.json"

    static func cleanPlate(_ plate: String) -> String {
        plate.replacingOccurrences(of: "-", with: "")
            .replacingOccurrences(of: " ", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
    }

    static func lookup(licensePlate: String) async throws -> RdwVehicleData {
        let clean = cleanPlate(licensePlate)
        guard clean.count >= 4 else {
            throw NSError(domain: "RdwLookup", code: 400, userInfo: [NSLocalizedDescriptionKey: "Kenteken is te kort"])
        }

        guard let vehicleUrl = URL(string: "\(baseVehicleUrl)?kenteken=\(clean)") else {
            throw NSError(domain: "RdwLookup", code: 400, userInfo: [NSLocalizedDescriptionKey: "Ongeldige URL"])
        }

        var request = URLRequest(url: vehicleUrl)
        request.timeoutInterval = 7.0

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "RdwLookup", code: 500, userInfo: [NSLocalizedDescriptionKey: "RDW API niet bereikbaar"])
        }

        guard let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]],
              let first = jsonArray.first else {
            throw NSError(domain: "RdwLookup", code: 404, userInfo: [NSLocalizedDescriptionKey: "Geen voertuig gevonden voor kenteken \(clean)"])
        }

        let rawMake = (first["merk"] as? String) ?? ""
        let rawModel = (first["handelsbenaming"] as? String) ?? ""
        let rawDate = (first["datum_eerste_toelating"] as? String) ?? ""
        let year = rawDate.count >= 4 ? String(rawDate.prefix(4)) : nil
        let vehicleType = first["voertuigsoort"] as? String

        let make = capitalizeWords(rawMake)
        let model = capitalizeWords(rawModel)

        let engineType = await lookupEngineType(cleanPlate: clean, rawModel: rawModel)

        return RdwVehicleData(
            licensePlate: clean,
            make: make,
            model: model,
            year: year,
            engineType: engineType,
            vehicleType: vehicleType
        )
    }

    static func determineEngineType(from fuelDescriptions: [String], rawModel: String = "") -> String {
        let lowerFuels = fuelDescriptions.map { $0.lowercased() }
        let hasElectric = lowerFuels.contains { $0.contains("elektr") }
        let hasHybrid = lowerFuels.contains { $0.contains("hybride") || $0.contains("hybrid") }
        let hasFossil = lowerFuels.contains {
            $0.contains("benzine") || $0.contains("diesel") || $0.contains("lpg") || $0.contains("cng") || $0.contains("alcohol")
        }

        if (hasElectric && hasFossil) || hasHybrid {
            return "PHEV"
        } else if hasElectric && !hasFossil {
            return "EV"
        } else if hasFossil {
            return "ICE"
        } else {
            let upper = rawModel.uppercased()
            if upper.contains("PHEV") || upper.contains("HYBRID") {
                return "PHEV"
            } else if upper.contains("EV") || upper.contains("ELECTRIC") || upper.hasPrefix("E-") {
                return "EV"
            } else {
                return "ICE"
            }
        }
    }

    private static func lookupEngineType(cleanPlate: String, rawModel: String) async -> String {
        guard let url = URL(string: "\(baseFuelUrl)?kenteken=\(cleanPlate)") else {
            return determineEngineType(from: [], rawModel: rawModel)
        }
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 5.0
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, http.statusCode == 200,
               let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                let fuels = jsonArray.compactMap { $0["brandstof_omschrijving"] as? String }
                return determineEngineType(from: fuels, rawModel: rawModel)
            }
        } catch {
            // Ignore fuel lookup error and fallback
        }
        return determineEngineType(from: [], rawModel: rawModel)
    }

    private static func capitalizeWords(_ text: String) -> String {
        text.lowercased().split(separator: " ").map { $0.prefix(1).uppercased() + $0.dropFirst() }.joined(separator: " ")
    }
}
