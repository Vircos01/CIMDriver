import SwiftUI
import SwiftData
import Charts

struct DashboardView: View {
    @Environment(TrackingManager.self) private var trackingManager
    @Query private var trips: [Trip]
    @Query private var vehicles: [Vehicle]
    @Query private var addresses: [SavedAddress]
    @Query private var rules: [ClassificationRule]
    @Query private var workDays: [WorkDay]
    @Query private var settingsList: [AppSettings]
    
    @State private var timeFilter: TimeFilter = .week
    @State private var selectedVehicleId: UUID?
    
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
    
    private var appSettings: AppSettings? { settingsList.first }
    
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
                    
                    // 1.5 Actiekaarten (TO_REVIEW)
                    if tripsToReviewCount > 0 {
                        reviewCard(
                            title: "Actie vereist",
                            message: "\(tripsToReviewCount) ritten moeten nog beoordeeld worden.",
                            buttonTitle: "Bekijk ritten",
                            color: .red
                        ) {
                            selectedTab = 1
                        }
                    }
                    
                    if workDaysToReviewCount > 0 {
                        reviewCard(
                            title: "Controleer werkuren",
                            message: "\(workDaysToReviewCount) werkdagen moeten nog bevestigd worden.",
                            buttonTitle: "Bekijk werkuren",
                            color: .orange
                        ) {
                            selectedTab = 2
                        }
                    }
                    
                    // 2. Voertuig Info & Waarschuwingen
                    if let activeVehicle = activeVehicle {
                        if activeVehicle.privateKmYearlyLimit > 0 {
                            privateKmWarningCard(limit: activeVehicle.privateKmYearlyLimit, current: stats.ytdPriveKm)
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
                    
                    // 7. Voertuig Odometer Overzicht
                    if !vehicles.isEmpty {
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
                .padding(.vertical)
            }
            .navigationTitle("dashboard_title")
            .background(Color(.systemGroupedBackground))
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !vehicles.isEmpty {
                        Menu {
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
        }
    }
    
    // MARK: - Components
    
    private var activeVehicle: Vehicle? {
        vehicles.first { $0.id == selectedVehicleId } ?? vehicles.first(where: { $0.isDefault }) ?? vehicles.first
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
    
    private func privateKmWarningCard(limit: Int, current: Double) -> some View {
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
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
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
    
    private func reviewCard(title: String, message: String, buttonTitle: String, color: Color, action: @escaping () -> Void) -> some View {
        VStack(spacing: 12) {
            Text(title)
                .font(.headline)
                .foregroundStyle(color)
            Text(message)
                .font(.subheadline)
                .multilineTextAlignment(.center)
            Button(action: action) {
                Text(buttonTitle)
                    .bold()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.borderedProminent)
            .tint(color)
        }
        .padding()
        .background(color.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
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
