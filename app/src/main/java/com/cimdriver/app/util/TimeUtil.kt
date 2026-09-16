package com.cimdriver.app.util

import java.util.Calendar

object TimeUtil {
    fun roundToNearestQuarterHour(timeInMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        
        val minute = cal.get(Calendar.MINUTE)
        
        when {
            minute < 8 -> {
                cal.set(Calendar.MINUTE, 0)
            }
            minute < 23 -> {
                cal.set(Calendar.MINUTE, 15)
            }
            minute < 38 -> {
                cal.set(Calendar.MINUTE, 30)
            }
            minute < 53 -> {
                cal.set(Calendar.MINUTE, 45)
            }
            else -> {
                cal.set(Calendar.MINUTE, 0)
                cal.add(Calendar.HOUR_OF_DAY, 1)
            }
        }
        
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        return cal.timeInMillis
    }
}
