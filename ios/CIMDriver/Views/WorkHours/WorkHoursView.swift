import SwiftUI
import SwiftData

struct WorkHoursView: View {
    @Query(sort: \WorkDay.date, order: .reverse) private var workDays: [WorkDay]
    @Environment(\.modelContext) private var modelContext
    @State private var showingAddWorkDay = false
    @State private var exportURL: URL?
    
    private var groupedWorkDays: [(key: String, value: [WorkDay])] {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "nl_NL")
        
        let grouped = Dictionary(grouping: workDays) { wd in
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
                    Section(header: Text(group.key).font(.headline)) {
                        ForEach(group.value) { workDay in
                            NavigationLink(value: workDay) {
                                WorkDayRowView(workDay: workDay)
                            }
                        }
                        .onDelete { offsets in
                            deleteWorkDays(at: offsets, in: group.value)
                        }
                    }
                }
            }
            .navigationTitle("Werkuren")
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
    
    private func exportWorkDaysCSV() {
        exportURL = ExportUtil.generateWorkDaysCSV(workDays: workDays)
    }
    
    private func exportWorkDaysPDF() {
        exportURL = ExportUtil.generateWorkDaysPDF(workDays: workDays)
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
