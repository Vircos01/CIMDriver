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
        rules: List<ClassificationRule>
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
            trip.tripType,
            addressType(trip.startAddress),
            addressType(trip.endAddress),
            rules = rules
        )
    }

    fun exportToCsv(
        context: Context,
        uri: Uri,
        trips: List<Trip>,
        savedAddresses: List<SavedAddress> = emptyList(),
        rules: List<ClassificationRule> = emptyList()
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: return
        val writer = OutputStreamWriter(outputStream)
        
        val dateFormat = SimpleDateFormat("d-M-yyyy", java.util.Locale.forLanguageTag("nl-NL"))
        
        // Write CSV Header
        writer.append("date;departure;arrival;kilometers;businesspurpose;purposeexplanation;deviatingexplanation\n")
        
        trips.forEach { trip ->
            val dateStr = dateFormat.format(Date(trip.startTime))
            val distanceKm = Math.round(trip.distanceMeters / 1000.0).toString()
            
            // Escape semicolons in strings
            val startAddr = trip.startAddress?.replace(";", ",") ?: ""
            val endAddr = trip.endAddress?.replace(";", ",") ?: ""
            
            val deviatingExplanation = trip.note?.replace(";", ",")?.replace("\n", " ") ?: ""
            
            val businessPurpose = trip.projectCode?.replace(";", ",") ?: trip.tripType.replace(" ", "_")
            
            writer.append("$dateStr;$startAddr;$endAddr;$distanceKm;$businessPurpose;;$deviatingExplanation\n")
        }
        
        writer.flush()
        writer.close()
    }

    fun exportToPdf(
        context: Context,
        uri: Uri,
        trips: List<Trip>,
        savedAddresses: List<SavedAddress> = emptyList(),
        rules: List<ClassificationRule> = emptyList()
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
        canvas.drawText("CIMDriver Rittenregistratie", 50f, yPosition, titlePaint)
        yPosition += 40f
        
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.forLanguageTag("nl-NL"))
        
        // Draw header
        canvas.drawText(context.getString(R.string.date), 50f, yPosition, paint)
        canvas.drawText(context.getString(R.string.from), 150f, yPosition, paint)
        canvas.drawText(context.getString(R.string.to), 300f, yPosition, paint)
        canvas.drawText(context.getString(R.string.km), 450f, yPosition, paint)
        canvas.drawText(context.getString(R.string.type), 500f, yPosition, paint)
        yPosition += 20f
        
        canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
        yPosition += 20f
        
        trips.forEach { trip ->
            if (yPosition > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
            
            val dateStr = dateFormat.format(Date(trip.startTime))
            val startAddr = trip.startAddress?.take(20) ?: ""
            val endAddr = trip.endAddress?.take(20) ?: ""
            val distanceKm = String.format(Locale.getDefault(), "%.1f", trip.distanceMeters / 1000.0)
            val typeStr = when (classifyForExport(trip, savedAddresses, rules)) {
                TripCategory.BUSINESS -> "Z"
                TripCategory.PRIVATE -> "P"
                TripCategory.COMMUTE -> "W"
                null -> ""
            }
            
            canvas.drawText(dateStr, 50f, yPosition, paint)
            canvas.drawText(startAddr, 150f, yPosition, paint)
            canvas.drawText(endAddr, 300f, yPosition, paint)
            canvas.drawText(distanceKm, 450f, yPosition, paint)
            canvas.drawText(typeStr, 500f, yPosition, paint)
            
            yPosition += 20f
        }
        
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

    fun exportWorkHoursToPdf(context: Context, uri: Uri, workDays: List<com.cimdriver.app.data.local.entity.WorkDay>) {
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
        canvas.drawText("CIMDriver Werkurenregistratie", 50f, yPosition, titlePaint)
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
            if (yPosition > 800f) {
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
            val totalMillis = effectiveEnd - effectiveStart - (workDay.breakMinutes * 60000L)
            val totalHours = String.format(Locale.getDefault(), "%.1f", totalMillis / 3600000f)
            
            canvas.drawText(dateStr, 50f, yPosition, paint)
            canvas.drawText(arrWork, 150f, yPosition, paint)
            canvas.drawText(depWork, 250f, yPosition, paint)
            canvas.drawText("${workDay.breakMinutes}m", 350f, yPosition, paint)
            canvas.drawText(totalHours, 450f, yPosition, paint)
            
            yPosition += 20f
        }
        
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
