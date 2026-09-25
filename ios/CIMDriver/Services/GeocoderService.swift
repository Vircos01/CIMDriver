import Foundation
import CoreLocation

class GeocoderService {
    private let geocoder = CLGeocoder()
    
    func reverseGeocode(location: CLLocation) async -> String? {
        do {
            let placemarks = try await geocoder.reverseGeocodeLocation(location)
            guard let placemark = placemarks.first else { return nil }
            
            var addressString = ""
            if let street = placemark.thoroughfare {
                addressString += street
                if let number = placemark.subThoroughfare {
                    addressString += " \(number)"
                }
            }
            if let city = placemark.locality {
                if !addressString.isEmpty {
                    addressString += ", "
                }
                addressString += city
            }
            
            return addressString.isEmpty ? nil : addressString
        } catch {
            print("Geocoding failed: \(error)")
            return nil
        }
    }
}
