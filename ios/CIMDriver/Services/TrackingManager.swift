import CoreLocation
import SwiftData
import UserNotifications
import Observation
import UIKit
#if canImport(ActivityKit)
import ActivityKit
#endif

@Observable
class TrackingManager: NSObject, BluetoothServiceDelegate, LocationServiceDelegate {
    static let shared = TrackingManager()

    private enum NotificationAction {
        static let startTrip = "START_TRIP"
    }

    private enum NotificationCategory {
        static let potentialTripStart = "POTENTIAL_TRIP_START"
    }
    
    enum TrackingState {
        case idle
        case active
        case gracePeriod
    }
    
    let bluetoothService = BluetoothService()
    let locationService = LocationService()
    let geocoderService = GeocoderService()
    let recoveryManager = RecoveryManager()
    
    var modelContext: ModelContext?
    
    var state: TrackingState = .idle
    var activeTrip: Trip?
    
    private var gracePeriodTask: DispatchWorkItem?
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    private var lastGeofenceNotificationAt: Date?
    private var lastGeofenceAutoStartAt: Date?
    private let geofenceNotificationCooldown: TimeInterval = 15 * 60
    private let geofenceAutoStartCooldown: TimeInterval = 10 * 60
    
    #if canImport(ActivityKit)
    private var liveActivity: Activity<TrackingAttributes>?
    #endif
    
    private override init() {
        super.init()
        bluetoothService.delegate = self
        locationService.delegate = self
        configureNotificationActions()
    }
    
    func setup(modelContext: ModelContext) {
        self.modelContext = modelContext
        
        let fetchDescriptor = FetchDescriptor<Trip>(predicate: #Predicate { $0.status == "ACTIVE" })
        if let activeTrips = try? modelContext.fetch(fetchDescriptor), let active = activeTrips.first {
            let recoveryAction = recoveryManager.recoverIfNeeded(context: modelContext, trip: active)

            switch recoveryAction {
            case .keepActive:
                self.activeTrip = active
                self.state = .active
                self.locationService.startTracking()
                self.locationService.startMonitoringSignificantLocationChanges()
                
                #if canImport(ActivityKit)
                self.startLiveActivity(startTime: active.startTime)
                #endif
            case .finalize:
                recoveryManager.finalizeRecoveredTripIfNeeded(context: modelContext, trip: active)
                finalizeTrip(active)
                try? modelContext.save()
                self.activeTrip = nil
                self.state = .idle
                self.locationService.stopMonitoringSignificantLocationChanges()
            }
        } else {
            self.locationService.stopMonitoringSignificantLocationChanges()
        }
        
        cleanupOldLocationPoints()
        updateGeofences()
    }
    
    func updateGeofences() {
        guard let context = modelContext else { return }
        
        // Clear existing geofences
        locationService.stopAllGeofences()
        
        // Setup new geofences
        let descriptor = FetchDescriptor<SavedAddress>()
        if let addresses = try? context.fetch(descriptor) {
            for address in addresses {
                if let lat = address.latitude, let lon = address.longitude {
                    let coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
                    let id = address.id.uuidString
                    locationService.startMonitoringGeofence(id: id, coordinate: coordinate, radius: 200.0)
                }
            }
        }
    }
    
    private func cleanupOldLocationPoints() {
        guard let context = modelContext else { return }
        let settings = (try? context.fetch(FetchDescriptor<AppSettings>()))?.first
        let retentionDays = settings?.locationRetentionDays ?? 365
        
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -retentionDays, to: Date()) ?? Date()
        
        let descriptor = FetchDescriptor<Trip>(predicate: #Predicate { $0.startTime < cutoffDate })
        if let oldTrips = try? context.fetch(descriptor) {
            for trip in oldTrips where trip.status != "ACTIVE" && !trip.locationPoints.isEmpty {
                trip.locationPoints.removeAll()
            }
            try? context.save()
        }
    }
    
    func requestPermissions() {
        locationService.requestPermissions()
        requestNotificationPermissionsIfNeeded()
    }
    
    // MARK: - BluetoothServiceDelegate
    
    func didConnectToVehicleDevice() {
        if state == .idle {
            startNewTrip()
            return
        }
        
        // Cancel eventuele grace period
        if let task = gracePeriodTask {
            task.cancel()
            gracePeriodTask = nil
            state = .active
            
            if backgroundTask != .invalid {
                UIApplication.shared.endBackgroundTask(backgroundTask)
                backgroundTask = .invalid
            }
            
            #if canImport(ActivityKit)
            Task {
                await updateLiveActivity(isGracePeriod: false)
            }
            #endif
            
            // Trip was ongoing, just continue
        } else {
            // Start nieuwe rit
            startNewTrip()
        }
    }
    
    func didDisconnectFromVehicleDevice() {
        let settings = (try? modelContext?.fetch(FetchDescriptor<AppSettings>()))?.first
        let gracePeriodSeconds = settings?.gracePeriodSec ?? 180
        
        backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "GracePeriodTimer") { [weak self] in
            self?.finalizeTrip()
        }
        
        // Start grace period
        let task = DispatchWorkItem { [weak self] in
            self?.finalizeTrip()
        }
        gracePeriodTask = task
        state = .gracePeriod
        
        #if canImport(ActivityKit)
        Task {
            await updateLiveActivity(isGracePeriod: true)
        }
        #endif
        
        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(gracePeriodSeconds), execute: task)
    }
    
    // MARK: - Trip Lifecycle
    
    private func startNewTrip() {
        guard let context = modelContext else { return }
        
        let vehicles = (try? context.fetch(FetchDescriptor<Vehicle>())) ?? []
        let defaultVehicle = vehicles.first(where: { $0.isDefault }) ?? vehicles.first
        
        let newTrip = Trip(startTime: Date(), status: "ACTIVE", odometerStart: defaultVehicle?.odometerCurrent ?? 0)
        newTrip.vehicle = defaultVehicle
        context.insert(newTrip)
        try? context.save()
        WidgetManager.updateWidget(context: context)
        activeTrip = newTrip
        state = .active
        
        #if canImport(ActivityKit)
        startLiveActivity(startTime: newTrip.startTime)
        #endif
        
        locationService.startTracking()
    }
    
    private func finalizeTrip() {
        guard let trip = activeTrip else { return }
        finalizeTrip(trip)
    }

    private func finalizeTrip(_ trip: Trip) {
        if trip === activeTrip {
            locationService.stopTracking()
        }

        trip.endTime = trip.endTime ?? Date()
        trip.status = "TO_REVIEW"

        var totalDistance: Double = 0
        let sortedPoints = trip.locationPoints.sorted { $0.timestamp < $1.timestamp }
        if sortedPoints.count > 1 {
            for i in 1..<sortedPoints.count {
                let p1 = sortedPoints[i-1]
                let p2 = sortedPoints[i]
                let loc1 = CLLocation(latitude: p1.latitude, longitude: p1.longitude)
                let loc2 = CLLocation(latitude: p2.latitude, longitude: p2.longitude)
                totalDistance += loc2.distance(from: loc1)
            }
        }

        trip.distanceMeters = Int(totalDistance)
        let newOdometer = trip.odometerStart + trip.distanceMeters
        trip.odometerEnd = newOdometer
        if let vehicle = trip.vehicle {
            vehicle.odometerCurrent = newOdometer
        }

        if let lastPoint = trip.locationPoints.last {
            let loc = CLLocation(latitude: lastPoint.latitude, longitude: lastPoint.longitude)
            Task {
                let resolvedAddress = await geocoderService.reverseGeocode(location: loc)

                var finalAddress = resolvedAddress
                if let context = modelContext {
                    let addresses = (try? context.fetch(FetchDescriptor<SavedAddress>())) ?? []
                    if let match = AddressMatching.findBestMatch(
                        lat: loc.coordinate.latitude,
                        lon: loc.coordinate.longitude,
                        input: resolvedAddress,
                        addresses: addresses
                    ) {
                        finalAddress = match.address.label
                    }
                }

                trip.endAddress = finalAddress
                handleWorkDayUpdates(trip: trip)
                classifyTrip(trip: trip)
                try? modelContext?.save()
                if let context = modelContext {
                    WidgetManager.updateWidget(context: context)
                }
            }
        } else {
            handleWorkDayUpdates(trip: trip)
            classifyTrip(trip: trip)
            try? modelContext?.save()
            if let context = modelContext {
                WidgetManager.updateWidget(context: context)
            }
        }

        if trip === activeTrip {
            activeTrip = nil
            state = .idle
            gracePeriodTask = nil

            if backgroundTask != .invalid {
                UIApplication.shared.endBackgroundTask(backgroundTask)
                backgroundTask = .invalid
            }

            #if canImport(ActivityKit)
            Task {
                await stopLiveActivity()
            }
            #endif
        }
    }
    
    private func classifyTrip(trip: Trip) {
        guard let context = modelContext else { return }
        let rules = (try? context.fetch(FetchDescriptor<ClassificationRule>())) ?? []
        let settingsList = (try? context.fetch(FetchDescriptor<AppSettings>())) ?? []
        
        let category = TripClassification.classify(
            tripType: trip.tripType.isEmpty ? nil : trip.tripType,
            startAddressType: nil,
            endAddressType: nil,
            startAddress: trip.startAddress,
            endAddress: trip.endAddress,
            rules: rules,
            timestamp: trip.startTime,
            appSettings: settingsList.first
        )
        trip.tripType = category.rawValue
    }
    
    private func handleWorkDayUpdates(trip: Trip) {
        guard let context = modelContext else { return }
        
        let addresses = (try? context.fetch(FetchDescriptor<SavedAddress>())) ?? []
        let settings = (try? context.fetch(FetchDescriptor<AppSettings>()))?.first
        let endMatch = AddressMatching.findBestMatch(
            lat: trip.locationPoints.last?.latitude,
            lon: trip.locationPoints.last?.longitude,
            input: trip.endAddress,
            addresses: addresses
        )
        
        let now = trip.endTime ?? Date()
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: now)
        let allWorkDays = (try? context.fetch(FetchDescriptor<WorkDay>())) ?? []
        let todayWorkDays = allWorkDays.filter { calendar.isDate($0.date, inSameDayAs: now) }
        let activeWorkDay = todayWorkDays.first(where: { $0.departureTime == nil })
        let defaultBreak = settings?.breakMinutes ?? 30
        let toleranceMinutes = settings?.toleranceMinutes ?? 30
        
        if endMatch?.address.isWorkLocation == true {
            let roundedArrival = WorkHoursNormalizer.roundToNearestQuarterHour(now)
            
            if let activeWorkDay {
                activeWorkDay.lastArrivalTime = now
                activeWorkDay.roundedArrivalTime = activeWorkDay.roundedArrivalTime ?? roundedArrival
                activeWorkDay.breakMinutes = WorkHoursNormalizer.effectiveBreakMinutes(
                    start: activeWorkDay.roundedArrivalTime ?? activeWorkDay.arrivalTime,
                    end: activeWorkDay.roundedDepartureTime ?? activeWorkDay.departureTime ?? activeWorkDay.lastArrivalTime ?? now,
                    configuredBreakMinutes: activeWorkDay.breakMinutes,
                    toleranceMinutes: toleranceMinutes
                )
            } else {
                let workDay = WorkDay(
                    date: startOfDay,
                    firstDepartureTime: trip.startTime,
                    arrivalTime: now,
                    roundedArrivalTime: roundedArrival,
                    breakMinutes: defaultBreak,
                    workLocationLabel: endMatch?.address.label,
                    status: "IN_PROGRESS"
                )
                workDay.breakMinutes = WorkHoursNormalizer.effectiveBreakMinutes(
                    start: workDay.roundedArrivalTime ?? workDay.arrivalTime,
                    end: workDay.lastArrivalTime ?? workDay.arrivalTime,
                    configuredBreakMinutes: defaultBreak,
                    toleranceMinutes: toleranceMinutes
                )
                context.insert(workDay)
                sendNotification(title: "Werkdag Gestart", body: "Aangekomen op werklocatie.")
            }
        } else if endMatch?.address.isHomeLocation == true, let activeWorkDay {
            activeWorkDay.departureTime = now
            activeWorkDay.lastArrivalTime = now
            activeWorkDay.roundedDepartureTime = WorkHoursNormalizer.roundToNearestQuarterHour(now)
            activeWorkDay.breakMinutes = WorkHoursNormalizer.effectiveBreakMinutes(
                start: activeWorkDay.roundedArrivalTime ?? activeWorkDay.arrivalTime,
                end: activeWorkDay.roundedDepartureTime ?? now,
                configuredBreakMinutes: activeWorkDay.breakMinutes > 0 ? activeWorkDay.breakMinutes : defaultBreak,
                toleranceMinutes: toleranceMinutes
            )
            activeWorkDay.status = "TO_REVIEW"
            sendNotification(title: "Werkdag Beëindigd", body: "Je bent weer thuis.")
        }
    }
    
    private func sendNotification(title: String, body: String, categoryIdentifier: String? = nil) {
        guard shouldSendNotification(title: title) else { return }

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        if let categoryIdentifier {
            content.categoryIdentifier = categoryIdentifier
        }
        
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
    
    private func shouldSendNotification(title: String) -> Bool {
        guard let settings = appSettings() else { return true }

        switch title {
        case "Werkdag Gestart", "Werkdag Beëindigd":
            return settings.workdayNotificationsEnabled ?? true
        case "Geofence Herinnering", "Mogelijke rit gestart":
            return settings.geofenceNotificationsEnabled ?? true
        case "Tracking Automatisch Gestart", "Tracking Klaar om te Starten", "Rit automatisch gestart":
            return settings.autoStartNotificationsEnabled ?? true
        default:
            return true
        }
    }
    
    private func appSettings() -> AppSettings? {
        guard let context = modelContext else { return nil }
        return (try? context.fetch(FetchDescriptor<AppSettings>()))?.first
    }
    
    // MARK: - LocationServiceDelegate
    
    func didUpdateLocation(_ location: CLLocation) {
        guard let context = modelContext, let trip = activeTrip else { return }
        
        let point = LocationPoint(
            trip: trip,
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            altitude: location.altitude,
            speed: location.speed,
            accuracy: location.horizontalAccuracy,
            timestamp: location.timestamp
        )
        context.insert(point)
        trip.locationPoints.append(point)
        
        // Asynchronously set start address if this is the first point
        if trip.locationPoints.count == 1 {
            Task {
                let resolvedAddress = await geocoderService.reverseGeocode(location: location)
                
                var finalAddress = resolvedAddress
                if let context = modelContext {
                    let addresses = (try? context.fetch(FetchDescriptor<SavedAddress>())) ?? []
                    if let match = AddressMatching.findBestMatch(
                        lat: location.coordinate.latitude,
                        lon: location.coordinate.longitude,
                        input: resolvedAddress,
                        addresses: addresses
                    ) {
                        finalAddress = match.address.label
                    }
                }
                
                trip.startAddress = finalAddress
            }
        }
        
        try? context.save()
    }
    
    func didEnterGeofence(identifier: String) {
        guard let address = savedAddress(for: identifier) else { return }
        print("Entered geofence: \(address.label) [\(address.addressType ?? "UNKNOWN")]")
        
        // Failsafe: when we arrive at a known address while a trip is still active,
        // use the same flow as a Bluetooth disconnect so the grace period can finish the trip cleanly.
        if state == .active || state == .gracePeriod {
            if gracePeriodTask == nil {
                didDisconnectFromVehicleDevice()
            }
        }
    }
    
    func didExitGeofence(identifier: String) {
        guard let address = savedAddress(for: identifier) else { return }
        print("Exited geofence: \(address.label) [\(address.addressType ?? "UNKNOWN")]")

        locationService.boostAccuracyTemporarily()

        guard state == .idle else { return }

        if shouldAutoStartTrip(afterLeaving: address) {
            startTripFromGeofenceExit(address)
        } else {
            notifyAboutPotentialTripStart(for: address)
        }
    }
    
    private func savedAddress(for identifier: String) -> SavedAddress? {
        guard let uuid = UUID(uuidString: identifier), let context = modelContext else { return nil }
        let descriptor = FetchDescriptor<SavedAddress>()
        guard let addresses = try? context.fetch(descriptor) else { return nil }
        return addresses.first(where: { $0.id == uuid })
    }

    private func shouldAutoStartTrip(afterLeaving address: SavedAddress) -> Bool {
        guard appSettings()?.autoStartNotificationsEnabled ?? true else { return false }

        let normalizedType = (address.addressType ?? "").uppercased()
        return normalizedType == "THUIS" || normalizedType == "WERK"
    }

    private func startTripFromGeofenceExit(_ address: SavedAddress) {
        let now = Date()
        if let lastAutoStartAt = lastGeofenceAutoStartAt,
           now.timeIntervalSince(lastAutoStartAt) < geofenceAutoStartCooldown {
            notifyAboutPotentialTripStart(for: address)
            return
        }

        lastGeofenceAutoStartAt = now
        startNewTrip()

        sendNotification(
            title: "Rit automatisch gestart",
            body: "Je hebt '\(address.label)' verlaten. Tracking is gestart als geofence-failsafe."
        )

        locationService.boostAccuracyTemporarily(duration: 300)
    }
    
    private func notifyAboutPotentialTripStart(for address: SavedAddress) {
        let now = Date()
        if let lastNotificationAt = lastGeofenceNotificationAt,
           now.timeIntervalSince(lastNotificationAt) < geofenceNotificationCooldown {
            return
        }
        lastGeofenceNotificationAt = now
        
        sendNotification(
            title: "Mogelijke rit gestart",
            body: notificationBody(for: address),
            categoryIdentifier: NotificationCategory.potentialTripStart
        )
    }
    
    private func notificationBody(for address: SavedAddress) -> String {
        return "Je hebt '\(address.label)' verlaten. Controleer of je rit gestart moet worden."
    }
    
    private func configureNotificationActions() {
        let startTripAction = UNNotificationAction(
            identifier: NotificationAction.startTrip,
            title: "Start rit",
            options: []
        )

        let potentialTripCategory = UNNotificationCategory(
            identifier: NotificationCategory.potentialTripStart,
            actions: [startTripAction],
            intentIdentifiers: [],
            options: []
        )

        UNUserNotificationCenter.current().setNotificationCategories([potentialTripCategory])
    }

    func handleNotificationAction(identifier: String) {
        guard identifier == NotificationAction.startTrip else { return }
        guard state == .idle else { return }
        startNewTrip()
    }

    private func requestNotificationPermissionsIfNeeded() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            guard settings.authorizationStatus == .notDetermined else { return }
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
        }
    }
    
    func didReceiveSignificantLocationUpdate(_ location: CLLocation) {
        guard state == .active, activeTrip != nil else { return }
        
        // Recovery hook: if the app was relaunched by a significant location change,
        // make sure we keep tracking the existing active trip.
        locationService.startTracking()
    }
    
    // MARK: - Live Activity
    
    #if canImport(ActivityKit)
    private func startLiveActivity(startTime: Date) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        
        let attributes = TrackingAttributes(vehicleName: "Carkit", startTime: startTime)
        let contentState = TrackingAttributes.ContentState(isGracePeriod: false)
        
        do {
            liveActivity = try Activity.request(
                attributes: attributes,
                content: .init(state: contentState, staleDate: nil),
                pushType: nil
            )
        } catch {
            print("Failed to start Live Activity: \(error)")
        }
    }
    
    private func updateLiveActivity(isGracePeriod: Bool) async {
        guard let activity = liveActivity else { return }
        let contentState = TrackingAttributes.ContentState(isGracePeriod: isGracePeriod)
        await activity.update(ActivityContent(state: contentState, staleDate: nil))
    }
    
    private func stopLiveActivity() async {
        guard let activity = liveActivity else { return }
        let contentState = TrackingAttributes.ContentState(isGracePeriod: false)
        await activity.end(ActivityContent(state: contentState, staleDate: nil), dismissalPolicy: .immediate)
        liveActivity = nil
    }
    #endif
}
