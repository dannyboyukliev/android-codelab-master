package com.sap.codelab.view.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ViewMemoViewModel @Inject constructor(
    private val repository: IMemoRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _memo: MutableStateFlow<Memo?> = MutableStateFlow(null)
    val memo: StateFlow<Memo?> = _memo

    fun loadMemo(memoId: Long) {
        viewModelScope.launch(dispatcher) {
            _memo.value = repository.getMemoById(memoId)
        }
    }
}
