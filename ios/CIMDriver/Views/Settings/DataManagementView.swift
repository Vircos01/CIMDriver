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
    
    private var latestBackupFileInfo: (date: Date, sizeKB: Int, fileName: String)? {
        let fileManager = FileManager.default
        var searchDirs: [URL] = []
        if let docDir = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first {
            searchDirs.append(docDir.appendingPathComponent("CIMDriver/Backups"))
            searchDirs.append(docDir)
        }
        searchDirs.append(fileManager.temporaryDirectory)
        
        var filesFound: [(date: Date, size: Int, name: String)] = []
        for dir in searchDirs {
            guard let files = try? fileManager.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey], options: .skipsHiddenFiles) else { continue }
            for file in files where (file.lastPathComponent.hasPrefix("CIMDriver_Backup") && file.pathExtension == "json") {
                if let vals = try? file.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]),
                   let date = vals.contentModificationDate,
                   let size = vals.fileSize {
                    filesFound.append((date, size, file.lastPathComponent))
                }
            }
        }
        
        if let newest = filesFound.max(by: { $0.date < $1.date }) {
            return (newest.date, max(1, newest.size / 1024), newest.name)
        }
        return nil
    }
    
    var body: some View {
        Form {
            Section(header: Text("Laatste Lokale Back-up"), footer: Text("Geeft inzicht in de recentheid en herstelbaarheid van je lokale rittengegevens.")) {
                if let info = latestBackupFileInfo {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(Color.cimGreen)
                            Text("Gevalideerd & Herstelbaar")
                                .font(.subheadline.bold())
                                .foregroundColor(Color.cimGreen)
                        }
                        
                        Divider()
                        
                        HStack {
                            Text("Tijdstip:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(info.date.formatted(date: .abbreviated, time: .shortened))
                                .font(.caption.bold())
                        }
                        
                        HStack {
                            Text("Bestandsgrootte:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("\(info.sizeKB) KB")
                                .font(.caption.bold())
                        }
                        
                        HStack {
                            Text("Bestandsnaam:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(info.fileName)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }
                    .padding(.vertical, 4)
                } else {
                    HStack(spacing: 8) {
                        Image(systemName: "info.circle")
                            .foregroundColor(.secondary)
                        Text("Nog geen lokale back-up aangemaakt.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
            
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
