import SwiftUI
import SwiftData

struct ClassificationRulesView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var rules: [ClassificationRule]
    
    @State private var showingAddRule = false
    @State private var ruleToEdit: ClassificationRule?
    
    var body: some View {
        List {
            if rules.isEmpty {
                Text("Geen aangepaste regels gevonden.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(rules) { rule in
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
            modelContext.delete(rules[index])
        }
        try? modelContext.save()
    }
}
