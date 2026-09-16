package com.cimdriver.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cimdriver.app.service.GeocoderService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class GeocoderServiceTest {

    @Test
    fun testOsrmRoute() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val service = GeocoderService(appContext)
        val route = service.getOsrmRoute(52.3676, 4.9041, 51.9225, 4.47917)
        println("OSRM Route size: ${route.size}")
        assertTrue("Route should not be empty", route.isNotEmpty())
    }

    @Test
    fun testGeocoding() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val service = GeocoderService(appContext)
        val coords = service.getCoordinatesForAddress("Amsterdam")
        println("Coords: $coords")
        assertNotNull("Coordinates should not be null", coords)
    }
}
