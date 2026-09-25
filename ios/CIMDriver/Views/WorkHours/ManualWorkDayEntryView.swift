import SwiftUI
import SwiftData

struct ManualWorkDayEntryView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    var workDayToEdit: WorkDay?
    
    @State private var date: Date
    @State private var firstDepartureTime: Date
    @State private var arrivalTime: Date
    @State private var departureTime: Date
    @State private var breakMinutes: String
    @State private var note: String
    
    init(workDayToEdit: WorkDay? = nil) {
        self.workDayToEdit = workDayToEdit
        let now = Date()
        _date = State(initialValue: workDayToEdit?.date ?? now)
        _firstDepartureTime = State(initialValue: workDayToEdit?.firstDepartureTime ?? Calendar.current.date(bySettingHour: 7, minute: 30, second: 0, of: now)!)
        _arrivalTime = State(initialValue: workDayToEdit?.arrivalTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: now)!)
        _departureTime = State(initialValue: workDayToEdit?.departureTime ?? Calendar.current.date(bySettingHour: 17, minute: 0, second: 0, of: now)!)
        _breakMinutes = State(initialValue: String(workDayToEdit?.breakMinutes ?? 30))
        _note = State(initialValue: workDayToEdit?.note ?? "")
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Datum")) {
                    DatePicker("Datum", selection: $date, displayedComponents: .date)
                }
                
                Section(header: Text("Tijden")) {
                    DatePicker("Vertrek (Thuis)", selection: $firstDepartureTime, displayedComponents: .hourAndMinute)
                    DatePicker("Aankomst (Werk)", selection: $arrivalTime, displayedComponents: .hourAndMinute)
                    DatePicker("Vertrek (Werk)", selection: $departureTime, displayedComponents: .hourAndMinute)
                }
                
                Section(header: Text("Pauze")) {
                    TextField("Pauze in minuten", text: $breakMinutes)
                        .keyboardType(.numberPad)
                }
                
                Section(header: Text("Notities")) {
                    TextField("Optionele notitie", text: $note, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(workDayToEdit == nil ? "Nieuwe Werkdag" : "Werkdag Aanpassen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuleer") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Opslaan") {
                        saveWorkDay()
                    }
                }
            }
        }
    }
    
    private func saveWorkDay() {
        guard let settings = (try? modelContext.fetch(FetchDescriptor<AppSettings>()))?.first else {
            dismiss()
            return
        }
        
        let breakMinInput = Int(breakMinutes) ?? settings.breakMinutes
        let roundedArrival = WorkHoursNormalizer.roundToNearestQuarterHour(arrivalTime)
        let roundedDeparture = WorkHoursNormalizer.roundToNearestQuarterHour(departureTime)
        let effectiveBreak = WorkHoursNormalizer.effectiveBreakMinutes(
            start: roundedArrival,
            end: roundedDeparture,
            configuredBreakMinutes: breakMinInput,
            toleranceMinutes: settings.toleranceMinutes
        )
        
        if let workDay = workDayToEdit {
            workDay.date = date
            workDay.firstDepartureTime = firstDepartureTime
            workDay.arrivalTime = arrivalTime
            workDay.departureTime = departureTime
            workDay.lastArrivalTime = departureTime
            workDay.roundedArrivalTime = roundedArrival
            workDay.roundedDepartureTime = roundedDeparture
            workDay.breakMinutes = effectiveBreak
            workDay.note = note.isEmpty ? nil : note
            workDay.status = "APPROVED"
        } else {
            let newWorkDay = WorkDay(
                date: date,
                firstDepartureTime: firstDepartureTime,
                arrivalTime: arrivalTime,
                departureTime: departureTime,
                lastArrivalTime: departureTime,
                roundedArrivalTime: roundedArrival,
                roundedDepartureTime: roundedDeparture,
                breakMinutes: effectiveBreak,
                status: "APPROVED",
                note: note.isEmpty ? nil : note
            )
            modelContext.insert(newWorkDay)
        }
        
        try? modelContext.save()
        dismiss()
    }
}
