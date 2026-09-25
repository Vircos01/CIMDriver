import Foundation

struct DashboardStatsCalculator {
    
    static func classifyTrip(
        trip: Trip,
        addresses: [SavedAddress],
        rules: [ClassificationRule]
    ) -> TripCategory {
        let startAddressType = addresses.first(where: { $0.address.lowercased() == (trip.startAddress ?? "").lowercased() })?.addressType
        let endAddressType = addresses.first(where: { $0.address.lowercased() == (trip.endAddress ?? "").lowercased() })?.addressType
        
        return TripClassification.classify(
            tripType: trip.tripType,
            startAddressType: startAddressType,
            endAddressType: endAddressType,
            startAddress: trip.startAddress,
            endAddress: trip.endAddress,
            rules: rules
        )
    }
    
    static func calculateDashboardStats(
        tripList: [Trip],
        timeFilter: TimeFilter,
        selectedVehicleId: UUID?,
        vehicleList: [Vehicle],
        rules: [ClassificationRule],
        addresses: [SavedAddress]
    ) -> DashboardStats {
        var cal = Calendar.current
        cal.firstWeekday = 2 // Monday
        let now = Date()
        let currentWeek = cal.component(.weekOfYear, from: now)
        let currentMonth = cal.component(.month, from: now)
        let currentYear = cal.component(.year, from: now)
        
        var prevDate: Date
        switch timeFilter {
        case .week: prevDate = cal.date(byAdding: .weekOfYear, value: -1, to: now) ?? now
        case .month: prevDate = cal.date(byAdding: .month, value: -1, to: now) ?? now
        case .year: prevDate = cal.date(byAdding: .year, value: -1, to: now) ?? now
        }
        
        let prevWeek = cal.component(.weekOfYear, from: prevDate)
        let prevMonth = cal.component(.month, from: prevDate)
        let prevYear = cal.component(.year, from: prevDate)
        
        var totalZakelijkMeters = 0
        var totalPriveMeters = 0
        var totalWoonWerkMeters = 0
        
        var totalZakelijkMs = 0.0
        var totalPriveMs = 0.0
        var totalWoonWerkMs = 0.0
        
        var prevZakelijkMeters = 0
        var prevPriveMeters = 0
        var prevWoonWerkMeters = 0
        
        var ytdPriveMeters = 0
        
        let activeVehicleId = selectedVehicleId ?? vehicleList.first(where: { $0.isDefault })?.id ?? vehicleList.first?.id
        let filteredTripList = activeVehicleId != nil ? tripList.filter { $0.vehicle?.id == activeVehicleId } : tripList
        
        for trip in filteredTripList {
            let status = TripStatus(rawValue: trip.status) ?? .active
            if status == .done || status == .merged || status == .toReview {
                let tripDate = trip.startTime
                let tripYear = cal.component(.year, from: tripDate)
                let tripMonth = cal.component(.month, from: tripDate)
                let tripWeek = cal.component(.weekOfYear, from: tripDate)
                
                let isMatch: Bool
                switch timeFilter {
                case .week: isMatch = (tripYear == currentYear && tripWeek == currentWeek)
                case .month: isMatch = (tripYear == currentYear && tripMonth == currentMonth)
                case .year: isMatch = (tripYear == currentYear)
                }
                
                let isPrevMatch: Bool
                switch timeFilter {
                case .week: isPrevMatch = (tripYear == prevYear && tripWeek == prevWeek)
                case .month: isPrevMatch = (tripYear == prevYear && tripMonth == prevMonth)
                case .year: isPrevMatch = (tripYear == prevYear)
                }
                
                if isMatch || isPrevMatch || tripYear == currentYear {
                    let endTime = trip.endTime ?? trip.startTime
                    let durationMs = endTime.timeIntervalSince(trip.startTime) * 1000 // In milliseconds
                    let category = classifyTrip(trip: trip, addresses: addresses, rules: rules)
                    let meters = trip.distanceMeters
                    
                    if tripYear == currentYear && category == .privateTrip {
                        ytdPriveMeters += meters
                    }
                    
                    if isMatch {
                        if category == .business {
                            totalZakelijkMeters += meters
                            totalZakelijkMs += durationMs
                        } else if category == .commute {
                            totalWoonWerkMeters += meters
                            totalWoonWerkMs += durationMs
                        } else if category == .privateTrip {
                            totalPriveMeters += meters
                            totalPriveMs += durationMs
                        }
                    }
                    
                    if isPrevMatch {
                        if category == .business {
                            prevZakelijkMeters += meters
                        } else if category == .commute {
                            prevWoonWerkMeters += meters
                        } else if category == .privateTrip {
                            prevPriveMeters += meters
                        }
                    }
                }
            }
        }
        
        return DashboardStats(
            zakelijkKm: Double(totalZakelijkMeters) / 1000.0,
            priveKm: Double(totalPriveMeters) / 1000.0,
            woonWerkKm: Double(totalWoonWerkMeters) / 1000.0,
            zakelijkMin: Int(totalZakelijkMs / (1000 * 60)),
            priveMin: Int(totalPriveMs / (1000 * 60)),
            woonWerkMin: Int(totalWoonWerkMs / (1000 * 60)),
            prevZakelijkKm: Double(prevZakelijkMeters) / 1000.0,
            prevPriveKm: Double(prevPriveMeters) / 1000.0,
            prevWoonWerkKm: Double(prevWoonWerkMeters) / 1000.0,
            ytdPriveKm: Double(ytdPriveMeters) / 1000.0
        )
    }
    
    static func calculateDistanceChartData(
        tripList: [Trip],
        timeFilter: TimeFilter,
        selectedVehicleId: UUID?,
        vehicleList: [Vehicle],
        rules: [ClassificationRule],
        addresses: [SavedAddress]
    ) -> [DistanceChartItem] {
        var cal = Calendar.current
        cal.firstWeekday = 2 // Monday
        let now = Date()
        let currentWeek = cal.component(.weekOfYear, from: now)
        let currentMonth = cal.component(.month, from: now)
        let currentYear = cal.component(.year, from: now)
        
        let activeVehicleId = selectedVehicleId ?? vehicleList.first(where: { $0.isDefault })?.id ?? vehicleList.first?.id
        let filteredTripList = activeVehicleId != nil ? tripList.filter { $0.vehicle?.id == activeVehicleId } : tripList
        
        var items: [DistanceChartItem] = []
        
        switch timeFilter {
        case .week:
            let shortDays = ["Zo", "Ma", "Di", "Wo", "Do", "Vr", "Za"]
            for i in 1...7 {
                let dayLabel = shortDays[i - 1]
                var zKm = 0.0
                var pKm = 0.0
                var wKm = 0.0
                
                for trip in filteredTripList {
                    let status = TripStatus(rawValue: trip.status) ?? .active
                    if status == .done || status == .merged || status == .toReview {
                        let tripDate = trip.startTime
                        if cal.component(.year, from: tripDate) == currentYear &&
                            cal.component(.weekOfYear, from: tripDate) == currentWeek &&
                            cal.component(.weekday, from: tripDate) == i {
                            
                            let km = Double(trip.distanceMeters) / 1000.0
                            let category = classifyTrip(trip: trip, addresses: addresses, rules: rules)
                            
                            if category == .business { zKm += km }
                            if category == .commute { wKm += km }
                            if category == .privateTrip { pKm += km }
                        }
                    }
                }
                items.append(DistanceChartItem(label: dayLabel, zakelijkKm: zKm, priveKm: pKm, woonWerkKm: wKm, totalKm: zKm + pKm + wKm))
            }
            // Rotate so Monday is first
            if items.count == 7 {
                return [items[1], items[2], items[3], items[4], items[5], items[6], items[0]]
            }
            return items
            
        case .month:
            let range = cal.range(of: .day, in: .month, for: now)!
            let daysInMonth = range.count
            
            for i in 1...daysInMonth {
                var zKm = 0.0
                var pKm = 0.0
                var wKm = 0.0
                
                for trip in filteredTripList {
                    let status = TripStatus(rawValue: trip.status) ?? .active
                    if status == .done || status == .merged || status == .toReview {
                        let tripDate = trip.startTime
                        if cal.component(.year, from: tripDate) == currentYear &&
                            cal.component(.month, from: tripDate) == currentMonth &&
                            cal.component(.day, from: tripDate) == i {
                            
                            let km = Double(trip.distanceMeters) / 1000.0
                            let category = classifyTrip(trip: trip, addresses: addresses, rules: rules)
                            
                            if category == .business { zKm += km }
                            if category == .commute { wKm += km }
                            if category == .privateTrip { pKm += km }
                        }
                    }
                }
                items.append(DistanceChartItem(label: "\(i)", zakelijkKm: zKm, priveKm: pKm, woonWerkKm: wKm, totalKm: zKm + pKm + wKm))
            }
            return items
            
        case .year:
            let range = cal.range(of: .weekOfYear, in: .year, for: now)!
            let weeksInYear = range.count
            
            for i in 1...weeksInYear {
                var zKm = 0.0
                var pKm = 0.0
                var wKm = 0.0
                
                for trip in filteredTripList {
                    let status = TripStatus(rawValue: trip.status) ?? .active
                    if status == .done || status == .merged || status == .toReview {
                        let tripDate = trip.startTime
                        if cal.component(.year, from: tripDate) == currentYear &&
                            cal.component(.weekOfYear, from: tripDate) == i {
                            
                            let km = Double(trip.distanceMeters) / 1000.0
                            let category = classifyTrip(trip: trip, addresses: addresses, rules: rules)
                            
                            if category == .business { zKm += km }
                            if category == .commute { wKm += km }
                            if category == .privateTrip { pKm += km }
                        }
                    }
                }
                items.append(DistanceChartItem(label: "W\(i)", zakelijkKm: zKm, priveKm: pKm, woonWerkKm: wKm, totalKm: zKm + pKm + wKm))
            }
            return items
        }
    }
    
    static func calculateCategoryBreakdown(
        tripList: [Trip],
        timeFilter: TimeFilter,
        selectedVehicleId: UUID?,
        vehicleList: [Vehicle],
        rules: [ClassificationRule],
        addresses: [SavedAddress]
    ) -> [CategoryBreakdownItem] {
        let stats = calculateDashboardStats(
            tripList: tripList,
            timeFilter: timeFilter,
            selectedVehicleId: selectedVehicleId,
            vehicleList: vehicleList,
            rules: rules,
            addresses: addresses
        )
        
        let total = stats.zakelijkKm + stats.woonWerkKm + stats.priveKm
        guard total > 0 else { return [] }
        
        var breakdown: [CategoryBreakdownItem] = []
        if stats.zakelijkKm > 0 {
            breakdown.append(CategoryBreakdownItem(categoryName: "Zakelijk", distanceKm: stats.zakelijkKm, percentage: (stats.zakelijkKm / total) * 100))
        }
        if stats.woonWerkKm > 0 {
            breakdown.append(CategoryBreakdownItem(categoryName: "Woon-Werk", distanceKm: stats.woonWerkKm, percentage: (stats.woonWerkKm / total) * 100))
        }
        if stats.priveKm > 0 {
            breakdown.append(CategoryBreakdownItem(categoryName: "Privé", distanceKm: stats.priveKm, percentage: (stats.priveKm / total) * 100))
        }
        return breakdown
    }
}

