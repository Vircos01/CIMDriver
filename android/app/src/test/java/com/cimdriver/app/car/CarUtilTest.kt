package com.cimdriver.app.car

import org.junit.Assert.assertEquals
import org.junit.Test

class CarUtilTest {

    @Test
    fun testGetLocalizedTripType_knownTypes() {
        assertEquals("Woon-werk", CarUtil.getLocalizedTripType("Home To Work"))
        assertEquals("Woon-werk", CarUtil.getLocalizedTripType("COMMUTE"))
        assertEquals("Zakelijk overleg", CarUtil.getLocalizedTripType("Business Meeting"))
        assertEquals("Klantbezoek", CarUtil.getLocalizedTripType("Customer Visit"))
        assertEquals("Klant declarabel", CarUtil.getLocalizedTripType("Customer Billable"))
        assertEquals("In opdracht CIMSOLUTIONS", CarUtil.getLocalizedTripType("Commissioned By CIMSOLUTIONS"))
        assertEquals("Cursus / Examen", CarUtil.getLocalizedTripType("Exam Course"))
        assertEquals("Auto-onderhoud", CarUtil.getLocalizedTripType("Car Maintenance"))
        assertEquals("Privé", CarUtil.getLocalizedTripType("PRIVATE"))
        assertEquals("Privé", CarUtil.getLocalizedTripType("PERSONAL"))
        assertEquals("Zakelijk", CarUtil.getLocalizedTripType("BUSINESS"))
    }

    @Test
    fun testGetLocalizedTripType_nullAndCustom() {
        assertEquals("Onbekend", CarUtil.getLocalizedTripType(null))
        assertEquals("Project X", CarUtil.getLocalizedTripType("Project X"))
    }
}

