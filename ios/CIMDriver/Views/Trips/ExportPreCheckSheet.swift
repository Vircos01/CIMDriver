import SwiftUI
import SwiftData

struct ExportPreCheckSheet: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    let trips: [Trip]
    let periodTitle: String
    let isPDF: Bool
    let onConfirm: () -> Void
    
    @State private var localTrips: [Trip]
    
    init(trips: [Trip], periodTitle: String, isPDF: Bool, onConfirm: @escaping () -> Void) {
        self.trips = trips
        self.periodTitle = periodTitle
        self.isPDF = isPDF
        self.onConfirm = onConfirm
        _localTrips = State(initialValue: trips)
    }
    
    private var unreviewedTrips: [Trip] {
        localTrips.filter { $0.status == TripStatus.toReview.rawValue }
    }
    
    private var detourWithoutNotes: [Trip] {
        localTrips.filter { $0.privateDetourMeters > 0 && ($0.note == nil || $0.note?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == true) }
    }
    
    private var odometerGapsCount: Int {
        var count = 0
        let groupedByVehicle = Dictionary(grouping: localTrips.sorted(by: { $0.startTime < $1.startTime })) { $0.vehicle?.id ?? UUID() }
        for (_, vTrips) in groupedByVehicle {
            for i in 0..<(vTrips.count - 1) {
                let current = vTrips[i]
                let next = vTrips[i + 1]
                if let endOdo = current.odometerEnd, next.odometerStart > endOdo {
                    count += 1
                }
            }
        }
        return count
    }
    
    private var isFiscallyCompliant: Bool {
        unreviewedTrips.isEmpty && detourWithoutNotes.isEmpty && odometerGapsCount == 0
    }
    
    private var totalKm: Double {
        Double(localTrips.reduce(0) { $0 + $1.distanceMeters }) / 1000.0
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header Status Card
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 12) {
                            Image(systemName: isFiscallyCompliant ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                                .font(.title)
                                .foregroundColor(isFiscallyCompliant ? Color.cimGreen : .orange)
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text(isFiscallyCompliant ? "Fiscaal Sluitend voor Belastingdienst" : "Aandachtspunten vóór Export")
                                    .font(.headline)
                                    .foregroundColor(isFiscallyCompliant ? Color.cimGreen : .primary)
                                Text(isFiscallyCompliant 
                                     ? "Alle ritten zijn geclassificeerd en sluiten aan."
                                     : "Er zijn openstaande punten die toelichting vereisen.")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(isFiscallyCompliant ? Color.cimGreen.opacity(0.12) : Color.orange.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    
                    // Checklist Items
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Volledigheidscontrole (\(periodTitle))")
                            .font(.subheadline.bold())
                            .foregroundColor(.secondary)
                        
                        // 1. Unreviewed Trips
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: unreviewedTrips.isEmpty ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                                .foregroundColor(unreviewedTrips.isEmpty ? Color.cimGreen : .orange)
                                .font(.headline)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Ritclassificatie")
                                    .font(.body.bold())
                                if unreviewedTrips.isEmpty {
                                    Text("Alle \(localTrips.count) ritten zijn definitief geclassificeerd.")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Text("\(unreviewedTrips.count) rit(ten) staan nog op 'Nog beoordelen'.")
                                        .font(.caption)
                                        .foregroundColor(.orange)
                                    
                                    Button(action: approveAllUnreviewed) {
                                        Text("✓ Direct goedkeuren (\(unreviewedTrips.count))")
                                            .font(.caption.bold())
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 6)
                                            .background(Color.cimNavy)
                                            .foregroundColor(.white)
                                            .clipShape(RoundedRectangle(cornerRadius: 8))
                                    }
                                    .padding(.top, 4)
                                }
                            }
                            Spacer()
                        }
                        
                        Divider()
                        
                        // 2. Odometer Gaps
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: odometerGapsCount == 0 ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                                .foregroundColor(odometerGapsCount == 0 ? Color.cimGreen : .orange)
                                .font(.headline)
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Aansluiting kilometerstanden")
                                    .font(.body.bold())
                                if odometerGapsCount == 0 {
                                    Text("Geen onverklaarde kilometerstand-onderbrekingen gevonden.")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Text("\(odometerGapsCount) kilometerstand-gat(en) gedetecteerd in deze periode.")
                                        .font(.caption)
                                        .foregroundColor(.orange)
                                }
                            }
                            Spacer()
                        }
                        
                        Divider()
                        
                        // 3. Detour Notes
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: detourWithoutNotes.isEmpty ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                                .foregroundColor(detourWithoutNotes.isEmpty ? Color.cimGreen : .orange)
                                .font(.headline)
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Toelichting privé-omrijden")
                                    .font(.body.bold())
                                if detourWithoutNotes.isEmpty {
                                    Text("Alle afwijkingen en omrijkilometers hebben een notitie.")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Text("\(detourWithoutNotes.count) rit(ten) met privé-omrijden missen een toelichting.")
                                        .font(.caption)
                                        .foregroundColor(.orange)
                                }
                            }
                            Spacer()
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    
                    // Export Details Summary
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Export Specificaties")
                            .font(.subheadline.bold())
                            .foregroundColor(.secondary)
                        
                        HStack {
                            Text("Bestandsformaat")
                            Spacer()
                            Text(isPDF ? "PDF Rapport" : "CSV Tabel")
                                .bold()
                        }
                        .font(.subheadline)
                        
                        HStack {
                            Text("Periode")
                            Spacer()
                            Text(periodTitle)
                                .foregroundColor(.secondary)
                        }
                        .font(.subheadline)
                        
                        HStack {
                            Text("Aantal ritten")
                            Spacer()
                            Text("\(localTrips.count)")
                                .foregroundColor(.secondary)
                        }
                        .font(.subheadline)
                        
                        HStack {
                            Text("Totale afstand")
                            Spacer()
                            Text(String(format: "%.1f km", totalKm))
                                .bold()
                        }
                        .font(.subheadline)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    
                    if !isFiscallyCompliant {
                        Text("⚠️ Let op: De Belastingdienst kan toelichting vragen bij ongeclassificeerde ritten of gaten in de kilometerteller.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    
                    // Buttons
                    VStack(spacing: 12) {
                        Button(action: {
                            dismiss()
                            onConfirm()
                        }) {
                            Text(isFiscallyCompliant ? "Exporteer (Fiscaal Sluitend)" : "Toch Exporteren")
                                .bold()
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(isFiscallyCompliant ? Color.cimGreen : Color.cimNavy)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        Button("Annuleren", role: .cancel) {
                            dismiss()
                        }
                        .foregroundColor(.secondary)
                    }
                    .padding(.top, 8)
                }
                .padding()
            }
            .navigationTitle("Exportcontrole")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Sluiten") { dismiss() }
                }
            }
        }
    }
    
    private func approveAllUnreviewed() {
        for trip in unreviewedTrips {
            trip.status = TripStatus.done.rawValue
        }
        try? modelContext.save()
        // Refresh local trips
        localTrips = trips
    }
}

