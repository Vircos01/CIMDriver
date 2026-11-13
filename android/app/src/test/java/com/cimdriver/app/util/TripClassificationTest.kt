package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.ClassificationRule
import com.cimdriver.app.data.local.entity.FavoriteRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun confidenceAssessmentHighForCommute() {
        val assessment = TripClassification.assessConfidence(
            tripType = null,
            startAddressType = "THUIS",
            endAddressType = "WERK",
            isBluetoothTriggered = true
        )
        assertEquals(TripCategory.COMMUTE, assessment.category)
        assert(assessment.confidenceScore >= 90)
        assert(assessment.isHighConfidence)
    }

    @Test
    fun confidenceAssessmentForExactRule() {
        val rule = ClassificationRule(
            name = "Vaste route",
            startAddress = "Kerkstraat 1",
            endAddress = "Dorpsplein 5",
            category = "BUSINESS"
        )
        val assessment = TripClassification.assessConfidence(
            tripType = null,
            startAddress = "Kerkstraat 1",
            endAddress = "Dorpsplein 5",
            rules = listOf(rule)
        )
        assertEquals(TripCategory.BUSINESS, assessment.category)
        assert(assessment.confidenceScore >= 95)
    }

    @Test
    fun confidenceAssessmentForFavoriteRouteMatch() {
        val favorite = FavoriteRoute(
            id = 1L,
            name = "Klant Vestiging Noord",
            startAddress = "Hoofdstraat 10, Utrecht",
            endAddress = "Industrieweg 4, Amsterdam",
            tripType = "BUSINESS",
            projectCode = "PRJ-999"
        )
        val assessment = TripClassification.assessConfidence(
            tripType = null,
            startAddress = "Hoofdstraat 10, Utrecht",
            endAddress = "Industrieweg 4, Amsterdam",
            favoriteRoutes = listOf(favorite)
        )
        assertEquals(TripCategory.BUSINESS, assessment.category)
        assertEquals(95, assessment.confidenceScore)
        assertTrue(assessment.reasoning.contains("Klant Vestiging Noord"))
        assertEquals("PRJ-999", assessment.suggestedProjectCode)
    }

    @Test
    fun favoriteRouteReverseMatchWorks() {
        val favorite = FavoriteRoute(
            id = 2L,
            name = "Woon-Werk Route",
            startAddress = "Thuisadres 1, Almere",
            endAddress = "Kantoor CIM, Vianen",
            tripType = "COMMUTE"
        )
        val assessment = TripClassification.assessConfidence(
            tripType = null,
            startAddress = "Kantoor CIM, Vianen",
            endAddress = "Thuisadres 1, Almere",
            favoriteRoutes = listOf(favorite)
        )
        assertEquals(TripCategory.COMMUTE, assessment.category)
        assertEquals(95, assessment.confidenceScore)
        assertTrue(assessment.reasoning.contains("Woon-Werk Route"))
    }
}
