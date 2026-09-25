import SwiftUI
import CoreLocation
import SwiftData
import UserNotifications
import UIKit

struct DiagnosticsView: View {
    @State private var locationAuthStatus: CLAuthorizationStatus = .notDetermined
    @State private var notificationGranted: Bool = false
    @State private var trackingManager = TrackingManager.shared
    
    @Query(sort: \WorkDay.date, order: .reverse) private var workDays: [WorkDay]
    
    private var appVersionText: String {
        let shortVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        let gitCommit = (Bundle.main.object(forInfoDictionaryKey: "GIT_COMMIT") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if let gitCommit, !gitCommit.isEmpty, gitCommit != "$(GIT_COMMIT)", gitCommit != "unknown" {
            return "\(shortVersion)-\(gitCommit)"
        }

        let displayVersion = Bundle.main.object(forInfoDictionaryKey: "APP_DISPLAY_VERSION") as? String
        let cleanedDisplayVersion = displayVersion?.trimmingCharacters(in: .whitespacesAndNewlines)

        if let cleanedDisplayVersion, !cleanedDisplayVersion.isEmpty, cleanedDisplayVersion != "$(APP_DISPLAY_VERSION)", cleanedDisplayVersion != shortVersion, cleanedDisplayVersion != "\(shortVersion)-", cleanedDisplayVersion != "unknown" {
            return cleanedDisplayVersion
        }

        return shortVersion
    }
    
    private var locationPermissionStatusText: String {
        switch locationAuthStatus {
        case .authorizedAlways:
            return "Altijd"
        case .authorizedWhenInUse:
            return "Bij gebruik"
        case .denied, .restricted:
            return "Geweigerd"
        case .notDetermined:
            return "Nog niet gevraagd"
        @unknown default:
            return "Onbekend"
        }
    }
    
    private var locationPermissionHint: String {
        switch locationAuthStatus {
        case .authorizedAlways:
            return "Locatie permissie is correct ingesteld voor achtergrondtracking."
        case .authorizedWhenInUse:
            return "Zet locatie op 'Altijd' zodat ritten ook starten wanneer je telefoon vergrendeld is."
        case .denied, .restricted:
            return "Schakel locatie permissie in via Instellingen > Privacy en beveiliging > Locatievoorzieningen."
        case .notDetermined:
            return "Geef locatie toestemming om ritten automatisch op de achtergrond te registreren."
        @unknown default:
            return "Controleer de locatie permissie in iOS Instellingen."
        }
    }
    
    private var notificationHint: String {
        notificationGranted
            ? "Notificaties zijn ingeschakeld voor statusmeldingen en geofence-herinneringen."
            : "Schakel notificaties in zodat CIMDriver ritstatus en herinneringen kan tonen."
    }
    
    var activeWorkDay: WorkDay? {
        let calendar = Calendar.current
        return workDays.first(where: { calendar.isDateInToday($0.date) && $0.departureTime == nil })
    }
    
    var body: some View {
        Form {
            Section(header: Text("Live Status")) {
                HStack {
                    Text("Tracking Status")
                    Spacer()
                    switch trackingManager.state {
                    case .idle:
                        Text("Inactief").foregroundStyle(.gray)
                    case .active:
                        Text("Actieve Rit").foregroundStyle(.green)
                    case .gracePeriod:
                        Text("Grace Period").foregroundStyle(.orange)
                    }
                }
                
                HStack {
                    Text("Werkdag Status")
                    Spacer()
                    if let wd = activeWorkDay {
                        Text("Actief (vanaf \(wd.firstDepartureTime.formatted(date: .omitted, time: .shortened)))")
                            .foregroundStyle(.green)
                    } else {
                        Text("Niet gestart")
                            .foregroundStyle(.gray)
                    }
                }
                
                if let trip = trackingManager.activeTrip {
                    HStack {
                        Text("Huidige Rit Start")
                        Spacer()
                        Text(trip.startTime.formatted(date: .omitted, time: .shortened))
                            .foregroundStyle(.secondary)
                    }
                }
            }
            
            Section(header: Text("App Versie")) {
                HStack {
                    Text("Versie")
                    Spacer()
                    Text(appVersionText)
                        .foregroundStyle(.secondary)
                }
            }
            
            Section(header: Text("Systeem Permissies"), footer: Text("Voor betrouwbare automatische ritregistratie heeft CIMDriver locatie op 'Altijd' en notificaties nodig.")) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Locatie")
                        Spacer()
                        Text(locationPermissionStatusText)
                            .foregroundStyle(locationAuthStatus == .authorizedAlways ? .green : locationAuthStatus == .authorizedWhenInUse ? .orange : .red)
                    }
                    Text(locationPermissionHint)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    
                    if locationAuthStatus != .authorizedAlways {
                        Button("Locatie instellen") {
                            if locationAuthStatus == .notDetermined || locationAuthStatus == .authorizedWhenInUse {
                                trackingManager.requestPermissions()
                            } else {
                                openAppSettings()
                            }
                        }
                    }
                }
                
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Notificaties")
                        Spacer()
                        Text(notificationGranted ? "Toegestaan" : "Geweigerd")
                            .foregroundStyle(notificationGranted ? .green : .red)
                    }
                    Text(notificationHint)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    
                    if !notificationGranted {
                        Button("Notificaties instellen") {
                            openAppSettings()
                        }
                    }
                }
            }
            
            Section(header: Text("Achtergrond Processen"), footer: Text("Wanneer locatie niet op 'Altijd' staat, kan achtergrondtracking beperkt of onderbroken worden.")) {
                HStack {
                    Text("Auto-Start Service")
                    Spacer()
                    Text("Actief").foregroundStyle(.green)
                }
                HStack {
                    Text("Locatie Updates")
                    Spacer()
                    Text(locationAuthStatus == .authorizedAlways ? "Ingeschakeld" : "Gepauzeerd")
                        .foregroundStyle(locationAuthStatus == .authorizedAlways ? .green : .orange)
                }
            }
            
            Section(header: Text("Aanbevolen Controle")) {
                Text("Controleer in iOS Instellingen of Locatie op 'Altijd' staat, notificaties zijn toegestaan en Bluetooth voor je voertuig correct is gekoppeld.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("App Diagnostiek")
        .onAppear {
            checkPermissions()
        }
    }
    
    private func openAppSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    private func checkPermissions() {
        let manager = CLLocationManager()
        locationAuthStatus = manager.authorizationStatus
        
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.notificationGranted = (settings.authorizationStatus == .authorized)
            }
        }
    }
}
