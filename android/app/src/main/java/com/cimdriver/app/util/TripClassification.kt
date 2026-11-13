package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.ClassificationRule
import java.util.Calendar

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
        startAddress: String? = null,
        endAddress: String? = null,
        defaultCategory: TripCategory = TripCategory.PRIVATE,
        homeWorkAsCommute: Boolean = true,
        customerAsBusiness: Boolean = true,
        rules: List<ClassificationRule> = emptyList(),
        timestamp: Long? = null,
        workDaysStr: String? = null,
        workStartTime: String? = null,
        workEndTime: String? = null,
        favoriteRoutes: List<com.cimdriver.app.data.local.entity.FavoriteRoute> = emptyList()
    ): TripCategory? {
        val isCommuteAddressPair = (startAddressType == "THUIS" && endAddressType == "WERK") ||
            (startAddressType == "WERK" && endAddressType == "THUIS")

        val outsideHours = if (timestamp != null && workDaysStr != null && workStartTime != null && workEndTime != null) {
            isOutsideWorkHours(timestamp, workDaysStr, workStartTime, workEndTime)
        } else false

        val matchingRule = findMatchingRule(tripType, startAddressType, endAddressType, startAddress, endAddress, rules)
        if (matchingRule != null) {
            return when (matchingRule.category) {
                "BUSINESS" -> TripCategory.BUSINESS
                "COMMUTE" -> TripCategory.COMMUTE
                "PRIVATE" -> TripCategory.PRIVATE
                else -> defaultCategory
            }
        }

        val matchingFavorite = findMatchingFavoriteRoute(startAddress, endAddress, favoriteRoutes)
        if (matchingFavorite != null) {
            return when (matchingFavorite.tripType.uppercase()) {
                "BUSINESS" -> TripCategory.BUSINESS
                "COMMUTE" -> TripCategory.COMMUTE
                "PRIVATE" -> TripCategory.PRIVATE
                else -> if (matchingFavorite.tripType in businessTripTypes) TripCategory.BUSINESS else defaultCategory
            }
        }
        // If rules exist but none matched, fall through to built-in classification logic
        // so that smart defaults (e.g. KLANT→THUIS = BUSINESS) still apply.

        return when {
            homeWorkAsCommute && (isCommuteAddressPair || tripType == "COMMUTE" || tripType == DYNAMICS_HOME_TO_WORK) -> TripCategory.COMMUTE
            customerAsBusiness && (startAddressType == "KLANT" || endAddressType == "KLANT") -> TripCategory.BUSINESS
            tripType in businessTripTypes -> TripCategory.BUSINESS
            tripType == "PRIVATE" || tripType == DYNAMICS_PERSONAL -> TripCategory.PRIVATE
            outsideHours -> TripCategory.PRIVATE
            else -> defaultCategory
        }
    }

    fun resolveTripType(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        startAddress: String?,
        endAddress: String?,
        rules: List<ClassificationRule>
    ): String? = findMatchingRule(tripType, startAddressType, endAddressType, startAddress, endAddress, rules)?.tripType ?: tripType

    fun findMatchingRule(
        tripType: String?,
        startAddressType: String?,
        endAddressType: String?,
        startAddress: String?,
        endAddress: String?,
        rules: List<ClassificationRule>
    ): ClassificationRule? = rules.filter { rule ->
        val tripMatches = rule.tripType.isNullOrBlank() || rule.tripType.equals(tripType, ignoreCase = true)
        
        val typeDirectionForward = (rule.startAddressType.isNullOrBlank() || rule.startAddressType.equals(startAddressType, ignoreCase = true)) &&
            (rule.endAddressType.isNullOrBlank() || rule.endAddressType.equals(endAddressType, ignoreCase = true))
            
        val typeDirectionReverse = (rule.startAddressType.isNullOrBlank() || rule.startAddressType.equals(endAddressType, ignoreCase = true)) &&
            (rule.endAddressType.isNullOrBlank() || rule.endAddressType.equals(startAddressType, ignoreCase = true))

        val exactDirectionForward = (!rule.startAddress.isNullOrBlank() && rule.startAddress.equals(startAddress, ignoreCase = true)) &&
            (!rule.endAddress.isNullOrBlank() && rule.endAddress.equals(endAddress, ignoreCase = true))
            
        val exactDirectionReverse = (!rule.startAddress.isNullOrBlank() && rule.startAddress.equals(endAddress, ignoreCase = true)) &&
            (!rule.endAddress.isNullOrBlank() && rule.endAddress.equals(startAddress, ignoreCase = true))
            
        // Rule matches if trip type matches AND (types match OR exact addresses match)
        // If a rule defines startAddress/endAddress, it must match them exactly.
        val matchesTypes = typeDirectionForward || typeDirectionReverse
        val matchesExact = exactDirectionForward || exactDirectionReverse
        
        val usesExactAddresses = !rule.startAddress.isNullOrBlank() && !rule.endAddress.isNullOrBlank()
        
        tripMatches && (if (usesExactAddresses) matchesExact else matchesTypes)
    }.maxByOrNull { rule ->
        listOf(rule.startAddressType, rule.endAddressType, rule.tripType, rule.startAddress, rule.endAddress).count { !it.isNullOrBlank() }
    }

    fun findMatchingFavoriteRoute(
        startAddress: String?,
        endAddress: String?,
        favoriteRoutes: List<com.cimdriver.app.data.local.entity.FavoriteRoute>
    ): com.cimdriver.app.data.local.entity.FavoriteRoute? {
        if (startAddress.isNullOrBlank() || endAddress.isNullOrBlank()) return null
        val cleanStart = startAddress.trim()
        val cleanEnd = endAddress.trim()
        return favoriteRoutes.firstOrNull { route ->
            val s = route.startAddress.trim()
            val e = route.endAddress.trim()
            (s.equals(cleanStart, ignoreCase = true) && e.equals(cleanEnd, ignoreCase = true)) ||
            (s.equals(cleanEnd, ignoreCase = true) && e.equals(cleanStart, ignoreCase = true))
        }
    }

    data class ConfidenceAssessment(
        val category: TripCategory,
        val confidenceScore: Int, // 0 - 100
        val reasoning: String,
        val suggestedProjectCode: String? = null
    ) {
        val isHighConfidence: Boolean get() = confidenceScore >= 85
    }

    fun assessConfidence(
        tripType: String?,
        startAddressType: String? = null,
        endAddressType: String? = null,
        startAddress: String? = null,
        endAddress: String? = null,
        rules: List<ClassificationRule> = emptyList(),
        timestamp: Long? = null,
        workDaysStr: String? = null,
        workStartTime: String? = null,
        workEndTime: String? = null,
        isBluetoothTriggered: Boolean = false,
        defaultCategory: TripCategory = TripCategory.PRIVATE,
        suggestedProjectCode: String? = null,
        favoriteRoutes: List<com.cimdriver.app.data.local.entity.FavoriteRoute> = emptyList()
    ): ConfidenceAssessment {
        val matchingRule = findMatchingRule(tripType, startAddressType, endAddressType, startAddress, endAddress, rules)
        val matchingFavorite = findMatchingFavoriteRoute(startAddress, endAddress, favoriteRoutes)
        val isCommutePair = (startAddressType == "THUIS" && endAddressType == "WERK") ||
            (startAddressType == "WERK" && endAddressType == "THUIS")
        val isCustomer = startAddressType == "KLANT" || endAddressType == "KLANT"
        val outsideHours = if (timestamp != null && workDaysStr != null && workStartTime != null && workEndTime != null) {
            isOutsideWorkHours(timestamp, workDaysStr, workStartTime, workEndTime)
        } else false

        var score = 50
        val category: TripCategory
        val reasoning: String
        var effectiveProjectCode = suggestedProjectCode

        if (matchingRule != null) {
            val ruleCat = when (matchingRule.category) {
                "BUSINESS" -> TripCategory.BUSINESS
                "COMMUTE" -> TripCategory.COMMUTE
                "PRIVATE" -> TripCategory.PRIVATE
                else -> defaultCategory
            }
            category = ruleCat
            val isExact = !matchingRule.startAddress.isNullOrBlank() && !matchingRule.endAddress.isNullOrBlank()
            score = if (isExact) 96 else 88
            reasoning = if (isExact) "Vaste route-regel matched exact" else "Classificatieregel matched adrestypes"
        } else if (matchingFavorite != null) {
            category = when (matchingFavorite.tripType.uppercase()) {
                "BUSINESS" -> TripCategory.BUSINESS
                "COMMUTE" -> TripCategory.COMMUTE
                "PRIVATE" -> TripCategory.PRIVATE
                else -> if (matchingFavorite.tripType in businessTripTypes) TripCategory.BUSINESS else defaultCategory
            }
            score = 95
            reasoning = "Komt exact overeen met favoriete route '${matchingFavorite.name}'"
            if (!matchingFavorite.projectCode.isNullOrBlank()) {
                effectiveProjectCode = matchingFavorite.projectCode
            }
        } else if (isCommutePair || tripType == "COMMUTE" || tripType == DYNAMICS_HOME_TO_WORK) {
            category = TripCategory.COMMUTE
            score = 92
            reasoning = "Herkend als vaste woon-werktraject (Thuis ↔ Werk)"
        } else if (isCustomer) {
            category = TripCategory.BUSINESS
            score = 88
            reasoning = "Rit van of naar een opgeslagen klantlocatie"
        } else if (outsideHours) {
            category = TripCategory.PRIVATE
            score = 82
            reasoning = "Gereden buiten reguliere werktijden of in het weekend"
        } else if (tripType in businessTripTypes) {
            category = TripCategory.BUSINESS
            score = 75
            reasoning = "Zakelijk rittype geselecteerd"
        } else if (tripType == "PRIVATE" || tripType == DYNAMICS_PERSONAL) {
            category = TripCategory.PRIVATE
            score = 75
            reasoning = "Privé rittype geselecteerd"
        } else {
            category = defaultCategory
            score = 45
            reasoning = "Onbekende route; controleer de classificatie"
        }

        if (isBluetoothTriggered && score < 95) {
            score = minOf(98, score + 6)
        }

        return ConfidenceAssessment(
            category = category,
            confidenceScore = score,
            reasoning = reasoning,
            suggestedProjectCode = effectiveProjectCode
        )
    }

    fun isOutsideWorkHours(timestamp: Long, workDaysStr: String, workStartTime: String, workEndTime: String): Boolean {
        if (workDaysStr.isBlank() || workStartTime.isBlank() || workEndTime.isBlank()) return false
        
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        
        // Calendar days: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
        // Settings workDays format: "1,2,3,4,5" where 1=Mon, 7=Sun
        val calDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to our format (1=Mon, 7=Sun)
        val ourDayOfWeek = if (calDayOfWeek == Calendar.SUNDAY) 7 else calDayOfWeek - 1
        
        val workDaysList = workDaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        
        if (!workDaysList.contains(ourDayOfWeek)) {
            return true // It's outside work days
        }
        
        // Check hours
        try {
            val startParts = workStartTime.split(":")
            val endParts = workEndTime.split(":")
            if (startParts.size >= 2 && endParts.size >= 2) {
                val startHour = startParts[0].toInt()
                val startMin = startParts[1].toInt()
                val endHour = endParts[0].toInt()
                val endMin = endParts[1].toInt()
                
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMin = cal.get(Calendar.MINUTE)
                
                val currentMins = currentHour * 60 + currentMin
                val startMins = startHour * 60 + startMin
                val endMins = endHour * 60 + endMin
                
                if (currentMins < startMins || currentMins > endMins) {
                    return true // Outside work hours
                }
            }
        } catch (e: Exception) {
            // If parsing fails, fall back to false
        }
        return false
    }
}
