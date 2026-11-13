import Foundation
import UserNotifications

enum MonthlyClosingNotificationManager {
    static let notificationIdentifier = "com.cimdriver.monthly_closing"
    
    static func scheduleMonthlyClosingReminder() {
        let center = UNUserNotificationCenter.current()
        
        center.removePendingNotificationRequests(withIdentifiers: [notificationIdentifier])
        
        let content = UNMutableNotificationContent()
        content.title = "Fiscale Maandafsluiting"
        content.body = "De rittenadministratie van de afgelopen maand staat klaar voor export. Tik om te controleren en te delen."
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.day = 1
        dateComponents.hour = 9
        dateComponents.minute = 0
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: notificationIdentifier, content: content, trigger: trigger)
        
        center.add(request) { error in
            if let error = error {
                print("Failed to schedule monthly closing notification: \(error)")
            }
        }
    }
}

