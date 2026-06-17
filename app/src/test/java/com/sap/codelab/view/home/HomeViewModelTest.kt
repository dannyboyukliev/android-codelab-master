package com.sap.codelab.view.home

import com.sap.codelab.geofence.FakeGeofenceManager
import com.sap.codelab.model.Memo
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
internal class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMemoRepository
    private lateinit var geofenceManager: FakeGeofenceManager
    private lateinit var viewModel: HomeViewModel

    private val openMemo = Memo(id = 1, title = "Open", description = "Desc", reminderDate = 0, reminderLatitude = null, reminderLongitude = null, isDone = false)
    private val doneMemo = Memo(id = 2, title = "Done", description = "Desc", reminderDate = 0, reminderLatitude = null, reminderLongitude = null, isDone = true)

    @Before
    fun setup() {
        repository = FakeMemoRepository()
        geofenceManager = FakeGeofenceManager()
        viewModel = HomeViewModel(repository, geofenceManager, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `loadOpenMemos emits only non-done memos`() = runTest {
        repository.memos.addAll(listOf(openMemo, doneMemo))

        viewModel.loadOpenMemos()

        assertEquals(listOf(openMemo), viewModel.memos.value)
    }

    @Test
    fun `loadAllMemos emits all memos`() = runTest {
        repository.memos.addAll(listOf(openMemo, doneMemo))

        viewModel.loadAllMemos()

        assertEquals(listOf(openMemo, doneMemo), viewModel.memos.value)
    }

    @Test
    fun `refreshMemos reloads with the current open filter`() = runTest {
        repository.memos.add(openMemo)
        viewModel.loadOpenMemos()

        repository.memos.add(Memo(id = 3, title = "Open2", description = "Desc", reminderDate = 0, reminderLatitude = null, reminderLongitude = null, isDone = false))
        viewModel.refreshMemos()

        assertEquals(2, viewModel.memos.value.size)
    }

    @Test
    fun `refreshMemos reloads with the current show-all filter`() = runTest {
        repository.memos.add(openMemo)
        viewModel.loadAllMemos()

        repository.memos.add(doneMemo)
        viewModel.refreshMemos()

        assertEquals(2, viewModel.memos.value.size)
    }

    @Test
    fun `updateMemo marks memo as done when checked`() = runTest {
        repository.memos.add(openMemo)

        viewModel.updateMemo(openMemo, isChecked = true)

        assertTrue(repository.memos.first { it.id == openMemo.id }.isDone)
    }

    @Test
    fun `updateMemo does not modify memo when unchecked`() = runTest {
        repository.memos.add(openMemo)

        viewModel.updateMemo(openMemo, isChecked = false)

        assertFalse(repository.memos.first { it.id == openMemo.id }.isDone)
    }

    @Test
    fun `updateMemo removes geofence when memo is marked done`() = runTest {
        repository.memos.add(openMemo)

        viewModel.updateMemo(openMemo, isChecked = true)

        assertEquals(listOf(openMemo.id), geofenceManager.removedGeofenceIds)
    }

    @Test
    fun `updateMemo does not remove geofence when unchecked`() = runTest {
        repository.memos.add(openMemo)

        viewModel.updateMemo(openMemo, isChecked = false)

        assertTrue(geofenceManager.removedGeofenceIds.isEmpty())
    }
}
