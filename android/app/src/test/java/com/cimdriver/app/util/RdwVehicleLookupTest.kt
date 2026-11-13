package com.cimdriver.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RdwVehicleLookupTest {

    @Test
    fun `cleanPlate removes dashes spaces and converts to uppercase`() {
        assertEquals("G788NL", RdwVehicleLookup.cleanPlate("g-788-nl"))
        assertEquals("AB123C", RdwVehicleLookup.cleanPlate("  ab - 123 - c  "))
        assertEquals("12AB34", RdwVehicleLookup.cleanPlate("12-ab-34"))
    }

    @Test
    fun `capitalizeWords capitalizes each word properly`() {
        assertEquals("Volkswagen", RdwVehicleLookup.capitalizeWords("VOLKSWAGEN"))
        assertEquals("Polo", RdwVehicleLookup.capitalizeWords("POLO"))
        assertEquals("Model 3", RdwVehicleLookup.capitalizeWords("MODEL 3"))
        assertEquals("Mercedes-benz", RdwVehicleLookup.capitalizeWords("MERCEDES-BENZ"))
    }

    @Test
    fun `determineEngineTypeFromFuels detects EV PHEV and ICE`() {
        // Pure electric
        assertEquals("EV", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("Elektriciteit")))

        // Hybrid / PHEV (fossil + electric)
        assertEquals("PHEV", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("Benzine", "Elektriciteit")))
        assertEquals("PHEV", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("Diesel", "Elektriciteit")))

        // Pure fossil
        assertEquals("ICE", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("Benzine")))
        assertEquals("ICE", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("Diesel")))
        assertEquals("ICE", RdwVehicleLookup.determineEngineTypeFromFuels(listOf("LPG")))

        // Fallback on model name if fuel list is empty
        assertEquals("EV", RdwVehicleLookup.determineEngineTypeFromFuels(emptyList(), "Model 3 EV"))
        assertEquals("PHEV", RdwVehicleLookup.determineEngineTypeFromFuels(emptyList(), "Passat GTE PHEV"))
        assertEquals("ICE", RdwVehicleLookup.determineEngineTypeFromFuels(emptyList(), "Golf 1.4 TSI"))
    }
}

