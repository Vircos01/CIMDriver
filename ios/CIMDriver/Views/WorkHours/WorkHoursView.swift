import SwiftUI
import SwiftData

struct WorkHoursView: View {
    @Query(sort: \WorkDay.date, order: .reverse) private var workDays: [WorkDay]
    @Query private var trips: [Trip]
    @Query private var savedAddresses: [SavedAddress]
    @Query private var settingsList: [AppSettings]
    @Environment(\.modelContext) private var modelContext
    @State private var showingAddWorkDay = false
    @State private var exportURL: URL?
    @State private var searchText = ""
    @State private var lastApprovedWorkDay: WorkDay? = nil
    @State private var undoDepTime: Date? = nil
    @State private var undoRoundedDepTime: Date? = nil
    @State private var undoBreakMins: Int = 0
    @State private var undoProjectCode: String? = nil
    @State private var undoStatus: String = "TO_REVIEW"
    @State private var showUndoBanner = false
    
    private var groupedWorkDays: [(key: String, value: [WorkDay])] {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "nl_NL")
        
        let filteredWorkDays = workDays.filter { workDay in
            searchText.isEmpty || [workDay.projectCode, workDay.workLocationLabel, workDay.note]
                .compactMap { $0 }
                .contains { $0.localizedCaseInsensitiveContains(searchText) }
        }
        let grouped = Dictionary(grouping: filteredWorkDays) { wd in
            formatter.string(from: wd.date)
        }
        
        return grouped.sorted { (a, b) in
            if let dateA = a.value.first?.date, let dateB = b.value.first?.date {
                return dateA > dateB
            }
            return false
        }
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if showUndoBanner {
                    undoBanner
                }

                List {
                    ForEach(groupedWorkDays, id: \.key) { group in
                        Section {
                            ForEach(group.value) { workDay in
                                NavigationLink(value: workDay) {
                                    let dayTrips = trips.filter { Calendar.current.isDate($0.startTime, inSameDayAs: workDay.date) }
                                    WorkDayRowView(
                                        workDay: workDay,
                                        tripsOnDay: dayTrips,
                                        settings: settingsList.first,
                                        savedAddresses: savedAddresses,
                                        onApprove: { suggestion in
                                            quickApprove(workDay: workDay, suggestion: suggestion)
                                        }
                                    )
                                }
                            }
                            .onDelete { offsets in
                                deleteWorkDays(at: offsets, in: group.value)
                            }
                        } header: {
                            HStack {
                                let hours = totalHours(for: group.value)
                                Text("\(group.key) • \(String(format: "%.1f", hours))u")
                                    .font(.headline)
                                Spacer()
                                Menu {
                                    Button {
                                        exportWorkDaysPDF(workDays: group.value, title: group.key)
                                    } label: {
                                        Label("Exporteer PDF (\(group.key))", systemImage: "doc.richtext")
                                    }
                                    Button {
                                        exportWorkDaysCSV(workDays: group.value, title: group.key)
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
            .navigationTitle("Werkuren")
            .searchable(text: $searchText, prompt: "Zoek op project, locatie of notitie")
            .navigationDestination(for: WorkDay.self) { workDay in
                ManualWorkDayEntryView(workDayToEdit: workDay)
            }
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Menu {
                        Button {
                            exportWorkDaysCSV()
                        } label: {
                            Label("Exporteer CSV", systemImage: "tablecells")
                        }
                        
                        Button {
                            exportWorkDaysPDF()
                        } label: {
                            Label("Exporteer PDF", systemImage: "doc.richtext")
                        }
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                    }
                    
                    Button(action: { showingAddWorkDay = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingAddWorkDay) {
                ManualWorkDayEntryView()
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
                if workDays.isEmpty {
                    ContentUnavailableView(
                        "Geen Werkuren",
                        systemImage: "clock",
                        description: Text("Je hebt nog geen werkuren geregistreerd.")
                    )
                }
            }
        }
    }

    private var undoBanner: some View {
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
        .padding(12)
        .background(Color.orange.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private func quickApprove(workDay: WorkDay, suggestion: WorkDaySuggestion) {
        lastApprovedWorkDay = workDay
        undoDepTime = workDay.departureTime
        undoRoundedDepTime = workDay.roundedDepartureTime
        undoBreakMins = workDay.breakMinutes
        undoProjectCode = workDay.projectCode
        undoStatus = workDay.status

        let effectiveDep = workDay.departureTime ?? suggestion.suggestedDepartureTime
        workDay.departureTime = effectiveDep
        workDay.roundedDepartureTime = workDay.roundedDepartureTime ?? effectiveDep
        if workDay.breakMinutes == 0 {
            workDay.breakMinutes = suggestion.suggestedBreakMinutes
        }
        if workDay.projectCode == nil || workDay.projectCode?.isEmpty == true {
            workDay.projectCode = suggestion.suggestedProjectCode
        }
        workDay.status = "APPROVED"
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
    
    private func deleteWorkDays(at offsets: IndexSet, in group: [WorkDay]) {
        for index in offsets {
            let wd = group[index]
            modelContext.delete(wd)
        }
        try? modelContext.save()
    }
    
    private func totalHours(for days: [WorkDay]) -> Double {
        days.reduce(0.0) { sum, wd in
            if let dep = wd.roundedDepartureTime ?? wd.departureTime {
                let arr = wd.roundedArrivalTime ?? wd.arrivalTime
                let sec = max(0, dep.timeIntervalSince(arr))
                let min = max(0, (sec / 60.0) - Double(wd.breakMinutes))
                return sum + (min / 60.0)
            }
            return sum
        }
    }
    
    private func exportWorkDaysCSV(workDays: [WorkDay]? = nil, title: String? = nil) {
        let daysToExport = workDays ?? self.workDays
        exportURL = ExportUtil.generateWorkDaysCSV(workDays: daysToExport, periodTitle: title)
    }
    
    private func exportWorkDaysPDF(workDays: [WorkDay]? = nil, title: String? = nil) {
        let daysToExport = workDays ?? self.workDays
        exportURL = ExportUtil.generateWorkDaysPDF(workDays: daysToExport, periodTitle: title)
    }
}

struct WorkDayRowView: View {
    let workDay: WorkDay
    var tripsOnDay: [Trip] = []
    var settings: AppSettings? = nil
    var savedAddresses: [SavedAddress] = []
    var onApprove: ((WorkDaySuggestion) -> Void)? = nil

    private var suggestion: WorkDaySuggestion {
        WorkHoursTriage.evaluateWorkDay(
            workDay: workDay,
            tripsOnDay: tripsOnDay,
            settings: settings,
            savedAddresses: savedAddresses
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(workDay.date.formatted(date: .abbreviated, time: .omitted))
                    .font(.subheadline.bold())
                Spacer()
                let isHigh = suggestion.isHighConfidence
                Text("\(suggestion.confidenceScore)% zeker")
                    .font(.caption2.bold())
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(isHigh ? Color.cimGreen.opacity(0.15) : Color.orange.opacity(0.15))
                    .foregroundColor(isHigh ? Color.cimGreen : Color.orange)
                    .clipShape(Capsule())
            }

            // Plain language status badge
            let isApproved = workDay.status == "APPROVED" || workDay.status == "DONE"
            let isHighConf = isApproved || suggestion.isHighConfidence
            HStack(spacing: 6) {
                Image(systemName: isHighConf ? "checkmark.circle.fill" : "info.circle.fill")
                    .foregroundColor(isHighConf ? .cimGreen : .orange)
                    .font(.caption2)
                Text(suggestion.statusText)
                    .font(.caption)
                    .foregroundColor(isHighConf ? .cimGreen : .orange)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background((isHighConf ? Color.cimGreen : Color.orange).opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 6))

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Start: \(workDay.roundedArrivalTime?.formatted(date: .omitted, time: .shortened) ?? workDay.arrivalTime.formatted(date: .omitted, time: .shortened))")
                        .font(.subheadline)
                    if let depTime = workDay.roundedDepartureTime ?? workDay.departureTime {
                        Text("Einde: \(depTime.formatted(date: .omitted, time: .shortened))")
                            .font(.subheadline)
                    } else {
                        Text("Einde: -")
                            .font(.subheadline)
                    }
                    Text("Pauze: \(workDay.breakMinutes) min")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    if let code = workDay.projectCode, !code.isEmpty {
                        Text("Project: \(code)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
                if let depTime = workDay.roundedDepartureTime ?? workDay.departureTime {
                    let arrTime = workDay.roundedArrivalTime ?? workDay.arrivalTime
                    let totalSeconds = depTime.timeIntervalSince(arrTime)
                    let totalMinutes = max(0, (totalSeconds / 60) - Double(workDay.breakMinutes))
                    Text(String(format: "%.1f u", totalMinutes / 60.0))
                        .font(.headline)
                } else {
                    Text("Actief")
                        .font(.headline)
                        .foregroundColor(.cimGreen)
                }
            }
            .foregroundStyle(.secondary)

            // Smart suggestion card if review required
            let isActivelyRunningToday = Calendar.current.isDateInToday(workDay.date) && workDay.departureTime == nil && workDay.lastArrivalTime == nil
            if workDay.status == "TO_REVIEW" && !isActivelyRunningToday {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Slimme suggestie:")
                        .font(.caption2.bold())
                        .foregroundColor(.orange)
                    let depStr = suggestion.suggestedDepartureTime?.formatted(date: .omitted, time: .shortened) ?? "17:00"
                    Text("Vertrek: \(depStr) • Pauze: \(suggestion.suggestedBreakMinutes)m\(suggestion.suggestedProjectCode.map { " • Project: \($0)" } ?? "")")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        Spacer()
                        Button(action: {
                            onApprove?(suggestion)
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "checkmark")
                                Text("Akkoord met suggestie")
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
                .padding(8)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .padding(.vertical, 4)
    }
}
