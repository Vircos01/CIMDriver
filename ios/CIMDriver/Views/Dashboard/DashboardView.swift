import SwiftUI
import SwiftData
import Charts

struct DashboardView: View {
    @Environment(TrackingManager.self) private var trackingManager
    @Environment(\.modelContext) private var modelContext
    @Query private var trips: [Trip]
    @Query private var vehicles: [Vehicle]
    @Query private var addresses: [SavedAddress]
    @Query private var rules: [ClassificationRule]
    @Query private var workDays: [WorkDay]
    @Query private var targets: [HoursTarget]
    @Query private var projectCodes: [ProjectCode]
    @Query private var settingsList: [AppSettings]
    @Query private var allChecks: [OdometerCheck]
    
    @AppStorage("monthly_closing_banner_dismissed_month") private var dismissedClosingMonth: String = ""
    @State private var showingMonthlyClosingSheet = false
    @State private var monthlyClosingTrips: [Trip] = []
    @State private var monthlyClosingTitle: String = ""
    @State private var exportURL: URL? = nil
    
    @State private var timeFilter: TimeFilter = .week
    @State private var selectedVehicleId: UUID?
    @State private var lastApprovedWorkDay: WorkDay? = nil
    @State private var undoDepTime: Date? = nil
    @State private var undoRoundedDepTime: Date? = nil
    @State private var undoBreakMins: Int = 0
    @State private var undoProjectCode: String? = nil
    @State private var undoStatus: String = "TO_REVIEW"
    @State private var showUndoBanner: Bool = false
    
    @Binding var selectedTab: Int
    
    // Derived Stats
    private var stats: DashboardStats {
        DashboardStatsCalculator.calculateDashboardStats(
            tripList: trips,
            timeFilter: timeFilter,
            selectedVehicleId: selectedVehicleId,
            vehicleList: vehicles,
            rules: rules,
            addresses: addresses
        )
    }
    
    private var categoryBreakdown: [CategoryBreakdownItem] {
        DashboardStatsCalculator.calculateCategoryBreakdown(
            tripList: trips,
            timeFilter: timeFilter,
            selectedVehicleId: selectedVehicleId,
            vehicleList: vehicles,
            rules: rules,
            addresses: addresses
        )
    }
    
    private var tripsToReviewCount: Int {
        trips.filter { $0.status == TripStatus.toReview.rawValue }.count
    }
    
    private var workDaysToReviewCount: Int {
        workDays.filter { $0.status == "TO_REVIEW" }.count
    }

    private var workDayCandidates: [WorkDayReviewCandidate] {
        let cal = Calendar.current
        return workDays.filter { $0.status == "TO_REVIEW" }.compactMap { wd in
            let dayTrips = trips.filter { cal.isDate($0.startTime, inSameDayAs: wd.date) }
            let suggestion = WorkHoursTriage.evaluateWorkDay(
                workDay: wd,
                tripsOnDay: dayTrips,
                settings: appSettings,
                savedAddresses: addresses
            )
            let isToday = cal.isDateInToday(wd.date)
            let isActivelyRunning = isToday && (wd.departureTime == nil && wd.lastArrivalTime == nil)
            if isActivelyRunning {
                return nil
            }
            return WorkDayReviewCandidate(workDay: wd, suggestion: suggestion)
        }
    }
    
    private var appSettings: AppSettings? { settingsList.first }

    private var currentTargetProgress: [(target: HoursTarget, progress: TargetProgressSnapshot)] {
        let year = Calendar.current.component(.year, from: Date())
        return targets.filter { $0.isActive && $0.year == year }.map { target in
            (target, TargetCalculator.progress(
                for: target,
                workDays: workDays,
                projectCodes: projectCodes,
                employmentStartDate: appSettings?.employmentStartDate
            ))
        }
    }
    
    private var distanceChartData: [DistanceChartItem] {
        DashboardStatsCalculator.calculateDistanceChartData(
            tripList: trips,
            timeFilter: timeFilter,
            selectedVehicleId: selectedVehicleId,
            vehicleList: vehicles,
            rules: rules,
            addresses: addresses
        )
    }
    
    private var workHoursChartData: [WorkHoursChartItem] {
        // Simple aggregation for the selected time filter
        // For simplicity, we just aggregate the workDays in the current filter range
        let calendar = Calendar.current
        let now = Date()
        
        let startDate: Date
        switch timeFilter {
        case .week: startDate = calendar.date(byAdding: .day, value: -7, to: now) ?? now
        case .month: startDate = calendar.date(byAdding: .month, value: -1, to: now) ?? now
        case .year: startDate = calendar.date(byAdding: .year, value: -1, to: now) ?? now
        }
        
        let filteredWorkDays = workDays.filter { $0.date >= startDate }
        
        var aggregated: [String: Double] = [:]
        
        let formatter = DateFormatter()
        formatter.dateFormat = (timeFilter == .week || timeFilter == .month) ? "EEE dd" : "MMM yyyy"
        
        for wd in filteredWorkDays {
            let label = formatter.string(from: wd.date)
            let arr = wd.roundedArrivalTime ?? wd.arrivalTime
            let dep = wd.roundedDepartureTime ?? wd.departureTime ?? arr
            let seconds = max(0, dep.timeIntervalSince(arr))
            let minutes = max(0, (seconds / 60.0) - Double(wd.breakMinutes))
            aggregated[label, default: 0] += (minutes / 60.0)
        }
        
        return aggregated.map { WorkHoursChartItem(label: $0.key, hours: $0.value) }
            .sorted { $0.label < $1.label } // roughly sorted
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    
                    // 0. Branding Banner
                    brandingBanner
                    
                    // 1. Tracking Status (Bestaand)
                    statusCard
                    
                    // 1.25 Fiscale Maandafsluiting Banner
                    MonthlyClosingBannerView(
                        dismissedMonth: $dismissedClosingMonth,
                        trips: trips,
                        onOpenClosing: { selectedTrips, title in
                            monthlyClosingTrips = selectedTrips
                            monthlyClosingTitle = title
                            showingMonthlyClosingSheet = true
                        }
                    )
                    
                    // 1.5 Dagelijkse Inbox (Uitzonderingen & Triage)
                    dailyInboxCard
                    
                    // 2. Voertuig Info & Waarschuwingen
                    if let activeVehicle = activeVehicle {
                        if activeVehicle.privateKmYearlyLimit > 0 {
                            let proRataLimit = DashboardStatsCalculator.calculateProRataLimit(
                                yearlyLimit: activeVehicle.privateKmYearlyLimit,
                                inServiceDate: activeVehicle.inServiceDate,
                                endServiceDate: activeVehicle.endServiceDate
                            )
                            privateKmWarningCard(
                                limit: proRataLimit, 
                                current: stats.ytdPriveKm,
                                isProRata: activeVehicle.inServiceDate != nil || activeVehicle.endServiceDate != nil
                            )
                        }
                    }
                    
                    // 3. Time Filter Picker
                    Picker("Periode", selection: $timeFilter) {
                        ForEach(TimeFilter.allCases) { filter in
                            Text(filter.rawValue).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    
                    // 4. KPI Cards
                    HStack(spacing: 12) {
                        kpiCard(
                            title: "Zakelijk",
                            km: stats.zakelijkKm + stats.woonWerkKm,
                            prevKm: stats.prevZakelijkKm + stats.prevWoonWerkKm,
                            minutes: stats.zakelijkMin + stats.woonWerkMin,
                            color: .cimNavy,
                            icon: "briefcase.fill",
                            woonWerkKm: stats.woonWerkKm,
                            woonWerkMin: stats.woonWerkMin
                        )
                        kpiCard(
                            title: "Privé",
                            km: stats.priveKm,
                            prevKm: stats.prevPriveKm,
                            minutes: stats.priveMin,
                            color: .orange,
                            icon: "car.fill"
                        )
                    }
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal)
                    
                    // 4.5. Categorieën PieChart
                    if !categoryBreakdown.isEmpty {
                        CategoryPieChart(data: categoryBreakdown)
                            .padding(.horizontal)
                    }
                    
                    // 5. Swift Charts - Stacked Bar Chart
                    DistanceStackedChart(data: distanceChartData)
                        .padding(.horizontal)
                    
                    // 6. Swift Charts - Work Hours Bar Chart
                    WorkHoursBarChart(data: workHoursChartData)
                        .padding(.horizontal)

                    if !currentTargetProgress.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Uren- & omzetdoelen")
                                    .font(.title2.bold())
                                Spacer()
                                NavigationLink(destination: HoursTargetsView()) {
                                    Text("Beheren")
                                        .font(.subheadline.bold())
                                        .foregroundColor(.accentColor)
                                }
                            }
                            .padding(.horizontal)

                            ForEach(currentTargetProgress, id: \.target.id) { item in
                                NavigationLink(destination: HoursTargetsView()) {
                                    VStack(alignment: .leading, spacing: 8) {
                                        HStack {
                                            Text(item.target.name).font(.headline)
                                                .foregroundColor(.primary)
                                            Spacer()
                                            Text(item.progress.percentage, format: .percent.precision(.fractionLength(0)))
                                                .font(.headline)
                                                .foregroundColor(.primary)
                                        }
                                        ProgressView(value: min(max(item.progress.percentage, 0), 1))
                                            .tint(item.progress.isBehind ? .orange : .cimGreen)
                                        HStack {
                                            if item.target.targetType == "REVENUE" {
                                                Text(item.progress.accumulatedRevenue, format: .currency(code: Locale.current.currency?.identifier ?? "EUR"))
                                                Spacer()
                                                Text("Doel: \(item.progress.effectiveTarget, format: .currency(code: Locale.current.currency?.identifier ?? "EUR"))")
                                            } else {
                                                Text("\(item.progress.accumulatedHours, format: .number.precision(.fractionLength(1))) uur")
                                                Spacer()
                                                Text("Doel: \(item.progress.effectiveTarget, format: .number.precision(.fractionLength(1))) uur")
                                            }
                                        }
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                        if item.progress.proRataFactor < 1 {
                                            Text("Pro-rata: \(item.progress.proRataFactor, format: .percent.precision(.fractionLength(0))) van jaar")
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                        }
                                    }
                                    .padding()
                                    .background(Color(.secondarySystemGroupedBackground))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .padding(.horizontal)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    
                    // 7. Voertuig Odometer Overzicht
                    if !vehicles.isEmpty {
                        if vehicles.count >= 2 {
                            multiVehicleFleetCard
                                .padding(.top, 10)
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Kilometerstanden")
                                    .font(.title2.bold())
                                    .padding(.horizontal)
                                
                                ForEach(vehicles) { vehicle in
                                    vehicleRow(vehicle: vehicle)
                                }
                            }
                            .padding(.top, 10)
                        }
                    }
                }
                .padding(.vertical)
            }
            .navigationTitle("dashboard_title")
            .background(Color(.systemGroupedBackground))
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !vehicles.isEmpty {
                        Menu {
                            Button(action: { selectedVehicleId = UUID(uuidString: "00000000-0000-0000-0000-000000000000")! }) {
                                HStack {
                                    Text(String(localized: "all_vehicles"))
                                    if selectedVehicleId == UUID(uuidString: "00000000-0000-0000-0000-000000000000")! {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                            ForEach(vehicles) { vehicle in
                                Button(action: { selectedVehicleId = vehicle.id }) {
                                    HStack {
                                        Text("\(vehicle.name) - \(vehicle.licensePlate)")
                                        if selectedVehicleId == vehicle.id {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            Image(systemName: "car.fill")
                        }
                    }
                }
            }
            .onAppear {
                if selectedVehicleId == nil {
                    selectedVehicleId = vehicles.first(where: { $0.isDefault })?.id ?? vehicles.first?.id
                }
                
                // Request permissions when the user lands on the dashboard
                trackingManager.requestPermissions()
            }
            .sheet(isPresented: $showingMonthlyClosingSheet) {
                ExportPreCheckSheet(
                    trips: monthlyClosingTrips,
                    periodTitle: monthlyClosingTitle,
                    isPDF: true,
                    onConfirm: {
                        let minTime = monthlyClosingTrips.map(\.startTime).min() ?? Date.distantPast
                        let maxTime = monthlyClosingTrips.compactMap(\.endTime).max() ?? (monthlyClosingTrips.map(\.startTime).max() ?? Date.distantFuture)
                        let relevantChecks = allChecks.filter { $0.timestamp >= minTime.addingTimeInterval(-86400) && $0.timestamp <= maxTime.addingTimeInterval(86400) }
                        exportURL = ExportUtil.generateTripsPDF(trips: monthlyClosingTrips, periodTitle: monthlyClosingTitle, odometerChecks: relevantChecks)
                    }
                )
            }
            .sheet(isPresented: Binding(get: { exportURL != nil }, set: { if !$0 { exportURL = nil } })) {
                if let url = exportURL {
                    ShareSheet(activityItems: [url])
                }
            }
        }
    }
    
    // MARK: - Components
    
    private var activeVehicle: Vehicle? {
        if selectedVehicleId == UUID(uuidString: "00000000-0000-0000-0000-000000000000")! { return nil }
        return vehicles.first { $0.id == selectedVehicleId } ?? vehicles.first(where: { $0.isDefault }) ?? vehicles.first
    }
    
    private var statusCard: some View {
        VStack(spacing: 12) {
            Image(systemName: statusIcon)
                .font(.system(size: 40))
                .foregroundStyle(statusColor)
                .symbolEffect(.pulse, isActive: trackingManager.state == .active)
            
            Text(LocalizedStringKey(statusText))
                .font(.headline)
                .foregroundStyle(statusColor)
            
            if trackingManager.state == .active, let activeTrip = trackingManager.activeTrip {
                let isPrivate = activeTrip.tripType == "PRIVATE"
                let currentTypeLabel = isPrivate ? "Privé" : (activeTrip.tripType == "COMMUTE" ? "Woon-werk" : "Zakelijk")
                
                VStack(spacing: 8) {
                    Text("Huidige rit: \(currentTypeLabel)")
                        .font(.subheadline.bold())
                        .foregroundStyle(isPrivate ? Color.orange : Color.cimNavy)
                    
                    Button(action: {
                        trackingManager.toggleActiveTripType()
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: isPrivate ? "briefcase.fill" : "person.fill")
                            Text(isPrivate ? "Wissel naar Zakelijk" : "Wissel naar Privé")
                        }
                        .font(.caption.bold())
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(isPrivate ? Color.cimNavy : Color.orange)
                        .foregroundColor(.white)
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
        .background(statusColor.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(statusColor.opacity(0.3), lineWidth: 1)
        )
        .padding(.horizontal)
        .onTapGesture {
            // Manual override for testing purposes in Simulator
            if trackingManager.state == .idle {
                trackingManager.didConnectToVehicleDevice()
            } else {
                trackingManager.didDisconnectFromVehicleDevice()
            }
        }
    }
    
    private var brandingBanner: some View {
        HStack {
            Image("Banner")
                .resizable()
                .scaledToFit()
                .frame(height: 50)
            Spacer()
        }
        .padding(.horizontal)
        .padding(.top, 5)
    }
    
    private func privateKmWarningCard(limit: Int, current: Double, isProRata: Bool = false) -> some View {
        let progress = min(current / Double(limit), 1.0)
        let isDanger = progress >= 1.0
        let isWarning = progress >= 0.8 && !isDanger
        
        let progressColor: Color = isDanger ? .red : (isWarning ? .orange : .cimGreen)
        
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Privégebruik (YTD)", systemImage: "car")
                    .font(.headline)
                    .foregroundColor(isDanger ? .red : .primary)
                
                Spacer()
                
                Text("\(Int(current)) / \(limit) km")
                    .font(.headline)
                    .bold()
            }
            
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 7)
                        .frame(height: 14)
                        .foregroundColor(Color.secondary.opacity(0.2))
                    
                    RoundedRectangle(cornerRadius: 7)
                        .frame(width: max(0, geometry.size.width * CGFloat(progress)), height: 14)
                        .foregroundColor(progressColor)
                }
            }
            .frame(height: 14)
            
            if isProRata {
                Text("Aangepaste grens (pro rata).")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, -4)
            }

            if stats.projectedYearEndPriveKm > 0 {
                let projected = Int(stats.projectedYearEndPriveKm)
                let exceedsProjected = projected > limit
                HStack(spacing: 6) {
                    Image(systemName: exceedsProjected ? "exclamationmark.triangle.fill" : "chart.line.uptrend.xyaxis")
                        .foregroundColor(exceedsProjected ? .red : .secondary)
                        .font(.caption)
                    Text("Prognose jaareinde: ~\(projected) km\(exceedsProjected ? " (overschrijding verwacht!)" : "")")
                        .font(.caption)
                        .foregroundColor(exceedsProjected ? .red : .secondary)
                }
                .padding(.top, -2)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
    
    private var multiVehicleFleetCard: some View {
        let calendar = Calendar.current
        let now = Date()
        let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? now
        let startOfYear = calendar.date(from: calendar.dateComponents([.year], from: now)) ?? now
        
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Wagenpark Overzicht", systemImage: "car.2.fill")
                    .font(.title3.bold())
                    .foregroundStyle(Color.cimNavy)
                Spacer()
                Text("\(vehicles.count) voertuigen")
                    .font(.caption.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.cimNavy.opacity(0.1))
                    .foregroundStyle(Color.cimNavy)
                    .clipShape(Capsule())
            }
            .padding(.horizontal)
            
            VStack(spacing: 12) {
                ForEach(Array(vehicles.enumerated()), id: \.element.id) { index, vehicle in
                    if index > 0 {
                        Divider()
                    }
                    
                    let vehicleTrips = trips.filter { $0.vehicle?.id == vehicle.id }
                    let monthTrips = vehicleTrips.filter { $0.startTime >= startOfMonth }
                    let monthKm = monthTrips.reduce(0.0) { $0 + Double($1.distanceMeters) / 1000.0 }
                    
                    let yearPrivateTrips = vehicleTrips.filter {
                        $0.startTime >= startOfYear &&
                        ($0.tripType == "PRIVATE" || $0.tripType == TripClassification.dynamicsPersonal)
                    }
                    let yearPrivateKm = yearPrivateTrips.reduce(0.0) {
                        $0 + (Double($1.distanceMeters + $1.privateDetourMeters) / 1000.0)
                    }
                    
                    let proRataLimit = DashboardStatsCalculator.calculateProRataLimit(
                        yearlyLimit: vehicle.privateKmYearlyLimit > 0 ? vehicle.privateKmYearlyLimit : 500,
                        inServiceDate: vehicle.inServiceDate,
                        endServiceDate: vehicle.endServiceDate
                    )
                    let isProRata = vehicle.inServiceDate != nil || vehicle.endServiceDate != nil
                    let privateProgress = proRataLimit > 0 ? min(1.0, max(0.0, yearPrivateKm / Double(proRataLimit))) : 0.0
                    let warningThreshold = Double(proRataLimit) * 0.8
                    let statusColor: Color = {
                        if yearPrivateKm > Double(proRataLimit) { return .red }
                        if yearPrivateKm >= warningThreshold { return .orange }
                        return .cimGreen
                    }()
                    
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text("\(vehicle.make) \(vehicle.model)")
                                    .font(.headline)
                                HStack(spacing: 6) {
                                    Text(vehicle.licensePlate)
                                        .font(.caption2.bold())
                                        .padding(.horizontal, 5)
                                        .padding(.vertical, 2)
                                        .background(Color(red: 1.0, green: 0.85, blue: 0.3))
                                        .foregroundColor(.black)
                                        .clipShape(RoundedRectangle(cornerRadius: 3))
                                    if vehicle.isDefault {
                                        Text("Standaard")
                                            .font(.caption2)
                                            .foregroundStyle(Color.cimNavy)
                                    }
                                }
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("\(vehicle.odometerCurrent) km")
                                    .font(.headline.bold())
                                    .foregroundStyle(Color.cimNavy)
                                Text("\(monthTrips.count) ritten (\(Int(monthKm)) km) deze mnd")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        
                        // Privé Progress Bar (naar rato)
                        VStack(spacing: 3) {
                            HStack {
                                Text("Privé dit jaar:")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                if isProRata {
                                    Text("(naar rato)")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary.opacity(0.8))
                                }
                                Spacer()
                                Text(String(format: "%.1f / %d km", yearPrivateKm, proRataLimit))
                                    .font(.caption2.bold())
                                    .foregroundStyle(statusColor)
                            }
                            
                            ProgressView(value: privateProgress)
                                .tint(statusColor)
                            
                            HStack {
                                let hint: String = {
                                    if yearPrivateKm > Double(proRataLimit) { return "Fiscaal bijtellingsrisico (overschreden)" }
                                    if yearPrivateKm >= warningThreshold {
                                        let remaining = max(0, Int(Double(proRataLimit) - yearPrivateKm))
                                        return "Nog \(remaining) km tot bijtellingsgrens"
                                    }
                                    return isProRata ? "Veilig binnen \(proRataLimit) km privé-norm (pro rata)" : "Veilig binnen \(proRataLimit) km privé-norm"
                                }()
                                Text(hint)
                                    .font(.caption2)
                                    .foregroundStyle(statusColor)
                                Spacer()
                            }
                        }
                    }
                }
            }
            .padding()
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal)
        }
    }
    
    private func vehicleRow(vehicle: Vehicle) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text("\(vehicle.make) \(vehicle.model)")
                    .font(.headline)
                Text(vehicle.licensePlate)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("\(vehicle.odometerCurrent) km")
                    .font(.title3.bold())
                    .foregroundStyle(Color.cimNavy)
                
                if let checkDate = vehicle.lastOdometerCheckTimestamp {
                    Text("Check: \(checkDate.formatted(date: .numeric, time: .omitted))")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
    
    private func quickApprove(candidate: WorkDayReviewCandidate) {
        let wd = candidate.workDay
        let sug = candidate.suggestion
        lastApprovedWorkDay = wd
        undoDepTime = wd.departureTime
        undoRoundedDepTime = wd.roundedDepartureTime
        undoBreakMins = wd.breakMinutes
        undoProjectCode = wd.projectCode
        undoStatus = wd.status

        let effectiveDep = wd.departureTime ?? sug.suggestedDepartureTime
        wd.departureTime = effectiveDep
        wd.roundedDepartureTime = wd.roundedDepartureTime ?? effectiveDep
        if wd.breakMinutes == 0 {
            wd.breakMinutes = sug.suggestedBreakMinutes
        }
        if wd.projectCode == nil || wd.projectCode?.isEmpty == true {
            wd.projectCode = sug.suggestedProjectCode
        }
        wd.status = "APPROVED"
        try? modelContext.save()
        withAnimation {
            showUndoBanner = true
        }
    }

    private func undoApproval() {
        guard let wd = lastApprovedWorkDay else { return }
        wd.departureTime = undoDepTime
        wd.roundedDepartureTime = undoRoundedDepTime
        wd.breakMinutes = undoBreakMins
        wd.projectCode = undoProjectCode
        wd.status = undoStatus
        try? modelContext.save()
        withAnimation {
            showUndoBanner = false
            lastApprovedWorkDay = nil
        }
    }

    private var dailyInboxCard: some View {
        let effectiveWorkDays = max(workDaysToReviewCount, workDayCandidates.count)
        let totalItems = tripsToReviewCount + effectiveWorkDays
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Dagelijkse Inbox", systemImage: "tray.full.fill")
                    .font(.headline)
                    .foregroundColor(totalItems > 0 ? Color.cimNavy : .secondary)
                Spacer()
                if totalItems > 0 {
                    Text("\(totalItems) open")
                        .font(.caption.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.red.opacity(0.15))
                        .foregroundColor(.red)
                        .clipShape(Capsule())
                } else {
                    Text("Bijgewerkt")
                        .font(.caption.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.cimGreen.opacity(0.15))
                        .foregroundColor(Color.cimGreen)
                        .clipShape(Capsule())
                }
            }

            if showUndoBanner {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.cimGreen)
                    Text("Werkdag goedgekeurd")
                        .font(.subheadline)
                    Spacer()
                    Button("Ongedaan maken") {
                        undoApproval()
                    }
                    .font(.subheadline.bold())
                    .foregroundColor(.orange)
                }
                .padding(10)
                .background(Color.orange.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            
            if totalItems == 0 {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Color.cimGreen)
                    Text("Alles staat goed. Geen ritten, uren of meldingen die aandacht vereisen.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            } else {
                if tripsToReviewCount > 0 {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(tripsToReviewCount) ritten te beoordelen")
                                .font(.subheadline.bold())
                            Text("Classificeer type en projectcode")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Button("Beoordelen") {
                            selectedTab = 1
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.cimNavy)
                        .controlSize(.small)
                    }
                }

                if !workDayCandidates.isEmpty {
                    if tripsToReviewCount > 0 {
                        Divider()
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Werkuren ter controle (\(workDayCandidates.count))")
                            .font(.caption.bold())
                            .foregroundColor(.orange)

                        ForEach(workDayCandidates.prefix(3)) { candidate in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(candidate.workDay.date.formatted(date: .abbreviated, time: .omitted))
                                        .font(.subheadline.bold())
                                    Spacer()
                                    let isHigh = candidate.suggestion.isHighConfidence
                                    Text("\(candidate.suggestion.confidenceScore)% zeker")
                                        .font(.caption2.bold())
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(isHigh ? Color.cimGreen.opacity(0.15) : Color.orange.opacity(0.15))
                                        .foregroundColor(isHigh ? Color.cimGreen : Color.orange)
                                        .clipShape(Capsule())
                                }

                                Text(candidate.suggestion.statusText)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)

                                HStack {
                                    Text("\(candidate.suggestion.suggestedBreakMinutes)m pauze\(candidate.suggestion.suggestedProjectCode.map { " • \($0)" } ?? "")")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                    Spacer()
                                    Button(action: {
                                        quickApprove(candidate: candidate)
                                    }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "checkmark")
                                            Text("Akkoord")
                                        }
                                        .font(.caption.bold())
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 5)
                                        .background(Color.cimNavy)
                                        .foregroundColor(.white)
                                        .clipShape(Capsule())
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(10)
                            .background(Color(.systemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }

                        if workDayCandidates.count > 3 {
                            Button("Bekijk alle \(workDayCandidates.count) werkdagen →") {
                                selectedTab = 2
                            }
                            .font(.caption)
                            .foregroundColor(.accentColor)
                        }
                    }
                } else if workDaysToReviewCount > 0 {
                    if tripsToReviewCount > 0 {
                        Divider()
                    }
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(workDaysToReviewCount) werkdagen te bevestigen")
                                .font(.subheadline.bold())
                            Text("Aanwezigheid en uren afronden")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Button("Bevestigen") {
                            selectedTab = 2
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.orange)
                        .controlSize(.small)
                    }
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
    
    private func kpiCard(title: String, km: Double, prevKm: Double, minutes: Int, color: Color, icon: String, woonWerkKm: Double = 0, woonWerkMin: Int = 0) -> some View {
        VStack(alignment: .center, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Text(String(format: "%.1f km", km))
                .font(.title2.bold())
                .foregroundColor(color)
            
            if prevKm > 0 {
                let trend = ((km - prevKm) / prevKm) * 100
                let isUp = trend >= 0
                let trendColor = title == "Privé" ? (isUp ? Color.red : Color.cimGreen) : (isUp ? Color.cimGreen : Color.red)
                
                HStack(spacing: 4) {
                    Image(systemName: isUp ? "arrow.up.right" : "arrow.down.right")
                        .font(.caption2.bold())
                    Text(String(format: "%.1f%%", abs(trend)))
                        .font(.caption)
                }
                .foregroundColor(trendColor)
            } else {
                Text("-")
                    .font(.caption)
                    .foregroundColor(.clear)
            }
            
            if title == "Zakelijk" {
                if woonWerkKm > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "building.2.crop.circle.fill")
                            .font(.caption2)
                        Text(String(format: "%.1f km", woonWerkKm))
                            .font(.caption2)
                    }
                    .foregroundColor(.blue)
                    .padding(.top, 2)
                }
                
                if km > 0 {
                    let compensation = km * (appSettings?.businessCompensation ?? 0.23)
                    Text(String(format: "€ %.2f", compensation))
                        .font(.subheadline.bold())
                        .foregroundColor(Color.cimGreen)
                        .padding(.top, 4)
                }
            }
            
            Spacer(minLength: 0)
            
            Divider().padding(.vertical, 4)
            
            let h = minutes / 60
            let m = minutes % 60
            Text(h > 0 ? "\(h)u \(m)m" : "\(m)m")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var statusIcon: String {
        switch trackingManager.state {
        case .idle: return "car.fill"
        case .active: return "location.fill"
        case .gracePeriod: return "pause.circle.fill"
        }
    }
    
    private var statusText: String {
        switch trackingManager.state {
        case .idle: return "dashboard_status_idle"
        case .active: return "dashboard_status_active"
        case .gracePeriod: return "dashboard_status_grace"
        }
    }
    
    private var statusColor: Color {
        switch trackingManager.state {
        case .idle: return .secondary
        case .active: return .cimGreen
        case .gracePeriod: return .orange
        }
    }
}

struct AutoPreviewProviderDashboardView1: PreviewProvider {
    static var previews: some View {
            DashboardView(selectedTab: .constant(0))
                .modelContainer(PreviewContainer.shared.container)
                .environment(TrackingManager.shared)
    }
}

// MARK: - MonthlyClosingBannerView

struct MonthlyClosingBannerView: View {
    @Binding var dismissedMonth: String
    let trips: [Trip]
    let onOpenClosing: ([Trip], String) -> Void
    
    private var isVisible: Bool {
        let calendar = Calendar.current
        let now = Date()
        let dayOfMonth = calendar.component(.day, from: now)
        let dfKey = DateFormatter()
        dfKey.dateFormat = "yyyy-MM"
        let currentMonthKey = dfKey.string(from: now)
        return dayOfMonth <= 7 && dismissedMonth != currentMonthKey
    }
    
    private var prevMonthName: String {
        let calendar = Calendar.current
        let now = Date()
        let prevMonthDate = calendar.date(byAdding: .month, value: -1, to: now) ?? now
        let df = DateFormatter()
        df.locale = Locale(identifier: "nl_NL")
        df.dateFormat = "MMMM yyyy"
        return df.string(from: prevMonthDate).capitalized
    }
    
    private var currentMonthKey: String {
        let dfKey = DateFormatter()
        dfKey.dateFormat = "yyyy-MM"
        return dfKey.string(from: Date())
    }
    
    var body: some View {
        if isVisible {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "calendar.badge.checkmark")
                            .font(.title3)
                            .foregroundColor(.cimGreen)
                        Text("Fiscale Maandafsluiting gereed")
                            .font(.headline)
                            .foregroundColor(.primary)
                    }
                    Spacer()
                    Button(action: {
                        dismissedMonth = currentMonthKey
                    }) {
                        Image(systemName: "xmark")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Text("De rittenadministratie van \(prevMonthName) staat klaar voor controle en export naar je boekhouder of administratie.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                Button(action: {
                    let calendar = Calendar.current
                    let now = Date()
                    let prevMonthDate = calendar.date(byAdding: .month, value: -1, to: now) ?? now
                    let startOfPrev = calendar.date(from: calendar.dateComponents([.year, .month], from: prevMonthDate)) ?? prevMonthDate
                    let startOfThis = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? now
                    let pTrips = trips.filter { $0.startTime >= startOfPrev && $0.startTime < startOfThis }
                    let selectedTrips = pTrips.isEmpty ? trips : pTrips
                    onOpenClosing(selectedTrips, prevMonthName)
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.up")
                        Text("Controleren & Delen (\(prevMonthName))")
                            .bold()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                .buttonStyle(.borderedProminent)
                .tint(.cimNavy)
            }
            .padding()
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.cimGreen.opacity(0.3), lineWidth: 1.5)
            )
            .padding(.horizontal)
        }
    }
}
