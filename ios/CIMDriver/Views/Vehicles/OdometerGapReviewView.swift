import SwiftUI
import SwiftData

struct OdometerGapReviewView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    let vehicle: Vehicle
    
    @Query private var allTrips: [Trip]
    @Query private var allChecks: [OdometerCheck]
    @Query private var allResolutions: [OdometerGapResolution]
    
    @State private var selectedTab: Int = 0 // 0 = Openstaand, 1 = Historie / Opgelost
    @State private var selectedGapForResolution: OdometerGap?
    @State private var recalculationPreview: RecalculationPreview?
    @State private var showingPreviewSheet = false
    @State private var bannerMessage: String?
    @State private var showingBanner = false
    
    private var vehicleTrips: [Trip] {
        allTrips.filter { $0.vehicle?.id == vehicle.id }
    }
    
    private var vehicleChecks: [OdometerCheck] {
        allChecks.filter { $0.vehicleId == vehicle.id }
    }
    
    private var vehicleResolutions: [OdometerGapResolution] {
        allResolutions
            .filter { $0.vehicleId == vehicle.id }
            .sorted { $0.detectedAt > $1.detectedAt }
    }
    
    private var detectedGaps: [OdometerGap] {
        OdometerGapAnalyzer.analyze(vehicle: vehicle, trips: vehicleTrips)
    }
    
    private var totalGapKm: Int {
        detectedGaps.reduce(0) { $0 + abs($1.gapKm) }
    }
    
    private var smallGapsCount: Int {
        detectedGaps.filter { abs($0.gapKm) <= 5 }.count
    }
    
    var body: some View {
        VStack(spacing: 0) {
            Picker("Tabs", selection: $selectedTab) {
                Text("Openstaand (\(detectedGaps.count))").tag(0)
                Text("Historie (\(vehicleResolutions.count))").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()
            
            if let banner = bannerMessage, showingBanner {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.white)
                    Text(banner)
                        .font(.subheadline)
                        .foregroundColor(.white)
                    Spacer()
                    Button(action: { showingBanner = false }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.white)
                    }
                }
                .padding()
                .background(Color.cimGreen)
                .transition(.move(edge: .top).combined(with: .opacity))
            }
            
            if selectedTab == 0 {
                openGapsListView
            } else {
                historyListView
            }
        }
        .navigationTitle("Gaten in \(vehicle.name)")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: triggerRecalculatePreview) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                }
            }
        }
        .sheet(item: $selectedGapForResolution) { gap in
            NavigationStack {
                ResolveGapSheet(gap: gap, vehicle: vehicle) { fixType, note, startAddr, endAddr, tripType in
                    resolveGap(
                        gap: gap,
                        fixType: fixType,
                        note: note,
                        customStartAddress: startAddr,
                        customEndAddress: endAddr,
                        tripType: tripType
                    )
                }
            }
        }
        .sheet(isPresented: $showingPreviewSheet) {
            if let preview = recalculationPreview {
                NavigationStack {
                    RecalculateConfirmationSheet(preview: preview) {
                        applyRecalculation(preview: preview)
                    }
                }
            }
        }
    }
    
    // MARK: - Open Gaps List
    
    private var openGapsListView: some View {
        List {
            // Status Banner
            Section {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 12) {
                        Image(systemName: detectedGaps.isEmpty ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                            .font(.title2)
                            .foregroundColor(detectedGaps.isEmpty ? .cimGreen : .red)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(detectedGaps.isEmpty ? "Km-registratie is 100% sluitend!" : "\(detectedGaps.count) gaten gevonden (\(totalGapKm) km)")
                                .font(.headline)
                                .foregroundColor(detectedGaps.isEmpty ? .cimGreen : .primary)
                            Text(detectedGaps.isEmpty ? "Alle ritten sluiten naadloos op elkaar aan. Voldoet aan Belastingdienst eisen." : "De beginstand van ritten wijkt af van de vorige eindstand.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    if smallGapsCount > 0 {
                        Button(action: batchResolveSmallGaps) {
                            HStack {
                                Image(systemName: "wand.and.stars")
                                Text("Los alle kleine gaten op (< 5 km) (\(smallGapsCount))")
                                    .bold()
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.cimGreen)
                        .padding(.top, 4)
                    }
                }
                .padding(.vertical, 4)
            }
            
            // Recalculate Button Section
            Section {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 12) {
                        Image(systemName: "plus.forwardslash.minus")
                            .font(.title3)
                            .foregroundColor(.cimGreen)
                            .padding(8)
                            .background(Color.cimGreen.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("KMs opnieuw doorberekenen")
                                .font(.headline)
                            Text("Herbereken alle tellerstanden chronologisch op basis van ritten en km-controles.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Button(action: triggerRecalculatePreview) {
                        HStack {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Tellerstanden herberekenen")
                                .bold()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.cimNavy)
                }
                .padding(.vertical, 4)
            }
            
            // Gap Items
            if detectedGaps.isEmpty {
                Section {
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.shield.fill")
                            .font(.system(size: 44))
                            .foregroundColor(.cimGreen)
                        Text("Geen gaten gevonden")
                            .font(.headline)
                        Text("Jouw kilometeradministratie voor dit voertuig is compleet en aaneengesloten.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                }
            } else {
                Section("Gedetecteerde verschillen") {
                    ForEach(detectedGaps) { gap in
                        GapRowView(gap: gap) {
                            selectedGapForResolution = gap
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - History List
    
    private var historyListView: some View {
        List {
            if vehicleResolutions.isEmpty {
                Section {
                    VStack(spacing: 12) {
                        Image(systemName: "clock")
                            .font(.system(size: 44))
                            .foregroundColor(.secondary)
                        Text("Nog geen opgeloste gaten")
                            .font(.headline)
                        Text("Zodra je gaten oplost of toelichtingen toevoegt, verschijnen ze hier in het logboek.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                }
            } else {
                ForEach(vehicleResolutions) { res in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(resolutionTitle(for: res.resolution))
                                .font(.headline)
                            Spacer()
                            Text(res.status)
                                .font(.caption.bold())
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.cimGreen.opacity(0.15))
                                .foregroundColor(.cimGreen)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                        
                        Text("Gat van \(res.gapKm) km (\(res.detectedAt.formatted(date: .abbreviated, time: .shortened)))")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        if let note = res.resolutionNote, !note.isEmpty {
                            Text("Toelichting: \(note)")
                                .font(.caption)
                                .bold()
                                .foregroundColor(.primary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
    
    private func resolutionTitle(for resolution: String) -> String {
        switch resolution {
        case "CREATE_PRIVATE_TRIP": return "Rit aangemaakt"
        case "ADJUST_SINGLE_TRIP": return "Rit aangepast"
        case "SYNC_VEHICLE_ODOMETER": return "Actuele stand gelijkgezet"
        case "MARK_AS_EXPLAINED": return "Verklaard (Belastingdienst)"
        default: return "Genegeerd"
        }
    }
    
    // MARK: - Actions
    
    private func triggerRecalculatePreview() {
        let preview = OdometerRecalculator.preview(
            vehicle: vehicle,
            trips: vehicleTrips,
            odometerChecks: vehicleChecks,
            resolutions: vehicleResolutions
        )
        self.recalculationPreview = preview
        self.showingPreviewSheet = true
    }
    
    private func applyRecalculation(preview: RecalculationPreview) {
        OdometerRecalculator.apply(preview: preview, vehicle: vehicle, in: modelContext)
        showingPreviewSheet = false
        showBanner("\(preview.modifiedTripsCount) ritten succesvol herberekend!")
    }
    
    private func batchResolveSmallGaps() {
        let smallGaps = detectedGaps.filter { abs($0.gapKm) <= 5 }
        var resolvedCount = 0
        for gap in smallGaps {
            if gap.gapKm > 0 {
                resolveGap(
                    gap: gap,
                    fixType: .createPrivateTrip,
                    note: "Automatische aansluiting (< 5 km)",
                    customStartAddress: nil,
                    customEndAddress: nil,
                    tripType: "PERSONAL"
                )
            } else {
                resolveGap(
                    gap: gap,
                    fixType: .adjustSingleTrip,
                    note: "Automatische correctie overlap (< 5 km)",
                    customStartAddress: nil,
                    customEndAddress: nil,
                    tripType: "PERSONAL"
                )
            }
            resolvedCount += 1
        }
        showBanner("\(resolvedCount) kleine gaten automatisch gecorrigeerd!")
    }
    
    private func resolveGap(
        gap: OdometerGap,
        fixType: FixType,
        note: String?,
        customStartAddress: String?,
        customEndAddress: String?,
        tripType: String
    ) {
        var affectedIds: [UUID] = []
        let now = Date()
        
        switch fixType {
        case .createPrivateTrip:
            let startAddr = customStartAddress?.isEmpty == false ? customStartAddress! : (gap.previousTrip?.endAddress ?? "Laatste bekende bestemming")
            let endAddr = customEndAddress?.isEmpty == false ? customEndAddress! : (gap.nextTrip?.startAddress ?? startAddr)
            let defaultNote = (tripType == "BUSINESS") ? "Zakelijke rit voor sluitende tellerstand" : "Privérit voor sluitende tellerstand"
            
            let tStart = min(gap.gapTimeStart, gap.gapTimeEnd)
            var tEnd = max(gap.gapTimeStart, gap.gapTimeEnd)
            if tEnd <= tStart {
                let distanceKm = Double(abs(gap.gapKm))
                let durationSec = max(300, min(3600, distanceKm * 60))
                tEnd = tStart.addingTimeInterval(durationSec)
            }
            
            let trip = Trip(
                vehicle: vehicle,
                startTime: tStart,
                endTime: tEnd,
                startAddress: startAddr,
                endAddress: endAddr,
                distanceMeters: abs(gap.gapKm) * 1000,
                tripType: tripType,
                note: note?.isEmpty == false ? note : defaultNote,
                status: "APPROVED",
                isManual: true,
                odometerStart: gap.expectedOdometer,
                odometerEnd: gap.actualOdometer
            )
            modelContext.insert(trip)
            affectedIds.append(trip.id)
            
        case .adjustSingleTrip:
            if let prev = gap.previousTrip {
                let prevDistKm = Int(round(Double(prev.distanceMeters) / 1000.0))
                let newDist = max(1, prevDistKm + gap.gapKm)
                prev.distanceMeters = newDist * 1000
                prev.odometerEnd = prev.odometerStart + newDist
                affectedIds.append(prev.id)
            } else if let next = gap.nextTrip {
                let nextDistKm = Int(round(Double(next.distanceMeters) / 1000.0))
                let newDist = max(1, nextDistKm - gap.gapKm)
                let nextEnd = next.odometerEnd ?? (next.odometerStart + nextDistKm)
                next.distanceMeters = newDist * 1000
                next.odometerStart = nextEnd - newDist
                affectedIds.append(next.id)
            }
            
        case .syncVehicleOdometer:
            vehicle.odometerCurrent = gap.expectedOdometer
            
        case .markAsExplained, .ignore:
            break
        }
        
        let resolution = OdometerGapResolution(
            vehicleId: vehicle.id,
            detectedAt: now,
            resolvedAt: now,
            gapType: gap.gapType.rawValue,
            gapStartTimestamp: gap.gapTimeStart,
            gapEndTimestamp: gap.gapTimeEnd,
            expectedOdometer: gap.expectedOdometer,
            actualOdometer: gap.actualOdometer,
            gapKm: gap.gapKm,
            resolution: fixType.rawValue,
            resolutionNote: note,
            affectedTripIds: affectedIds.isEmpty ? nil : affectedIds.map { $0.uuidString }.joined(separator: ","),
            status: fixType == .ignore ? "IGNORED" : "RESOLVED"
        )
        modelContext.insert(resolution)
        
        if fixType != .syncVehicleOdometer {
            let latest = vehicleTrips.max(by: { $0.startTime < $1.startTime })
            let latestEnd = latest.map { OdometerGapAnalyzer.effectiveOdometerEnd(trip: $0) }
            if let end = latestEnd, end > vehicle.odometerCurrent {
                vehicle.odometerCurrent = end
            }
        }
        
        try? modelContext.save()
        showBanner("Gat succesvol opgelost!")
    }
    
    private func showBanner(_ message: String) {
        withAnimation {
            bannerMessage = message
            showingBanner = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            withAnimation {
                showingBanner = false
            }
        }
    }
}

// MARK: - GapRowView

struct GapRowView: View {
    let gap: OdometerGap
    let onResolve: () -> Void
    
    private var badgeColor: Color {
        switch gap.gapType {
        case .missingKm: return .red
        case .missingTrip: return Color(red: 0.76, green: 0.25, blue: 0.05)
        case .overlappingKm: return .orange
        case .odometerReset: return .purple
        case .firstTripGap: return .blue
        case .currentStandGap: return .orange
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(gap.gapType.localizedTitle)
                    .font(.caption.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(badgeColor.opacity(0.12))
                    .foregroundColor(badgeColor)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                
                Spacer()
                
                Text("\(gap.gapKm > 0 ? "+" : "")\(gap.gapKm) km")
                    .font(.headline.bold())
                    .foregroundColor(badgeColor)
            }
            
            HStack(spacing: 6) {
                Image(systemName: "calendar")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("\(gap.gapTimeStart.formatted(date: .abbreviated, time: .shortened)) - \(gap.gapTimeEnd.formatted(date: .abbreviated, time: .shortened))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            HStack(spacing: 6) {
                Image(systemName: "gauge")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("Stand springt van \(gap.expectedOdometer) km naar \(gap.actualOdometer) km")
                    .font(.caption.bold())
                    .foregroundColor(.primary)
            }
            
            if gap.previousTrip != nil || gap.nextTrip != nil {
                VStack(alignment: .leading, spacing: 4) {
                    if let prev = gap.previousTrip {
                        let prevKm = Int(round(Double(prev.distanceMeters) / 1000.0))
                        Text("Vorige rit: \(prev.startAddress ?? "Onbekend") → \(prev.endAddress ?? "Onbekend") (\(prevKm) km, eindstand \(OdometerGapAnalyzer.effectiveOdometerEnd(trip: prev)) km)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    if let next = gap.nextTrip {
                        let nextKm = Int(round(Double(next.distanceMeters) / 1000.0))
                        Text("Volgende rit: \(next.startAddress ?? "Onbekend") → \(next.endAddress ?? "Onbekend") (\(nextKm) km, beginstand \(next.odometerStart) km)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(8)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            
            HStack {
                Spacer()
                Button(action: onResolve) {
                    HStack(spacing: 6) {
                        Image(systemName: "wrench.and.screwdriver.fill")
                        Text("Oplossen")
                            .bold()
                    }
                    .font(.subheadline)
                }
                .buttonStyle(.borderedProminent)
                .tint(.cimGreen)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - ResolveGapSheet

struct ResolveGapSheet: View {
    @Environment(\.dismiss) private var dismiss
    let gap: OdometerGap
    let vehicle: Vehicle
    let onApply: (FixType, String?, String?, String?, String) -> Void
    
    @State private var selectedFix: FixType
    @State private var tripType: String = "PERSONAL"
    @State private var startAddress: String
    @State private var endAddress: String
    @State private var note: String = ""
    
    init(gap: OdometerGap, vehicle: Vehicle, onApply: @escaping (FixType, String?, String?, String?, String) -> Void) {
        self.gap = gap
        self.vehicle = vehicle
        self.onApply = onApply
        
        let initialFix = gap.suggestedFixes.first?.type ?? .createPrivateTrip
        _selectedFix = State(initialValue: initialFix)
        
        let prevEnd = gap.previousTrip?.endAddress ?? "Onbekend vertrekpunt"
        let nextStart = gap.nextTrip?.startAddress ?? prevEnd
        _startAddress = State(initialValue: prevEnd)
        _endAddress = State(initialValue: prevEnd == nextStart && !prevEnd.starts(with: "Onbekend") ? "\(prevEnd) (retour)" : nextStart)
    }
    
    var body: some View {
        Form {
            Section("Verschil details") {
                LabeledContent("Tellerprong", value: "\(gap.expectedOdometer) → \(gap.actualOdometer) km")
                LabeledContent("Verschil", value: "\(gap.gapKm > 0 ? "+" : "")\(gap.gapKm) km")
                Text("Afstand en tellerstanden worden exact sluitend vastgelegd op \(abs(gap.gapKm)) km.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Section("Kies oplossing") {
                ForEach(gap.suggestedFixes) { fix in
                    Button(action: { selectedFix = fix.type }) {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: selectedFix == fix.type ? "largecircle.fill.circle" : "circle")
                                .foregroundColor(selectedFix == fix.type ? .cimGreen : .secondary)
                                .font(.title3)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(fix.title)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text(fix.description)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.vertical, 4)
                }
            }
            
            if selectedFix == .createPrivateTrip {
                Section("Ritdetails") {
                    Picker("Categorie", selection: $tripType) {
                        Text("Privé").tag("PERSONAL")
                        Text("Zakelijk").tag("BUSINESS")
                        Text("Woon-werk").tag("COMMUTE")
                    }
                    .pickerStyle(.segmented)
                    
                    TextField("Vertreklocatie", text: $startAddress)
                    TextField("Bestemming", text: $endAddress)
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Snelle suggesties (\(abs(gap.gapKm)) km):")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        HStack {
                            Button("Boodschappen") {
                                endAddress = "Supermarkt / Boodschappen"
                                note = "Boodschappen"
                                tripType = "PERSONAL"
                            }
                            .buttonStyle(.bordered)
                            .font(.caption)
                            
                            Button("Klantbezoek") {
                                endAddress = "Klant / Afspraak"
                                note = "Klantbezoek"
                                tripType = "BUSINESS"
                            }
                            .buttonStyle(.bordered)
                            .font(.caption)
                            
                            Button("Tanken") {
                                endAddress = "Tankstation / Wasstraat"
                                note = "Tanken / wassen"
                            }
                            .buttonStyle(.bordered)
                            .font(.caption)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            
            if selectedFix == .syncVehicleOdometer {
                Section("Tellerstand synchroniseren") {
                    Text("De actuele tellerstand van het voertuig (\(gap.actualOdometer) km) wordt direct gelijkgezet aan \(gap.expectedOdometer) km (de eindstand van de vorige rit). Er worden geen ritten toegevoegd of verwijderd.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
            
            if selectedFix == .markAsExplained {
                Section("Verklaringen") {
                    HStack {
                        Button("APK / Keuring") { note = "APK / Keuring" }
                            .buttonStyle(.bordered)
                            .font(.caption)
                        Button("Garage / Onderhoud") { note = "Garage / Onderhoud" }
                            .buttonStyle(.bordered)
                            .font(.caption)
                        Button("Proefrit monteur") { note = "Proefrit monteur" }
                            .buttonStyle(.bordered)
                            .font(.caption)
                    }
                }
            }
            
            Section("Notitie / Toelichting (optioneel)") {
                TextField("Bijv. Boodschappen, APK, onderhoud...", text: $note)
            }
        }
        .navigationTitle("Gat oplossen")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Annuleren") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Toepassen") {
                    onApply(
                        selectedFix,
                        note.isEmpty ? nil : note,
                        startAddress.isEmpty ? nil : startAddress,
                        endAddress.isEmpty ? nil : endAddress,
                        tripType
                    )
                    dismiss()
                }
                .bold()
                .tint(.cimGreen)
            }
        }
    }
}

// MARK: - RecalculateConfirmationSheet

struct RecalculateConfirmationSheet: View {
    @Environment(\.dismiss) private var dismiss
    let preview: RecalculationPreview
    let onConfirm: () -> Void
    
    var body: some View {
        Form {
            Section("Samenvatting doorberekening") {
                LabeledContent("Totaal aantal ritten", value: "\(preview.totalTrips)")
                LabeledContent("Ritten met gecorrigeerde stand", value: "\(preview.modifiedTripsCount)")
                    .foregroundColor(preview.modifiedTripsCount > 0 ? .orange : .primary)
                LabeledContent("Behouden kilometerijkers (foto's)", value: "\(preview.preservedChecksCount)")
                LabeledContent("Huidige tellerstand", value: "\(preview.oldCurrentOdometer) → \(preview.newCurrentOdometer) km")
                LabeledContent("Resterende gaten na berekening", value: "\(preview.remainingGaps.count)")
                    .foregroundColor(preview.remainingGaps.isEmpty ? .cimGreen : .orange)
            }
            
            if preview.modifiedTrips.isEmpty {
                Section {
                    Text("Alle ritten staan al exact chronologisch in lijn met de tellerstanden. Er hoeven geen ritten te worden aangepast.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            } else {
                Section("Gewijzigde ritten (\(preview.modifiedTrips.count))") {
                    ForEach(preview.modifiedTrips) { mod in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(mod.startTime.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption.bold())
                                Spacer()
                                Text("\(mod.distanceKm) km")
                                    .font(.caption.bold())
                            }
                            Text("\(mod.startAddress ?? "Onbekend") → \(mod.endAddress ?? "Onbekend")")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack {
                                Text("Oud: \(mod.oldStart) - \(mod.oldEnd) km")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text("Nieuw: \(mod.newStart) - \(mod.newEnd) km")
                                    .font(.caption2.bold())
                                    .foregroundColor(.cimGreen)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }
            }
        }
        .navigationTitle("Herberekenen")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Annuleren") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Toepassen") {
                    onConfirm()
                }
                .bold()
                .tint(.cimGreen)
                .disabled(preview.modifiedTripsCount == 0 && preview.oldCurrentOdometer == preview.newCurrentOdometer)
            }
        }
    }
}
