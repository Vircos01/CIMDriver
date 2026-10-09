import SwiftUI
import SwiftData

struct ClassificationRulesView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var rules: [ClassificationRule]

    private var orderedRules: [ClassificationRule] {
        rules.sorted { $0.orderIndex == $1.orderIndex ? $0.name < $1.name : $0.orderIndex < $1.orderIndex }
    }
    
    @State private var showingAddRule = false
    @State private var ruleToEdit: ClassificationRule?
    
    var body: some View {
        List {
            if orderedRules.isEmpty {
                Text("Geen aangepaste regels gevonden.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(orderedRules) { rule in
                    Button(action: {
                        ruleToEdit = rule
                    }) {
                        VStack(alignment: .leading) {
                            Text(rule.name).font(.headline)
                            
                            let startStr = rule.startAddressType ?? rule.startAddress ?? "*"
                            let endStr = rule.endAddressType ?? rule.endAddress ?? "*"
                            
                            Text("\(startStr) ⇄ \(endStr) | \(rule.tripType ?? "*") | \(rule.category)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack(spacing: 8) {
                                Text("Prioriteit \(rule.orderIndex + 1)")
                                if !rule.isEnabled { Text("Uitgeschakeld") }
                                if rule.autoApprove { Text("Automatisch akkoord") }
                            }
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete(perform: deleteRules)
            }
        }
        .navigationTitle("Aangepaste Regels")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: {
                    showingAddRule = true
                }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAddRule) {
            NavigationStack {
                EditClassificationRuleView()
            }
        }
        .sheet(item: $ruleToEdit) { rule in
            NavigationStack {
                EditClassificationRuleView(existingRule: rule)
            }
        }
    }
    
    private func deleteRules(offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(orderedRules[index])
        }
        try? modelContext.save()
    }
}
