package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.ClassificationRule

enum class TripCategory {
    BUSINESS,
    PRIVATE,
    COMMUTE
}

object TripClassification {
    const val DYNAMICS_HOME_TO_WORK = "Home To Work"
    const val DYNAMICS_BUSINESS_MEETING = "Business Meeting"
    const val DYNAMICS_PERSONAL = "PERSONAL"

    private val businessTripTypes = setOf(
        "BUSINESS",
        "Business Meeting",
        "Customer Visit",
        "Customer Billable",
        "Commissioned By CIMSOLUTIONS",
        "Exam Course",
        "Car Maintenance"
    )

    fun classify(
        tripType: String?,
        startAddressType: String? = null,
        endAddressType: String? = null,
        defaultCategory: TripCategory = TripCategory.PRIVATE,
        homeWorkAsCommute: Boolean = true,
        customerAsBusiness: Boolean = true,
        rules: List<ClassificationRule> = emptyList()
    ): TripCategory? {
        val isCommuteAddressPair = (startAddressType == "THUIS" && endAddressType == "WERK") ||
            (startAddressType == "WERK" && endAddressType == "THUIS")

        val matchingRule = findMatchingRule(tripType, startAddressType, endAddressType, rules)
        if (matchingRule != null) {
            return when (matchingRule.category) {
                "BUSINESS" -> TripCategory.BUSINESS
                "COMMUTE" -> TripCategory.COMMUTE
                "PRIVATE" -> TripCategory.PRIVATE
                else -> defaultCategory
            }
        }
        // If rules exist but none matched, fall through to built-in classification logic
        // so that smart defaults (e.g. KLANT→THUIS = BUSINESS) still apply.

        return when {
            homeWorkAsCommute && (isCommuteAddressPair || tripType == "COMMUTE" || tripType == DYNAMICS_HOME_TO_WORK) -> TripCategory.COMMUTE
            customerAsBusiness && (startAddressType == "KLANT" || endAddressType == "KLANT") -> TripCategory.BUSINESS
            tripType in businessTripTypes -> TripCategory.BUSINESS
            tripType == "PRIVATE" || tripType == DYNAMICS_PERSONAL -> TripCategory.PRIVATE
            else -> defaultCategory
        }
    }

    fun resolveTripType(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        rules: List<ClassificationRule>
    ): String? = findMatchingRule(tripType, startAddressType, endAddressType, rules)?.tripType ?: tripType

    private fun findMatchingRule(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        rules: List<ClassificationRule>
    ): ClassificationRule? = rules.filter { rule ->
        val tripMatches = rule.tripType.isNullOrBlank() || rule.tripType.equals(tripType, ignoreCase = true)
        
        val directionForward = (rule.startAddressType.isNullOrBlank() || rule.startAddressType.equals(startAddressType, ignoreCase = true)) &&
            (rule.endAddressType.isNullOrBlank() || rule.endAddressType.equals(endAddressType, ignoreCase = true))
            
        val directionReverse = (rule.startAddressType.isNullOrBlank() || rule.startAddressType.equals(endAddressType, ignoreCase = true)) &&
            (rule.endAddressType.isNullOrBlank() || rule.endAddressType.equals(startAddressType, ignoreCase = true))
            
        tripMatches && (directionForward || directionReverse)
    }.maxByOrNull { rule ->
        listOf(rule.startAddressType, rule.endAddressType, rule.tripType).count { !it.isNullOrBlank() }
    }
}
