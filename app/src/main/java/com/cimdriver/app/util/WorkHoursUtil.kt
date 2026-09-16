package com.cimdriver.app.util

import java.util.Locale

data class DayWorkHours(
    val dayOfWeek: Int, // 1=Mon, 2=Tue, ..., 7=Sun
    val isEnabled: Boolean,
    val startTime: String,
    val endTime: String
)

object WorkHoursUtil {

    // Parses string like "1=08:00-18:00;2=08:00-18:00" or backwards compatible "1,2,3,4,5"
    fun parseWorkHoursString(
        input: String,
        defaultStart: String = "08:00",
        defaultEnd: String = "18:00"
    ): List<DayWorkHours> {
        val result = mutableListOf<DayWorkHours>()
        
        if (input.contains("=")) {
            // New format e.g. "1=08:00-18:00;2=08:00-18:00"
            val daysMap = mutableMapOf<Int, Pair<String, String>>()
            input.split(";").filter { it.isNotBlank() }.forEach { part ->
                val eqIndex = part.indexOf('=')
                if (eqIndex > 0) {
                    val day = part.substring(0, eqIndex).toIntOrNull() ?: return@forEach
                    val times = part.substring(eqIndex + 1)
                    val dashIndex = times.indexOf('-', times.indexOf(':') + 1) // find dash after first time
                    if (dashIndex > 0) {
                        val start = times.substring(0, dashIndex)
                        val end = times.substring(dashIndex + 1)
                        daysMap[day] = Pair(start, end)
                    }
                }
            }
            
            for (i in 1..7) {
                if (daysMap.containsKey(i)) {
                    val times = daysMap[i]!!
                    result.add(DayWorkHours(i, true, times.first, times.second))
                } else {
                    result.add(DayWorkHours(i, false, defaultStart, defaultEnd))
                }
            }
        } else {
            // Old format, e.g. "1,2,3,4,5" or ""
            val enabledDays = input.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            for (i in 1..7) {
                result.add(DayWorkHours(i, enabledDays.contains(i), defaultStart, defaultEnd))
            }
        }
        
        return result
    }

    fun serializeWorkHours(days: List<DayWorkHours>): String {
        return days.filter { it.isEnabled }
            .joinToString(";") { "${it.dayOfWeek}=${it.startTime}-${it.endTime}" }
    }

    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Maandag"
            2 -> "Dinsdag"
            3 -> "Woensdag"
            4 -> "Donderdag"
            5 -> "Vrijdag"
            6 -> "Zaterdag"
            7 -> "Zondag"
            else -> "Onbekend"
        }
    }
}

