import WidgetKit
import SwiftUI

struct WidgetSnapshotEntry: TimelineEntry {
    let date: Date
    let businessKm: Double
    let privateKm: Double
    let commuteKm: Double
    let isActiveTrip: Bool
    let activeTripStartTime: Date?
    let activeTripDistanceMeters: Int
    let vehicleName: String?
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> WidgetSnapshotEntry {
        WidgetSnapshotEntry(
            date: Date(),
            businessKm: 248,
            privateKm: 61,
            commuteKm: 132,
            isActiveTrip: true,
            activeTripStartTime: Date().addingTimeInterval(-42 * 60),
            activeTripDistanceMeters: 18400,
            vehicleName: "Volvo XC40"
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (WidgetSnapshotEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WidgetSnapshotEntry>) -> Void) {
        let entry = loadEntry()
        let nextUpdateDate = Calendar.current.date(byAdding: .minute, value: 15, to: Date()) ?? Date().addingTimeInterval(900)
        completion(Timeline(entries: [entry], policy: .after(nextUpdateDate)))
    }
    
    private func loadEntry() -> WidgetSnapshotEntry {
        let defaults = UserDefaults(suiteName: "group.com.cimdriver.app")
        return WidgetSnapshotEntry(
            date: Date(),
            businessKm: defaults?.double(forKey: "widget_month_business_km") ?? 0,
            privateKm: defaults?.double(forKey: "widget_month_private_km") ?? 0,
            commuteKm: defaults?.double(forKey: "widget_month_commute_km") ?? 0,
            isActiveTrip: defaults?.bool(forKey: "widget_active_trip_is_active") ?? false,
            activeTripStartTime: defaults?.object(forKey: "widget_active_trip_start_time") as? Date,
            activeTripDistanceMeters: defaults?.integer(forKey: "widget_active_trip_distance_meters") ?? 0,
            vehicleName: defaults?.string(forKey: "widget_active_trip_vehicle_name")
        )
    }
}

struct DashboardWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    var entry: Provider.Entry

    var body: some View {
        switch family {
        case .systemMedium:
            mediumWidget
        default:
            smallWidget
        }
    }
    
    private var smallWidget: some View {
        ZStack(alignment: .topLeading) {
            Image("WidgetLogo")
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                .scaleEffect(1.15)
                .opacity(0.28)
            
            VStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 12) {
                    statusPill(
                        systemName: entry.isActiveTrip ? "record.circle.fill" : "pause.circle.fill",
                        text: entry.isActiveTrip ? "Actief" : "Pauze",
                        tint: entry.isActiveTrip ? .green : .orange
                    )
                    
                    statusPill(
                        systemName: "clock.fill",
                        text: entry.isActiveTrip ? activeMinutesText : "Stand-by",
                        tint: .secondary
                    )
                    
                    statusPill(
                        systemName: entry.isActiveTrip ? "arrow.trianglehead.branch" : "car.fill",
                        text: entry.isActiveTrip ? distanceText(kilometersFromMeters(entry.activeTripDistanceMeters)) : "Geen rit",
                        tint: entry.isActiveTrip ? .green : .secondary
                    )
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                
                Spacer(minLength: 0)
            }
            .padding(16)
        }
        .containerBackground(for: .widget) {
            Color(UIColor.systemBackground)
        }
    }
    
    private var mediumWidget: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Deze Maand")
                        .font(.headline)
                    Text(entry.isActiveTrip ? activeTripSubtitle : "Geen actieve rit")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer()
                Image("WidgetLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 42, height: 42)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            
            HStack(spacing: 12) {
                metricView(title: "Zakelijk", value: entry.businessKm, color: .green)
                metricView(title: "Privé", value: entry.privateKm, color: .red)
                metricView(title: "Woon-Werk", value: entry.commuteKm, color: .orange)
            }
        }
        .containerBackground(for: .widget) {
            Color(UIColor.systemBackground)
        }
    }
    
    private func smallStatusRow(systemName: String, text: String, tint: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemName)
                .font(.system(size: 12, weight: .semibold))
                .frame(width: 14)
                .foregroundStyle(tint)
            Text(text)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(tint)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
    }
    
    private func statusPill(systemName: String, text: String, tint: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemName)
                .font(.system(size: 13, weight: .bold))
            Text(text)
                .font(.system(size: 14, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.88)
            Spacer(minLength: 0)
        }
        .foregroundStyle(tint)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial.opacity(0.72), in: Capsule())
    }
    
    private func metricView(title: String, value: Double, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(distanceText(value))
                .font(.headline)
                .foregroundStyle(color)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    
    private var activeMinutesText: String {
        guard let startTime = entry.activeTripStartTime else { return "0 min" }
        let minutes = max(0, Int(Date().timeIntervalSince(startTime) / 60))
        return "\(minutes) min"
    }
    
    private var activeTripSubtitle: String {
        let base = entry.vehicleName?.isEmpty == false ? entry.vehicleName! : "Actieve rit"
        guard let startTime = entry.activeTripStartTime else { return base }
        let minutes = max(0, Int(Date().timeIntervalSince(startTime) / 60))
        return "\(base) · \(minutes) min"
    }
    
    private func kilometersFromMeters(_ meters: Int) -> Double {
        Double(meters) / 1000.0
    }
    
    private func distanceText(_ value: Double) -> String {
        if value >= 10 {
            return String(format: "%.0f km", value)
        }
        return String(format: "%.1f km", value)
    }
}

struct DashboardWidget: Widget {
    let kind: String = "DashboardWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            DashboardWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("CIMDriver")
        .description("Toont actieve ritstatus en je maandtotalen voor Zakelijk, Privé en Woon-Werk.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
