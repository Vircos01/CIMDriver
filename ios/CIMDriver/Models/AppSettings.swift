import Foundation
import SwiftData

@Model
final class AppSettings {
    @Attribute(.unique) var id: String = "singleton"
    
    // Werkuren
    var workStartTime: Date
    var workEndTime: Date
    var workDays: String // e.g. "1=08:00-18:00;2=08:00-18:00"
    var breakMinutes: Int
    var toleranceMinutes: Int = 30
    
    // Tracking
    var gracePeriodSec: Int
    var trackingIntervalSec: Int
    
    // Classificatie
    var classificationDefault: String
    var classifyHomeWorkAsCommute: Bool
    var classifyCustomerAsBusiness: Bool
    
    // Auto-merge
    var autoMergeEnabled: Bool
    var mergeTimeLimitMin: Int
    var mergeDistanceM: Int
    
    // Vergoeding
    var businessCompensation: Double // € per km
    
    // UI
    var themeMode: String // "SYSTEM", "LIGHT", "DARK"
    
    // Km-stand
    var odometerReminder: Bool
    var odometerReminderIntervalDays: Int
    
    // Notificaties
    var workdayNotificationsEnabled: Bool?
    var geofenceNotificationsEnabled: Bool?
    var autoStartNotificationsEnabled: Bool?
    
    // Privacy
    var locationRetentionDays: Int

    init(
        id: String = "singleton",
        workStartTime: Date = Date(), // Should be configured properly later
        workEndTime: Date = Date(),
        workDays: String = "1=08:00-17:00;2=08:00-17:00;3=08:00-17:00;4=08:00-17:00;5=08:00-17:00",
        breakMinutes: Int = 30,
        toleranceMinutes: Int = 30,
        gracePeriodSec: Int = 180,
        trackingIntervalSec: Int = 10,
        classificationDefault: String = "PRIVATE",
        classifyHomeWorkAsCommute: Bool = true,
        classifyCustomerAsBusiness: Bool = true,
        autoMergeEnabled: Bool = true,
        mergeTimeLimitMin: Int = 30,
        mergeDistanceM: Int = 400,
        businessCompensation: Double = 0.23,
        themeMode: String = "SYSTEM",
        odometerReminder: Bool = true,
        odometerReminderIntervalDays: Int = 30,
        workdayNotificationsEnabled: Bool = true,
        geofenceNotificationsEnabled: Bool = true,
        autoStartNotificationsEnabled: Bool = true,
        locationRetentionDays: Int = 365
    ) {
        self.id = id
        self.workStartTime = workStartTime
        self.workEndTime = workEndTime
        self.workDays = workDays
        self.breakMinutes = breakMinutes
        self.toleranceMinutes = toleranceMinutes
        self.gracePeriodSec = gracePeriodSec
        self.trackingIntervalSec = trackingIntervalSec
        self.classificationDefault = classificationDefault
        self.classifyHomeWorkAsCommute = classifyHomeWorkAsCommute
        self.classifyCustomerAsBusiness = classifyCustomerAsBusiness
        self.autoMergeEnabled = autoMergeEnabled
        self.mergeTimeLimitMin = mergeTimeLimitMin
        self.mergeDistanceM = mergeDistanceM
        self.businessCompensation = businessCompensation
        self.themeMode = themeMode
        self.odometerReminder = odometerReminder
        self.odometerReminderIntervalDays = odometerReminderIntervalDays
        self.workdayNotificationsEnabled = workdayNotificationsEnabled
        self.geofenceNotificationsEnabled = geofenceNotificationsEnabled
        self.autoStartNotificationsEnabled = autoStartNotificationsEnabled
        self.locationRetentionDays = locationRetentionDays
    }
}
