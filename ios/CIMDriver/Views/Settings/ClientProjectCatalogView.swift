import SwiftUI
import SwiftData

struct ClientProjectCatalogView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Client.name) private var clients: [Client]
    @Query(sort: \ProjectCode.code) private var projects: [ProjectCode]
    @Query private var trips: [Trip]
    @Query private var workDays: [WorkDay]
    @Query private var settingsList: [AppSettings]

    @State private var showArchived = false
    @State private var showClientEditor = false
    @State private var showProjectEditor = false
    @State private var clientToEdit: Client?
    @State private var projectToEdit: ProjectCode?

    private var visibleClients: [Client] { clients.filter { showArchived || $0.isActive } }
    private var visibleProjects: [ProjectCode] { projects.filter { showArchived || $0.isActive } }

    var body: some View {
        List {
            Section {
                Toggle("Toon gearchiveerde items", isOn: $showArchived)
            }

            Section("Klanten") {
                ForEach(visibleClients) { client in
                    HStack {
                        Circle()
                            .fill(Color(hex: client.color))
                            .frame(width: 12, height: 12)
                        Text(client.name)
                            .foregroundStyle(client.isActive ? .primary : .secondary)
                        Spacer()
                        Menu {
                            Button("Bewerken", systemImage: "pencil") { clientToEdit = client }
                            Button(client.isActive ? "Archiveren" : "Herstellen", systemImage: client.isActive ? "archivebox" : "arrow.uturn.backward") {
                                client.isActive.toggle()
                                try? modelContext.save()
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }

            Section("Projectcodes") {
                ForEach(visibleProjects) { project in
                    Button {
                        projectToEdit = project
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(project.code).font(.headline)
                                Spacer()
                                Text(project.hourlyRate, format: .currency(code: Locale.current.currency?.identifier ?? "EUR"))
                                    .font(.subheadline)
                            }
                            HStack(spacing: 6) {
                                if let clientName = clients.first(where: { $0.id == project.clientId })?.name {
                                    Text(clientName)
                                }
                                if let details = project.details, !details.isEmpty {
                                    Text(details)
                                }
                                if !project.isBillable {
                                    Text("Niet declarabel")
                                }
                                if !project.isActive {
                                    Text("Gearchiveerd")
                                }
                            }
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .swipeActions {
                        Button(project.isActive ? "Archiveer" : "Herstel", systemImage: project.isActive ? "archivebox" : "arrow.uturn.backward") {
                            project.isActive.toggle()
                            try? modelContext.save()
                        }
                        .tint(project.isActive ? .orange : .green)
                    }
                }
            }
        }
        .navigationTitle("Klanten & Projectcodes")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button("Klant toevoegen", systemImage: "person.badge.plus") { showClientEditor = true }
                    Button("Projectcode toevoegen", systemImage: "number") { showProjectEditor = true }
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showClientEditor) {
            ClientEditorView()
        }
        .sheet(item: $clientToEdit) { client in
            ClientEditorView(client: client)
        }
        .sheet(isPresented: $showProjectEditor) {
            ProjectCodeEditorView()
        }
        .sheet(item: $projectToEdit) { project in
            ProjectCodeEditorView(project: project)
        }
        .onAppear(perform: archiveUnusedProjects)
    }

    private func archiveUnusedProjects() {
        let archiveDays = settingsList.first?.autoArchiveProjectDays ?? 0
        guard archiveDays > 0 else { return }
        let threshold = Date().addingTimeInterval(-Double(archiveDays) * 86_400)

        for project in projects where project.isActive {
            let lastTrip = trips.filter { $0.projectCode == project.code }.map(\.startTime).max() ?? .distantPast
            let lastWorkDay = workDays.filter { $0.projectCode == project.code }.map(\.date).max() ?? .distantPast
            let lastActivity = max(project.createdAt, max(lastTrip, lastWorkDay))
            if lastActivity < threshold {
                project.isActive = false
            }
        }
        try? modelContext.save()
    }
}

private struct ClientEditorView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let client: Client?
    @State private var name: String

    init(client: Client? = nil) {
        self.client = client
        _name = State(initialValue: client?.name ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Klantnaam", text: $name)
            }
            .navigationTitle(client == nil ? "Nieuwe klant" : "Klant bewerken")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Annuleer") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan", action: save).disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private func save() {
        let cleanedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if let client {
            client.name = cleanedName
        } else {
            modelContext.insert(Client(name: cleanedName))
        }
        try? modelContext.save()
        dismiss()
    }
}

private struct ProjectCodeEditorView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Client.name) private var clients: [Client]
    @Query private var projects: [ProjectCode]
    let project: ProjectCode?

    @State private var code: String
    @State private var details: String
    @State private var rate: String
    @State private var clientId: UUID?
    @State private var isBillable: Bool
    @State private var duplicateCode = false

    init(project: ProjectCode? = nil) {
        self.project = project
        _code = State(initialValue: project?.code ?? "")
        _details = State(initialValue: project?.details ?? "")
        _rate = State(initialValue: project.map { String($0.hourlyRate) } ?? "")
        _clientId = State(initialValue: project?.clientId)
        _isBillable = State(initialValue: project?.isBillable ?? true)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Project") {
                    TextField("Projectcode", text: $code)
                    TextField("Omschrijving (optioneel)", text: $details)
                    Picker("Klant", selection: $clientId) {
                        Text("Geen klant").tag(UUID?.none)
                        ForEach(clients.filter(\.isActive)) { client in
                            Text(client.name).tag(UUID?.some(client.id))
                        }
                    }
                }

                Section("Tarief") {
                    TextField("Vast uurtarief (€)", text: $rate)
                        .keyboardType(.decimalPad)
                    Toggle("Declarabel", isOn: $isBillable)
                }

                if duplicateCode {
                    Section {
                        Text("Deze projectcode bestaat al.")
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(project == nil ? "Nieuwe projectcode" : "Projectcode bewerken")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Annuleer") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Opslaan", action: save) }
            }
        }
    }

    private func save() {
        let cleanedCode = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedCode.isEmpty else { return }
        guard !projects.contains(where: { $0.code.caseInsensitiveCompare(cleanedCode) == .orderedSame && $0.id != project?.id }) else {
            duplicateCode = true
            return
        }
        let hourlyRate = Double(rate.replacingOccurrences(of: ",", with: ".")) ?? 0
        if let project {
            project.code = cleanedCode
            project.details = details.isEmpty ? nil : details
            project.hourlyRate = max(0, hourlyRate)
            project.clientId = clientId
            project.isBillable = isBillable
        } else {
            modelContext.insert(ProjectCode(
                code: cleanedCode,
                clientId: clientId,
                details: details.isEmpty ? nil : details,
                hourlyRate: hourlyRate,
                isBillable: isBillable
            ))
        }
        try? modelContext.save()
        dismiss()
    }
}

private extension Color {
    init(hex: String) {
        let value = UInt64(hex.trimmingCharacters(in: CharacterSet(charactersIn: "#")), radix: 16) ?? 0x1976D2
        self.init(.sRGB, red: Double((value >> 16) & 0xff) / 255, green: Double((value >> 8) & 0xff) / 255, blue: Double(value & 0xff) / 255, opacity: 1)
    }
}