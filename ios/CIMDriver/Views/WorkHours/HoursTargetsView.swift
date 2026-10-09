import SwiftUI
import SwiftData

struct HoursTargetsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var targets: [HoursTarget]
    @Query private var workDays: [WorkDay]
    @Query private var projectCodes: [ProjectCode]
    @Query private var clients: [Client]
    @Query private var settingsList: [AppSettings]

    @State private var showNewTarget = false
    @State private var targetToEdit: HoursTarget?

    private var currentYear: Int { Calendar.current.component(.year, from: Date()) }
    private var currentTargets: [HoursTarget] { targets.filter { $0.isActive && $0.year == currentYear }.sorted { $0.name < $1.name } }

    var body: some View {
        List {
            if currentTargets.isEmpty {
                ContentUnavailableView("Geen targets", systemImage: "flag", description: Text("Voeg een uren- of omzetdoel toe voor \(currentYear)."))
            }

            ForEach(currentTargets) { target in
                let progress = progress(for: target)
                let subtitle = targetSubtitle(for: target)
                Button {
                    targetToEdit = target
                } label: {
                    TargetProgressCard(target: target, progress: progress, subtitle: subtitle)
                }
                .buttonStyle(.plain)
                .swipeActions {
                    Button("Archiveer", systemImage: "archivebox") {
                        target.isActive = false
                        try? modelContext.save()
                    }
                    .tint(.orange)
                }
            }
        }
        .navigationTitle("Targets \(currentYear)")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showNewTarget = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $showNewTarget) {
            HoursTargetEditorView()
        }
        .sheet(item: $targetToEdit) { target in
            HoursTargetEditorView(target: target)
        }
    }

    private func progress(for target: HoursTarget) -> TargetProgressSnapshot {
        TargetCalculator.progress(
            for: target,
            workDays: workDays,
            projectCodes: projectCodes,
            employmentStartDate: settingsList.first?.employmentStartDate
        )
    }

    private func targetSubtitle(for target: HoursTarget) -> String? {
        var parts: [String] = []
        if let clientId = target.clientId, let client = clients.first(where: { $0.id == clientId }) {
            parts.append(client.name)
        }
        if let projectId = target.projectCodeId, let project = projectCodes.first(where: { $0.id == projectId }) {
            parts.append(project.code)
        }
        return parts.isEmpty ? nil : parts.joined(separator: " • ")
    }
}

private struct TargetProgressCard: View {
    let target: HoursTarget
    let progress: TargetProgressSnapshot
    let subtitle: String?

    private var currencyCode: String { Locale.current.currency?.identifier ?? "EUR" }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(target.name).font(.headline).foregroundStyle(.primary)
                    if target.targetType == "REVENUE" {
                        Text("\(progress.accumulatedRevenue, format: .currency(code: currencyCode)) / \(progress.effectiveTarget, format: .currency(code: currencyCode))")
                    } else {
                        Text("\(progress.accumulatedHours, format: .number.precision(.fractionLength(1))) / \(progress.effectiveTarget, format: .number.precision(.fractionLength(1))) uur")
                    }
                }
                Spacer()
                Text(progress.percentage, format: .percent.precision(.fractionLength(0)))
                    .font(.headline)
                    .foregroundStyle(progress.isBehind ? .orange : .cimGreen)
            }

            ProgressView(value: min(max(progress.percentage, 0), 1))
                .tint(progress.isBehind ? .orange : .cimGreen)

            if let subtitle {
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
            if progress.proRataFactor < 1 {
                Text("Pro-rata: \(progress.proRataFactor, format: .percent.precision(.fractionLength(0))) van jaardoel")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }

}

private struct HoursTargetEditorView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Client.name) private var clients: [Client]
    @Query(sort: \ProjectCode.code) private var projects: [ProjectCode]

    let target: HoursTarget?
    @State private var name: String
    @State private var targetType: String
    @State private var hours: String
    @State private var revenue: String
    @State private var year: Int
    @State private var clientId: UUID?
    @State private var projectCodeId: UUID?

    init(target: HoursTarget? = nil) {
        self.target = target
        _name = State(initialValue: target?.name ?? "")
        _targetType = State(initialValue: target?.targetType ?? "HOURS")
        _hours = State(initialValue: target.map { String($0.targetHours) } ?? "1600")
        _revenue = State(initialValue: target.map { String($0.targetRevenue) } ?? "50000")
        _year = State(initialValue: target?.year ?? Calendar.current.component(.year, from: Date()))
        _clientId = State(initialValue: target?.clientId)
        _projectCodeId = State(initialValue: target?.projectCodeId)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Doel") {
                    TextField("Naam", text: $name)
                    Picker("Type", selection: $targetType) {
                        Text("Uren").tag("HOURS")
                        Text("Omzet").tag("REVENUE")
                    }
                    .pickerStyle(.segmented)

                    if targetType == "REVENUE" {
                        TextField("Jaardoel omzet (€)", text: $revenue).keyboardType(.decimalPad)
                    } else {
                        TextField("Jaardoel uren", text: $hours).keyboardType(.decimalPad)
                    }
                    Stepper("Jaar: \(year)", value: $year, in: 2000...2200)
                }

                Section("Toepassen op") {
                    Picker("Klant", selection: $clientId) {
                        Text("Alle klanten").tag(UUID?.none)
                        ForEach(clients.filter(\.isActive)) { client in
                            Text(client.name).tag(UUID?.some(client.id))
                        }
                    }
                    Picker("Projectcode", selection: $projectCodeId) {
                        Text("Alle projecten").tag(UUID?.none)
                        ForEach(projects.filter(\.isActive).filter { clientId == nil || $0.clientId == clientId }) { project in
                            Text(project.code).tag(UUID?.some(project.id))
                        }
                    }
                    if targetType == "REVENUE", let selectedProject = projects.first(where: { $0.id == projectCodeId }) {
                        LabeledContent("Vast projecttarief", value: selectedProject.hourlyRate.formatted(.currency(code: Locale.current.currency?.identifier ?? "EUR")) + "/uur")
                    }
                }
            }
            .navigationTitle(target == nil ? "Nieuw target" : "Target bewerken")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Annuleer") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan", action: save).disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .onChange(of: projectCodeId) { _, projectId in
                if let projectId, let project = projects.first(where: { $0.id == projectId }) {
                    clientId = project.clientId
                }
            }
        }
    }

    private func save() {
        let parsedHours = Double(hours.replacingOccurrences(of: ",", with: ".")) ?? 0
        let parsedRevenue = Double(revenue.replacingOccurrences(of: ",", with: ".")) ?? 0
        if let target {
            target.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
            target.targetType = targetType
            target.targetHours = max(0, parsedHours)
            target.targetRevenue = max(0, parsedRevenue)
            target.year = year
            target.clientId = clientId
            target.projectCodeId = projectCodeId
        } else {
            modelContext.insert(HoursTarget(
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                clientId: clientId,
                projectCodeId: projectCodeId,
                targetType: targetType,
                targetHours: parsedHours,
                targetRevenue: parsedRevenue,
                year: year
            ))
        }
        try? modelContext.save()
        dismiss()
    }
}