import SwiftUI
import SwiftData

struct AddFuelView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @Query private var vehicles: [Vehicle]
    
    @State private var selectedVehicle: Vehicle?
    @State private var date: Date = Date()
    @State private var litersString: String = ""
    @State private var pricePerLiterString: String = ""
    @State private var totalCostString: String = ""
    @State private var odometerString: String = ""
    @State private var stationName: String = ""
    @State private var notes: String = ""
    @State private var chargingType: String? = nil
    
    private var isEV: Bool {
        selectedVehicle?.engineType == "EV" || selectedVehicle?.engineType == "PHEV"
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Voertuig & Datum") {
                    if vehicles.isEmpty {
                        Text("Geen voertuigen beschikbaar")
                            .foregroundStyle(.red)
                    } else {
                        Picker("Voertuig", selection: $selectedVehicle) {
                            ForEach(vehicles) { vehicle in
                                Text(vehicle.name).tag(vehicle as Vehicle?)
                            }
                        }
                    }
                    
                    DatePicker("Datum", selection: $date)
                }
                
                if isEV {
                    Section("Standaard Laadtarieven") {
                        HStack(spacing: 8) {
                            presetButton(title: "🏠 Thuis", rate: 0.28, type: "HOME")
                            presetButton(title: "🔌 Publiek", rate: 0.45, type: "PUBLIC")
                            presetButton(title: "⚡ Snel", rate: 0.69, type: "FAST")
                        }
                        .buttonStyle(.bordered)
                    }
                }
                
                Section(isEV ? "Laadsessie details" : "Tankbeurt details") {
                    TextField(isEV ? "Stroom (kWh)" : "Liters", text: $litersString)
                        .keyboardType(.decimalPad)
                        .onChange(of: litersString) { _, _ in recalculateTotal() }
                    
                    TextField(isEV ? "Prijs per kWh (€)" : "Prijs per liter (€)", text: $pricePerLiterString)
                        .keyboardType(.decimalPad)
                        .onChange(of: pricePerLiterString) { _, _ in recalculateTotal() }
                        
                    TextField("Totale kosten (€)", text: $totalCostString)
                        .keyboardType(.decimalPad)
                        
                    TextField("Kilometerstand", text: $odometerString)
                        .keyboardType(.numberPad)
                }
                
                Section("Optioneel") {
                    TextField(isEV ? "Laadpaal / Aanbieder" : "Tankstation", text: $stationName)
                    TextField("Notities", text: $notes)
                }
            }
            .navigationTitle(isEV ? "Laadsessie toevoegen" : "Tankbeurt toevoegen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleren") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan") {
                        saveFillUp()
                    }
                    .disabled(selectedVehicle == nil || litersString.isEmpty || totalCostString.isEmpty)
                }
            }
            .onAppear {
                if selectedVehicle == nil {
                    selectedVehicle = vehicles.first
                }
            }
        }
    }
    
    private func presetButton(title: String, rate: Double, type: String) -> some View {
        Button(action: {
            chargingType = type
            pricePerLiterString = String(format: "%.2f", rate)
            recalculateTotal()
        }) {
            VStack(spacing: 2) {
                Text(title)
                    .font(.caption.bold())
                Text("€\(String(format: "%.2f", rate))/kWh")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
        .tint(chargingType == type ? .accentColor : .secondary)
    }
    
    private func recalculateTotal() {
        let amount = Double(litersString.replacingOccurrences(of: ",", with: ".")) ?? 0
        let unitPrice = Double(pricePerLiterString.replacingOccurrences(of: ",", with: ".")) ?? 0
        if amount > 0 && unitPrice > 0 {
            totalCostString = String(format: "%.2f", amount * unitPrice)
        }
    }
    
    private func saveFillUp() {
        guard let vehicle = selectedVehicle else { return }
        
        let liters = Double(litersString.replacingOccurrences(of: ",", with: ".")) ?? 0
        let pricePerLiter = Double(pricePerLiterString.replacingOccurrences(of: ",", with: ".")) ?? 0
        let totalCost = Double(totalCostString.replacingOccurrences(of: ",", with: ".")) ?? 0
        let odometer = Int(odometerString) ?? 0
        
        let fillUp = FuelFillUp(
            vehicle: vehicle,
            date: date,
            liters: liters,
            pricePerLiter: pricePerLiter,
            totalCost: totalCost,
            odometer: odometer,
            stationName: stationName,
            notes: notes,
            chargingType: isEV ? chargingType : nil
        )
        
        modelContext.insert(fillUp)
        dismiss()
    }
}
