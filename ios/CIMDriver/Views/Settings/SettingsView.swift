import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var settingsList: [AppSettings]
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Mijn Data") {
                    NavigationLink(destination: VehicleSelectionView()) {
                        Label("Mijn Auto's", systemImage: "car.2")
                    }
                    NavigationLink(destination: AddressListView()) {
                        Label("Opgeslagen Adressen", systemImage: "mappin.and.ellipse")
                    }
                }
                
                if let settings = settingsList.first {
                    AppSettingsForm(settings: settings)
                } else {
                    Section {
                        Text("Instellingen laden...")
                    }
                }
                
                Section("Diagnostiek & Systeem") {
                    NavigationLink(destination: DataManagementView()) {
                        Label("Data Beheer & Backup", systemImage: "externaldrive.fill")
                    }
                    NavigationLink(destination: DiagnosticsView()) {
                        Label("App Diagnostiek", systemImage: "stethoscope")
                    }
                }
                
                Section("Over") {
                    NavigationLink(destination: AboutView()) {
                        Label("Over CIMDriver", systemImage: "info.circle")
                    }
                }
            }
            .navigationTitle("Instellingen")
            .onAppear {
                if settingsList.isEmpty {
                    let defaultSettings = AppSettings()
                    modelContext.insert(defaultSettings)
                    try? modelContext.save()
                }
            }
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

struct AutoPreviewProviderSettingsView1: PreviewProvider {
    static var previews: some View {
            SettingsView()
                .modelContainer(PreviewContainer.shared.container)
    }
}
struct AppSettingsForm: View {
    @Bindable var settings: AppSettings
    
    var body: some View {
        Section("Algemeen") {
            Picker("Thema", selection: $settings.themeMode) {
                Text("Systeem").tag("SYSTEM")
                Text("Licht").tag("LIGHT")
                Text("Donker").tag("DARK")
            }
        }
        
        Section("App Gedrag") {
            NavigationLink(destination: TripSettingsView(settings: settings)) {
                Label("Rit & Classificatie", systemImage: "car.fill")
            }
            NavigationLink(destination: TrackingSettingsView(settings: settings)) {
                Label("Tracking & Privacy", systemImage: "location.fill")
            }
            NavigationLink(destination: NotificationSettingsView(settings: settings)) {
                Label("Notificaties", systemImage: "bell.fill")
            }
        }
        
        Section("Werkuren") {
            NavigationLink(destination: WorkDaysEditor(workDaysString: $settings.workDays)) {
                Label("Werkdagen & Tijden", systemImage: "calendar")
            }
            Stepper("Standaard Pauze: \(settings.breakMinutes) min", value: $settings.breakMinutes, in: 0...120, step: 5)
            Stepper("Marge / Tolerantie: \(settings.toleranceMinutes) min", value: $settings.toleranceMinutes, in: 0...60, step: 5)
        }
    }
}

struct TripSettingsView: View {
    @Bindable var settings: AppSettings
    
    var body: some View {
        Form {
            Section("Classificatie (Standaard)") {
                Picker("Standaard Rit Type", selection: $settings.classificationDefault) {
                    Text("Privé").tag("PRIVATE")
                    Text("Zakelijk").tag("BUSINESS")
                    Text("Woon-Werk").tag("COMMUTE")
                }
                Toggle("Thuis-Werk is Woon-Werk", isOn: $settings.classifyHomeWorkAsCommute)
                Toggle("Klantbezoek is Zakelijk", isOn: $settings.classifyCustomerAsBusiness)
                
                NavigationLink(destination: ClassificationRulesView()) {
                    Text("Aangepaste Regels")
                }
            }
            
            Section("Vergoedingen") {
                HStack {
                    Text("Zakelijk (€/km)")
                    Spacer()
                    TextField("0.23", value: $settings.businessCompensation, format: .number)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
            }
            
            Section("Auto-Merge Ritten") {
                Toggle("Auto-merge inschakelen", isOn: $settings.autoMergeEnabled)
                if settings.autoMergeEnabled {
                    Stepper("Tijdslimiet: \(settings.mergeTimeLimitMin) min", value: $settings.mergeTimeLimitMin, in: 5...120, step: 5)
                    Stepper("Max Afstand: \(settings.mergeDistanceM) m", value: $settings.mergeDistanceM, in: 50...2000, step: 50)
                }
            }
        }
        .navigationTitle("Rit & Classificatie")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct TrackingSettingsView: View {
    @Bindable var settings: AppSettings
    
    var body: some View {
        Form {
            Section("Tracking & Privacy") {
                Stepper("Start Vertraging: \(settings.gracePeriodSec) sec", value: $settings.gracePeriodSec, in: 30...300, step: 30)
                Stepper("Tracking Interval: \(settings.trackingIntervalSec) sec", value: $settings.trackingIntervalSec, in: 1...60, step: 1)
                Stepper("Data Bewaren: \(settings.locationRetentionDays) dagen", value: $settings.locationRetentionDays, in: 30...730, step: 30)
            }
        }
        .navigationTitle("Tracking & Privacy")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct NotificationSettingsView: View {
    @Bindable var settings: AppSettings
    
    var body: some View {
        Form {
            Section("Notificaties") {
                Toggle("Werkdag meldingen", isOn: Binding(
                    get: { settings.workdayNotificationsEnabled ?? true },
                    set: { settings.workdayNotificationsEnabled = $0 }
                ))
                Toggle("Geofence herinneringen", isOn: Binding(
                    get: { settings.geofenceNotificationsEnabled ?? true },
                    set: { settings.geofenceNotificationsEnabled = $0 }
                ))
                Toggle("Auto-start meldingen", isOn: Binding(
                    get: { settings.autoStartNotificationsEnabled ?? true },
                    set: { settings.autoStartNotificationsEnabled = $0 }
                ))
            }

            Section("Km-stand herinnering") {
                Toggle("Km-stand Herinnering", isOn: $settings.odometerReminder)
                if settings.odometerReminder {
                    Stepper("Elke \(settings.odometerReminderIntervalDays) dagen", value: $settings.odometerReminderIntervalDays, in: 1...365, step: 7)
                }
            }
        }
        .navigationTitle("Notificaties")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct WorkDaysEditor: View {
    @Binding var workDaysString: String
    
    struct DayConfig: Identifiable {
        let id: Int
        let name: String
        var isActive: Bool
        var startTime: Date
        var endTime: Date
    }
    
    @State private var days: [DayConfig] = []
    
    let dayNames = ["Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag", "Zaterdag", "Zondag"]
    
    var body: some View {
        Form {
            ForEach($days) { $day in
                Section {
                    Toggle(isOn: $day.isActive) {
                        Text(day.name).bold()
                    }
                    if day.isActive {
                        DatePicker("Starttijd", selection: $day.startTime, displayedComponents: .hourAndMinute)
                        DatePicker("Eindtijd", selection: $day.endTime, displayedComponents: .hourAndMinute)
                    }
                }
            }
        }
        .navigationTitle("Werkdagen configureren")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            parseString()
        }
        .onChange(of: days.map { "\($0.isActive)-\($0.startTime)-\($0.endTime)" }) { _, _ in
            saveString()
        }
    }
    
    private func parseString() {
        // format: 1=08:00-17:00;2=08:00-17:00
        var configMap: [Int: (Date, Date)] = [:]
        
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        
        let parts = workDaysString.split(separator: ";")
        for p in parts {
            let dp = p.split(separator: "=")
            if dp.count == 2, let dNum = Int(dp[0]) {
                let times = dp[1].split(separator: "-")
                if times.count == 2, let st = formatter.date(from: String(times[0])), let et = formatter.date(from: String(times[1])) {
                    configMap[dNum] = (st, et)
                }
            }
        }
        
        var newDays: [DayConfig] = []
        for i in 1...7 {
            if let config = configMap[i] {
                newDays.append(DayConfig(id: i, name: dayNames[i-1], isActive: true, startTime: config.0, endTime: config.1))
            } else {
                newDays.append(DayConfig(id: i, name: dayNames[i-1], isActive: false, startTime: formatter.date(from: "08:00") ?? Date(), endTime: formatter.date(from: "17:00") ?? Date()))
            }
        }
        self.days = newDays
    }
    
    private func saveString() {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        
        let active = days.filter { $0.isActive }
        let strings = active.map { "\($0.id)=\(formatter.string(from: $0.startTime))-\(formatter.string(from: $0.endTime))" }
        workDaysString = strings.joined(separator: ";")
    }
}
