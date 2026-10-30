import Foundation

enum GapType: String, CaseIterable, Codable {
    case missingKm = "MISSING_KM"
    case overlappingKm = "OVERLAPPING_KM"
    case missingTrip = "MISSING_TRIP"
    case odometerReset = "ODOMETER_RESET"
    case firstTripGap = "FIRST_TRIP_GAP"
    case currentStandGap = "CURRENT_STAND_GAP"
    
    var localizedTitle: String {
        switch self {
        case .missingKm: return "Ontbrekende kilometers"
        case .missingTrip: return "Mogelijk vergeten rit"
        case .overlappingKm: return "Overlappende kilometers"
        case .odometerReset: return "Tellerstand gereset"
        case .firstTripGap: return "Startstand afwijking"
        case .currentStandGap: return "Verschil met actuele teller"
        }
    }
}

enum FixType: String, CaseIterable, Codable {
    case createPrivateTrip = "CREATE_PRIVATE_TRIP"
    case adjustSingleTrip = "ADJUST_SINGLE_TRIP"
    case markAsExplained = "MARK_AS_EXPLAINED"
    case syncVehicleOdometer = "SYNC_VEHICLE_ODOMETER"
    case ignore = "IGNORE"
    
    var localizedTitle: String {
        switch self {
        case .createPrivateTrip: return "Rit toevoegen"
        case .adjustSingleTrip: return "Rit aanpassen"
        case .markAsExplained: return "Verklaard (Belastingdienst)"
        case .syncVehicleOdometer: return "Tellerstand gelijkzetten"
        case .ignore: return "Negeren"
        }
    }
}

struct SuggestedFix: Identifiable, Hashable {
    var id: String { type.rawValue + title }
    let type: FixType
    let title: String
    let description: String
}

struct OdometerGap: Identifiable {
    let id: String
    let vehicleId: UUID
    let gapType: GapType
    let previousTrip: Trip?
    let nextTrip: Trip?
    let expectedOdometer: Int
    let actualOdometer: Int
    let gapKm: Int
    let gapTimeStart: Date
    let gapTimeEnd: Date
    let suggestedFixes: [SuggestedFix]
}

struct OdometerGapAnalyzer {
    
    static func effectiveOdometerEnd(trip: Trip) -> Int {
        if let odoEnd = trip.odometerEnd {
            return odoEnd
        }
        return trip.odometerStart + Int(round(Double(trip.distanceMeters) / 1000.0))
    }
    
    static func effectiveEndTime(trip: Trip) -> Date {
        if let end = trip.endTime {
            return end
        }
        let fallbackSeconds = max(60, Double(trip.distanceMeters) / 15.0)
        return trip.startTime.addingTimeInterval(fallbackSeconds)
    }
    
    static func analyze(vehicle: Vehicle, trips: [Trip]) -> [OdometerGap] {
        let vehicleTrips = trips.filter { $0.vehicle?.id == vehicle.id }
        let sortedTrips = vehicleTrips.sorted { $0.startTime < $1.startTime }
        var gaps: [OdometerGap] = []
        
        if sortedTrips.isEmpty {
            if vehicle.odometerCurrent != vehicle.odometerStart {
                let gapKm = vehicle.odometerCurrent - vehicle.odometerStart
                let start = vehicle.inServiceDate ?? Date()
                let end = Date()
                gaps.append(
                    OdometerGap(
                        id: "gap_no_trips_\(vehicle.id.uuidString)",
                        vehicleId: vehicle.id,
                        gapType: .currentStandGap,
                        previousTrip: nil,
                        nextTrip: nil,
                        expectedOdometer: vehicle.odometerStart,
                        actualOdometer: vehicle.odometerCurrent,
                        gapKm: gapKm,
                        gapTimeStart: start,
                        gapTimeEnd: end,
                        suggestedFixes: generateFixes(
                            gapType: .currentStandGap,
                            gapKm: gapKm,
                            prevTrip: nil,
                            nextTrip: nil
                        )
                    )
                )
            }
            return gaps
        }
        
        // 1. Check start of first trip against vehicle.odometerStart
        if let firstTrip = sortedTrips.first, firstTrip.odometerStart != vehicle.odometerStart {
            let gapKm = firstTrip.odometerStart - vehicle.odometerStart
            let start = vehicle.inServiceDate ?? firstTrip.startTime.addingTimeInterval(-86400)
            let end = firstTrip.startTime
            gaps.append(
                OdometerGap(
                    id: "gap_first_\(firstTrip.id.uuidString)",
                    vehicleId: vehicle.id,
                    gapType: .firstTripGap,
                    previousTrip: nil,
                    nextTrip: firstTrip,
                    expectedOdometer: vehicle.odometerStart,
                    actualOdometer: firstTrip.odometerStart,
                    gapKm: gapKm,
                    gapTimeStart: start,
                    gapTimeEnd: end,
                    suggestedFixes: generateFixes(
                        gapType: .firstTripGap,
                        gapKm: gapKm,
                        prevTrip: nil,
                        nextTrip: firstTrip
                    )
                )
            )
        }
        
        // 2. Check gaps between consecutive trips
        if sortedTrips.count > 1 {
            for i in 0..<(sortedTrips.count - 1) {
                let prev = sortedTrips[i]
                let next = sortedTrips[i + 1]
                let prevEnd = effectiveOdometerEnd(trip: prev)
                let prevEndTime = effectiveEndTime(trip: prev)
                let diff = next.odometerStart - prevEnd
                let isTimeOverlap = next.startTime < prevEndTime
                
                if diff != 0 {
                    let gapType: GapType
                    if diff < -50 {
                        gapType = .odometerReset
                    } else if diff < 0 || isTimeOverlap {
                        gapType = .overlappingKm
                    } else if next.startTime.timeIntervalSince(prevEndTime) > (48 * 3600) {
                        gapType = .missingTrip
                    } else {
                        gapType = .missingKm
                    }
                    
                    let timeStart = isTimeOverlap ? min(prev.startTime, next.startTime) : prevEndTime
                    let timeEnd = isTimeOverlap ? max(prevEndTime, effectiveEndTime(trip: next)) : next.startTime
                    
                    gaps.append(
                        OdometerGap(
                            id: "gap_\(prev.id.uuidString)_\(next.id.uuidString)",
                            vehicleId: vehicle.id,
                            gapType: gapType,
                            previousTrip: prev,
                            nextTrip: next,
                            expectedOdometer: prevEnd,
                            actualOdometer: next.odometerStart,
                            gapKm: diff,
                            gapTimeStart: timeStart,
                            gapTimeEnd: timeEnd,
                            suggestedFixes: generateFixes(
                                gapType: gapType,
                                gapKm: diff,
                                prevTrip: prev,
                                nextTrip: next
                            )
                        )
                    )
                }
            }
        }
        
        // 3. Check latest trip end against vehicle.odometerCurrent
        if let lastTrip = sortedTrips.last {
            let lastTripEnd = effectiveOdometerEnd(trip: lastTrip)
            let lastTripEndTime = effectiveEndTime(trip: lastTrip)
            let currentDiff = vehicle.odometerCurrent - lastTripEnd
            
            if currentDiff != 0 {
                let gapType: GapType = .currentStandGap
                gaps.append(
                    OdometerGap(
                        id: "gap_last_\(lastTrip.id.uuidString)",
                        vehicleId: vehicle.id,
                        gapType: gapType,
                        previousTrip: lastTrip,
                        nextTrip: nil,
                        expectedOdometer: lastTripEnd,
                        actualOdometer: vehicle.odometerCurrent,
                        gapKm: currentDiff,
                        gapTimeStart: lastTripEndTime,
                        gapTimeEnd: Date(),
                        suggestedFixes: generateFixes(
                            gapType: gapType,
                            gapKm: currentDiff,
                            prevTrip: lastTrip,
                            nextTrip: nil
                        )
                    )
                )
            }
        }
        
        return gaps
    }
    
    static func isChainAirtight(vehicle: Vehicle, trips: [Trip]) -> Bool {
        return analyze(vehicle: vehicle, trips: trips).isEmpty
    }
    
    static func generateFixes(
        gapType: GapType,
        gapKm: Int,
        prevTrip: Trip?,
        nextTrip: Trip?
    ) -> [SuggestedFix] {
        var fixes: [SuggestedFix] = []
        
        if gapKm > 0 {
            let fromAddr = prevTrip?.endAddress ?? "Onbekend vertrekpunt"
            let toAddr = nextTrip?.startAddress ?? "Onbekend eindpunt"
            if gapKm <= 30 {
                fixes.append(
                    SuggestedFix(
                        type: .createPrivateTrip,
                        title: "Rit toevoegen (\(gapKm) km)",
                        description: "Maak een sluitende aansluitende rit van \(fromAddr) naar \(toAddr)."
                    )
                )
            } else {
                fixes.append(
                    SuggestedFix(
                        type: .createPrivateTrip,
                        title: "Verzamelrit toevoegen (\(gapKm) km)",
                        description: "Maak één rit van \(fromAddr) naar \(toAddr). Tip: Bij meerdere ritten is handmatig los toevoegen via 'Rit Toevoegen' fiscaal zuiverder."
                    )
                )
            }
        } else if gapKm < 0 {
            if prevTrip != nil {
                fixes.append(
                    SuggestedFix(
                        type: .adjustSingleTrip,
                        title: "Vorige rit inkorten (\(abs(gapKm)) km)",
                        description: "Verlaag de eindstand van de vorige rit zodat deze aansluit op de volgende rit."
                    )
                )
            }
        }
        
        // For current stand discrepancy: option to synchronize vehicle odometer with latest trip
        if gapType == .currentStandGap && prevTrip != nil {
            let endStand = effectiveOdometerEnd(trip: prevTrip!)
            fixes.insert(
                SuggestedFix(
                    type: .syncVehicleOdometer,
                    title: "Actuele tellerstand gelijkzetten (\(endStand) km)",
                    description: "Zet de actuele tellerstand van het voertuig gelijk aan de eindstand van de vorige rit (\(endStand) km)."
                ),
                at: 0
            )
        }
        
        // Always available: Mark as explained for Dutch tax compliance
        fixes.append(
            SuggestedFix(
                type: .markAsExplained,
                title: "Markeren met toelichting (Belastingdienst)",
                description: "Registreer een officiële verklaring voor het verschil (bijv. garagebezoek, APK, proefrit)."
            )
        )
        
        // Deliberately ignore
        fixes.append(
            SuggestedFix(
                type: .ignore,
                title: "Negeren",
                description: "Negeer dit verschil bewust in de rapportages."
            )
        )
        
        return fixes
    }
}

