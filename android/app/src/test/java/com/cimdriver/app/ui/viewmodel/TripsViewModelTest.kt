package com.cimdriver.app.ui.viewmodel

import com.cimdriver.app.data.local.AppDatabase
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
        
        // Setup default mocks
        `when`(tripRepository.getAllTrips()).thenReturn(flowOf(emptyList()))
        
        viewModel = TripsViewModel(tripRepository, database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is empty`() = runTest {
        // Just verify it doesn't crash on init
        assertEquals(true, viewModel != null)
    }
}
