import SwiftUI
import ActivityKit
import WidgetKit

struct TrackingAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic state (bijv. snelheid of de huidige tijd)
        var isGracePeriod: Bool = false
    }
    
    // Fixed properties
    var vehicleName: String
    var startTime: Date
}

#if WIDGET_EXTENSION
struct TrackingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TrackingAttributes.self) { context in
            // Lock screen / banner UI
            HStack {
                VStack(alignment: .leading) {
                    Text(context.state.isGracePeriod ? "Rit stopt bijna..." : "Rit bezig")
                        .font(.headline)
                        .foregroundStyle(context.state.isGracePeriod ? .orange : .cimGreen)
                    Text(context.attributes.vehicleName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(timerInterval: context.attributes.startTime...Date.distantFuture)
                    .monospacedDigit()
                    .font(.title2.bold())
            }
            .padding()
            .activityBackgroundTint(Color(UIColor.systemBackground))
            .activitySystemActionForegroundColor(.cimGreen)
            
        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI
                DynamicIslandExpandedRegion(.leading) {
                    Label("CIMDriver", systemImage: "car.fill")
                        .foregroundStyle(context.state.isGracePeriod ? .orange : .cimGreen)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(timerInterval: context.attributes.startTime...Date.distantFuture)
                        .monospacedDigit()
                        .foregroundStyle(.cimGreen)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.isGracePeriod ? "Bluetooth verbroken" : "Automatische registratie actief")
                        .font(.caption)
                }
            } compactLeading: {
                Image(systemName: "car.fill")
                    .foregroundStyle(context.state.isGracePeriod ? .orange : .cimGreen)
            } compactTrailing: {
                Text(timerInterval: context.attributes.startTime...Date.distantFuture, countsDown: false)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 40)
                    .font(.caption2)
            } minimal: {
                Image(systemName: "car.fill")
                    .foregroundStyle(.cimGreen)
            }
        }
    }
}
#endif
