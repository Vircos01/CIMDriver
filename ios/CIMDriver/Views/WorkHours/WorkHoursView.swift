import SwiftUI
import SwiftData

struct WorkHoursView: View {
    @Query(sort: \WorkDay.date, order: .reverse) private var workDays: [WorkDay]
    @Environment(\.modelContext) private var modelContext
    @State private var showingAddWorkDay = false
    @State private var exportURL: URL?
    @State private var searchText = ""
    
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
            List {
                ForEach(groupedWorkDays, id: \.key) { group in
                    Section {
                        ForEach(group.value) { workDay in
                            NavigationLink(value: workDay) {
                                WorkDayRowView(workDay: workDay)
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
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(workDay.date.formatted(date: .abbreviated, time: .omitted))
                    .font(.subheadline.bold())
                Spacer()
                Text(workDay.status == "TO_REVIEW" ? "Nog beoordelen" : workDay.status)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.orange.opacity(0.2))
                    .foregroundStyle(.orange)
                    .clipShape(Capsule())
            }
            
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
                }
                Spacer()
                // Bereken uren
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
        }
        .padding(.vertical, 4)
    }
}
