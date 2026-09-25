package com.cimdriver.app.util

import com.cimdriver.app.data.local.entity.SavedAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressMatchingTest {
    @Test
    fun normalizesAccentsPunctuationAndHouseNumberSpacing() {
        assertEquals("rue12aamsterdam", AddressMatching.normalize("Rue 12-A, Amsterdam"))
        assertTrue(AddressMatching.matches("Rue 12 A Amsterdam", "rue 12-a, Amsterdam"))
    }

    @Test
    fun findsSavedAddressByLabelOrNormalizedAddress() {
        val saved = SavedAddress(label = "Kantoor", address = "Kerkstraat 12-A, Utrecht")

        assertEquals(saved, AddressMatching.findSavedAddress("kantoor", listOf(saved)))
        assertEquals(saved, AddressMatching.findSavedAddress("Kerkstraat 12 A Utrecht", listOf(saved)))
    }
}
