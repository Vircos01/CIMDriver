import SwiftUI
import SwiftData

struct VehicleSelectionView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var vehicles: [Vehicle]
    
    @State private var showingAddVehicle = false
    @State private var vehicleToEdit: Vehicle?
    
    var body: some View {
        List {
            if vehicles.isEmpty {
                Text("Geen voertuigen gevonden. Voeg een voertuig toe.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(vehicles) { vehicle in
                    Button(action: {
                        setDefaultVehicle(vehicle)
                    }) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(vehicle.name)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text("\(vehicle.make) \(vehicle.model) - \(vehicle.licensePlate)")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if vehicle.isDefault {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.cimGreen)
                                    .font(.title2)
                            }
                            
                            Button(action: {
                                vehicleToEdit = vehicle
                            }) {
                                Image(systemName: "info.circle")
                                    .foregroundColor(.accentColor)
                                    .padding(.leading, 8)
                            }
                        }
                    }
                }
                .onDelete(perform: deleteVehicles)
            }
        }
        .navigationTitle("Mijn Auto's")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showingAddVehicle = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAddVehicle) {
            NavigationStack {
                VehicleEditView()
            }
        }
        .sheet(item: $vehicleToEdit) { vehicle in
            NavigationStack {
                VehicleEditView(existingVehicle: vehicle)
            }
        }
    }

    
    private func setDefaultVehicle(_ selectedVehicle: Vehicle) {
        for vehicle in vehicles {
            vehicle.isDefault = (vehicle.id == selectedVehicle.id)
        }
        try? modelContext.save()
    }
    
    private func deleteVehicles(offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(vehicles[index])
        }
        try? modelContext.save()
    }
}

struct VehicleEditView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    var existingVehicle: Vehicle?
    
    @State private var name: String = ""
    @State private var licensePlate: String = ""
    @State private var make: String = ""
    @State private var model: String = ""
    @State private var privateKmYearlyLimit: String = "500"
    @State private var odometerStart: String = "0"
    @State private var usageType: String = "MIXED"
    @State private var odometerCorrectionStrategy: String = "DISTRIBUTE"
    
    var body: some View {
        Form {
            Section(header: Text("Voertuig details")) {
                TextField("Bijnaam (bijv. Auto van de zaak)", text: $name)
                TextField("Kenteken", text: $licensePlate)
                TextField("Merk", text: $make)
                TextField("Model", text: $model)
            }
            
            Section(
                header: Text("Kilometerstand & Limieten"),
                footer: Text("De beginstand is de kilometerstand op het moment dat je de auto begon te registreren. De limiet privé (standaard 500 km) helpt je de bijtellingsgrens te bewaken.")
            ) {
                HStack {
                    Text("Beginstand (km)")
                    Spacer()
                    TextField("0", text: $odometerStart)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                }
                HStack {
                    Text("Limiet Privé (km/jr)")
                    Spacer()
                    TextField("500", text: $privateKmYearlyLimit)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                }
            }
            
            Section(header: Text("Geavanceerd")) {
                Picker("Gebruikstype", selection: $usageType) {
                    Text("Gemengd (Privé & Zakelijk)").tag("MIXED")
                    Text("Puur Zakelijk").tag("BUSINESS_ONLY")
                    Text("Puur Privé").tag("PRIVATE_ONLY")
                }
                
                Picker("Kilometer Kalibratie", selection: $odometerCorrectionStrategy) {
                    Text("Verschil uitsmeren over ritten").tag("DISTRIBUTE")
                    Text("Correctierit aanmaken").tag("CREATE_TRIP")
                    Text("Gat openlaten").tag("LEAVE_GAP")
                }
            }
            
            if let v = existingVehicle {
                Section("Bluetooth Carkit") {
                    NavigationLink(destination: VehicleBluetoothView(vehicle: v)) {
                        Label("Beheer Bluetooth Carkits", systemImage: "car.side")
                    }
                }
            }
        }
        .navigationTitle(existingVehicle != nil ? "Bewerk Auto" : "Nieuwe Auto")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if let v = existingVehicle {
                name = v.name
                licensePlate = v.licensePlate
                make = v.make
                model = v.model
                privateKmYearlyLimit = String(v.privateKmYearlyLimit)
                odometerStart = String(v.odometerStart)
                usageType = v.usageType
                odometerCorrectionStrategy = v.odometerCorrectionStrategy
            }
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Annuleer") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Opslaan") {
                    saveVehicle()
                    dismiss()
                }
                .disabled(name.isEmpty || licensePlate.isEmpty)
            }
        }
    }
    
    private func saveVehicle() {
        if let v = existingVehicle {
            v.name = name
            v.licensePlate = licensePlate
            v.make = make
            v.model = model
            v.privateKmYearlyLimit = Int(privateKmYearlyLimit) ?? 500
            v.usageType = usageType
            v.odometerCorrectionStrategy = odometerCorrectionStrategy
        } else {
            let vehicle = Vehicle(
                name: name,
                licensePlate: licensePlate,
                make: make,
                model: model,
                odometerStart: Int(odometerStart) ?? 0,
                odometerCurrent: Int(odometerStart) ?? 0,
                privateKmYearlyLimit: Int(privateKmYearlyLimit) ?? 500,
                usageType: usageType,
                odometerCorrectionStrategy: odometerCorrectionStrategy
            )
            modelContext.insert(vehicle)
        }
        try? modelContext.save()
    }
}

struct AutoPreviewProviderVehicleSelectionView1: PreviewProvider {
    static var previews: some View {
            NavigationStack {
                VehicleSelectionView()
                    .modelContainer(PreviewContainer.shared.container)
            }
    }
}
struct OdometerCalibrationView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    var vehicle: Vehicle
    
    @State private var newOdometerString: String = ""
    @State private var showingWarning = false
    @State private var calculatedDifference: Int = 0
    
    var body: some View {
        Form {
            Section("Huidige Stand") {
                LabeledContent("App Odometer", value: "\(vehicle.odometerCurrent) km")
                if let lastCheck = vehicle.lastOdometerCheckTimestamp {
                    LabeledContent("Laatste IJking", value: lastCheck.formatted(date: .abbreviated, time: .omitted))
                } else {
                    LabeledContent("Laatste IJking", value: "Nooit")
                }
            }
            
            Section("Nieuwe Stand") {
                TextField("Stand op dashboard (km)", text: $newOdometerString)
                    .keyboardType(.numberPad)
                    .onChange(of: newOdometerString) { _, newValue in
                        if let newStand = Int(newValue) {
                            calculatedDifference = newStand - vehicle.odometerCurrent
                        } else {
                            calculatedDifference = 0
                        }
                    }
                
                if calculatedDifference != 0 {
                    LabeledContent("Verschil", value: "\(calculatedDifference > 0 ? "+" : "")\(calculatedDifference) km")
                        .foregroundStyle(abs(calculatedDifference) > 50 ? .orange : .primary)
                }
            }
            
            if abs(calculatedDifference) > 50 {
                Section {
                    Text("Let op: Het verschil is relatief groot. De app zal dit verschil corrigeren volgens de strategie: \(correctionStrategyText()).")
                        .font(.footnote)
                        .foregroundStyle(.orange)
                }
            }
            
            Section {
                Button(action: {
                    if abs(calculatedDifference) > 100 {
                        showingWarning = true
                    } else {
                        saveCalibration()
                    }
                }) {
                    Text("Opslaan & Kalibreren")
                        .frame(maxWidth: .infinity)
                        .bold()
                }
                .tint(.cimGreen)
                .disabled(newOdometerString.isEmpty || Int(newOdometerString) == nil)
            }
        }
        .navigationTitle("Kalibratie")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            newOdometerString = String(vehicle.odometerCurrent)
        }
        .alert("Groot Verschil", isPresented: $showingWarning) {
            Button("Toch Opslaan", role: .destructive) {
                saveCalibration()
            }
            Button("Annuleer", role: .cancel) { }
        } message: {
            Text("Je staat op het punt een verschil van \(calculatedDifference) km te corrigeren. Weet je zeker dat de ingevulde stand klopt?")
        }
    }
    
    private func correctionStrategyText() -> String {
        switch vehicle.odometerCorrectionStrategy {
        case "DISTRIBUTE": return "Uitsmeren over ritten"
        case "CREATE_TRIP": return "Correctierit aanmaken"
        case "LEAVE_GAP": return "Gat openlaten"
        default: return "Onbekend"
        }
    }
    
    private func saveCalibration() {
        guard let newStand = Int(newOdometerString) else { return }
        
        let diff = newStand - vehicle.odometerCurrent
        
        // Simpele logica: updaten we gewoon de teller, in een echte app genereren we hier een correctie.
        if vehicle.odometerCorrectionStrategy == "CREATE_TRIP" && diff != 0 {
            let correctionTrip = Trip(
                vehicle: vehicle,
                startTime: Date(),
                endTime: Date(),
                startAddress: "Kalibratie Correctie",
                endAddress: "Kalibratie Correctie",
                distanceMeters: diff * 1000,
                tripType: "PRIVATE",
                note: "Automatische correctierit",
                status: "APPROVED",
                isManual: true,
                odometerStart: vehicle.odometerCurrent,
                odometerEnd: newStand
            )
            modelContext.insert(correctionTrip)
        }
        
        vehicle.odometerCurrent = newStand
        vehicle.lastOdometerCheckTimestamp = Date()
        
        try? modelContext.save()
        dismiss()
    }
}
