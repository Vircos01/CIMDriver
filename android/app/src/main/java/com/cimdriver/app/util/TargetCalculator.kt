package com.cimdriver.app.util

import java.util.Calendar
import java.util.TimeZone

fun employmentTargetFactor(employmentStartDate: Long?, targetYear: Int): Double {
    if (employmentStartDate == null) return 1.0

    val employmentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = employmentStartDate
    }
    val employmentYear = employmentDate.get(Calendar.YEAR)

    return when {
        employmentYear < targetYear -> 1.0
        employmentYear > targetYear -> 0.0
        else -> (12 - employmentDate.get(Calendar.MONTH)) / 12.0
    }
}