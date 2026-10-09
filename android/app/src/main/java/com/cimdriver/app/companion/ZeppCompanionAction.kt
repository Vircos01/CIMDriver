package com.cimdriver.app.companion

import kotlinx.serialization.Serializable

@Serializable
data class ZeppCompanionActionRequest(
    val action: String = "",
    val tripType: String = "BUSINESS",
    val note: String = ""
)

@Serializable
data class ZeppCompanionActionResponse(
    val ok: Boolean = true,
    val message: String = "OK"
)
