package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MonthlyClosingRequest(
    val openExport: Boolean = false,
    val title: String = ""
)

object MonthlyClosingStore {
    private val _request = MutableStateFlow(MonthlyClosingRequest())
    val request: StateFlow<MonthlyClosingRequest> = _request.asStateFlow()

    fun requestClosing(title: String) {
        _request.value = MonthlyClosingRequest(openExport = true, title = title)
    }

    fun dismiss() {
        _request.value = MonthlyClosingRequest()
    }
}

