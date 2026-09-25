import Foundation
import MapKit
import Combine

@Observable
class AddressSearchViewModel: NSObject {
    var searchQuery = "" {
        didSet {
            if searchQuery.isEmpty {
                suggestions = []
            } else {
                completer.queryFragment = searchQuery
            }
        }
    }
    
    var suggestions: [MKLocalSearchCompletion] = []
    var isSearching = false
    
    private var completer = MKLocalSearchCompleter()
    
    override init() {
        super.init()
        self.completer.resultTypes = .address
        self.completer.delegate = self
    }
    
    func geocode(completion: MKLocalSearchCompletion, result: @escaping (CLLocationCoordinate2D?, String?) -> Void) {
        let request = MKLocalSearch.Request(completion: completion)
        let search = MKLocalSearch(request: request)
        
        isSearching = true
        search.start { response, error in
            self.isSearching = false
            guard let response = response, let mapItem = response.mapItems.first else {
                result(nil, nil)
                return
            }
            
            var addressString = ""
            let placemark = mapItem.placemark
            if let thoroughfare = placemark.thoroughfare {
                addressString += thoroughfare
                if let subThoroughfare = placemark.subThoroughfare {
                    addressString += " " + subThoroughfare
                }
            }
            if let locality = placemark.locality {
                if !addressString.isEmpty { addressString += ", " }
                addressString += locality
            }
            if addressString.isEmpty {
                addressString = completion.title
            }
            
            result(mapItem.placemark.coordinate, addressString)
        }
    }
}

extension AddressSearchViewModel: MKLocalSearchCompleterDelegate {
    func completerDidUpdateResults(_ completer: MKLocalSearchCompleter) {
        self.suggestions = completer.results
    }
    
    func completer(_ completer: MKLocalSearchCompleter, didFailWithError error: Error) {
        print("Address search error: \(error.localizedDescription)")
    }
}
