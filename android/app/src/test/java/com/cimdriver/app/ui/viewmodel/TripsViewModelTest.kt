package com.cimdriver.app.ui.viewmodel

import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.SettingsDao
import com.cimdriver.app.data.repository.TripRepository
import com.cimdriver.app.data.local.entity.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tripRepository: TripRepository
    private lateinit var database: AppDatabase
    private lateinit var viewModel: TripsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tripRepository = mock(TripRepository::class.java)
        database = mock(AppDatabase::class.java)
        val settingsDao = mock(SettingsDao::class.java)
        
        `when`(database.settingsDao()).thenReturn(settingsDao)
        `when`(settingsDao.getSettings()).thenReturn(flowOf(null))
        `when`(tripRepository.getAllTrips()).thenReturn(flowOf(emptyList()))
        `when`(tripRepository.getAllFavoriteRoutes()).thenReturn(flowOf(emptyList()))
        `when`(tripRepository.getAllVehicles()).thenReturn(flowOf(emptyList()))
        `when`(tripRepository.getUniqueProjectCodes()).thenReturn(flowOf(emptyList()))
        `when`(tripRepository.getClassificationRules()).thenReturn(flowOf(emptyList()))
        `when`(tripRepository.getSavedAddresses()).thenReturn(flowOf(emptyList()))
        
        viewModel = TripsViewModel(tripRepository, database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is empty`() = runTest {
        assertEquals(emptyList<Trip>(), viewModel.trips.value)
    }
}
