package com.cimdriver.app.util

enum class TripStatus(val value: String) {
    ACTIVE("ACTIVE"),
    DONE("DONE"),
    MERGED("MERGED"),
    TO_REVIEW("TO_REVIEW");

    companion object {
        fun from(value: String): TripStatus {
            return entries.find { it.value == value } ?: DONE
        }
    }
}
