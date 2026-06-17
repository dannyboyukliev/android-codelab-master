package com.sap.codelab.view.detail

import com.sap.codelab.model.Memo
import com.sap.codelab.repository.FakeMemoRepository
import com.sap.codelab.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ViewMemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMemoRepository
    private lateinit var viewModel: ViewMemoViewModel

    @Before
    fun setup() {
        repository = FakeMemoRepository()
        viewModel = ViewMemoViewModel(repository, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `memo is null before loading`() {
        assertNull(viewModel.memo.value)
    }

    @Test
    fun `loadMemo emits the correct memo`() = runTest {
        val memo = Memo(id = 1, title = "Title", description = "Desc", reminderDate = 0, reminderLatitude = null, reminderLongitude = null, isDone = false)
        repository.memos.add(memo)

        viewModel.loadMemo(1L)

        assertEquals(memo, viewModel.memo.value)
    }
}
