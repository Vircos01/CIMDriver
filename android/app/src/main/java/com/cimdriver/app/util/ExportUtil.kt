package com.cimdriver.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.cimdriver.app.data.local.entity.ClassificationRule
import com.cimdriver.app.data.local.entity.SavedAddress
import com.cimdriver.app.data.local.entity.Trip
import com.cimdriver.app.data.local.entity.Vehicle
import com.cimdriver.app.data.local.entity.OdometerCheck
import com.cimdriver.app.data.local.entity.OdometerGapResolution
import com.cimdriver.app.ui.viewmodel.DashboardStats
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R

object ExportUtil {

    fun exportSummaryCsv(
        context: Context,
        uri: Uri,
        period: String,
        stats: DashboardStats,
        workHours: Double
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        OutputStreamWriter(outputStream).use { writer ->
            writer.append("periode;zakelijk_km;woon_werk_km;prive_km;zakelijk_min;woon_werk_min;prive_min;werkuren\n")
            writer.append(
                "$period;${formatDecimal(stats.zakelijkKm)};${formatDecimal(stats.woonWerkKm)};" +
                    "${formatDecimal(stats.priveKm)};${stats.zakelijkMin};${stats.woonWerkMin};" +
                    "${stats.priveMin};${formatDecimal(workHours)}\n"
            )
        }
    }

    fun exportSummaryPdf(
        context: Context,
        uri: Uri,
        period: String,
        stats: DashboardStats,
        workHours: Double
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 20f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 13f }

        canvas.drawText("CIMDriver Periode-rapport", 50f, 60f, titlePaint)
        canvas.drawText(period, 50f, 90f, bodyPaint)
        canvas.drawText("Zakelijk: ${formatDecimal(stats.zakelijkKm)} km", 50f, 145f, bodyPaint)
        canvas.drawText("Woon-werk: ${formatDecimal(stats.woonWerkKm)} km", 50f, 175f, bodyPaint)
        canvas.drawText("Privé: ${formatDecimal(stats.priveKm)} km", 50f, 205f, bodyPaint)
        canvas.drawText("Werkuren: ${formatDecimal(workHours)} uur", 50f, 255f, bodyPaint)
        canvas.drawText("Zakelijke tijd: ${stats.zakelijkMin} minuten", 50f, 285f, bodyPaint)
        canvas.drawText("Woon-werktijd: ${stats.woonWerkMin} minuten", 50f, 315f, bodyPaint)
        canvas.drawText("Privétijd: ${stats.priveMin} minuten", 50f, 345f, bodyPaint)

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()
    }

    private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun classifyForExport(
        trip: Trip,
        savedAddresses: List<SavedAddress>,
        rules: List<ClassificationRule>,
        settings: com.cimdriver.app.data.local.entity.Settings? = null
    ): TripCategory? {
        fun addressType(address: String?): String? {
            val saved = AddressMatching.findSavedAddress(address, savedAddresses) ?: return null
            return saved.addressType ?: when {
                saved.isHomeLocation -> "THUIS"
                saved.isWorkLocation -> "WERK"
                saved.isCustomerLocation -> "KLANT"
                else -> null
            }
        }
        return TripClassification.classify(
            tripType = trip.tripType,
            startAddressType = addressType(trip.startAddress),
            endAddressType = addressType(trip.endAddress),
            rules = rules,
            defaultCategory = if (settings?.classificationDefault == "BUSINESS") TripCategory.BUSINESS else TripCategory.PRIVATE,
            homeWorkAsCommute = settings?.classifyHomeWorkAsCommute ?: true,
            customerAsBusiness = settings?.classifyCustomerAsBusiness ?: true,
            timestamp = trip.startTime,
            workDaysStr = settings?.workDays,
            workStartTime = settings?.workStartTime,
            workEndTime = settings?.workEndTime
        )
    }

    fun exportToCsv(
        context: Context,
        uri: Uri,
        trips: List<Trip>,
        savedAddresses: List<SavedAddress> = emptyList(),
        rules: List<ClassificationRule> = emptyList(),
        settings: com.cimdriver.app.data.local.entity.Settings? = null,
        vehicles: List<Vehicle> = emptyList()
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val writer = OutputStreamWriter(outputStream)
        
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", java.util.Locale.forLanguageTag("nl-NL"))
        val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
        
        // Write CSV Header - Belastingdienst compliant
        writer.append("datum;vertrektijd;aankomsttijd;kenteken;voertuig;teller_start;teller_eind;afstand_km;prive_omrij_km;karakter;vertrekadres;aankomstadres;ritdoel_project;route_toelichting\n")
        
        trips.forEach { trip ->
            val dateStr = dateFormat.format(Date(trip.startTime))
            val startTimeStr = timeFormat.format(Date(trip.startTime))
            val endTimeStr = trip.endTime?.let { timeFormat.format(Date(it)) } ?: ""
            val vehicle = vehicles.find { it.id == trip.vehicleId }
            val plate = vehicle?.licensePlate ?: ""
            val vehicleName = vehicle?.let { "${it.make} ${it.model}".trim() } ?: ""
            
            val odoStart = trip.odometerStart
            val odoEnd = trip.odometerEnd ?: (trip.odometerStart + Math.round(trip.distanceMeters / 1000.0).toInt())
            val distanceKm = String.format(Locale.US, "%.1f", trip.distanceMeters / 1000.0)
            val detourKm = if (trip.privateDetourMeters > 0) String.format(Locale.US, "%.1f", trip.privateDetourMeters / 1000.0) else "0.0"
            
            val category = classifyForExport(trip, savedAddresses, rules, settings)
            val typeStr = when (category) {
                TripCategory.BUSINESS -> "Zakelijk"
                TripCategory.COMMUTE -> "Woon-werk"
                TripCategory.PRIVATE -> "Privé"
                null -> trip.tripType
            }
            
            val startAddr = trip.startAddress?.replace(";", ",")?.replace("\n", " ") ?: ""
            val endAddr = trip.endAddress?.replace(";", ",")?.replace("\n", " ") ?: ""
            val purpose = trip.projectCode?.replace(";", ",") ?: trip.tripType.replace(";", ",")
            val deviatingExplanation = trip.note?.replace(";", ",")?.replace("\n", " ") ?: ""
            
            writer.append("$dateStr;$startTimeStr;$endTimeStr;$plate;$vehicleName;$odoStart;$odoEnd;$distanceKm;$detourKm;$typeStr;$startAddr;$endAddr;$purpose;$deviatingExplanation\n")
        }
        
        writer.flush()
        writer.close()
    }

    fun exportToPdf(
        context: Context,
        uri: Uri,
        trips: List<Trip>,
        savedAddresses: List<SavedAddress> = emptyList(),
        rules: List<ClassificationRule> = emptyList(),
        settings: com.cimdriver.app.data.local.entity.Settings? = null,
        title: String? = null,
        vehicles: List<Vehicle> = emptyList()
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // Landscape A4
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isFakeBoldText = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        var yPosition = 35f
        val docTitle = if (!title.isNullOrBlank()) "CIMDriver Rittenregistratie - $title" else "CIMDriver Rittenregistratie"
        canvas.drawText(docTitle, 35f, yPosition, titlePaint)
        yPosition += 16f
        canvas.drawText("Sluitende rittenadministratie conform richtlijnen Belastingdienst Nederland", 35f, yPosition, metaPaint)
        yPosition += 18f

        // Document header met voertuig- en periodedetails
        val defaultVehicle = trips.firstNotNullOfOrNull { t -> vehicles.find { it.id == t.vehicleId } } 
            ?: vehicles.find { it.isDefault } ?: vehicles.firstOrNull()
        val vehicleStr = defaultVehicle?.let { "${it.make} ${it.model} (${it.licensePlate})" } ?: "Alle voertuigen"
        val periodStr = if (!title.isNullOrBlank()) title else "Volledig overzicht"
        canvas.drawText("Voertuig: $vehicleStr   |   Periode: $periodStr   |   Aantal ritten: ${trips.size}", 35f, yPosition, metaPaint)
        yPosition += 18f
        
        canvas.drawLine(35f, yPosition, 807f, yPosition, linePaint)
        yPosition += 14f

        fun drawTableHeader(c: Canvas, y: Float) {
            c.drawText("Datum & Tijd", 35f, y, headerPaint)
            c.drawText("Kenteken", 120f, y, headerPaint)
            c.drawText("Start km", 185f, y, headerPaint)
            c.drawText("Eind km", 240f, y, headerPaint)
            c.drawText("Afstand", 295f, y, headerPaint)
            c.drawText("Omrij", 345f, y, headerPaint)
            c.drawText("Type", 390f, y, headerPaint)
            c.drawText("Vertrekadres", 420f, y, headerPaint)
            c.drawText("Aankomstadres", 570f, y, headerPaint)
            c.drawText("Doel / Notitie", 720f, y, headerPaint)
        }

        drawTableHeader(canvas, yPosition)
        yPosition += 6f
        canvas.drawLine(35f, yPosition, 807f, yPosition, linePaint)
        yPosition += 14f
        
        val dateTimeFormat = SimpleDateFormat("dd-MM-yy HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
        var totalDistMeters = 0L
        var totalZakelijkMeters = 0L
        var totalWoonWerkMeters = 0L
        var totalPriveMeters = 0L
        var totalDetourMeters = 0L

        trips.forEach { trip ->
            if (yPosition > 520f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 35f
                drawTableHeader(canvas, yPosition)
                yPosition += 6f
                canvas.drawLine(35f, yPosition, 807f, yPosition, linePaint)
                yPosition += 14f
            }
            
            val dateStr = dateTimeFormat.format(Date(trip.startTime))
            val vehicle = vehicles.find { it.id == trip.vehicleId }
            val plateStr = (vehicle?.licensePlate ?: "").take(9)
            val odoStartStr = String.format(Locale.US, "%,d", trip.odometerStart).replace(',', '.')
            val odoEndVal = trip.odometerEnd ?: (trip.odometerStart + Math.round(trip.distanceMeters / 1000.0).toInt())
            val odoEndStr = String.format(Locale.US, "%,d", odoEndVal).replace(',', '.')
            val distStr = String.format(Locale.getDefault(), "%.1f km", trip.distanceMeters / 1000.0)
            val detourStr = if (trip.privateDetourMeters > 0) String.format(Locale.getDefault(), "%.1f km", trip.privateDetourMeters / 1000.0) else "-"
            
            val category = classifyForExport(trip, savedAddresses, rules, settings)
            val typeStr = when (category) {
                TripCategory.BUSINESS -> "Zakelijk"
                TripCategory.PRIVATE -> "Privé"
                TripCategory.COMMUTE -> "Woon-werk"
                null -> ""
            }

            totalDistMeters += trip.distanceMeters
            totalDetourMeters += trip.privateDetourMeters
            when (category) {
                TripCategory.BUSINESS -> totalZakelijkMeters += trip.distanceMeters
                TripCategory.COMMUTE -> totalWoonWerkMeters += trip.distanceMeters
                TripCategory.PRIVATE -> totalPriveMeters += trip.distanceMeters
                null -> {}
            }
            
            val startAddr = (trip.startAddress ?: "").take(25)
            val endAddr = (trip.endAddress ?: "").take(25)
            val noteOrPurpose = (trip.projectCode ?: trip.note ?: "").take(16)
            
            canvas.drawText(dateStr, 35f, yPosition, bodyPaint)
            canvas.drawText(plateStr, 120f, yPosition, bodyPaint)
            canvas.drawText(odoStartStr, 185f, yPosition, bodyPaint)
            canvas.drawText(odoEndStr, 240f, yPosition, bodyPaint)
            canvas.drawText(distStr, 295f, yPosition, bodyPaint)
            canvas.drawText(detourStr, 345f, yPosition, bodyPaint)
            canvas.drawText(typeStr, 390f, yPosition, bodyPaint)
            canvas.drawText(startAddr, 420f, yPosition, bodyPaint)
            canvas.drawText(endAddr, 570f, yPosition, bodyPaint)
            canvas.drawText(noteOrPurpose, 720f, yPosition, bodyPaint)
            
            yPosition += 15f
        }

        // Totals summary block at bottom
        if (yPosition > 470f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 35f
        }
        yPosition += 10f
        canvas.drawLine(35f, yPosition, 807f, yPosition, linePaint)
        yPosition += 16f
        val zakelijkKmStr = String.format(Locale.getDefault(), "%.1f", totalZakelijkMeters / 1000.0)
        val woonWerkKmStr = String.format(Locale.getDefault(), "%.1f", totalWoonWerkMeters / 1000.0)
        val priveKmStr = String.format(Locale.getDefault(), "%.1f", totalPriveMeters / 1000.0)
        val detourKmStr = String.format(Locale.getDefault(), "%.1f", totalDetourMeters / 1000.0)
        val totaalKmStr = String.format(Locale.getDefault(), "%.1f", totalDistMeters / 1000.0)

        canvas.drawText("TOTALEN: Totaal: $totaalKmStr km   |   Zakelijk: $zakelijkKmStr km   |   Woon-werk: $woonWerkKmStr km   |   Privé: $priveKmStr km   |   Privé-omrijden: $detourKmStr km", 35f, yPosition, headerPaint)
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
    }
    fun exportWorkHoursToCsv(context: Context, uri: Uri, workDays: List<com.cimdriver.app.data.local.entity.WorkDay>) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val writer = OutputStreamWriter(outputStream)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.forLanguageTag("nl-NL"))
        val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
        
        // Write CSV Header
        writer.append("Datum,Aankomst Werk,Vertrek Werk,Pauze (min),Netto Uren,Locatie,Status\n")
        
        workDays.forEach { workDay ->
            val dateStr = dateFormat.format(Date(workDay.date))
            val exactArr = workDay.arrivalTime
            val exactDep = workDay.departureTime ?: workDay.lastArrivalTime ?: exactArr
            
            val start = timeFormat.format(Date(workDay.roundedArrivalTime ?: exactArr))
            val end = timeFormat.format(Date(workDay.roundedDepartureTime ?: exactDep))
            
            val effectiveStart = workDay.roundedArrivalTime ?: exactArr
            val effectiveEnd = workDay.roundedDepartureTime ?: exactDep
            val totalMillis = effectiveEnd - effectiveStart - (workDay.breakMinutes * 60000L)
            val totalHours = String.format(Locale.getDefault(), "%.1f", totalMillis / 3600000f)
            
            val loc = workDay.workLocationLabel?.replace(",", " ") ?: ""
            
            writer.append("$dateStr,$start,$end,${workDay.breakMinutes},$totalHours,$loc,${workDay.status}\n")
        }
        
        writer.flush()
        writer.close()
    }

    fun exportWorkHoursToPdf(
        context: Context,
        uri: Uri,
        workDays: List<com.cimdriver.app.data.local.entity.WorkDay>,
        title: String? = null
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        
        var yPosition = 50f
        val headerTitle = if (!title.isNullOrBlank()) "CIMDriver Werkuren - $title" else "CIMDriver Werkurenregistratie"
        canvas.drawText(headerTitle, 50f, yPosition, titlePaint)
        yPosition += 40f
        
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", java.util.Locale.forLanguageTag("nl-NL"))
        val timeFormat = SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
        
        // Draw header
        canvas.drawText(context.getString(R.string.date), 50f, yPosition, paint)
        canvas.drawText(context.getString(R.string.start), 150f, yPosition, paint)
        canvas.drawText(context.getString(R.string.end), 250f, yPosition, paint)
        canvas.drawText(context.getString(R.string.break_time), 350f, yPosition, paint)
        canvas.drawText(context.getString(R.string.hours), 450f, yPosition, paint)
        yPosition += 20f
        
        canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
        yPosition += 20f
        
        workDays.forEach { workDay ->
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
            
            val exactArr = workDay.arrivalTime
            val exactDep = workDay.departureTime ?: workDay.lastArrivalTime ?: exactArr
            val arrWork = timeFormat.format(Date(workDay.roundedArrivalTime ?: exactArr))
            val depWork = timeFormat.format(Date(workDay.roundedDepartureTime ?: exactDep))
            val dateStr = dateFormat.format(Date(workDay.date))
            
            val effectiveStart = workDay.roundedArrivalTime ?: exactArr
            val effectiveEnd = workDay.roundedDepartureTime ?: exactDep
            val totalMillis = (effectiveEnd - effectiveStart - (workDay.breakMinutes * 60000L)).coerceAtLeast(0L)
            val totalHours = String.format(Locale.getDefault(), "%.1f", totalMillis / 3600000f)
            
            canvas.drawText(dateStr, 50f, yPosition, paint)
            canvas.drawText(arrWork, 150f, yPosition, paint)
            canvas.drawText(depWork, 250f, yPosition, paint)
            canvas.drawText("${workDay.breakMinutes}m", 350f, yPosition, paint)
            canvas.drawText(totalHours, 450f, yPosition, paint)
            
            yPosition += 20f
        }

        if (workDays.isNotEmpty()) {
            yPosition += 10f
            canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
            yPosition += 20f
            val totalHoursSum = workDays.sumOf { workDay ->
                val exactArr = workDay.arrivalTime
                val exactDep = workDay.departureTime ?: workDay.lastArrivalTime ?: exactArr
                val effectiveStart = workDay.roundedArrivalTime ?: exactArr
                val effectiveEnd = workDay.roundedDepartureTime ?: exactDep
                val totalMillis = (effectiveEnd - effectiveStart - (workDay.breakMinutes * 60000L)).coerceAtLeast(0L)
                totalMillis / 3600000.0
            }
            val boldPaint = Paint(paint).apply { isFakeBoldText = true }
            canvas.drawText("Totaal gewerkte uren:", 50f, yPosition, boldPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f uur", totalHoursSum), 450f, yPosition, boldPaint)
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
    }

    fun exportIntegrityReport(
        context: Context,
        uri: Uri,
        vehicle: Vehicle,
        trips: List<Trip>,
        checks: List<OdometerCheck>,
        resolutions: List<OdometerGapResolution>
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale.forLanguageTag("nl-NL"))

        var yPos = 50f

        // Document Title
        canvas.drawText("CIMDriver - Verantwoording KM-Registratie & Integriteitsrapport", 50f, yPos, titlePaint)
        yPos += 16f
        canvas.drawText("Opgesteld conform richtlijnen Belastingdienst Nederland | Sluitende Rittenadministratie", 50f, yPos, subtitlePaint)
        yPos += 20f

        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // 1. Vehicle overview box
        canvas.drawText("1. VOERTUIGGEGEVENS", 50f, yPos, headerPaint)
        yPos += 16f

        canvas.drawText("Kenteken: ${vehicle.licensePlate}", 50f, yPos, bodyPaint)
        canvas.drawText("Merk / Model: ${vehicle.make} ${vehicle.model}", 220f, yPos, bodyPaint)
        canvas.drawText("Gebruik: ${vehicle.usageType}", 400f, yPos, bodyPaint)
        yPos += 14f

        canvas.drawText("Starttellerstand: ${vehicle.odometerStart} km", 50f, yPos, bodyPaint)
        canvas.drawText("Huidige tellerstand: ${vehicle.odometerCurrent} km", 220f, yPos, bodyPaint)
        val totalRecordedKm = vehicle.odometerCurrent - vehicle.odometerStart
        canvas.drawText("Totaal geregistreerd: $totalRecordedKm km", 400f, yPos, bodyPaint)
        yPos += 22f

        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // 2. Tussentijdse controles
        canvas.drawText("2. TUSSENTIJDSE KM-CONTROLES & IJKPUNTEN (${checks.size})", 50f, yPos, headerPaint)
        yPos += 16f

        canvas.drawText("Datum", 50f, yPos, headerPaint)
        canvas.drawText("Stand", 130f, yPos, headerPaint)
        canvas.drawText("Afwijking", 190f, yPos, headerPaint)
        canvas.drawText("Correctie", 250f, yPos, headerPaint)
        canvas.drawText("Verklaring / Reden", 340f, yPos, headerPaint)
        canvas.drawText("Foto", 490f, yPos, headerPaint)
        yPos += 8f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 14f

        if (checks.isEmpty()) {
            canvas.drawText("Geen tussentijdse km-controles vastgelegd in deze periode.", 50f, yPos, bodyPaint)
            yPos += 16f
        } else {
            checks.sortedBy { it.timestamp }.forEach { check ->
                if (yPos > 790f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                val dateStr = dateOnlyFormat.format(Date(check.timestamp))
                val odoStr = "${check.correctedOdometer} km"
                val devStr = if (check.deviationKm != 0) "${if (check.deviationKm > 0) "+" else ""}${check.deviationKm} km" else "0 km"
                val stratStr = check.correctionStrategy
                val reasonStr = check.correctionReason?.take(26) ?: "-"
                val photoStr = if (check.photoPath != null) "Ja (Aanwezig)" else "Nee"

                canvas.drawText(dateStr, 50f, yPos, bodyPaint)
                canvas.drawText(odoStr, 130f, yPos, bodyPaint)
                canvas.drawText(devStr, 190f, yPos, bodyPaint)
                canvas.drawText(stratStr, 250f, yPos, bodyPaint)
                canvas.drawText(reasonStr, 340f, yPos, bodyPaint)
                canvas.drawText(photoStr, 490f, yPos, bodyPaint)
                yPos += 14f
            }
        }

        yPos += 10f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // 3. Gaten en Verklaringen
        canvas.drawText("3. VASTGELEGDE GATEN & VERANTWOORDING (${resolutions.size})", 50f, yPos, headerPaint)
        yPos += 16f

        canvas.drawText("Periode", 50f, yPos, headerPaint)
        canvas.drawText("Type", 140f, yPos, headerPaint)
        canvas.drawText("Km", 230f, yPos, headerPaint)
        canvas.drawText("Oplossing", 270f, yPos, headerPaint)
        canvas.drawText("Toelichting voor Belastingdienst", 370f, yPos, headerPaint)
        yPos += 8f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 14f

        if (resolutions.isEmpty()) {
            canvas.drawText("Geen geregistreerde gaten of afwijkingen aangetroffen.", 50f, yPos, bodyPaint)
            yPos += 16f
        } else {
            resolutions.forEach { res ->
                if (yPos > 790f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                val periodStr = dateOnlyFormat.format(Date(res.gapStartTimestamp))
                val typeStr = res.gapType.take(14)
                val kmStr = "${if (res.gapKm > 0) "+" else ""}${res.gapKm} km"
                val solStr = res.resolution.take(16)
                val noteStr = res.resolutionNote?.take(28) ?: "-"

                canvas.drawText(periodStr, 50f, yPos, bodyPaint)
                canvas.drawText(typeStr, 140f, yPos, bodyPaint)
                canvas.drawText(kmStr, 230f, yPos, bodyPaint)
                canvas.drawText(solStr, 270f, yPos, bodyPaint)
                canvas.drawText(noteStr, 370f, yPos, bodyPaint)
                yPos += 14f
            }
        }

        yPos += 20f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // 4. Verklaring van integriteit
        if (yPos > 710f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
        }

        canvas.drawText("4. VERKLARING VAN SLUITENDE REGISTRATIE", 50f, yPos, headerPaint)
        yPos += 16f

        val declarationText = "Ondergetekende verklaart dat bovenstaande kilometeradministratie een getrouwe en volledige weergave is van alle gemaakte ritten met voertuig ${vehicle.licensePlate}. Alle begin- en eindstanden sluiten aan of zijn gedocumenteerd met verifieerbare redenen conform de wettelijke vereisten van de Belastingdienst."
        val words = declarationText.split(" ")
        var currentLine = ""
        for (w in words) {
            if ((currentLine + " " + w).length > 95) {
                canvas.drawText(currentLine, 50f, yPos, bodyPaint)
                yPos += 14f
                currentLine = w
            } else {
                currentLine = if (currentLine.isEmpty()) w else "$currentLine $w"
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, 50f, yPos, bodyPaint)
            yPos += 24f
        }

        canvas.drawText("Datum: ${dateOnlyFormat.format(Date())}                           Handtekening bestuurder: _______________________", 50f, yPos, bodyPaint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
    }
    
    fun shareExportedFile(
        context: Context,
        fileName: String,
        mimeType: String,
        exportAction: (Uri) -> Unit
    ) {
        try {
            val exportDir = java.io.File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = java.io.File(exportDir, fileName)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Execute the export (will write to the URI)
            exportAction(uri)
            
            // Create intent
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(android.content.Intent.createChooser(intent, "Deel of E-mail via..."))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Fout bij voorbereiden export: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
