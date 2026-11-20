package com.cimdriver.app.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Model representing a trip extracted from the Leapmotor app (e.g. week overview).
 */
data class LeapmotorTelematicsTrip(
    val dateText: String,             // e.g. "2026/11/19"
    val startTimeText: String,        // e.g. "18:58"
    val endTimeText: String,          // e.g. "19:18"
    val startTimestamp: Long,         // epoch millis
    val endTimestamp: Long,           // epoch millis
    val durationMinutes: Int,         // duration in minutes (e.g. 20)
    val distanceKm: Double? = null,   // if expanded, e.g. 2.0
    val maxSpeedKmh: Int? = null,     // if expanded, e.g. 48
    val energyKwh: Double? = null     // if expanded, e.g. 0.1
)

/**
 * Result of parsing a Leapmotor app screenshot.
 */
data class LeapmotorScreenshotParseResult(
    val trips: List<LeapmotorTelematicsTrip>,
    val detectedOdometerKm: Int? = null,
    val rawText: String = ""
)

object LeapmotorParser {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val combinedTripRegex = Regex(
        """(\d{4}[/-]\d{1,2}[/-]\d{1,2})\s+(\d{1,2}[:.]\d{2})\s*[-–—~]\s*(\d{1,2}[:.]\d{2})"""
    )
    private val standaloneDateRegex = Regex("""^(\d{4}[/-]\d{1,2}[/-]\d{1,2})$""")
    private val standaloneTimeRangeRegex = Regex("""(\d{1,2}[:.]\d{2})\s*[-–—~]\s*(\d{1,2}[:.]\d{2})""")

    /**
     * Parses a screenshot URI from local storage using on-device ML Kit OCR.
     */
    suspend fun parseFromImageUri(context: Context, uri: Uri): LeapmotorScreenshotParseResult {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            parseFromText(visionText.text)
        } catch (e: Exception) {
            LeapmotorScreenshotParseResult(
                trips = emptyList(),
                rawText = "Fout bij verwerken van afbeelding: ${e.message}"
            )
        }
    }

    /**
     * Parses plain text (either from ML Kit or pasted by user).
     */
    fun parseFromText(text: String): LeapmotorScreenshotParseResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return LeapmotorScreenshotParseResult(emptyList(), null, text)
        }

        // 1. Detect top odometer reading (e.g. "308 km" in header before "Ritgegevens")
        val detectedOdometerKm = detectTopOdometer(lines)

        // 2. Detect trip intervals
        data class RawTrip(
            val dateStr: String,
            val startStr: String,
            val endStr: String,
            val lineIndex: Int
        )

        val rawTrips = mutableListOf<RawTrip>()
        var activeDate: String? = null

        for (i in lines.indices) {
            val line = lines[i]

            // Check if line contains both date and time range
            val matchComb = combinedTripRegex.find(line)
            if (matchComb != null) {
                val (d, st, et) = matchComb.destructured
                rawTrips.add(RawTrip(d, st.replace('.', ':'), et.replace('.', ':'), i))
                activeDate = d
                continue
            }

            // Check standalone date
            val matchDate = standaloneDateRegex.find(line)
            if (matchDate != null) {
                activeDate = matchDate.groupValues[1]
                continue
            }

            // Check standalone time range with active date
            val matchRange = standaloneTimeRangeRegex.find(line)
            if (matchRange != null && activeDate != null) {
                val (st, et) = matchRange.destructured
                rawTrips.add(RawTrip(activeDate, st.replace('.', ':'), et.replace('.', ':'), i))
            }
        }

        // 3. Scan for expanded details following any trip (e.g. distance, max speed, energy)
        val telematicsTrips = mutableListOf<LeapmotorTelematicsTrip>()

        for (raw in rawTrips) {
            var distanceKm: Double? = null
            var maxSpeedKmh: Int? = null
            var energyKwh: Double? = null

            // Scan up to 8 lines following the trip line
            val endScan = (raw.lineIndex + 8).coerceAtMost(lines.size - 1)
            for (j in (raw.lineIndex + 1)..endScan) {
                val fLine = lines[j]
                // Stop if another trip starts
                if (combinedTripRegex.containsMatchIn(fLine) || standaloneTimeRangeRegex.containsMatchIn(fLine)) {
                    break
                }

                // Afgelegde afstand (e.g. "Afgelegde afstand 2 km" or line j+1 has "2 km")
                if (fLine.contains("afstand", ignoreCase = true)) {
                    val mDist = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE).find(fLine)
                    if (mDist != null) {
                        distanceKm = mDist.groupValues[1].replace(',', '.').toDoubleOrNull()
                    } else if (j + 1 < lines.size) {
                        val nextLine = lines[j + 1]
                        val mDistNext = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE).find(nextLine)
                        if (mDistNext != null) {
                            distanceKm = mDistNext.groupValues[1].replace(',', '.').toDoubleOrNull()
                        }
                    }
                }

                // Maximale snelheid (e.g. "Maximale snelheid 48 km/h")
                if (fLine.contains("snelheid", ignoreCase = true)) {
                    val mSpeed = Regex("""(\d+)\s*km/h""", RegexOption.IGNORE_CASE).find(fLine)
                    if (mSpeed != null) {
                        maxSpeedKmh = mSpeed.groupValues[1].toIntOrNull()
                    } else if (j + 1 < lines.size) {
                        val nextLine = lines[j + 1]
                        val mSpeedNext = Regex("""(\d+)\s*km/h""", RegexOption.IGNORE_CASE).find(nextLine)
                        if (mSpeedNext != null) {
                            maxSpeedKmh = mSpeedNext.groupValues[1].toIntOrNull()
                        }
                    }
                }

                // Energieverbruik (e.g. "Totale energieverbruik 0.1 kWh")
                if (fLine.contains("energieverbruik", ignoreCase = true) || fLine.contains("verbruik", ignoreCase = true)) {
                    val mKwh = Regex("""(\d+(?:[.,]\d+)?)\s*kWh""", RegexOption.IGNORE_CASE).find(fLine)
                    if (mKwh != null) {
                        energyKwh = mKwh.groupValues[1].replace(',', '.').toDoubleOrNull()
                    } else if (j + 1 < lines.size) {
                        val nextLine = lines[j + 1]
                        val mKwhNext = Regex("""(\d+(?:[.,]\d+)?)\s*kWh""", RegexOption.IGNORE_CASE).find(nextLine)
                        if (mKwhNext != null) {
                            energyKwh = mKwhNext.groupValues[1].replace(',', '.').toDoubleOrNull()
                        }
                    }
                }
            }

            val (startMs, endMs, durationMin) = parseTimestamps(raw.dateStr, raw.startStr, raw.endStr)
            telematicsTrips.add(
                LeapmotorTelematicsTrip(
                    dateText = raw.dateStr,
                    startTimeText = raw.startStr,
                    endTimeText = raw.endStr,
                    startTimestamp = startMs,
                    endTimestamp = endMs,
                    durationMinutes = durationMin,
                    distanceKm = distanceKm,
                    maxSpeedKmh = maxSpeedKmh,
                    energyKwh = energyKwh
                )
            )
        }

        return LeapmotorScreenshotParseResult(
            trips = telematicsTrips,
            detectedOdometerKm = detectedOdometerKm,
            rawText = text
        )
    }

    private fun detectTopOdometer(lines: List<String>): Int? {
        val ritgegevensIndex = lines.indexOfFirst { it.contains("Ritgegevens", ignoreCase = true) }
        val searchLimit = if (ritgegevensIndex != -1) ritgegevensIndex else lines.size.coerceAtMost(15)

        for (i in 0 until searchLimit) {
            val line = lines[i]
            if (line.contains("uitgegeven", ignoreCase = true) ||
                line.contains("gereden", ignoreCase = true) ||
                line.contains("B10", ignoreCase = true)
            ) {
                // Check this line and subsequent 2 lines for "XXX km"
                for (k in i..(i + 2).coerceAtMost(searchLimit - 1)) {
                    val m = Regex("""(\d{2,7})\s*km""", RegexOption.IGNORE_CASE).find(lines[k])
                    if (m != null) {
                        return m.groupValues[1].toIntOrNull()
                    }
                }
            }
        }

        // Fallback: search for any standalone "XXX km" before Ritgegevens
        for (i in 0 until searchLimit) {
            val line = lines[i]
            val m = Regex("""^(\d{2,7})\s*km$""", RegexOption.IGNORE_CASE).find(line)
            if (m != null) {
                val value = m.groupValues[1].toIntOrNull()
                // Avoid capturing tiny distances if mistakenly at top
                if (value != null && value >= 10) {
                    return value
                }
            }
        }
        return null
    }

    private fun parseTimestamps(dateStr: String, startStr: String, endStr: String): Triple<Long, Long, Int> {
        val dateParts = dateStr.split('/', '-').mapNotNull { it.toIntOrNull() }
        val startParts = startStr.split(':').mapNotNull { it.toIntOrNull() }
        val endParts = endStr.split(':').mapNotNull { it.toIntOrNull() }

        val year = dateParts.getOrElse(0) { 2026 }
        val month = dateParts.getOrElse(1) { 1 }
        val day = dateParts.getOrElse(2) { 1 }

        val startH = startParts.getOrElse(0) { 0 }
        val startM = startParts.getOrElse(1) { 0 }

        val endH = endParts.getOrElse(0) { 0 }
        val endM = endParts.getOrElse(1) { 0 }

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, startH)
        cal.set(Calendar.MINUTE, startM)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis

        val calEnd = Calendar.getInstance()
        calEnd.set(Calendar.YEAR, year)
        calEnd.set(Calendar.MONTH, month - 1)
        calEnd.set(Calendar.DAY_OF_MONTH, day)
        calEnd.set(Calendar.HOUR_OF_DAY, endH)
        calEnd.set(Calendar.MINUTE, endM)
        calEnd.set(Calendar.SECOND, 0)
        calEnd.set(Calendar.MILLISECOND, 0)

        // Handle crossing midnight
        if (calEnd.timeInMillis < startMs) {
            calEnd.add(Calendar.DAY_OF_MONTH, 1)
        }
        val endMs = calEnd.timeInMillis

        val durationMinutes = ((endMs - startMs) / 60000L).toInt().coerceAtLeast(1)
        return Triple(startMs, endMs, durationMinutes)
    }
}
