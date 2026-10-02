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
                
                Section("Tankbeurt details") {
                    TextField("Liters", text: $litersString)
                        .keyboardType(.decimalPad)
                    
                    TextField("Prijs per liter (€)", text: $pricePerLiterString)
                        .keyboardType(.decimalPad)
                        
                    TextField("Totale kosten (€)", text: $totalCostString)
                        .keyboardType(.decimalPad)
                        
                    TextField("Kilometerstand", text: $odometerString)
                        .keyboardType(.numberPad)
                }
                
                Section("Optioneel") {
                    TextField("Tankstation", text: $stationName)
                    TextField("Notities", text: $notes)
                }
            }
            .navigationTitle("Tankbeurt toevoegen")
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
            notes: notes
        )
        
        modelContext.insert(fillUp)
        dismiss()
    }
}
