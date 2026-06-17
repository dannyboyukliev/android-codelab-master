package com.sap.codelab.view.create

import com.sap.codelab.geofence.FakeGeofenceManager
import com.sap.codelab.repository.FakeMemoRepository
import com.sap.codelab.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateMemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMemoRepository
    private lateinit var geofenceManager: FakeGeofenceManager
    private lateinit var viewModel: CreateMemoViewModel

    @Before
    fun setup() {
        repository = FakeMemoRepository()
        geofenceManager = FakeGeofenceManager()
        viewModel = CreateMemoViewModel(repository, geofenceManager, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `isMemoValid returns false when title is blank`() {
        viewModel.updateMemo(title = "", description = "Description")

        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun `isMemoValid returns false when description is blank`() {
        viewModel.updateMemo(title = "Title", description = "")

        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun `isMemoValid returns true when title and description are set`() {
        viewModel.updateMemo(title = "Title", description = "Description")

        assertTrue(viewModel.isMemoValid())
    }

    @Test
    fun `hasTitleError returns true when title is blank`() {
        viewModel.updateMemo(title = "", description = "Description")

        assertTrue(viewModel.hasTitleError())
    }

    @Test
    fun `hasTextError returns true when description is blank`() {
        viewModel.updateMemo(title = "Title", description = "")

        assertTrue(viewModel.hasTextError())
    }

    @Test
    fun `saveMemo persists memo to repository`() = runTest {
        viewModel.updateMemo(title = "Title", description = "Description")

        viewModel.saveMemo()

        assertEquals(1, repository.memos.size)
        assertEquals("Title", repository.memos.first().title)
        assertEquals("Description", repository.memos.first().description)
    }

    @Test
    fun `saveMemo persists picked location to repository`() = runTest {
        viewModel.setLocation(48.1351, 11.5820)
        viewModel.updateMemo(title = "Title", description = "Description")

        viewModel.saveMemo()

        assertEquals(48.1351, repository.memos.first().reminderLatitude, 0.0001)
        assertEquals(11.5820, repository.memos.first().reminderLongitude, 0.0001)
    }

    @Test
    fun `saveMemo transitions uiState to Saved`() = runTest {
        viewModel.updateMemo(title = "Title", description = "Description")

        viewModel.saveMemo()

        assertTrue(viewModel.uiState.value is CreateMemoUiState.Saved)
    }

    @Test
    fun `saveMemo registers geofence when location is set`() = runTest {
        viewModel.setLocation(48.1351, 11.5820)
        viewModel.updateMemo(title = "Title", description = "Description")

        viewModel.saveMemo()

        assertEquals(1, geofenceManager.addedGeofences.size)
        val (_, lat, lng) = geofenceManager.addedGeofences.first()
        assertEquals(48.1351, lat, 0.0001)
        assertEquals(11.5820, lng, 0.0001)
    }

    @Test
    fun `saveMemo does not register geofence when no location is set`() = runTest {
        viewModel.updateMemo(title = "Title", description = "Description")

        viewModel.saveMemo()

        assertTrue(geofenceManager.addedGeofences.isEmpty())
    }
}
