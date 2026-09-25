package com.cimdriver.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object WorkHoursNormalizer {
    const val DEFAULT_TOLERANCE_MINUTES = 90L

    fun normalize(
        actualTime: Long,
        dayStart: Long,
        configuredTime: String,
        toleranceMinutes: Long = DEFAULT_TOLERANCE_MINUTES
    ): Long {
        val configuredTimestamp = configuredTimestamp(dayStart, configuredTime) ?: return actualTime
        val differenceMinutes = abs(actualTime - configuredTimestamp) / 60_000L
        return if (differenceMinutes <= toleranceMinutes) configuredTimestamp else actualTime
    }

    private fun configuredTimestamp(dayStart: Long, configuredTime: String): Long? {
        return try {
            val normalizedTime = configuredTime.replace(".", ":")
            val format = SimpleDateFormat("HH:mm", Locale.ROOT).apply { isLenient = false }
            val parsed = format.parse(normalizedTime) ?: return null
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = dayStart
                val time = java.util.Calendar.getInstance().apply { time = parsed }
                set(java.util.Calendar.HOUR_OF_DAY, time.get(java.util.Calendar.HOUR_OF_DAY))
                set(java.util.Calendar.MINUTE, time.get(java.util.Calendar.MINUTE))
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        } catch (_: Exception) {
            null
        }
    }
}
