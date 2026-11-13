package com.cimdriver.app.ui.viewmodel

import com.cimdriver.app.data.local.AppDatabase
import com.cimdriver.app.data.local.dao.ClientDao
import com.cimdriver.app.data.local.dao.HoursTargetDao
import com.cimdriver.app.data.local.dao.ProjectCodeDao
import com.cimdriver.app.data.local.dao.SettingsDao
import com.cimdriver.app.data.local.dao.TripDao
import com.cimdriver.app.data.local.dao.WorkDayDao
import com.cimdriver.app.data.local.entity.ProjectCode
import com.cimdriver.app.data.local.entity.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ClientProjectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var clientDao: ClientDao
    private lateinit var projectCodeDao: ProjectCodeDao
    private lateinit var tripDao: TripDao
    private lateinit var hoursTargetDao: HoursTargetDao
    private lateinit var workDayDao: WorkDayDao
    private lateinit var settingsDao: SettingsDao
    private lateinit var viewModel: ClientProjectViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database = mock(AppDatabase::class.java)
        clientDao = mock(ClientDao::class.java)
        projectCodeDao = mock(ProjectCodeDao::class.java)
        tripDao = mock(TripDao::class.java)
        hoursTargetDao = mock(HoursTargetDao::class.java)
        workDayDao = mock(WorkDayDao::class.java)
        settingsDao = mock(SettingsDao::class.java)

        `when`(database.clientDao()).thenReturn(clientDao)
        `when`(database.projectCodeDao()).thenReturn(projectCodeDao)
        `when`(database.tripDao()).thenReturn(tripDao)
        `when`(database.hoursTargetDao()).thenReturn(hoursTargetDao)
        `when`(database.workDayDao()).thenReturn(workDayDao)
        `when`(database.settingsDao()).thenReturn(settingsDao)

        `when`(clientDao.getActiveClients()).thenReturn(flowOf(emptyList()))
        `when`(projectCodeDao.getAllProjectCodes()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRestoreProjectCode_updatesCreatedAtAndIsActive() = runTest {
        val oldTime = System.currentTimeMillis() - 60L * 86400_000L // 60 days ago
        val archivedProject = ProjectCode(
            id = 42L,
            code = "PRJ-OLD",
            description = "Test project",
            isActive = false,
            createdAt = oldTime
        )

        `when`(settingsDao.getSettingsSync()).thenReturn(Settings(autoArchiveProjectDays = 30))
        `when`(projectCodeDao.getAllProjectCodesSync()).thenReturn(listOf(archivedProject))

        viewModel = ClientProjectViewModel(database)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreProjectCode(archivedProject)
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = ArgumentCaptor.forClass(ProjectCode::class.java)
        verify(projectCodeDao).updateProjectCode(captureProjectCode(captor))

        assertEquals(42L, captor.value.id)
        assertTrue(captor.value.isActive)
        assertTrue("createdAt should be refreshed to current time", captor.value.createdAt > oldTime)
    }

    private fun captureProjectCode(captor: ArgumentCaptor<ProjectCode>): ProjectCode {
        captor.capture()
        return ProjectCode(id = 0L, code = "")
    }
}
