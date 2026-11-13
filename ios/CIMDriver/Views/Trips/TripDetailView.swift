import SwiftUI
import SwiftData
import MapKit

struct TripDetailView: View {
    @Environment(\.modelContext) private var modelContext
    let trip: Trip
    @Query private var rules: [ClassificationRule]
    @Query private var addresses: [SavedAddress]
    @Query private var settingsList: [AppSettings]
    @State private var showingEditSheet = false
    @Environment(\.dismiss) private var dismiss
    
    @State private var suggestedRule: ClassificationRule?
    @State private var showingSuggestionAlert = false
    @State private var showingDeleteAlert = false
    @State private var showingFavoriteAlert = false
    @State private var favoriteRouteName = ""
    
    private var assessment: TripClassification.ConfidenceAssessment {
        let startAddrType = addresses.first(where: { $0.address.lowercased() == (trip.startAddress ?? "").lowercased() })?.addressType
        let endAddrType = addresses.first(where: { $0.address.lowercased() == (trip.endAddress ?? "").lowercased() })?.addressType
        return TripClassification.assessConfidence(
            tripType: trip.tripType,
            startAddressType: startAddrType,
            endAddressType: endAddrType,
            startAddress: trip.startAddress,
            endAddress: trip.endAddress,
            rules: rules,
            timestamp: trip.startTime,
            appSettings: settingsList.first,
            isBluetoothTriggered: trip.vehicle != nil
        )
    }

    private var suggestedTripTypeString: String {
        switch assessment.category {
        case .commute: return "COMMUTE"
        case .business: return "BUSINESS"
        case .privateTrip: return "PRIVATE"
        }
    }
    
    private var coordinates: [CLLocationCoordinate2D] {
        trip.locationPoints.sorted { $0.timestamp < $1.timestamp }.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
    }
    
    private var durationString: String {
        guard let end = trip.endTime else { return "-" }
        let minutes = max(0, Int(end.timeIntervalSince(trip.startTime) / 60))
        return "\(minutes)m"
    }

    private var odometerSummaryText: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        formatter.locale = Locale(identifier: "nl_NL")
        let start = formatter.string(from: NSNumber(value: trip.odometerStart)) ?? "\(trip.odometerStart)"
        let end = trip.odometerEnd.flatMap { formatter.string(from: NSNumber(value: $0)) } ?? "—"
        return "Km-stand: \(start) km → \(end) km"
    }

    private var auditContextView: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("ℹ️ Ritaudit & Context")
                .font(.caption.bold())
                .foregroundStyle(.secondary)
            Text(trip.isManual ? "• Handmatig ingevoerde rit" : "• Automatisch geregistreerd via GPS")
                .font(.caption2)
                .foregroundStyle(.secondary)
            if let vehicle = trip.vehicle {
                Text("• Voertuig: \(vehicle.make) \(vehicle.model) (\(vehicle.licensePlate))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            if trip.privateDetourMeters > 0 {
                Text("• Privé-omrijden verwerkt: \(String(format: "%.1f km", Double(trip.privateDetourMeters) / 1000.0))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var reviewActionOverlay: some View {
        VStack(spacing: 8) {
            Spacer()
            
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("💡 Suggestie van CIMDriver")
                        .font(.caption.bold())
                        .foregroundColor(.primary)
                    Spacer()
                    Text("\(assessment.confidenceScore)% zeker")
                        .font(.caption2.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(assessment.isHighConfidence ? Color.cimGreen.opacity(0.15) : Color.blue.opacity(0.15))
                        .foregroundColor(assessment.isHighConfidence ? Color.cimGreen : Color.blue)
                        .clipShape(Capsule())
                }
                
                let catTitle = assessment.category == .commute ? "Woon-Werk" : (assessment.category == .business ? "Zakelijk" : "Privé")
                Text("Voorstel: \(catTitle) — \(assessment.reasoning)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Button(action: { quickClassify(suggestedTripTypeString) }) {
                    Text("✓ Accepteren (\(catTitle))")
                        .bold()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(assessment.isHighConfidence ? Color.cimGreen : Color.cimNavy)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding()
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(radius: 3)
            .padding(.horizontal)
            
            HStack(spacing: 12) {
                Button(action: { quickClassify("BUSINESS") }) {
                    Text("Zakelijk")
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.cimNavy.opacity(0.15))
                        .foregroundColor(Color.cimNavy)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                
                Button(action: { quickClassify("COMMUTE") }) {
                    Text("Woon-Werk")
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.blue.opacity(0.15))
                        .foregroundColor(.blue)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                
                Button(action: { quickClassify("PRIVATE") }) {
                    Text("Privé")
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.orange.opacity(0.15))
                        .foregroundColor(.orange)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
        .background(Color(.systemBackground).opacity(0.95))
        .shadow(radius: 5, y: -2)
    }

    private var infoCardOverlay: some View {
        VStack(alignment: .leading, spacing: 8) {
            let dateStr = trip.startTime.formatted(date: .abbreviated, time: .omitted)
            let startT = trip.startTime.formatted(date: .omitted, time: .shortened)
            let endT = trip.endTime?.formatted(date: .omitted, time: .shortened) ?? "-"
            
            Text("Datum: \(dateStr), \(startT) - \(endT)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            
            Text("Van: \(trip.startAddress ?? "Onbekend")")
                .font(.subheadline)
                .bold()
            Text("Naar: \(trip.endAddress ?? "Onbekend")")
                .font(.subheadline)
                .bold()
            
            HStack {
                Text("Afstand: ") + Text(String(format: "%.1f km", Double(trip.distanceMeters) / 1000.0)).bold()
                Text("Duur: \(durationString)")
                    .foregroundStyle(.secondary)
                Spacer()
                Text("Type: \(typeString)")
                    .foregroundStyle(.secondary)
            }
            .font(.subheadline)
            .padding(.top, 4)
            
            Text(odometerSummaryText)
                .font(.caption)
                .foregroundStyle(.secondary)
            
            if trip.privateDetourMeters > 0 {
                Text("Privé-omrijden: \(String(format: "%.1f km", Double(trip.privateDetourMeters) / 1000.0))")
                    .font(.caption)
                    .foregroundStyle(.orange)
            }

            Divider()

            auditContextView
        }
        .padding()
        .background(Color(.systemBackground).opacity(0.95))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 5)
        .padding()
    }
    
    private var typeString: String {
        switch trip.tripType {
        case "BUSINESS": return "Zakelijk"
        case "COMMUTE": return "Woon-Werk"
        default: return "Privé"
        }
    }
    
    var body: some View {
        ZStack(alignment: .top) {
            // Background Map
            if !coordinates.isEmpty {
                Map {
                    if coordinates.count >= 2 {
                        MapPolyline(coordinates: coordinates)
                            .stroke(.blue, lineWidth: 5)
                    }
                    
                    if let first = coordinates.first {
                        Annotation("Start", coordinate: first) {
                            Image(systemName: "mappin.circle.fill")
                                .foregroundColor(.green)
                                .background(.white)
                                .clipShape(Circle())
                        }
                    }
                    if coordinates.count >= 2, let last = coordinates.last {
                        Annotation("Eind", coordinate: last) {
                            Image(systemName: "mappin.circle.fill")
                                .foregroundColor(.red)
                                .background(.white)
                                .clipShape(Circle())
                        }
                    }
                }
                .edgesIgnoringSafeArea(.bottom)
            } else {
                VStack {
                    Spacer()
                    Image(systemName: "map")
                        .font(.largeTitle)
                        .foregroundColor(.gray)
                    Text("Geen routegegevens")
                        .foregroundColor(.gray)
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemGroupedBackground))
            }
            
            // Info Card Overlay
            infoCardOverlay
            
            // Action Buttons for TO_REVIEW
            if trip.status == TripStatus.toReview.rawValue {
                reviewActionOverlay
            }
        }
        .navigationTitle("Rit Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                HStack(spacing: 16) {
                    Button(action: { showingEditSheet = true }) {
                        Image(systemName: "pencil")
                    }
                    
                    Button(role: .destructive, action: { showingDeleteAlert = true }) {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                    
                    Menu {
                        Button(action: { showingFavoriteAlert = true }) {
                            Label("Opslaan als favoriet", systemImage: "star")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .sheet(isPresented: $showingEditSheet) {
            EditTripView(trip: trip)
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
        .alert("Opslaan als favoriete route", isPresented: $showingFavoriteAlert) {
            TextField("Naam route", text: $favoriteRouteName)
            Button("Annuleer", role: .cancel) {
                favoriteRouteName = ""
            }
            Button("Opslaan") {
                saveAsFavoriteRoute()
            }
        } message: {
            Text("Geef deze route een naam om hem later snel te kunnen selecteren.")
        }
        .alert("Rit Verwijderen", isPresented: $showingDeleteAlert) {
            Button("Annuleer", role: .cancel) { }
            Button("Verwijder", role: .destructive) {
                modelContext.delete(trip)
                try? modelContext.save()
                dismiss()
            }
        } message: {
            Text("Weet je zeker dat je deze rit wilt verwijderen? Dit kan niet ongedaan worden gemaakt.")
        }
    }
    
    private func quickClassify(_ type: String) {
        trip.tripType = type
        trip.status = TripStatus.done.rawValue
        try? modelContext.save()
        
        let allTrips = (try? modelContext.fetch(FetchDescriptor<Trip>())) ?? []
        if let suggestion = PatternDetector.detectRuleSuggestion(for: trip, context: modelContext, allTrips: allTrips) {
            self.suggestedRule = suggestion
            self.showingSuggestionAlert = true
        } else {
            dismiss()
        }
    }
    
    private func saveAsFavoriteRoute() {
        let route = FavoriteRoute(
            name: favoriteRouteName.isEmpty ? "Mijn Route" : favoriteRouteName,
            startAddress: trip.startAddress ?? "",
            endAddress: trip.endAddress ?? "",
            tripType: trip.tripType
        )
        modelContext.insert(route)
        try? modelContext.save()
        favoriteRouteName = ""
    }
}

struct AutoPreviewProviderTripDetailView1: PreviewProvider {
    static var previews: some View {
            NavigationStack {
                TripDetailView(trip: Trip(startTime: Date(), status: "TO_REVIEW", odometerStart: 0))
                    .modelContainer(PreviewContainer.shared.container)
            }
    }
}

struct EditTripView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @Query private var trips: [Trip]
    
    let trip: Trip
    
    @State private var tripType: String
    @State private var startAddress: String
    @State private var endAddress: String
    @State private var distanceString: String
    @State private var privateDetourKm: String
    @State private var notes: String
    @State private var projectCode: String
    @State private var startTime: Date
    @State private var endTime: Date
    @State private var odometerStartString: String
    @State private var odometerEndString: String
    
    @State private var suggestedRule: ClassificationRule?
    @State private var showingSuggestionAlert = false
    @State private var showingStartPicker = false
    @State private var showingEndPicker = false
    
    private var uniqueProjectCodes: [String] {
        Array(Set(trips.compactMap { $0.projectCode }).filter { !$0.isEmpty }).sorted()
    }
    
    init(trip: Trip) {
        self.trip = trip
        _tripType = State(initialValue: trip.tripType)
        _startAddress = State(initialValue: trip.startAddress ?? "")
        _endAddress = State(initialValue: trip.endAddress ?? "")
        _distanceString = State(initialValue: String(format: "%.1f", Double(trip.distanceMeters) / 1000.0))
        _privateDetourKm = State(initialValue: trip.privateDetourMeters > 0 ? String(format: "%.1f", Double(trip.privateDetourMeters) / 1000.0) : "")
        _notes = State(initialValue: trip.note ?? "")
        _projectCode = State(initialValue: trip.projectCode ?? "")
        _startTime = State(initialValue: trip.startTime)
        _endTime = State(initialValue: trip.endTime ?? trip.startTime.addingTimeInterval(Double(max(60, trip.distanceMeters / 15))))
        _odometerStartString = State(initialValue: String(trip.odometerStart))
        let effectiveEnd = trip.odometerEnd ?? (trip.odometerStart + Int(round(Double(trip.distanceMeters) / 1000.0)))
        _odometerEndString = State(initialValue: String(effectiveEnd))
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    Picker("Rit Type", selection: $tripType) {
                        Text("Zakelijk").tag("BUSINESS")
                        Text("Privé").tag("PRIVATE")
                        Text("Woon-werk").tag("COMMUTE")
                    }
                    
                    if tripType == "BUSINESS" {
                        HStack {
                            TextField("Projectcode", text: $projectCode)
                            if !uniqueProjectCodes.isEmpty {
                                Menu {
                                    ForEach(uniqueProjectCodes, id: \.self) { code in
                                        Button(code) { projectCode = code }
                                    }
                                } label: {
                                    Image(systemName: "chevron.down.circle")
                                        .foregroundColor(.accentColor)
                                }
                            }
                        }
                    }
                    
                    TextField("Afstand (km)", text: $distanceString)
                        .keyboardType(.decimalPad)
                    
                    if tripType != "PRIVATE" {
                        TextField("Privé-omrijden (km)", text: $privateDetourKm)
                            .keyboardType(.decimalPad)
                    }
                    
                    TextField("Optionele notitie", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
                
                Section("Tijdstippen") {
                    DatePicker("Starttijd", selection: $startTime)
                    DatePicker("Eindtijd", selection: $endTime)
                }
                
                Section("Tellerstanden") {
                    HStack {
                        Text("Beginstand (km)")
                        Spacer()
                        TextField("Beginstand", text: $odometerStartString)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Eindstand (km)")
                        Spacer()
                        TextField("Eindstand", text: $odometerEndString)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                
                Section("Locatie") {
                    HStack {
                        TextField("Start adres", text: $startAddress)
                        Button(action: { showingStartPicker = true }) {
                            Image(systemName: "book.pages")
                                .foregroundColor(.accentColor)
                        }
                    }
                    HStack {
                        TextField("Eind adres", text: $endAddress)
                        Button(action: { showingEndPicker = true }) {
                            Image(systemName: "book.pages")
                                .foregroundColor(.accentColor)
                        }
                    }
                }
            }
            .navigationTitle("Rit Bewerken")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleer") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan", action: saveTrip)
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
    
    private func saveTrip() {
        trip.tripType = tripType
        trip.startAddress = startAddress.isEmpty ? nil : startAddress
        trip.endAddress = endAddress.isEmpty ? nil : endAddress
        trip.note = notes.isEmpty ? nil : notes
        trip.projectCode = projectCode.isEmpty ? nil : projectCode
        trip.startTime = startTime
        trip.endTime = endTime
        
        let formatter = NumberFormatter()
        formatter.decimalSeparator = ","
        var parsedDistanceMeters = trip.distanceMeters
        if let dist = formatter.number(from: distanceString)?.doubleValue {
            parsedDistanceMeters = max(0, Int(dist * 1000))
        } else {
            formatter.decimalSeparator = "."
            if let dist = formatter.number(from: distanceString)?.doubleValue {
                parsedDistanceMeters = max(0, Int(dist * 1000))
            }
        }
        trip.distanceMeters = parsedDistanceMeters
        
        let detourKmVal = Double(privateDetourKm.replacingOccurrences(of: ",", with: ".")) ?? 0.0
        trip.privateDetourMeters = max(0, Int(detourKmVal * 1000.0))
        
        if let odoStart = Int(odometerStartString) {
            trip.odometerStart = odoStart
        }
        if let odoEnd = Int(odometerEndString) {
            trip.odometerEnd = odoEnd
        } else {
            trip.odometerEnd = trip.odometerStart + Int(round(Double(trip.distanceMeters) / 1000.0))
        }
        
        // Ensure vehicle current odometer reflects latest trip end
        if let vehicle = trip.vehicle, let end = trip.odometerEnd, end > vehicle.odometerCurrent {
            vehicle.odometerCurrent = end
        }
        
        trip.status = "APPROVED" // Rit is handmatig goedgekeurd door te bewerken
        try? modelContext.save()
        
        let allTrips = (try? modelContext.fetch(FetchDescriptor<Trip>())) ?? []
        if let suggestion = PatternDetector.detectRuleSuggestion(for: trip, context: modelContext, allTrips: allTrips) {
            self.suggestedRule = suggestion
            self.showingSuggestionAlert = true
        } else {
            dismiss()
        }
    }
}
