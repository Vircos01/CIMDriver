import SwiftUI
import SwiftData
import MapKit

struct TripDetailView: View {
    @Environment(\.modelContext) private var modelContext
    let trip: Trip
    @State private var showingEditSheet = false
    @Environment(\.dismiss) private var dismiss
    
    @State private var suggestedRule: ClassificationRule?
    @State private var showingSuggestionAlert = false
    @State private var showingDeleteAlert = false
    
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
                    MapPolyline(coordinates: coordinates)
                        .stroke(.blue, lineWidth: 5)
                    
                    if let first = coordinates.first {
                        Annotation("Start", coordinate: first) {
                            Image(systemName: "mappin.circle.fill")
                                .foregroundColor(.green)
                                .background(.white)
                                .clipShape(Circle())
                        }
                    }
                    if let last = coordinates.last {
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
                
                let numberFormatter: NumberFormatter = {
                    let formatter = NumberFormatter()
                    formatter.numberStyle = .decimal
                    formatter.groupingSeparator = "."
                    formatter.locale = Locale(identifier: "nl_NL")
                    return formatter
                }()
                let odometerStartText = numberFormatter.string(from: NSNumber(value: trip.odometerStart)) ?? String(trip.odometerStart)
                let odometerEndText = trip.odometerEnd.map {
                    numberFormatter.string(from: NSNumber(value: $0)) ?? String($0)
                } ?? "—"
                Text("Km-stand: \(odometerStartText) km → \(odometerEndText) km")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
            .background(Color(.systemBackground).opacity(0.95))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(radius: 5)
            .padding()
            
            // Action Buttons for TO_REVIEW
            if trip.status == TripStatus.toReview.rawValue {
                VStack {
                    Spacer()
                    HStack(spacing: 12) {
                        Button(action: { quickClassify("BUSINESS") }) {
                            Text("Zakelijk")
                                .bold()
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.cimNavy)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        Button(action: { quickClassify("COMMUTE") }) {
                            Text("Woon-Werk")
                                .bold()
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        Button(action: { quickClassify("PRIVATE") }) {
                            Text("Privé")
                                .bold()
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.orange)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground).opacity(0.95))
                    .shadow(radius: 5, y: -2)
                }
            }
        }
        .navigationTitle("Rit Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button(action: { showingEditSheet = true }) {
                        Label("Bewerken", systemImage: "pencil")
                    }
                    Button(role: .destructive, action: { showingDeleteAlert = true }) {
                        Label("Verwijderen", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
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
    
    let trip: Trip
    
    @State private var tripType: String
    @State private var startAddress: String
    @State private var endAddress: String
    @State private var distanceString: String
    @State private var notes: String
    @State private var projectCode: String
    
    @State private var suggestedRule: ClassificationRule?
    @State private var showingSuggestionAlert = false
    @State private var showingStartPicker = false
    @State private var showingEndPicker = false
    
    init(trip: Trip) {
        self.trip = trip
        _tripType = State(initialValue: trip.tripType)
        _startAddress = State(initialValue: trip.startAddress ?? "")
        _endAddress = State(initialValue: trip.endAddress ?? "")
        _distanceString = State(initialValue: String(format: "%.1f", Double(trip.distanceMeters) / 1000.0))
        _notes = State(initialValue: trip.note ?? "")
        _projectCode = State(initialValue: trip.projectCode ?? "")
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
                        TextField("Projectcode", text: $projectCode)
                    }
                    
                    TextField("Afstand (km)", text: $distanceString)
                        .keyboardType(.decimalPad)
                    
                    TextField("Optionele notitie", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
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
        
        let formatter = NumberFormatter()
        formatter.decimalSeparator = ","
        if let dist = formatter.number(from: distanceString)?.doubleValue {
            trip.distanceMeters = Int(dist * 1000)
        } else {
            formatter.decimalSeparator = "."
            if let dist = formatter.number(from: distanceString)?.doubleValue {
                trip.distanceMeters = Int(dist * 1000)
            }
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
