import SwiftUI
import SwiftData

struct FuelView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FuelFillUp.date, order: .reverse) private var fillUps: [FuelFillUp]
    @Query private var vehicles: [Vehicle]
    
    @State private var showingAddFuel = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if !fillUps.isEmpty {
                    FuelStatsView(fillUps: fillUps)
                }
                
                List {
                    if fillUps.isEmpty {
                    ContentUnavailableView(
                        "Geen tankbeurten",
                        systemImage: "fuelpump",
                        description: Text("Voeg je eerste tankbeurt toe om je brandstofkosten bij te houden.")
                    )
                } else {
                    ForEach(fillUps) { fillUp in
                        FuelRowView(fillUp: fillUp, vehicles: vehicles)
                        }
                        .onDelete(perform: deleteFillUps)
                    }
                }
            }
            .navigationTitle("Tankbeurten")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { showingAddFuel = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingAddFuel) {
                AddFuelView()
            }
        }
    }
    
    private func deleteFillUps(offsets: IndexSet) {
        withAnimation {
            for index in offsets {
                modelContext.delete(fillUps[index])
            }
        }
    }
}

struct FuelStatsView: View {
    let fillUps: [FuelFillUp]
    
    var totalCost: Double {
        fillUps.reduce(0) { $0 + $1.totalCost }
    }
    
    var averageConsumption: Double {
        let sorted = fillUps.sorted { $0.date < $1.date }
        if sorted.count < 2 { return 0.0 }
        
        let firstOdo = sorted.first!.odometer
        let lastOdo = sorted.last!.odometer
        let distance = lastOdo - firstOdo
        
        let litersUsed = sorted.dropFirst().reduce(0) { $0 + $1.liters }
        
        if distance > 0 {
            return (litersUsed / Double(distance)) * 100
        }
        return 0.0
    }
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Totale kosten")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("€\(totalCost, specifier: "%.2f")")
                    .font(.title3)
                    .fontWeight(.bold)
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Gemiddeld verbruik")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(averageConsumption, specifier: "%.1f") L/100km")
                    .font(.title3)
                    .fontWeight(.bold)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemGroupedBackground))
    }
}

struct FuelRowView: View {
    let fillUp: FuelFillUp
    let vehicles: [Vehicle]
    
    var vehicleName: String {
        vehicles.first(where: { $0.id == fillUp.vehicle?.id })?.name ?? "Onbekend voertuig"
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(fillUp.date, format: .dateTime.day().month().year().hour().minute())
                    .font(.headline)
                Spacer()
                Text(vehicleName)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            
            HStack {
                VStack(alignment: .leading) {
                    Text("Liters: \(fillUp.liters, specifier: "%.2f") L")
                    Text("Totaal: €\(fillUp.totalCost, specifier: "%.2f")")
                }
                .font(.subheadline)
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text("Prijs/L: €\(fillUp.pricePerLiter, specifier: "%.2f")")
                    Text("Km-stand: \(fillUp.odometer) km")
                }
                .font(.subheadline)
                .foregroundStyle(.secondary)
            }
            
            if let station = fillUp.stationName, !station.isEmpty {
                Text(station)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
