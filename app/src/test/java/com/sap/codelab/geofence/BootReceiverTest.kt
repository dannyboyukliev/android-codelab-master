package com.sap.codelab.geofence

import com.sap.codelab.model.Memo
import com.sap.codelab.repository.FakeMemoRepository
import com.sap.codelab.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class BootReceiverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMemoRepository
    private lateinit var geofenceManager: FakeGeofenceManager

    private val memoWithLocation = Memo(id = 1, title = "Near Cafe", description = "Pick up coffee", reminderDate = 0, reminderLatitude = 48.1351, reminderLongitude = 11.5820, isDone = false)
    private val memoWithoutLocation = Memo(id = 2, title = "General", description = "No location", reminderDate = 0, reminderLatitude = 0.0, reminderLongitude = 0.0, isDone = false)
    private val doneMemoWithLocation = Memo(id = 3, title = "Done", description = "Already done", reminderDate = 0, reminderLatitude = 52.5200, reminderLongitude = 13.4050, isDone = true)

    @Before
    fun setup() {
        repository = FakeMemoRepository()
        geofenceManager = FakeGeofenceManager()
    }

    @Test
    fun `re-registers geofences for open memos with a location`() = runTest {
        repository.memos.addAll(listOf(memoWithLocation, memoWithoutLocation, doneMemoWithLocation))

        reRegisterAll()

        assertEquals(1, geofenceManager.addedGeofences.size)
        val (id, lat, lng) = geofenceManager.addedGeofences.first()
        assertEquals(1L, id)
        assertEquals(48.1351, lat, 0.0001)
        assertEquals(11.5820, lng, 0.0001)
    }

    @Test
    fun `skips open memos without a location`() = runTest {
        repository.memos.add(memoWithoutLocation)

        reRegisterAll()

        assertTrue(geofenceManager.addedGeofences.isEmpty())
    }

    @Test
    fun `skips done memos even when they have a location`() = runTest {
        repository.memos.add(doneMemoWithLocation)

        reRegisterAll()

        assertTrue(geofenceManager.addedGeofences.isEmpty())
    }

    @Test
    fun `registers all open memos with locations`() = runTest {
        val second = Memo(id = 4, title = "Second", description = "Desc", reminderDate = 0, reminderLatitude = 51.5074, reminderLongitude = -0.1278, isDone = false)
        repository.memos.addAll(listOf(memoWithLocation, second))

        reRegisterAll()

        assertEquals(2, geofenceManager.addedGeofences.size)
    }

    private suspend fun reRegisterAll() {
        repository.getOpen()
            .filter { it.reminderLatitude != 0.0 || it.reminderLongitude != 0.0 }
            .forEach { memo ->
                geofenceManager.add(memo.id, memo.reminderLatitude, memo.reminderLongitude)
            }
    }
}
