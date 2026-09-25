import Foundation
import SwiftData

struct BackupData: Codable {
    var trips: [TripDTO]
    var vehicles: [VehicleDTO]
    var addresses: [AddressDTO]
    var rules: [RuleDTO]
    var workDays: [WorkDayDTO]
    var settings: AppSettingsDTO?
}

// Basic DTOs
struct TripDTO: Codable {
    var id: UUID
    var vehicleId: UUID?
    var startTime: Date
    var endTime: Date?
    var startAddress: String?
    var endAddress: String?
    var distanceMeters: Int
    var tripType: String
    var note: String?
    var status: String
    var isManual: Bool
    var odometerStart: Int
    var odometerEnd: Int?
    var projectCode: String?
}

struct VehicleDTO: Codable {
    var id: UUID
    var name: String
    var licensePlate: String
    var make: String
    var model: String
    var odometerStart: Int
    var odometerCurrent: Int
    var privateKmYearlyLimit: Int
    var isDefault: Bool
    var usageType: String
    var odometerCorrectionStrategy: String
}

struct AddressDTO: Codable {
    var id: UUID
    var label: String
    var address: String
    var latitude: Double?
    var longitude: Double?
}

struct RuleDTO: Codable {
    var id: UUID
    var name: String
    var startAddressType: String?
    var endAddressType: String?
    var startAddress: String?
    var endAddress: String?
    var tripType: String?
    var category: String
}

struct WorkDayDTO: Codable {
    var id: UUID
    var date: Date
    var firstDepartureTime: Date
    var arrivalTime: Date
    var departureTime: Date?
    var lastArrivalTime: Date?
    var roundedArrivalTime: Date?
    var roundedDepartureTime: Date?
    var breakMinutes: Int
    var workLocationLabel: String?
    var projectCode: String?
    var status: String
    var note: String?
}

struct AppSettingsDTO: Codable {
    var workStartTime: Date
    var workEndTime: Date
    var workDays: String
    var breakMinutes: Int
    var toleranceMinutes: Int
    var gracePeriodSec: Int
    var trackingIntervalSec: Int
    var classificationDefault: String
    var classifyHomeWorkAsCommute: Bool
    var classifyCustomerAsBusiness: Bool
    var autoMergeEnabled: Bool
    var mergeTimeLimitMin: Int
    var mergeDistanceM: Int
    var businessCompensation: Double
    var themeMode: String
    var odometerReminder: Bool
    var odometerReminderIntervalDays: Int
    var locationRetentionDays: Int
}

@MainActor
class BackupManager {
    static let shared = BackupManager()
    
    func createBackup(context: ModelContext) -> URL? {
        let trips = (try? context.fetch(FetchDescriptor<Trip>())) ?? []
        let vehicles = (try? context.fetch(FetchDescriptor<Vehicle>())) ?? []
        let addresses = (try? context.fetch(FetchDescriptor<SavedAddress>())) ?? []
        let rules = (try? context.fetch(FetchDescriptor<ClassificationRule>())) ?? []
        let workDays = (try? context.fetch(FetchDescriptor<WorkDay>())) ?? []
        let settings = (try? context.fetch(FetchDescriptor<AppSettings>())) ?? []
        
        let backup = BackupData(
            trips: trips.map { t in TripDTO(id: t.id, vehicleId: t.vehicle?.id, startTime: t.startTime, endTime: t.endTime, startAddress: t.startAddress, endAddress: t.endAddress, distanceMeters: t.distanceMeters, tripType: t.tripType, note: t.note, status: t.status, isManual: t.isManual, odometerStart: t.odometerStart, odometerEnd: t.odometerEnd, projectCode: t.projectCode) },
            vehicles: vehicles.map { v in VehicleDTO(id: v.id, name: v.name, licensePlate: v.licensePlate, make: v.make, model: v.model, odometerStart: v.odometerStart, odometerCurrent: v.odometerCurrent, privateKmYearlyLimit: v.privateKmYearlyLimit, isDefault: v.isDefault, usageType: v.usageType, odometerCorrectionStrategy: v.odometerCorrectionStrategy) },
            addresses: addresses.map { a in AddressDTO(id: a.id, label: a.label, address: a.address, latitude: a.latitude, longitude: a.longitude) },
            rules: rules.map { r in RuleDTO(id: r.id, name: r.name, startAddressType: r.startAddressType, endAddressType: r.endAddressType, startAddress: r.startAddress, endAddress: r.endAddress, tripType: r.tripType, category: r.category) },
            workDays: workDays.map { w in WorkDayDTO(id: w.id, date: w.date, firstDepartureTime: w.firstDepartureTime, arrivalTime: w.arrivalTime, departureTime: w.departureTime, lastArrivalTime: w.lastArrivalTime, roundedArrivalTime: w.roundedArrivalTime, roundedDepartureTime: w.roundedDepartureTime, breakMinutes: w.breakMinutes, workLocationLabel: w.workLocationLabel, projectCode: w.projectCode, status: w.status, note: w.note) },
            settings: settings.first.map { s in AppSettingsDTO(workStartTime: s.workStartTime, workEndTime: s.workEndTime, workDays: s.workDays, breakMinutes: s.breakMinutes, toleranceMinutes: s.toleranceMinutes, gracePeriodSec: s.gracePeriodSec, trackingIntervalSec: s.trackingIntervalSec, classificationDefault: s.classificationDefault, classifyHomeWorkAsCommute: s.classifyHomeWorkAsCommute, classifyCustomerAsBusiness: s.classifyCustomerAsBusiness, autoMergeEnabled: s.autoMergeEnabled, mergeTimeLimitMin: s.mergeTimeLimitMin, mergeDistanceM: s.mergeDistanceM, businessCompensation: s.businessCompensation, themeMode: s.themeMode, odometerReminder: s.odometerReminder, odometerReminderIntervalDays: s.odometerReminderIntervalDays, locationRetentionDays: s.locationRetentionDays) }
        )
        
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        
        do {
            let data = try encoder.encode(backup)
            let tempDir = FileManager.default.temporaryDirectory
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyyMMdd_HHmmss"
            let fileName = "CIMDriver_Backup_\(formatter.string(from: Date())).json"
            let fileURL = tempDir.appendingPathComponent(fileName)
            
            try data.write(to: fileURL)
            return fileURL
        } catch {
            print("Backup failed: \(error)")
            return nil
        }
    }
    
    func restoreBackup(url: URL, context: ModelContext) -> Bool {
        do {
            guard url.startAccessingSecurityScopedResource() else { return false }
            defer { url.stopAccessingSecurityScopedResource() }
            
            let data = try Data(contentsOf: url)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let backup = try decoder.decode(BackupData.self, from: data)
            
            // Wipe current data
            try? context.delete(model: Trip.self)
            try? context.delete(model: Vehicle.self)
            try? context.delete(model: SavedAddress.self)
            try? context.delete(model: ClassificationRule.self)
            try? context.delete(model: WorkDay.self)
            try? context.delete(model: AppSettings.self)
            
            // Restore
            var vehicleMap = [UUID: Vehicle]()
            for v in backup.vehicles {
                let vehicle = Vehicle(id: v.id, name: v.name, licensePlate: v.licensePlate, make: v.make, model: v.model, odometerStart: v.odometerStart, odometerCurrent: v.odometerCurrent, privateKmYearlyLimit: v.privateKmYearlyLimit, usageType: v.usageType, odometerCorrectionStrategy: v.odometerCorrectionStrategy)
                vehicle.isDefault = v.isDefault
                context.insert(vehicle)
                vehicleMap[v.id] = vehicle
            }
            
            for t in backup.trips {
                let vehicle = t.vehicleId.flatMap { vehicleMap[$0] }
                let trip = Trip(id: t.id, vehicle: vehicle, startTime: t.startTime, endTime: t.endTime, startAddress: t.startAddress, endAddress: t.endAddress, distanceMeters: t.distanceMeters, tripType: t.tripType, note: t.note, status: t.status, isManual: t.isManual, odometerStart: t.odometerStart, odometerEnd: t.odometerEnd, projectCode: t.projectCode)
                context.insert(trip)
            }
            
            for a in backup.addresses {
                context.insert(SavedAddress(id: a.id, label: a.label, address: a.address, latitude: a.latitude, longitude: a.longitude))
            }
            
            for r in backup.rules {
                context.insert(ClassificationRule(id: r.id, name: r.name, startAddressType: r.startAddressType, endAddressType: r.endAddressType, startAddress: r.startAddress, endAddress: r.endAddress, tripType: r.tripType, category: r.category))
            }
            
            for w in backup.workDays {
                let wd = WorkDay(id: w.id, date: w.date, firstDepartureTime: w.firstDepartureTime, arrivalTime: w.arrivalTime, departureTime: w.departureTime, lastArrivalTime: w.lastArrivalTime, roundedArrivalTime: w.roundedArrivalTime, roundedDepartureTime: w.roundedDepartureTime, breakMinutes: w.breakMinutes, workLocationLabel: w.workLocationLabel, projectCode: w.projectCode, status: w.status, note: w.note)
                context.insert(wd)
            }
            
            if let s = backup.settings {
                let settings = AppSettings(id: "singleton", workStartTime: s.workStartTime, workEndTime: s.workEndTime, workDays: s.workDays, breakMinutes: s.breakMinutes, toleranceMinutes: s.toleranceMinutes, gracePeriodSec: s.gracePeriodSec, trackingIntervalSec: s.trackingIntervalSec, classificationDefault: s.classificationDefault, classifyHomeWorkAsCommute: s.classifyHomeWorkAsCommute, classifyCustomerAsBusiness: s.classifyCustomerAsBusiness, autoMergeEnabled: s.autoMergeEnabled, mergeTimeLimitMin: s.mergeTimeLimitMin, mergeDistanceM: s.mergeDistanceM, businessCompensation: s.businessCompensation, themeMode: s.themeMode, odometerReminder: s.odometerReminder, odometerReminderIntervalDays: s.odometerReminderIntervalDays, locationRetentionDays: s.locationRetentionDays)
                context.insert(settings)
            }
            
            try context.save()
            return true
        } catch {
            print("Restore failed: \(error)")
            return false
        }
    }
}
