import SwiftUI
import SwiftData
import MapKit

struct AddressListView: View {
    @Query(sort: \SavedAddress.label) private var addresses: [SavedAddress]
    @Environment(\.modelContext) private var modelContext
    
    @State private var showingAddSheet = false
    @State private var addressToEdit: SavedAddress?
    
    var body: some View {
        List {
            ForEach(addresses) { address in
                VStack(alignment: .leading, spacing: 4) {
                    Text(address.label)
                        .font(.headline)
                    Text(address.address)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let lat = address.latitude, let lon = address.longitude {
                        Text(String(format: "%.4f, %.4f", lat, lon))
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    addressToEdit = address
                }
            }
            .onDelete(perform: deleteAddresses)
        }
        .navigationTitle("Opgeslagen Adressen")
        .toolbar {
            Button(action: { showingAddSheet = true }) {
                Label("Toevoegen", systemImage: "plus")
            }
        }
        .sheet(isPresented: $showingAddSheet) {
            NavigationStack {
                AddAddressView(addressToEdit: nil)
            }
        }
        .sheet(item: $addressToEdit) { address in
            NavigationStack {
                AddAddressView(addressToEdit: address)
            }
        }
    }
    
    private func deleteAddresses(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(addresses[index])
        }
        try? modelContext.save()
        TrackingManager.shared.updateGeofences()
    }
}

struct AddAddressView: View {
    private let geofenceRadiusMeters: CLLocationDistance = 200

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    var addressToEdit: SavedAddress?
    
    @State private var name: String
    @State private var addressString: String
    @State private var addressType: String
    @State private var projectCode: String
    
    @State private var searchVM = AddressSearchViewModel()
    @State private var selectedCoordinate: CLLocationCoordinate2D?
    @FocusState private var isAddressFocused: Bool
    @State private var mapCameraPosition: MapCameraPosition = .automatic
    
    init(addressToEdit: SavedAddress? = nil) {
        self.addressToEdit = addressToEdit
        _name = State(initialValue: addressToEdit?.label ?? "")
        _addressString = State(initialValue: addressToEdit?.address ?? "")
        _addressType = State(initialValue: addressToEdit?.addressType ?? "OTHER")
        _projectCode = State(initialValue: addressToEdit?.projectCode ?? "")
        
        if let lat = addressToEdit?.latitude, let lon = addressToEdit?.longitude {
            let coord = CLLocationCoordinate2D(latitude: lat, longitude: lon)
            _selectedCoordinate = State(initialValue: coord)
            _mapCameraPosition = State(initialValue: .region(MKCoordinateRegion(center: coord, latitudinalMeters: 500, longitudinalMeters: 500)))
        }
    }
    
    var body: some View {
        Form {
            Section("Adres Details") {
                TextField("Naam (bijv. Thuis of Klant X)", text: $name)
                
                TextField("Straat & Woonplaats", text: $searchVM.searchQuery)
                    .focused($isAddressFocused)
                    .onChange(of: searchVM.searchQuery) { _, newValue in
                        if addressString == newValue { return }
                        selectedCoordinate = nil
                        addressString = ""
                    }
                
                if isAddressFocused && !searchVM.suggestions.isEmpty {
                    ForEach(searchVM.suggestions, id: \.self) { suggestion in
                        Button(action: {
                            searchVM.geocode(completion: suggestion) { coord, formattedAddress in
                                if let coord = coord {
                                    self.selectedCoordinate = coord
                                    self.mapCameraPosition = .region(MKCoordinateRegion(center: coord, latitudinalMeters: 500, longitudinalMeters: 500))
                                }
                                if let formatted = formattedAddress {
                                    self.addressString = formatted
                                    self.searchVM.searchQuery = formatted
                                }
                                self.isAddressFocused = false
                            }
                        }) {
                            VStack(alignment: .leading) {
                                Text(suggestion.title).foregroundColor(.primary)
                                if !suggestion.subtitle.isEmpty {
                                    Text(suggestion.subtitle).font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
                
                Picker("Type", selection: $addressType) {
                    Text("Thuis").tag("THUIS")
                    Text("Werk").tag("WERK")
                    Text("Klant").tag("KLANT")
                    Text("Anders").tag("OTHER")
                }
                
                TextField("Projectcode (Optioneel)", text: $projectCode)
            }
            
            if let coord = selectedCoordinate {
                Section("Kaart & Geofence") {
                    AddressGeofenceMapView(
                        label: name.isEmpty ? addressString : name,
                        coordinate: coord,
                        radius: geofenceRadiusMeters
                    )
                }
            }
            
            if let edit = addressToEdit {
                Section {
                    Button(role: .destructive, action: { deleteAddress(edit) }) {
                        HStack {
                            Spacer()
                            Text("Adres Verwijderen")
                            Spacer()
                        }
                    }
                }
            }
        }
        .navigationTitle(addressToEdit == nil ? "Nieuw Adres" : "Adres Aanpassen")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Annuleer") {
                    dismiss()
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Opslaan", action: saveAddress)
                    .disabled(name.isEmpty || addressString.isEmpty || selectedCoordinate == nil)
            }
        }
        .onAppear {
            if let edit = addressToEdit {
                searchVM.searchQuery = edit.address
            }
        }
    }
    
    private func saveAddress() {
        if let edit = addressToEdit {
            edit.label = name
            edit.address = addressString
            edit.projectCode = projectCode.isEmpty ? nil : projectCode
            edit.addressType = addressType
            edit.latitude = selectedCoordinate?.latitude
            edit.longitude = selectedCoordinate?.longitude
        } else {
            let newAddress = SavedAddress(
                label: name,
                address: addressString,
                projectCode: projectCode.isEmpty ? nil : projectCode,
                addressType: addressType,
                latitude: selectedCoordinate?.latitude,
                longitude: selectedCoordinate?.longitude
            )
            modelContext.insert(newAddress)
        }
        try? modelContext.save()
        TrackingManager.shared.updateGeofences()
        dismiss()
    }
    
    private func deleteAddress(_ address: SavedAddress) {
        modelContext.delete(address)
        try? modelContext.save()
        TrackingManager.shared.updateGeofences()
        dismiss()
    }
}


private struct AddressGeofenceMapView: View {
    let label: String
    let coordinate: CLLocationCoordinate2D
    let radius: CLLocationDistance

    private var region: MKCoordinateRegion {
        MKCoordinateRegion(center: coordinate, latitudinalMeters: radius * 3, longitudinalMeters: radius * 3)
    }

    var body: some View {
        Map(initialPosition: .region(region), interactionModes: []) {
            Annotation(label, coordinate: coordinate) {
                VStack(spacing: 4) {
                    Image(systemName: "mappin.circle.fill")
                        .font(.title2)
                        .foregroundStyle(.red)
                        .background(Color.white.opacity(0.9), in: Circle())
                    Text(label)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(.thinMaterial, in: Capsule())
                }
            }

            MapCircle(center: coordinate, radius: radius)
                .foregroundStyle(.blue.opacity(0.18))
                .stroke(.blue.opacity(0.65), lineWidth: 2)
        }
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(alignment: .topTrailing) {
            Text("Geofence 200 m")
                .font(.caption2)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(.thinMaterial, in: Capsule())
                .padding(10)
        }
    }
}

struct AutoPreviewProviderAddressListView1: PreviewProvider {
    static var previews: some View {
            NavigationStack {
                AddressListView()
                    .modelContainer(PreviewContainer.shared.container)
            }
    }
}
