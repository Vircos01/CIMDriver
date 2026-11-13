import Foundation
import SwiftData
import UIKit

struct ExportUtil {
    
    // MARK: - CSV Export
    
    static func generateTripsCSV(trips: [Trip], periodTitle: String? = nil) -> URL? {
        let suffix = periodTitle?.replacingOccurrences(of: " ", with: "_").replacingOccurrences(of: "/", with: "-")
            ?? Date().formatted(date: .numeric, time: .omitted).replacingOccurrences(of: "/", with: "-")
        let fileName = "CIMDriver_Ritten_\(suffix).csv"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        var csvString = "Datum,Vertrek,Aankomst,Kenteken,Voertuig,Teller Start,Teller Eind,Afstand (km),Privé-omrijden (km),Type,Status,Start Adres,Eind Adres,Notities\n"
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
        
        let dateOnlyFormatter = DateFormatter()
        dateOnlyFormatter.dateStyle = .short
        
        for trip in trips.sorted(by: { $0.startTime > $1.startTime }) {
            let dateStr = dateOnlyFormatter.string(from: trip.startTime)
            let startStr = dateFormatter.string(from: trip.startTime)
            let endStr = trip.endTime != nil ? dateFormatter.string(from: trip.endTime!) : ""
            let plate = trip.vehicle?.licensePlate ?? ""
            let vehicleName = trip.vehicle != nil ? "\(trip.vehicle!.make) \(trip.vehicle!.model)".trimmingCharacters(in: .whitespaces) : ""
            
            let odoStart = trip.odometerStart
            let odoEnd = trip.odometerEnd ?? (trip.odometerStart + Int(round(Double(trip.distanceMeters) / 1000.0)))
            let distance = String(format: "%.1f", Double(trip.distanceMeters) / 1000.0)
            let detour = String(format: "%.1f", Double(trip.privateDetourMeters) / 1000.0)
            let type = trip.tripType
            let status = trip.status
            let startAddress = (trip.startAddress ?? "").replacingOccurrences(of: ",", with: ";")
            let endAddress = (trip.endAddress ?? "").replacingOccurrences(of: ",", with: ";")
            let notes = (trip.note ?? trip.projectCode ?? "").replacingOccurrences(of: ",", with: ";")
            
            let row = "\(dateStr),\(startStr),\(endStr),\(plate),\(vehicleName),\(odoStart),\(odoEnd),\(distance),\(detour),\(type),\(status),\(startAddress),\(endAddress),\(notes)\n"
            csvString.append(row)
        }
        
        do {
            try csvString.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            print("Failed to create CSV file: \(error)")
            return nil
        }
    }
    
    static func generateWorkDaysCSV(workDays: [WorkDay], periodTitle: String? = nil) -> URL? {
        let suffix = periodTitle?.replacingOccurrences(of: " ", with: "_").replacingOccurrences(of: "/", with: "-")
            ?? Date().formatted(date: .numeric, time: .omitted).replacingOccurrences(of: "/", with: "-")
        let fileName = "CIMDriver_Werkuren_\(suffix).csv"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        var csvString = "Datum,Eerste Vertrek,Aankomst,Vertrek Werk,Laatste Aankomst,Pauze (min),Totaal Uur,Project,Locatie,Notitie\n"
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        
        let timeFormatter = DateFormatter()
        timeFormatter.timeStyle = .short
        
        var sumHours = 0.0
        for wd in workDays.sorted(by: { $0.date > $1.date }) {
            let dateStr = dateFormatter.string(from: wd.date)
            let firstDep = timeFormatter.string(from: wd.firstDepartureTime)
            let arr = timeFormatter.string(from: wd.roundedArrivalTime ?? wd.arrivalTime)
            let dep = wd.departureTime != nil ? timeFormatter.string(from: wd.roundedDepartureTime ?? wd.departureTime!) : ""
            let lastArr = wd.lastArrivalTime != nil ? timeFormatter.string(from: wd.lastArrivalTime!) : ""
            
            var totalHours = 0.0
            if let depTime = wd.roundedDepartureTime ?? wd.departureTime {
                let arrTime = wd.roundedArrivalTime ?? wd.arrivalTime
                let seconds = max(0, depTime.timeIntervalSince(arrTime))
                let minutes = max(0, (seconds / 60.0) - Double(wd.breakMinutes))
                totalHours = minutes / 60.0
            }
            sumHours += totalHours
            
            let totalStr = String(format: "%.1f", totalHours)
            let project = (wd.projectCode ?? "").replacingOccurrences(of: ",", with: ";")
            let location = (wd.workLocationLabel ?? "").replacingOccurrences(of: ",", with: ";")
            let note = (wd.note ?? "").replacingOccurrences(of: ",", with: ";")
            
            let row = "\(dateStr),\(firstDep),\(arr),\(dep),\(lastArr),\(wd.breakMinutes),\(totalStr),\(project),\(location),\(note)\n"
            csvString.append(row)
        }
        
        // Total row
        csvString.append("TOTAAL,,,,,,,\(String(format: "%.1f", sumHours)),,,\n")
        
        do {
            try csvString.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            print("Failed to create CSV file: \(error)")
            return nil
        }
    }
    
    // MARK: - PDF Export
    
    static func generateTripsPDF(trips: [Trip], periodTitle: String? = nil, odometerChecks: [OdometerCheck] = []) -> URL? {
        let suffix = periodTitle?.replacingOccurrences(of: " ", with: "_").replacingOccurrences(of: "/", with: "-")
            ?? Date().formatted(date: .numeric, time: .omitted).replacingOccurrences(of: "/", with: "-")
        let fileName = "CIMDriver_Ritten_\(suffix).pdf"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        let pdfMetaData = [
            kCGPDFContextCreator: "CIMDriver iOS",
            kCGPDFContextAuthor: "CIMDriver User"
        ]
        
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = pdfMetaData as [String: Any]
        
        // A4 Landscape format: 841.8 x 595.2
        let pageWidth = 841.8
        let pageHeight = 595.2
        let pageRect = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect, format: format)
        
        let data = renderer.pdfData { (context) in
            context.beginPage()
            
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 18)
            ]
            let docTitle = periodTitle != nil ? "CIMDriver Rittenregistratie - \(periodTitle!)" : "CIMDriver Rittenregistratie"
            docTitle.draw(at: CGPoint(x: 35, y: 25), withAttributes: titleAttributes)
            
            let subAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 10),
                .foregroundColor: UIColor.darkGray
            ]
            "Sluitende rittenadministratie conform richtlijnen Belastingdienst Nederland"
                .draw(at: CGPoint(x: 35, y: 48), withAttributes: subAttributes)
            
            let defaultVehicle = trips.first?.vehicle
            let vehicleStr = defaultVehicle != nil ? "\(defaultVehicle!.make) \(defaultVehicle!.model) (\(defaultVehicle!.licensePlate))" : "Alle voertuigen"
            let periodStr = periodTitle ?? "Volledig overzicht"
            let metaStr = "Voertuig: \(vehicleStr)   |   Periode: \(periodStr)   |   Aantal ritten: \(trips.count)"
            metaStr.draw(at: CGPoint(x: 35, y: 64), withAttributes: subAttributes)
            
            // Header line
            let cgContext = context.cgContext
            cgContext.setStrokeColor(UIColor.lightGray.cgColor)
            cgContext.setLineWidth(1)
            cgContext.move(to: CGPoint(x: 35, y: 80))
            cgContext.addLine(to: CGPoint(x: 807, y: 80))
            cgContext.strokePath()
            
            // Table Header columns
            let columns = ["Datum & Tijd", "Kenteken", "Start km", "Eind km", "Afstand", "Omrij", "Type", "Vertrekadres", "Aankomstadres", "Doel / Notitie"]
            let xPositions: [CGFloat] = [35, 125, 190, 245, 300, 350, 395, 435, 580, 720]
            
            let headerAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 9)
            ]
            
            func drawTableHeader(at y: CGFloat) {
                for (i, column) in columns.enumerated() {
                    column.draw(at: CGPoint(x: xPositions[i], y: y), withAttributes: headerAttributes)
                }
                cgContext.move(to: CGPoint(x: 35, y: y + 14))
                cgContext.addLine(to: CGPoint(x: 807, y: y + 14))
                cgContext.strokePath()
            }
            
            drawTableHeader(at: 86)
            var currentY: CGFloat = 106
            
            let rowAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 8.5)
            ]
            
            let df = DateFormatter()
            df.dateFormat = "dd-MM-yy HH:mm"
            
            var totalDistMeters = 0
            var totalZakelijkMeters = 0
            var totalWoonWerkMeters = 0
            var totalPriveMeters = 0
            var totalDetourMeters = 0
            
            for trip in trips.sorted(by: { $0.startTime > $1.startTime }) {
                if currentY > pageHeight - 65 {
                    context.beginPage()
                    drawTableHeader(at: 30)
                    currentY = 50
                }
                
                let dateStr = df.string(from: trip.startTime)
                let plate = String((trip.vehicle?.licensePlate ?? "").prefix(9))
                let odoStartStr = "\(trip.odometerStart)"
                let odoEndVal = trip.odometerEnd ?? (trip.odometerStart + Int(round(Double(trip.distanceMeters) / 1000.0)))
                let odoEndStr = "\(odoEndVal)"
                let distStr = String(format: "%.1f km", Double(trip.distanceMeters) / 1000.0)
                let detourStr = trip.privateDetourMeters > 0 ? String(format: "%.1f km", Double(trip.privateDetourMeters) / 1000.0) : "-"
                
                let typeStr: String
                switch trip.tripType {
                case "BUSINESS": typeStr = "Zakelijk"
                case "COMMUTE": typeStr = "Woon-werk"
                case "PRIVATE": typeStr = "Privé"
                default: typeStr = trip.tripType
                }
                
                let startAddr = String((trip.startAddress ?? "Onbekend").prefix(24))
                let endAddr = String((trip.endAddress ?? "Onbekend").prefix(24))
                let note = String((trip.note ?? trip.projectCode ?? "").prefix(16))
                
                let rowData = [dateStr, plate, odoStartStr, odoEndStr, distStr, detourStr, typeStr, startAddr, endAddr, note]
                for (i, text) in rowData.enumerated() {
                    text.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: rowAttributes)
                }
                
                totalDistMeters += trip.distanceMeters
                totalDetourMeters += trip.privateDetourMeters
                if trip.tripType == "BUSINESS" { totalZakelijkMeters += trip.distanceMeters }
                else if trip.tripType == "COMMUTE" { totalWoonWerkMeters += trip.distanceMeters }
                else { totalPriveMeters += trip.distanceMeters }
                
                currentY += 15
            }
            
            // Bottom totals block
            if currentY > pageHeight - 50 {
                context.beginPage()
                currentY = 35
            }
            
            cgContext.move(to: CGPoint(x: 35, y: currentY + 4))
            cgContext.addLine(to: CGPoint(x: 807, y: currentY + 4))
            cgContext.strokePath()
            currentY += 10
            
            let totalKm = String(format: "%.1f", Double(totalDistMeters) / 1000.0)
            let zakKm = String(format: "%.1f", Double(totalZakelijkMeters) / 1000.0)
            let wwKm = String(format: "%.1f", Double(totalWoonWerkMeters) / 1000.0)
            let priKm = String(format: "%.1f", Double(totalPriveMeters) / 1000.0)
            let detKm = String(format: "%.1f", Double(totalDetourMeters) / 1000.0)
            
            let summaryStr = "TOTALEN: Totaal: \(totalKm) km   |   Zakelijk: \(zakKm) km   |   Woon-werk: \(wwKm) km   |   Privé: \(priKm) km   |   Privé-omrijden: \(detKm) km"
            summaryStr.draw(at: CGPoint(x: 35, y: currentY), withAttributes: headerAttributes)

            // Appendix: Fiscale Verificatie Kilometerstanden
            if !odometerChecks.isEmpty {
                context.beginPage()
                let appendixTitleAttributes: [NSAttributedString.Key: Any] = [
                    .font: UIFont.boldSystemFont(ofSize: 16)
                ]
                "Bijlage: Fiscale Verificatie Kilometerstanden (Audit Trail)"
                    .draw(at: CGPoint(x: 35, y: 25), withAttributes: appendixTitleAttributes)
                
                "Fotobewijs en fysieke meteropnames conform Belastingdienst-controleerbaarheid"
                    .draw(at: CGPoint(x: 35, y: 46), withAttributes: subAttributes)
                
                cgContext.move(to: CGPoint(x: 35, y: 64))
                cgContext.addLine(to: CGPoint(x: 807, y: 64))
                cgContext.strokePath()
                
                let checkCols = ["Datum & Tijd", "Kenteken", "Geregistreerd", "Geverifieerd", "Verschil", "Toelichting", "Fotobewijs"]
                let checkXPositions: [CGFloat] = [35, 140, 230, 310, 390, 460, 630]
                
                for (i, col) in checkCols.enumerated() {
                    col.draw(at: CGPoint(x: checkXPositions[i], y: 70), withAttributes: headerAttributes)
                }
                
                cgContext.move(to: CGPoint(x: 35, y: 84))
                cgContext.addLine(to: CGPoint(x: 807, y: 84))
                cgContext.strokePath()
                
                var checkY: CGFloat = 94
                for check in odometerChecks.sorted(by: { $0.timestamp < $1.timestamp }) {
                    if checkY > pageHeight - 65 {
                        context.beginPage()
                        checkY = 35
                    }
                    
                    let dateStr = df.string(from: check.timestamp)
                    let checkVehicle = trips.first(where: { $0.vehicle?.id == check.vehicleId })?.vehicle
                    let plate = checkVehicle?.licensePlate ?? "-"
                    let regStr = "\(check.registeredOdometer) km"
                    let corrStr = "\(check.correctedOdometer) km"
                    let diff = check.correctedOdometer - check.registeredOdometer
                    let diffStr = "\(diff > 0 ? "+\(diff)" : "\(diff)") km"
                    let note = "Periodieke opname"
                    
                    let rowTexts = [dateStr, plate, regStr, corrStr, diffStr, note]
                    for (i, txt) in rowTexts.enumerated() {
                        txt.draw(at: CGPoint(x: checkXPositions[i], y: checkY + 12), withAttributes: rowAttributes)
                    }
                    
                    var photoRendered = false
                    if let photoData = check.photoData, let image = UIImage(data: photoData) {
                        let imgRect = CGRect(x: 630, y: checkY, width: 50, height: 38)
                        image.draw(in: imgRect)
                        photoRendered = true
                    }
                    if !photoRendered {
                        (check.photoData != nil ? "Foto opgeslagen" : "Geen foto")
                            .draw(at: CGPoint(x: 630, y: checkY + 12), withAttributes: rowAttributes)
                    }
                    
                    checkY += 45
                    cgContext.move(to: CGPoint(x: 35, y: checkY - 4))
                    cgContext.addLine(to: CGPoint(x: 807, y: checkY - 4))
                    cgContext.strokePath()
                }
            }
        }
        
        do {
            try data.write(to: fileURL)
            return fileURL
        } catch {
            print("Failed to save PDF: \(error)")
            return nil
        }
    }
    
    static func generateWorkDaysPDF(workDays: [WorkDay], periodTitle: String? = nil) -> URL? {
        let suffix = periodTitle?.replacingOccurrences(of: " ", with: "_").replacingOccurrences(of: "/", with: "-")
            ?? Date().formatted(date: .numeric, time: .omitted).replacingOccurrences(of: "/", with: "-")
        let fileName = "CIMDriver_Werkuren_\(suffix).pdf"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        let pdfMetaData = [
            kCGPDFContextCreator: "CIMDriver iOS",
            kCGPDFContextAuthor: "CIMDriver User"
        ]
        
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = pdfMetaData as [String: Any]
        
        // A4 format: 595.2 x 841.8
        let pageWidth = 595.2
        let pageHeight = 841.8
        let pageRect = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect, format: format)
        
        let data = renderer.pdfData { (context) in
            context.beginPage()
            
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 22)
            ]
            let title = periodTitle != nil ? "Werkuren Registratie - \(periodTitle!)" : "Werkuren Registratie"
            title.draw(at: CGPoint(x: 25, y: 25), withAttributes: titleAttributes)
            
            var totalHoursSum = 0.0
            for wd in workDays {
                if let depTime = wd.roundedDepartureTime ?? wd.departureTime {
                    let arrTime = wd.roundedArrivalTime ?? wd.arrivalTime
                    let seconds = max(0, depTime.timeIntervalSince(arrTime))
                    let minutes = max(0, (seconds / 60.0) - Double(wd.breakMinutes))
                    totalHoursSum += (minutes / 60.0)
                }
            }
            
            let dateAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 11),
                .foregroundColor: UIColor.gray
            ]
            let dateStr = "Gegenereerd op: \(Date().formatted(date: .abbreviated, time: .shortened)) • \(workDays.count) dagen • Totaal: \(String(format: "%.1f", totalHoursSum)) uur"
            dateStr.draw(at: CGPoint(x: 25, y: 55), withAttributes: dateAttributes)
            
            // Draw table header
            var currentY: CGFloat = 90
            let columns = ["Datum", "Start", "Pauze", "Eind", "Uren", "Project / Notitie"]
            let xPositions: [CGFloat] = [25, 120, 190, 260, 330, 410]
            
            let headerAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 12)
            ]
            
            for (i, column) in columns.enumerated() {
                column.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: headerAttributes)
            }
            
            let cgContext = context.cgContext
            cgContext.setStrokeColor(UIColor.lightGray.cgColor)
            cgContext.setLineWidth(1)
            cgContext.move(to: CGPoint(x: 25, y: currentY + 18))
            cgContext.addLine(to: CGPoint(x: 570, y: currentY + 18))
            cgContext.strokePath()
            
            currentY += 25
            
            let rowAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 11)
            ]
            
            let dfDate = DateFormatter()
            dfDate.dateStyle = .short
            let dfTime = DateFormatter()
            dfTime.timeStyle = .short
            
            for wd in workDays.sorted(by: { $0.date > $1.date }) {
                if currentY > pageHeight - 50 {
                    context.beginPage()
                    currentY = 30
                }
                
                let dateStr = dfDate.string(from: wd.date)
                let firstDep = dfTime.string(from: wd.firstDepartureTime)
                let dep = wd.departureTime != nil ? dfTime.string(from: wd.roundedDepartureTime ?? wd.departureTime!) : ""
                let breakStr = "\(wd.breakMinutes)m"
                
                var totalHours = 0.0
                if let depTime = wd.roundedDepartureTime ?? wd.departureTime {
                    let arrTime = wd.roundedArrivalTime ?? wd.arrivalTime
                    let seconds = max(0, depTime.timeIntervalSince(arrTime))
                    let minutes = max(0, (seconds / 60.0) - Double(wd.breakMinutes))
                    totalHours = minutes / 60.0
                }
                let totalStr = String(format: "%.1fu", totalHours)
                let projectOrNote = String(([wd.projectCode, wd.note].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " - ")).prefix(20))
                
                let rowData = [dateStr, firstDep, breakStr, dep, totalStr, projectOrNote]
                for (i, text) in rowData.enumerated() {
                    text.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: rowAttributes)
                }
                
                currentY += 20
            }
            
            // Bottom total
            cgContext.move(to: CGPoint(x: 25, y: currentY + 4))
            cgContext.addLine(to: CGPoint(x: 570, y: currentY + 4))
            cgContext.strokePath()
            currentY += 10
            
            let summaryText = "TOTAAL GEWERKT: \(String(format: "%.1f", totalHoursSum)) uur (\(workDays.count) werkdagen)"
            summaryText.draw(at: CGPoint(x: 25, y: currentY), withAttributes: headerAttributes)
        }
        
        do {
            try data.write(to: fileURL)
            return fileURL
        } catch {
            print("Failed to save PDF: \(error)")
            return nil
        }
    }
}
