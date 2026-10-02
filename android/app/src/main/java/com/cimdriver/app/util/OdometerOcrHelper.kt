package com.cimdriver.app.util

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Extracts the most likely odometer reading from a photo using ML Kit OCR.
 * Returns the best candidate (largest plausible km value found), or null if none found.
 */
object OdometerOcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Analyse a photo URI and return the most likely odometer value as Int.
     * Looks for numeric sequences between 4 and 7 digits that look like km readings.
     */
    suspend fun readOdometerFromUri(uri: Uri, context: android.content.Context): Int? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()

            val candidates = mutableListOf<Int>()

            for (block in result.textBlocks) {
                for (line in block.lines) {
                    // Strip spaces, dots, commas to normalise "123.456" or "123 456"
                    val raw = line.text
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", "")
                        .replace("O", "0") // common OCR confusion
                        .replace("o", "0")
                        .replace("I", "1")
                        .replace("l", "1")

                    // Look for standalone numeric sequences that are plausible km values
                    // Odometer typically: 4–7 digits
                    val matches = Regex("(\\d{4,7})").findAll(raw)
                    for (match in matches) {
                        val value = match.value.toIntOrNull() ?: continue
                        // Sanity check: must be > 0 and < 2_000_000 km
                        if (value in 1..1_999_999) {
                            candidates.add(value)
                        }
                    }
                }
            }

            // Prefer the largest value (odometers tend to be the biggest number on the display)
            candidates.maxOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
