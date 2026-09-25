import Foundation
import SwiftData
import UIKit

struct ExportUtil {
    
    // MARK: - CSV Export
    
    static func generateTripsCSV(trips: [Trip]) -> URL? {
        let fileName = "CIMDriver_Ritten_\(Date().formatted(date: .numeric, time: .omitted)).csv"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName.replacingOccurrences(of: "/", with: "-"))
        
        var csvString = "Datum,Vertrek,Aankomst,Start Adres,Eind Adres,Afstand (km),Type,Status,Notities\n"
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
        
        for trip in trips.sorted(by: { $0.startTime > $1.startTime }) {
            let startStr = dateFormatter.string(from: trip.startTime)
            let endStr = trip.endTime != nil ? dateFormatter.string(from: trip.endTime!) : ""
            // Simple date extraction from startTime
            let dateOnlyFormatter = DateFormatter()
            dateOnlyFormatter.dateStyle = .short
            let dateStr = dateOnlyFormatter.string(from: trip.startTime)
            
            let startAddress = (trip.startAddress ?? "").replacingOccurrences(of: ",", with: ";")
            let endAddress = (trip.endAddress ?? "").replacingOccurrences(of: ",", with: ";")
            let distance = String(format: "%.1f", Double(trip.distanceMeters) / 1000.0)
            let type = trip.tripType
            let status = trip.status
            let notes = (trip.note ?? "").replacingOccurrences(of: ",", with: ";")
            
            let row = "\(dateStr),\(startStr),\(endStr),\(startAddress),\(endAddress),\(distance),\(type),\(status),\(notes)\n"
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
    
    static func generateWorkDaysCSV(workDays: [WorkDay]) -> URL? {
        let fileName = "CIMDriver_Werkuren_\(Date().formatted(date: .numeric, time: .omitted)).csv"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName.replacingOccurrences(of: "/", with: "-"))
        
        var csvString = "Datum,Eerste Vertrek,Aankomst,Vertrek Werk,Laatste Aankomst,Pauze (min),Totaal Uur\n"
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        
        let timeFormatter = DateFormatter()
        timeFormatter.timeStyle = .short
        
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
            
            let totalStr = String(format: "%.1f", totalHours)
            
            let row = "\(dateStr),\(firstDep),\(arr),\(dep),\(lastArr),\(wd.breakMinutes),\(totalStr)\n"
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
    
    // MARK: - PDF Export
    
    static func generateTripsPDF(trips: [Trip]) -> URL? {
        let fileName = "CIMDriver_Ritten_\(Date().formatted(date: .numeric, time: .omitted)).pdf"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName.replacingOccurrences(of: "/", with: "-"))
        
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
                .font: UIFont.boldSystemFont(ofSize: 24)
            ]
            let title = "Ritten Registratie"
            title.draw(at: CGPoint(x: 20, y: 20), withAttributes: titleAttributes)
            
            let dateAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .foregroundColor: UIColor.gray
            ]
            let dateStr = "Gegenereerd op: \(Date().formatted(date: .abbreviated, time: .shortened))"
            dateStr.draw(at: CGPoint(x: 20, y: 55), withAttributes: dateAttributes)
            
            // Draw table header
            var currentY = 100.0
            let columns = ["Datum", "Van", "Naar", "KM", "Type"]
            let xPositions: [CGFloat] = [20, 100, 250, 400, 470]
            
            let headerAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 14)
            ]
            
            for (i, column) in columns.enumerated() {
                column.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: headerAttributes)
            }
            
            currentY += 25
            
            let rowAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12)
            ]
            
            let dfDate = DateFormatter()
            dfDate.dateStyle = .short
            let dfTime = DateFormatter()
            dfTime.timeStyle = .short
            
            for trip in trips.sorted(by: { $0.startTime > $1.startTime }) {
                if currentY > pageHeight - 50 {
                    context.beginPage()
                    currentY = 20
                }
                
                let dateStr = dfDate.string(from: trip.startTime)
                let startAddr = String((trip.startAddress ?? "Onbekend").prefix(20))
                let endAddr = String((trip.endAddress ?? "Onbekend").prefix(20))
                let distance = String(format: "%.1f", Double(trip.distanceMeters) / 1000.0)
                let type = trip.tripType.prefix(3).uppercased() // e.g. ZAK or PRI
                
                let rowData = [dateStr, startAddr, endAddr, distance, type]
                
                for (i, text) in rowData.enumerated() {
                    text.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: rowAttributes)
                }
                
                currentY += 20
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
    
    static func generateWorkDaysPDF(workDays: [WorkDay]) -> URL? {
        let fileName = "CIMDriver_Werkuren_\(Date().formatted(date: .numeric, time: .omitted)).pdf"
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName.replacingOccurrences(of: "/", with: "-"))
        
        let pdfMetaData = [
            kCGPDFContextCreator: "CIMDriver iOS",
            kCGPDFContextAuthor: "CIMDriver User"
        ]
        
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = pdfMetaData as [String: Any]
        
        // A4 format
        let pageWidth = 595.2
        let pageHeight = 841.8
        let pageRect = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect, format: format)
        
        let data = renderer.pdfData { (context) in
            context.beginPage()
            
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 24)
            ]
            let title = "Werkuren Registratie"
            title.draw(at: CGPoint(x: 20, y: 20), withAttributes: titleAttributes)
            
            let dateAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .foregroundColor: UIColor.gray
            ]
            let dateStr = "Gegenereerd op: \(Date().formatted(date: .abbreviated, time: .shortened))"
            dateStr.draw(at: CGPoint(x: 20, y: 55), withAttributes: dateAttributes)
            
            // Draw table header
            var currentY = 100.0
            let columns = ["Datum", "Start", "Pauze", "Eind", "Uren"]
            let xPositions: [CGFloat] = [20, 150, 250, 350, 450]
            
            let headerAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 14)
            ]
            
            for (i, column) in columns.enumerated() {
                column.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: headerAttributes)
            }
            
            currentY += 25
            
            let rowAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12)
            ]
            
            let dfDate = DateFormatter()
            dfDate.dateStyle = .short
            let dfTime = DateFormatter()
            dfTime.timeStyle = .short
            
            for wd in workDays.sorted(by: { $0.date > $1.date }) {
                if currentY > pageHeight - 50 {
                    context.beginPage()
                    currentY = 20
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
                
                let totalStr = String(format: "%.1fh", totalHours)
                
                let rowData = [dateStr, firstDep, breakStr, dep, totalStr]
                
                for (i, text) in rowData.enumerated() {
                    text.draw(at: CGPoint(x: xPositions[i], y: currentY), withAttributes: rowAttributes)
                }
                
                currentY += 20
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
}
