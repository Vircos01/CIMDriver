import Foundation
import SwiftData

@Model
final class FavoriteRoute {
    var name: String
    var startAddress: String
    var endAddress: String
    var tripType: String
    var projectCode: String?
    
    init(name: String, startAddress: String, endAddress: String, tripType: String, projectCode: String? = nil) {
        self.name = name
        self.startAddress = startAddress
        self.endAddress = endAddress
        self.tripType = tripType
        self.projectCode = projectCode
    }
}
