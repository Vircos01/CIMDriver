package com.cimdriver.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FuelPromptStore {
    private val _pendingFillUpId = MutableStateFlow<Long?>(null)
    val pendingFillUpId: StateFlow<Long?> = _pendingFillUpId.asStateFlow()

    fun request(fillUpId: Long) {
        _pendingFillUpId.value = fillUpId
    }

    fun consume() {
        _pendingFillUpId.value = null
    }
}

