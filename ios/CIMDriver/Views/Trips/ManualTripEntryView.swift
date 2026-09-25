import SwiftUI
import SwiftData
import MapKit
import CoreLocation

struct ManualTripEntryView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @Query private var vehicles: [Vehicle]
    @Query private var rules: [ClassificationRule]
    @Query private var settingsList: [AppSettings]

    private var appSettings: AppSettings? { settingsList.first }
    
    @State private var selectedVehicleId: UUID?
    @State private var startTime: Date = Date()
    @State private var endTime: Date = Date().addingTimeInterval(3600)
    
    @State private var startAddress: String = ""
    @State private var endAddress: String = ""
    
    @State private var distanceKm: String = ""
    @State private var tripType: String = TripCategory.business.rawValue
    @State private var projectCode: String = ""
    
    @State private var showingStartPicker = false
    @State private var showingEndPicker = false
    @State private var note: String = ""
    
    @State private var suggestedRule: ClassificationRule?
    @State private var showingSuggestionAlert = false
    
    @State private var calculateTask: Task<Void, Never>?
    @State private var calculatedRoute: MKRoute?
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Voertuig")) {
                    Picker("Selecteer Voertuig", selection: $selectedVehicleId) {
                        Text("Geen voertuig geselecteerd").tag(UUID?.none)
                        ForEach(vehicles) { vehicle in
                            Text(vehicle.name).tag(UUID?.some(vehicle.id))
                        }
                    }
                }
                
                Section(header: Text("Tijden")) {
                    DatePicker("Starttijd", selection: $startTime)
                    DatePicker("Eindtijd", selection: $endTime)
                }
                
                Section(header: Text("Locatie & Afstand")) {
                    HStack {
                        TextField("Vertrekadres", text: $startAddress)
                            .onChange(of: startAddress) { _, _ in 
                                suggestClassification()
                                triggerDistanceCalculation()
                            }
                        Button(action: { showingStartPicker = true }) {
                            Image(systemName: "book.pages")
                                .foregroundColor(.accentColor)
                        }
                    }
                    HStack {
                        TextField("Aankomstadres", text: $endAddress)
                            .onChange(of: endAddress) { _, _ in 
                                suggestClassification()
                                triggerDistanceCalculation()
                            }
                        Button(action: { showingEndPicker = true }) {
                            Image(systemName: "book.pages")
                                .foregroundColor(.accentColor)
                        }
                    }
                    TextField("Afstand (km)", text: $distanceKm)
                        .keyboardType(.decimalPad)
                }
                
                Section(header: Text("Classificatie")) {
                    Picker("Rit Type", selection: $tripType) {
                        Text("Privé").tag(TripCategory.privateTrip.rawValue)
                        Text("Zakelijk").tag(TripCategory.business.rawValue)
                        Text("Woon-Werk").tag(TripCategory.commute.rawValue)
                    }
                    .pickerStyle(.segmented)
                    
                    if tripType == TripCategory.business.rawValue {
                        TextField("Projectcode", text: $projectCode)
                    }
                }
                
                Section(header: Text("Notities")) {
                    TextField("Optionele notitie", text: $note, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle("Nieuwe Rit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleer") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan") {
                        saveTrip()
                    }
                    .disabled(startAddress.isEmpty || endAddress.isEmpty || selectedVehicleId == nil)
                }
            }
            .onAppear {
                if selectedVehicleId == nil {
                    selectedVehicleId = vehicles.first(where: { $0.isDefault })?.id ?? vehicles.first?.id
                }
            }
            .alert("Regel Voorstel", isPresented: $showingSuggestionAlert, presenting: suggestedRule) { rule in
                Button("Opslaan") {
                    modelContext.insert(rule)
                    try? modelContext.save()
                    dismiss()
                }
                Button("Nee, bedankt", role: .cancel) {
                    dismiss()
                }
            } message: { rule in
                Text("Je hebt deze route nu 3 keer gereden als '\(rule.category)'. Wil je hier een vaste regel van maken?")
            }
            .sheet(isPresented: $showingStartPicker) {
                AddressSelectionSheet(selectedAddressText: $startAddress)
            }
            .sheet(isPresented: $showingEndPicker) {
                AddressSelectionSheet(selectedAddressText: $endAddress)
            }
        }
    }
    
    private func suggestClassification() {
        // Gebruik de TripClassification module om automatisch een suggestie te doen
        let category = TripClassification.classify(
            tripType: nil,
            startAddressType: nil,
            endAddressType: nil,
            startAddress: startAddress,
            endAddress: endAddress,
            rules: rules,
            timestamp: startTime,
            appSettings: appSettings
        )
        self.tripType = category.rawValue
    }
    
    private func triggerDistanceCalculation() {
        guard !startAddress.isEmpty && !endAddress.isEmpty else { return }
        
        calculateTask?.cancel()
        calculateTask = Task {
            do {
                try await Task.sleep(nanoseconds: 1_000_000_000) // 1 second debounce
                if !Task.isCancelled {
                    await calculateDistance()
                }
            } catch { }
        }
    }
    
    private func calculateDistance() async {
        let geocoder = CLGeocoder()
        
        do {
            let startPlacemarks = try await geocoder.geocodeAddressString(startAddress)
            let endPlacemarks = try await geocoder.geocodeAddressString(endAddress)
            
            guard let startPlacemark = startPlacemarks.first, let endPlacemark = endPlacemarks.first else { return }
            
            let request = MKDirections.Request()
            request.source = MKMapItem(placemark: MKPlacemark(placemark: startPlacemark))
            request.destination = MKMapItem(placemark: MKPlacemark(placemark: endPlacemark))
            request.transportType = .automobile
            
            let directions = MKDirections(request: request)
            let response = try await directions.calculate()
            
            if let route = response.routes.first {
                let distanceInKm = route.distance / 1000.0
                await MainActor.run {
                    self.distanceKm = String(format: "%.1f", distanceInKm)
                    self.calculatedRoute = route
                }
            }
        } catch {
            print("Error calculating distance: \(error)")
        }
    }
    
    private func saveTrip() {
        let vehicle = vehicles.first { $0.id == selectedVehicleId }
        
        let distanceMeters = (Double(distanceKm.replacingOccurrences(of: ",", with: ".")) ?? 0) * 1000
        
        let newTrip = Trip(
            vehicle: vehicle,
            startTime: startTime,
            endTime: endTime,
            startAddress: startAddress,
            endAddress: endAddress,
            distanceMeters: Int(distanceMeters),
            tripType: tripType,
            note: note.isEmpty ? nil : note,
            status: "DONE",
            isManual: true,
            odometerStart: vehicle?.odometerCurrent ?? 0,
            odometerEnd: (vehicle?.odometerCurrent ?? 0) + Int(distanceMeters),
            projectCode: projectCode.isEmpty ? nil : projectCode
        )
        
        if let route = calculatedRoute {
            let polyline = route.polyline
            let pointCount = polyline.pointCount
            var coordinates = [CLLocationCoordinate2D](repeating: kCLLocationCoordinate2DInvalid, count: pointCount)
            polyline.getCoordinates(&coordinates, range: NSRange(location: 0, length: pointCount))
            
            let totalTime = endTime.timeIntervalSince(startTime)
            let timePerPoint = pointCount > 1 ? totalTime / Double(pointCount - 1) : 0
            
            for i in 0..<pointCount {
                let coord = coordinates[i]
                let timestamp = startTime.addingTimeInterval(timePerPoint * Double(i))
                let locationPoint = LocationPoint(
                    latitude: coord.latitude,
                    longitude: coord.longitude,
                    altitude: 0,
                    speed: 0,
                    accuracy: 10,
                    timestamp: timestamp
                )
                newTrip.locationPoints.append(locationPoint)
            }
        }
        
        modelContext.insert(newTrip)
        
        if let v = vehicle {
            v.odometerCurrent += Int(distanceMeters)
        }
        
        try? modelContext.save()
        
        let allTrips = (try? modelContext.fetch(FetchDescriptor<Trip>())) ?? []
        if let suggestion = PatternDetector.detectRuleSuggestion(for: newTrip, context: modelContext, allTrips: allTrips) {
            self.suggestedRule = suggestion
            self.showingSuggestionAlert = true
        } else {
            dismiss()
        }
    }
}
