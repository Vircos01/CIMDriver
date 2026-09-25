package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.ClassificationRule
import org.junit.Assert.assertEquals
import org.junit.Test

class TripClassificationTest {
    @Test
    fun homeToWorkAddressPairWinsOverBusinessTripType() {
        assertEquals(
            TripCategory.COMMUTE,
            TripClassification.classify("Business Meeting", "THUIS", "WERK")
        )
    }

    @Test
    fun customerAddressIsBusiness() {
        assertEquals(
            TripCategory.BUSINESS,
            TripClassification.classify("PERSONAL", "THUIS", "KLANT")
        )
    }

    @Test
    fun personalTripIsPrivate() {
        assertEquals(TripCategory.PRIVATE, TripClassification.classify(TripClassification.DYNAMICS_PERSONAL))
    }

    @Test
    fun customRuleOverridesBuiltInClassification() {
        val rule = ClassificationRule(
            name = "Klant naar kantoor",
            startAddressType = "KLANT",
            endAddressType = "KANTOOR",
            tripType = "Business Meeting",
            category = "PRIVATE"
        )

        assertEquals(
            TripCategory.PRIVATE,
            TripClassification.classify("Business Meeting", "KLANT", "KANTOOR", rules = listOf(rule))
        )
    }
}
