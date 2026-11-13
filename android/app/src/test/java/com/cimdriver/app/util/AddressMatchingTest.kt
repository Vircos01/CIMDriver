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

    @Test
    fun findsParkingGarageDepartureMatchWhenGpsDelayed() {
        val duo = SavedAddress(
            id = 1,
            label = "DUO Groningen",
            address = "Kempkensberg 12, Groningen",
            latitude = 53.2050,
            longitude = 6.5820,
            isWorkLocation = true
        )
        val addresses = listOf(duo)

        // First GPS fix 600 meters outside garage: ~53.2090, 6.5870
        val match = AddressMatching.findParkingGarageDepartureMatch(
            firstGpsLat = 53.2090,
            firstGpsLon = 6.5870,
            parkedLat = 53.2050,
            parkedLon = 6.5820,
            parkedAddressLabel = "DUO Groningen",
            addresses = addresses,
            maxExitRadiusMeters = 1500f
        )

        org.junit.Assert.assertNotNull(match)
        assertEquals("DUO Groningen", match?.address?.label)
        assertTrue(match!!.distanceMeters > 100f && match.distanceMeters < 1500f)
    }

    @Test
    fun findsParkingGarageDepartureMatchWhenNoGpsFix() {
        val home = SavedAddress(
            id = 2,
            label = "Home",
            address = "Mellenssteeg 36, Haren",
            latitude = 53.1750,
            longitude = 6.6020,
            isHomeLocation = true
        )
        val addresses = listOf(home)

        val match = AddressMatching.findParkingGarageDepartureMatch(
            firstGpsLat = null,
            firstGpsLon = null,
            parkedLat = 53.1750,
            parkedLon = 6.6020,
            parkedAddressLabel = "Home",
            addresses = addresses
        )

        org.junit.Assert.assertNotNull(match)
        assertEquals("Home", match?.address?.label)
    }

    @Test
    fun returnsNullWhenFirstGpsFixTooFarFromGarage() {
        val office = SavedAddress(
            id = 3,
            label = "Kantoor",
            address = "Hoofdstraat 1, Assen",
            latitude = 52.9900,
            longitude = 6.5600
        )
        val addresses = listOf(office)

        // 10 km away
        val match = AddressMatching.findParkingGarageDepartureMatch(
            firstGpsLat = 53.0800,
            firstGpsLon = 6.5600,
            parkedLat = 52.9900,
            parkedLon = 6.5600,
            parkedAddressLabel = "Kantoor",
            addresses = addresses,
            maxExitRadiusMeters = 1500f
        )

        org.junit.Assert.assertNull(match)
    }
}
