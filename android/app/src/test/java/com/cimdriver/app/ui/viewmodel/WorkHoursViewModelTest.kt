package com.cimdriver.app.ui.viewmodel

import com.cimdriver.app.data.local.dao.SettingsDao
import com.cimdriver.app.data.local.dao.WorkDayDao
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
class WorkHoursViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workDayDao: WorkDayDao
    private lateinit var settingsDao: SettingsDao
    private lateinit var viewModel: WorkHoursViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workDayDao = mock(WorkDayDao::class.java)
        settingsDao = mock(SettingsDao::class.java)
        
        `when`(workDayDao.getAllWorkDays()).thenReturn(flowOf(emptyList()))
        `when`(workDayDao.getToReviewCount()).thenReturn(flowOf(0))
        `when`(settingsDao.getSettings()).thenReturn(flowOf(null))
        
        viewModel = WorkHoursViewModel(workDayDao, settingsDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is not null`() = runTest {
        assertEquals(true, viewModel != null)
    }

    @Test
    fun `test quickApproveWorkDay updates status and permits undo`() = runTest {
        val workDay = com.cimdriver.app.data.local.entity.WorkDay(
            id = 100L,
            date = 1715000000000L,
            firstDepartureTime = 1715000000000L + 8 * 3600_000L,
            arrivalTime = 1715000000000L + 8 * 3600_000L,
            departureTime = null,
            lastArrivalTime = null,
            breakMinutes = 0,
            workLocationLabel = "Kantoor",
            status = "TO_REVIEW"
        )
        val suggestion = com.cimdriver.app.util.WorkDaySuggestion(
            suggestedDepartureTime = 1715000000000L + 17 * 3600_000L,
            suggestedBreakMinutes = 30,
            suggestedProjectCode = "PRJ-TEST",
            confidenceScore = 90,
            statusText = "Compleet",
            isHighConfidence = true,
            requiresAttention = true
        )
        val candidate = com.cimdriver.app.util.WorkDayReviewCandidate(workDay, suggestion)

        viewModel.quickApproveWorkDay(candidate)
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = org.mockito.ArgumentCaptor.forClass(com.cimdriver.app.data.local.entity.WorkDay::class.java)
        verify(workDayDao).updateWorkDay(captureWorkDay(captor))
        assertEquals(100L, captor.value.id)
        assertEquals("APPROVED", captor.value.status)
        assertEquals(suggestion.suggestedDepartureTime, captor.value.departureTime)
        assertEquals(30, captor.value.breakMinutes)
        assertEquals("PRJ-TEST", captor.value.projectCode)

        // Now test undo
        viewModel.undoLastApproval()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(workDayDao).updateWorkDay(workDay)
    }

    private fun captureWorkDay(captor: org.mockito.ArgumentCaptor<com.cimdriver.app.data.local.entity.WorkDay>): com.cimdriver.app.data.local.entity.WorkDay {
        captor.capture()
        return com.cimdriver.app.data.local.entity.WorkDay(
            id = 0L,
            date = 0L,
            firstDepartureTime = 0L,
            arrivalTime = 0L,
            departureTime = null,
            lastArrivalTime = null,
            workLocationLabel = null
        )
    }
}
