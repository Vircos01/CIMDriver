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
}
