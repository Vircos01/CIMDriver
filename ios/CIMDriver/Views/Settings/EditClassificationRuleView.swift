import SwiftUI
import SwiftData

struct EditClassificationRuleView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query private var rules: [ClassificationRule]
    
    var existingRule: ClassificationRule?
    
    @State private var name: String = ""
    @State private var category: String = TripCategory.business.rawValue
    
    // We will simplify this form for now by just allowing text fields for matching addresses
    // In a full implementation, you might want pickers for "THUIS" / "WERK" etc.
    @State private var startAddress: String = ""
    @State private var endAddress: String = ""
    @State private var orderIndex = 0
    @State private var isEnabled = true
    @State private var autoApprove = false
    
    var body: some View {
        Form {
            Section("Algemeen") {
                TextField("Regel Naam (bijv. Klant X)", text: $name)
                
                Picker("Classificatie Categorie", selection: $category) {
                    Text("Zakelijk").tag(TripCategory.business.rawValue)
                    Text("Privé").tag(TripCategory.privateTrip.rawValue)
                    Text("Woon-Werk").tag(TripCategory.commute.rawValue)
                }
            }
            
            Section(header: Text("Match Voorwaarden"), footer: Text("Vul het exacte start- en eindadres in (of een deel daarvan) om ritten automatisch te classificeren. Laat leeg om elk adres te matchen.")) {
                TextField("Start adres (optioneel)", text: $startAddress)
                TextField("Eind adres (optioneel)", text: $endAddress)
            }

            Section("Automatisering") {
                Toggle("Regel ingeschakeld", isOn: $isEnabled)
                Toggle("Rit automatisch goedkeuren", isOn: $autoApprove)
                Stepper("Prioriteit: \(orderIndex + 1)", value: $orderIndex, in: 0...999)
            }
        }
        .navigationTitle(existingRule != nil ? "Bewerk Regel" : "Nieuwe Regel")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Annuleer") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Opslaan", action: saveRule)
                    .disabled(name.isEmpty)
            }
        }
        .onAppear {
            if let rule = existingRule {
                name = rule.name
                category = rule.category
                startAddress = rule.startAddress ?? ""
                endAddress = rule.endAddress ?? ""
                orderIndex = rule.orderIndex
                isEnabled = rule.isEnabled
                autoApprove = rule.autoApprove
            }
        }
    }
    
    private func saveRule() {
        if let rule = existingRule {
            rule.name = name
            rule.category = category
            rule.startAddress = startAddress.isEmpty ? nil : startAddress
            rule.endAddress = endAddress.isEmpty ? nil : endAddress
            rule.orderIndex = orderIndex
            rule.isEnabled = isEnabled
            rule.autoApprove = autoApprove
        } else {
            let newRule = ClassificationRule(
                name: name,
                startAddress: startAddress.isEmpty ? nil : startAddress,
                endAddress: endAddress.isEmpty ? nil : endAddress,
                category: category,
                orderIndex: (rules.map(\.orderIndex).max() ?? -1) + 1,
                isEnabled: isEnabled,
                autoApprove: autoApprove
            )
            modelContext.insert(newRule)
        }
        try? modelContext.save()
        dismiss()
    }
}
