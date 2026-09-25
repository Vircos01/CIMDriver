import Foundation
import CoreLocation

protocol LocationServiceDelegate: AnyObject {
    func didUpdateLocation(_ location: CLLocation)
    func didEnterGeofence(identifier: String)
    func didExitGeofence(identifier: String)
    func didReceiveSignificantLocationUpdate(_ location: CLLocation)
}

class LocationService: NSObject, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()
    private var geofenceBoostResetTask: DispatchWorkItem?
    weak var delegate: LocationServiceDelegate?
    
    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.showsBackgroundLocationIndicator = true
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10 // meters
        locationManager.pausesLocationUpdatesAutomatically = false
    }
    
    func requestPermissions() {
        if locationManager.authorizationStatus == .notDetermined {
            locationManager.requestWhenInUseAuthorization()
        } else if locationManager.authorizationStatus == .authorizedWhenInUse {
            locationManager.requestAlwaysAuthorization()
        }
    }
    
    func startTracking() {
        locationManager.startUpdatingLocation()
    }

    func startMonitoringSignificantLocationChanges() {
        locationManager.startMonitoringSignificantLocationChanges()
    }

    func stopMonitoringSignificantLocationChanges() {
        locationManager.stopMonitoringSignificantLocationChanges()
    }
    
    func stopTracking() {
        locationManager.stopUpdatingLocation()
        resetTrackingConfiguration()
    }

    func boostAccuracyTemporarily(duration: TimeInterval = 180) {
        geofenceBoostResetTask?.cancel()
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10
        locationManager.startUpdatingLocation()

        let task = DispatchWorkItem { [weak self] in
            self?.resetTrackingConfiguration()
        }
        geofenceBoostResetTask = task
        DispatchQueue.main.asyncAfter(deadline: .now() + duration, execute: task)
    }

    private func resetTrackingConfiguration() {
        geofenceBoostResetTask?.cancel()
        geofenceBoostResetTask = nil
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.distanceFilter = 50
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        // Filter out inaccurate locations
        guard location.horizontalAccuracy > 0 && location.horizontalAccuracy < 100 else { return }
        
        if manager.monitoredRegions.isEmpty {
            delegate?.didReceiveSignificantLocationUpdate(location)
        }

        delegate?.didUpdateLocation(location)
    }
    
    // MARK: - Geofencing
    
    func startMonitoringGeofence(id: String, coordinate: CLLocationCoordinate2D, radius: CLLocationDistance) {
        let region = CLCircularRegion(center: coordinate, radius: radius, identifier: id)
        region.notifyOnEntry = true
        region.notifyOnExit = true
        locationManager.startMonitoring(for: region)
    }
    
    func stopMonitoringGeofence(id: String) {
        for region in locationManager.monitoredRegions {
            if let circularRegion = region as? CLCircularRegion, circularRegion.identifier == id {
                locationManager.stopMonitoring(for: circularRegion)
            }
        }
    }
    
    func stopAllGeofences() {
        for region in locationManager.monitoredRegions {
            locationManager.stopMonitoring(for: region)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        if let circularRegion = region as? CLCircularRegion {
            delegate?.didEnterGeofence(identifier: circularRegion.identifier)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        if let circularRegion = region as? CLCircularRegion {
            delegate?.didExitGeofence(identifier: circularRegion.identifier)
        }
    }
}
