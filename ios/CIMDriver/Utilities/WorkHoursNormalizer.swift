import Foundation

enum WorkHoursNormalizer {
    static let defaultToleranceMinutes: TimeInterval = 90.0

    static func normalize(
        actualTime: Date,
        dayStart: Date,
        configuredTime: String,
        toleranceMinutes: TimeInterval = defaultToleranceMinutes
    ) -> Date {
        guard let configuredTimestamp = configuredTimestamp(dayStart: dayStart, configuredTime: configuredTime) else {
            return actualTime
        }
        
        let differenceMinutes = abs(actualTime.timeIntervalSince(configuredTimestamp)) / 60.0
        
        if differenceMinutes <= toleranceMinutes {
            return configuredTimestamp
        }
        
        return actualTime
    }
    
    static func roundToNearestQuarterHour(_ date: Date) -> Date {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let minute = components.minute ?? 0
        let roundedMinute = Int((Double(minute) / 15.0).rounded()) * 15
        
        var updated = components
        updated.minute = roundedMinute == 60 ? 0 : roundedMinute
        updated.second = 0
        updated.nanosecond = 0
        
        let baseDate = calendar.date(from: updated) ?? date
        return roundedMinute == 60 ? calendar.date(byAdding: .hour, value: 1, to: baseDate) ?? baseDate : baseDate
    }
    
    static func effectiveBreakMinutes(
        start: Date,
        end: Date,
        configuredBreakMinutes: Int,
        toleranceMinutes: Int
    ) -> Int {
        let durationMinutes = max(0, Int(end.timeIntervalSince(start) / 60.0))
        guard durationMinutes > 0 else { return 0 }
        
        let normalizedBreak = max(0, configuredBreakMinutes)
        if durationMinutes <= toleranceMinutes {
            return 0
        }
        
        return min(normalizedBreak, durationMinutes)
    }

    private static func configuredTimestamp(dayStart: Date, configuredTime: String) -> Date? {
        let normalizedTime = configuredTime.replacingOccurrences(of: ".", with: ":")
        
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        
        guard let parsed = formatter.date(from: normalizedTime) else { return nil }
        
        let calendar = Calendar.current
        
        let timeComponents = calendar.dateComponents([.hour, .minute], from: parsed)
        var dayComponents = calendar.dateComponents([.year, .month, .day], from: dayStart)
        
        dayComponents.hour = timeComponents.hour
        dayComponents.minute = timeComponents.minute
        dayComponents.second = 0
        dayComponents.nanosecond = 0
        
        return calendar.date(from: dayComponents)
    }
}
