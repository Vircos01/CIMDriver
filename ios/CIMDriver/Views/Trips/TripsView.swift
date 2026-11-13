import SwiftUI
import SwiftData

struct TripsView: View {
    @Query(sort: \Trip.startTime, order: .reverse) private var allTrips: [Trip]
    @Query private var allChecks: [OdometerCheck]
    @Environment(\.modelContext) private var modelContext
    @State private var showingAddTrip = false
    @State private var exportURL: URL?
    
    struct PendingExport: Identifiable {
        let id = UUID()
        let trips: [Trip]
        let title: String
        let isPDF: Bool
    }
    @State private var pendingExport: PendingExport?
    @State private var undoTrips: [(trip: Trip, oldType: String, oldStatus: String)] = []
    @State private var showUndoBanner = false
    
    enum TripFilter: String, CaseIterable, Identifiable {
        case all = "Alles"
        case toReview = "Nog beoordelen"
        case merged = "Samengevoegd"
        var id: String { self.rawValue }
    }
    
    @State private var filter: TripFilter = .all
    @State private var selection = Set<Trip>()
    @State private var isEditing = false
    @State private var showingMergeAlert = false
    @State private var viewMode: String = "Lijst"
    
    private var filteredTrips: [Trip] {
        switch filter {
        case .all:
            return allTrips.filter { $0.status != TripStatus.merged.rawValue }
        case .toReview:
            return allTrips.filter { $0.status == TripStatus.toReview.rawValue }
        case .merged:
            return allTrips.filter { $0.status == TripStatus.merged.rawValue }
        }
    }
    
    private var groupedTrips: [(key: String, value: [Trip])] {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "nl_NL")
        
        let grouped = Dictionary(grouping: filteredTrips) { trip in
            formatter.string(from: trip.startTime)
        }
        
        return grouped.sorted { (a, b) in
            if let dateA = a.value.first?.startTime, let dateB = b.value.first?.startTime {
                return dateA > dateB
            }
            return false
        }
    }
    
    var body: some View {
        NavigationStack {
            VStack {
                Picker("Filter", selection: $filter) {
                    ForEach(TripFilter.allCases) { filter in
                        Text(filter.rawValue).tag(filter)
                    }
                }
                .pickerStyle(.segmented)
                .padding()
                
                Picker("Weergave", selection: $viewMode) {
                    Text("Lijst").tag("Lijst")
                    Text("Kalender").tag("Kalender")
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                
                if viewMode == "Kalender" {
                    TripsCalendarView(trips: filteredTrips)
                } else {
                    if isEditing {
                        List(selection: $selection) {
                            tripListContent
                        }
                        .environment(\.editMode, .constant(.active))
                    } else {
                        List {
                            tripListContent
                        }
                        .environment(\.editMode, .constant(.inactive))
                    }
                }
                
                if isEditing && !selection.isEmpty {
                    VStack {
                        Divider()
                        Text("\(selection.count) geselecteerd")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack {
                            Button("Samenvoegen") {
                                showingMergeAlert = true
                            }
                            .disabled(!canMergeSelectedTrips)
                            
                            Spacer()
                            Button("Zakelijk") { batchUpdate(type: "BUSINESS") }
                            Spacer()
                            Button("Woon-Werk") { batchUpdate(type: "COMMUTE") }
                            Spacer()
                            Button("Privé") { batchUpdate(type: "PRIVATE") }
                        }
                        .buttonStyle(.bordered)
                        .padding()
                    }
                    .background(Color(.systemBackground))
                }
                
                if showUndoBanner {
                    HStack {
                        Text("\(undoTrips.count) ritten bijgewerkt")
                            .font(.caption)
                            .foregroundColor(.white)
                        Spacer()
                        Button("Ongedaan maken") {
                            performUndo()
                        }
                        .font(.caption.bold())
                        .foregroundColor(.yellow)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.cimNavy)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .navigationTitle("Ritten")
            .navigationDestination(for: Trip.self) { trip in
                TripDetailView(trip: trip)
            }
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button {
                        isEditing.toggle()
                        if !isEditing { selection.removeAll() }
                    } label: {
                        Image(systemName: isEditing ? "checkmark.circle.fill" : "checkmark.circle")
                    }
                    
                    if !isEditing {
                        Menu {
                            if !groupedTrips.isEmpty {
                                Section("Per maand") {
                                    ForEach(groupedTrips, id: \.key) { group in
                                        Menu(group.key) {
                                            Button {
                                                requestExport(trips: group.value, title: group.key, isPDF: true)
                                            } label: {
                                                Label("PDF (\(group.key))", systemImage: "doc.richtext")
                                            }
                                            Button {
                                                requestExport(trips: group.value, title: group.key, isPDF: false)
                                            } label: {
                                                Label("CSV (\(group.key))", systemImage: "tablecells")
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Section("Alles") {
                                Button {
                                    requestExport(trips: filteredTrips, title: "Alles", isPDF: true)
                                } label: {
                                    Label("Exporteer alles (PDF)", systemImage: "doc.richtext")
                                }
                                Button {
                                    requestExport(trips: filteredTrips, title: "Alles", isPDF: false)
                                } label: {
                                    Label("Exporteer alles (CSV)", systemImage: "tablecells")
                                }
                            }
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                        }
                        
                        Button(action: { showingAddTrip = true }) {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
            .sheet(isPresented: $showingAddTrip) {
                ManualTripEntryView()
            }
            .sheet(item: $pendingExport) { export in
                ExportPreCheckSheet(
                    trips: export.trips,
                    periodTitle: export.title,
                    isPDF: export.isPDF,
                    onConfirm: {
                        if export.isPDF {
                            exportTripsPDF(trips: export.trips, title: export.title)
                        } else {
                            exportTripsCSV(trips: export.trips, title: export.title)
                        }
                    }
                )
            }
            .sheet(isPresented: Binding(
                get: { exportURL != nil },
                set: { if !$0 { exportURL = nil } }
            )) {
                if let url = exportURL {
                    ShareSheet(activityItems: [url])
                }
            }
            .overlay {
                if filteredTrips.isEmpty {
                    ContentUnavailableView(
                        filter == .toReview ? "Geen ritten te beoordelen" : "Geen Ritten",
                        systemImage: "car",
                        description: Text(filter == .toReview ? "Je bent helemaal bij!" : "Zodra je gaat rijden met een verbonden carkit, verschijnen hier je ritten.")
                    )
                }
            }
            .alert("Ritten samenvoegen", isPresented: $showingMergeAlert) {
                Button("Annuleer", role: .cancel) { }
                Button("Samenvoegen") {
                    mergeSelectedTrips()
                }
                .disabled(!canMergeSelectedTrips)
            } message: {
                Text(mergeValidationMessage)
            }
        }
    }
    
    private func deleteTrips(at offsets: IndexSet, in group: [Trip]) {
        for index in offsets {
            let trip = group[index]
            modelContext.delete(trip)
        }
        try? modelContext.save()
    }
    
    private func batchUpdate(type: String) {
        undoTrips = selection.map { ($0, $0.tripType, $0.status) }
        for trip in selection {
            trip.tripType = type
            trip.status = TripStatus.done.rawValue
        }
        try? modelContext.save()
        isEditing = false
        selection.removeAll()
        withAnimation {
            showUndoBanner = true
        }
    }
    
    private func performUndo() {
        for (trip, oldType, oldStatus) in undoTrips {
            trip.tripType = oldType
            trip.status = oldStatus
        }
        try? modelContext.save()
        undoTrips.removeAll()
        withAnimation {
            showUndoBanner = false
        }
    }
    
    private func requestExport(trips: [Trip], title: String, isPDF: Bool) {
        pendingExport = PendingExport(trips: trips, title: title, isPDF: isPDF)
    }
    
    private func mergeSelectedTrips() {
        let tripsToMerge = selection.sorted { $0.startTime < $1.startTime }
        guard canMergeSelectedTrips else { return }
        guard let primaryTrip = tripsToMerge.first else { return }
        
        let mergedTrips = Array(tripsToMerge.dropFirst())
        let sortedPoints = tripsToMerge
            .flatMap(\.locationPoints)
            .sorted { $0.timestamp < $1.timestamp }
        let distinctNotes = Array(NSOrderedSet(array: tripsToMerge.compactMap(\.note).filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty })) as? [String] ?? []
        let dominantType = tripsToMerge.first(where: { $0.tripType != "PRIVATE" })?.tripType ?? primaryTrip.tripType
        
        primaryTrip.startTime = tripsToMerge.first?.startTime ?? primaryTrip.startTime
        primaryTrip.endTime = tripsToMerge.compactMap(\.endTime).max() ?? primaryTrip.endTime
        primaryTrip.startAddress = tripsToMerge.first?.startAddress ?? primaryTrip.startAddress
        primaryTrip.endAddress = tripsToMerge.last?.endAddress ?? primaryTrip.endAddress
        primaryTrip.distanceMeters = tripsToMerge.reduce(0) { $0 + $1.distanceMeters }
        primaryTrip.odometerStart = tripsToMerge.map(\.odometerStart).min() ?? primaryTrip.odometerStart
        primaryTrip.odometerEnd = tripsToMerge.compactMap(\.odometerEnd).max() ?? primaryTrip.odometerEnd
        primaryTrip.tripType = dominantType
        primaryTrip.note = distinctNotes.isEmpty ? nil : distinctNotes.joined(separator: "\n")
        primaryTrip.status = TripStatus.toReview.rawValue
        primaryTrip.locationPoints.removeAll()
        
        for point in sortedPoints {
            point.trip = primaryTrip
            primaryTrip.locationPoints.append(point)
        }
        
        for trip in mergedTrips {
            trip.status = TripStatus.merged.rawValue
            trip.note = "Samengevoegd in rit van \(primaryTrip.startTime.formatted(date: .abbreviated, time: .shortened))"
        }
        
        try? modelContext.save()
        isEditing = false
        selection.removeAll()
    }
    
    private var canMergeSelectedTrips: Bool {
        let tripsToMerge = Array(selection)
        guard tripsToMerge.count >= 2 else { return false }
        guard tripsToMerge.allSatisfy({ $0.status != TripStatus.active.rawValue && $0.status != TripStatus.merged.rawValue }) else { return false }
        
        let vehicleIDs = Set(tripsToMerge.compactMap { $0.vehicle?.id })
        return vehicleIDs.count <= 1
    }
    
    private var mergeValidationMessage: String {
        let tripsToMerge = Array(selection)
        if tripsToMerge.count < 2 {
            return "Selecteer minimaal twee ritten om samen te voegen."
        }
        if tripsToMerge.contains(where: { $0.status == TripStatus.active.rawValue }) {
            return "Actieve ritten kunnen niet worden samengevoegd."
        }
        if tripsToMerge.contains(where: { $0.status == TripStatus.merged.rawValue }) {
            return "Reeds samengevoegde ritten kunnen niet opnieuw worden samengevoegd."
        }
        let vehicleIDs = Set(tripsToMerge.compactMap { $0.vehicle?.id })
        if vehicleIDs.count > 1 {
            return "Alleen ritten van hetzelfde voertuig kunnen worden samengevoegd."
        }
        return "Wil je de geselecteerde ritten samenvoegen tot één rit?"
    }
    
    private func mergedNote(for trips: [Trip]) -> String? {
        let notes = trips.compactMap(\.note).filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        guard !notes.isEmpty else { return nil }
        return notes.joined(separator: "\n")
    }
    
    private func exportTripsCSV(trips: [Trip]? = nil, title: String? = nil) {
        let targetTrips = trips ?? filteredTrips
        exportURL = ExportUtil.generateTripsCSV(trips: targetTrips, periodTitle: title)
    }
    
    private func exportTripsPDF(trips: [Trip]? = nil, title: String? = nil) {
        let targetTrips = trips ?? filteredTrips
        let minTime = targetTrips.map(\.startTime).min() ?? Date.distantPast
        let maxTime = targetTrips.compactMap(\.endTime).max() ?? (targetTrips.map(\.startTime).max() ?? Date.distantFuture)
        let relevantChecks = allChecks.filter { $0.timestamp >= minTime.addingTimeInterval(-86400) && $0.timestamp <= maxTime.addingTimeInterval(86400) }
        exportURL = ExportUtil.generateTripsPDF(trips: targetTrips, periodTitle: title, odometerChecks: relevantChecks)
    }
    
    @ViewBuilder
    private var tripListContent: some View {
        ForEach(groupedTrips, id: \.key) { group in
            Section {
                ForEach(group.value) { trip in
                    NavigationLink(value: trip) {
                        TripRowView(trip: trip)
                    }
                    .tag(trip)
                }
                .onDelete { offsets in
                    deleteTrips(at: offsets, in: group.value)
                }
            } header: {
                HStack {
                    Text(group.key)
                        .font(.headline)
                    Spacer()
                    Menu {
                        Button {
                            requestExport(trips: group.value, title: group.key, isPDF: true)
                        } label: {
                            Label("Exporteer PDF (\(group.key))", systemImage: "doc.richtext")
                        }
                        Button {
                            requestExport(trips: group.value, title: group.key, isPDF: false)
                        } label: {
                            Label("Exporteer CSV (\(group.key))", systemImage: "tablecells")
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "square.and.arrow.up")
                            Text("Deel")
                                .font(.caption)
                        }
                        .foregroundColor(.accentColor)
                    }
                }
            }
        }
    }
}

struct TripRowView: View {
    let trip: Trip
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(trip.startTime.formatted(date: .abbreviated, time: .shortened))
                    .font(.subheadline.bold())
                Spacer()
                
                if trip.status == TripStatus.toReview.rawValue {
                    Text("Nog beoordelen")
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.orange.opacity(0.2))
                        .foregroundStyle(.orange)
                        .clipShape(Capsule())
                } else {
                    let typeText = trip.tripType == "BUSINESS" ? "Zakelijk" : (trip.tripType == "COMMUTE" ? "Woon-Werk" : "Privé")
                    let color = trip.tripType == "BUSINESS" ? Color.cimNavy : (trip.tripType == "COMMUTE" ? Color.blue : Color.orange)
                    Text(typeText)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(color.opacity(0.2))
                        .foregroundStyle(color)
                        .clipShape(Capsule())
                }
            }
            
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(trip.startAddress ?? "Onbekend startadres")
                        .font(.subheadline)
                    Text(trip.endAddress ?? "Onbekend eindadres")
                        .font(.subheadline)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(String(format: "%.1f km", Double(trip.distanceMeters) / 1000.0))
                        .font(.headline)
                    if trip.privateDetourMeters > 0 {
                        Text("• waarvan \(String(format: "%.1f", Double(trip.privateDetourMeters) / 1000.0)) km privé")
                            .font(.caption2)
                            .foregroundStyle(.orange)
                    }
                }
            }
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

struct AutoPreviewProviderTripsView1: PreviewProvider {
    static var previews: some View {
            TripsView()
                .modelContainer(PreviewContainer.shared.container)
    }
}
