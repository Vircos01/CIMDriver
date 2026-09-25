import SwiftUI
import Charts

struct FlatChartItem: Identifiable {
    let id = UUID()
    let label: String
    let category: String
    let km: Double
}

struct DistanceStackedChart: View {
    let data: [DistanceChartItem]
    
    // Convert to flat array for SwiftUI Charts
    private var chartData: [FlatChartItem] {
        var flatData: [FlatChartItem] = []
        for item in data {
            if item.zakelijkKm > 0 { flatData.append(FlatChartItem(label: item.label, category: "Zakelijk", km: item.zakelijkKm)) }
            if item.woonWerkKm > 0 { flatData.append(FlatChartItem(label: item.label, category: "Woon-Werk", km: item.woonWerkKm)) }
            if item.priveKm > 0 { flatData.append(FlatChartItem(label: item.label, category: "Privé", km: item.priveKm)) }
        }
        return flatData
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Afstand")
                .font(.headline)
            
            if chartData.isEmpty {
                Text("Geen data voor deze periode")
                    .foregroundColor(.secondary)
                    .frame(height: 200)
                    .frame(maxWidth: .infinity)
            } else {
                Chart(chartData) { item in
                    BarMark(
                        x: .value("Dag", item.label),
                        y: .value("Kilometer", item.km)
                    )
                    .foregroundStyle(by: .value("Categorie", item.category))
                }
                .chartForegroundStyleScale([
                    "Zakelijk": Color.cimNavy,
                    "Woon-Werk": Color.blue,
                    "Privé": Color.orange
                ])
                .frame(height: 200)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

struct CategoryPieChart: View {
    let data: [CategoryBreakdownItem]
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Rittypes")
                .font(.headline)
            
            if data.isEmpty {
                Text("Geen data")
                    .foregroundColor(.secondary)
                    .frame(height: 200)
                    .frame(maxWidth: .infinity)
            } else {
                if #available(iOS 17.0, *) {
                    Chart(data, id: \.id) { item in
                        SectorMark(
                            angle: .value("Afstand", item.distanceKm),
                            innerRadius: .ratio(0.5),
                            angularInset: 1.5
                        )
                        .foregroundStyle(by: .value("Categorie", item.categoryName))
                        .cornerRadius(5)
                    }
                    .chartForegroundStyleScale([
                        "Zakelijk": Color.cimNavy,
                        "Woon-Werk": Color.blue,
                        "Privé": Color.orange
                    ])
                    .frame(height: 200)
                } else {
                    Text("Pie charts vereisen iOS 17+")
                        .foregroundColor(.secondary)
                        .frame(height: 200)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

struct WorkHoursChartItem: Identifiable {
    let id = UUID()
    let label: String
    let hours: Double
}

struct WorkHoursBarChart: View {
    let data: [WorkHoursChartItem]
    
    var body: some View {
        VStack(alignment: .leading) {
            Text("Werkuren")
                .font(.headline)
            
            if data.isEmpty || data.allSatisfy({ $0.hours == 0 }) {
                Text("Geen werkuren geregistreerd")
                    .foregroundColor(.secondary)
                    .frame(height: 200)
                    .frame(maxWidth: .infinity)
            } else {
                Chart(data) { item in
                    BarMark(
                        x: .value("Dag", item.label),
                        y: .value("Uren", item.hours)
                    )
                    .foregroundStyle(Color.cimGreen)
                }
                .frame(height: 200)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}
