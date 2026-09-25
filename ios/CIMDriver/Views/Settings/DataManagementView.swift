import SwiftUI
import SwiftData

struct DataManagementView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var showingRestoreDialog = false
    @State private var showingClearLocationDialog = false
    @State private var showingResetAllDialog = false
    
    @State private var backupURL: URL?
    @State private var showShareSheet = false
    @State private var showingFileImporter = false
    @State private var alertMessage = ""
    @State private var showAlert = false
    
    @Query private var trips: [Trip]
    @Query private var locationPoints: [LocationPoint]
    
    var body: some View {
        Form {
            Section(header: Text("Back-up & Herstel"), footer: Text("Maak een JSON back-up van al je geregistreerde ritten, voertuigen en instellingen.")) {
                Button(action: createBackup) {
                    Label("Maak Back-up", systemImage: "arrow.up.doc.fill")
                }
                
                Button(action: { showingRestoreDialog = true }) {
                    Label("Herstel Back-up", systemImage: "arrow.down.doc.fill")
                        .foregroundStyle(.blue)
                }
            }
            
            Section(header: Text("Gegevens Wissen"), footer: Text("Waarschuwing: Gegevens wissen kan niet ongedaan worden gemaakt.")) {
                Button(role: .destructive, action: { showingClearLocationDialog = true }) {
                    Label("Wis alleen locatiegeschiedenis", systemImage: "location.slash.fill")
                        .foregroundStyle(.red)
                }
                
                Button(role: .destructive, action: { showingResetAllDialog = true }) {
                    Label("Wis alle gegevens", systemImage: "trash.fill")
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Data Beheer")
        .sheet(isPresented: $showShareSheet) {
            if let url = backupURL {
                ShareSheet(activityItems: [url])
            }
        }
        .alert("Herstel Back-up", isPresented: $showingRestoreDialog) {
            Button("Annuleer", role: .cancel) {}
            Button("Kies Bestand") {
                showingFileImporter = true
            }
        } message: {
            Text("Weet je zeker dat je alle huidige gegevens wilt overschrijven met een back-up?")
        }
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            handleFileImport(result)
        }
        .alert("Wis Locatiegeschiedenis", isPresented: $showingClearLocationDialog) {
            Button("Annuleer", role: .cancel) {}
            Button("Wis", role: .destructive) { clearLocationData() }
        } message: {
            Text("Weet je zeker dat je de opgeslagen locatiegeschiedenis (\(locationPoints.count) punten) wilt wissen? De ritten zelf blijven bewaard.")
        }
        .alert("Wis Alle Gegevens", isPresented: $showingResetAllDialog) {
            Button("Annuleer", role: .cancel) {}
            Button("Wis Alles", role: .destructive) { resetAllData() }
        } message: {
            Text("Weet je zeker dat je alle gegevens (ritten, adressen, voertuigen, instellingen) wilt wissen? De app keert terug naar de fabrieksinstellingen.")
        }
        .alert("Melding", isPresented: $showAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
    }
    
    private func createBackup() {
        if let url = BackupManager.shared.createBackup(context: modelContext) {
            backupURL = url
            showShareSheet = true
        } else {
            alertMessage = "Maken van back-up mislukt."
            showAlert = true
        }
    }

    private func handleFileImport(_ result: Result<[URL], Error>) {
        do {
            guard let selectedFile = try result.get().first else { return }

            let success = BackupManager.shared.restoreBackup(url: selectedFile, context: modelContext)

            if success {
                alertMessage = "Back-up succesvol hersteld!"
            } else {
                alertMessage = "Fout bij het herstellen van de back-up."
            }
            showAlert = true
        } catch {
            alertMessage = "Kan bestand niet inlezen."
            showAlert = true
        }
    }
    
    private func clearLocationData() {
        for point in locationPoints {
            modelContext.delete(point)
        }
        try? modelContext.save()
        alertMessage = "Locatiegeschiedenis succesvol gewist."
        showAlert = true
    }
    
    private func resetAllData() {
        try? modelContext.delete(model: Trip.self)
        try? modelContext.delete(model: LocationPoint.self)
        try? modelContext.delete(model: Vehicle.self)
        try? modelContext.delete(model: SavedAddress.self)
        try? modelContext.delete(model: WorkDay.self)
        try? modelContext.delete(model: ClassificationRule.self)
        
        try? modelContext.save()
        
        alertMessage = "Alle gegevens succesvol gewist."
        showAlert = true
    }
}
