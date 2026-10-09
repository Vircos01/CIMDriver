import SwiftUI
import SwiftData

struct TripsCalendarView: View {
    var trips: [Trip]
    
    @State private var selectedMonth: Date = Date()
    
    private let calendar = Calendar.current
    
    private var daysInMonth: [Date] {
        guard let monthInterval = calendar.dateInterval(of: .month, for: selectedMonth) else { return [] }
        var dates: [Date] = []
        var currentDate = monthInterval.start
        
        while currentDate < monthInterval.end {
            dates.append(currentDate)
            currentDate = calendar.date(byAdding: .day, value: 1, to: currentDate)!
        }
        return dates
    }
    
    private var startOffset: Int {
        let firstDay = daysInMonth.first!
        let weekday = calendar.component(.weekday, from: firstDay) // Sunday = 1
        return (weekday + 5) % 7 // Convert so Monday = 0
    }
    
    private func tripsFor(date: Date) -> [Trip] {
        trips.filter { calendar.isDate($0.startTime, inSameDayAs: date) }
    }
    
    var body: some View {
        VStack {
            // Month Selector
            HStack {
                Button(action: {
                    selectedMonth = calendar.date(byAdding: .month, value: -1, to: selectedMonth) ?? selectedMonth
                }) {
                    Image(systemName: "chevron.left")
                        .padding()
                }
                
                Spacer()
                
                Text(monthYearString(from: selectedMonth))
                    .font(.title2.bold())
                
                Spacer()
                
                Button(action: {
                    selectedMonth = calendar.date(byAdding: .month, value: 1, to: selectedMonth) ?? selectedMonth
                }) {
                    Image(systemName: "chevron.right")
                        .padding()
                }
            }
            
            // Weekday Headers
            HStack {
                let weekdays = ["Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo"]
                ForEach(weekdays, id: \.self) { day in
                    Text(day)
                        .frame(maxWidth: .infinity)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.bottom, 8)
            
            // Calendar Grid
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 8) {
                ForEach(0..<startOffset, id: \.self) { _ in
                    Color.clear.aspectRatio(1, contentMode: .fill)
                }
                
                ForEach(daysInMonth, id: \.self) { date in
                    CalendarDayView(date: date, trips: tripsFor(date: date))
                }
            }
            
            Spacer()
        }
        .padding()
    }
    
    private func monthYearString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "nl_NL")
        return formatter.string(from: date).capitalized
    }
}

struct CalendarDayView: View {
    let date: Date
    let trips: [Trip]
    
    var body: some View {
        let hasBusiness = trips.contains { $0.tripType == "BUSINESS" || $0.tripType == "Customer Visit" }
        let hasCommute = trips.contains { $0.tripType == "COMMUTE" || $0.tripType == "Home To Work" }
        let hasPrivate = trips.contains { $0.tripType == "PRIVATE" || $0.tripType == "PERSONAL" }
        
        let bgColor: Color = {
            if hasBusiness && hasPrivate { return Color.purple.opacity(0.2) }
            if hasBusiness { return Color.cimNavy.opacity(0.2) }
            if hasCommute { return Color.orange.opacity(0.2) }
            if hasPrivate { return Color.green.opacity(0.2) }
            return Color.clear
        }()
        
        VStack(spacing: 2) {
            Text("\(Calendar.current.component(.day, from: date))")
                .font(.body)
                .foregroundColor(trips.isEmpty ? .primary : .primary)
            
            if !trips.isEmpty {
                HStack(spacing: 2) {
                    ForEach(0..<min(3, trips.count), id: \.self) { _ in
                        Circle()
                            .fill(Color.primary.opacity(0.5))
                            .frame(width: 4, height: 4)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .background(bgColor)
        .clipShape(Circle())
        .onTapGesture {
            // Optional: Handle day tap
        }
    }
}
