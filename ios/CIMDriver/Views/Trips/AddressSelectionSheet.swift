import SwiftUI
import SwiftData

struct AddressSelectionSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \SavedAddress.label) private var savedAddresses: [SavedAddress]
    
    @Binding var selectedAddressText: String
    
    var body: some View {
        NavigationStack {
            List {
                if savedAddresses.isEmpty {
                    Text("Geen adressen opgeslagen in adresboek.")
                        .foregroundColor(.secondary)
                }
                ForEach(savedAddresses) { addr in
                    Button(action: {
                        selectedAddressText = addr.address
                        dismiss()
                    }) {
                        VStack(alignment: .leading) {
                            Text(addr.label)
                                .font(.headline)
                            Text(addr.address)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .navigationTitle("Kies Adres")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleer") {
                        dismiss()
                    }
                }
            }
        }
    }
}
