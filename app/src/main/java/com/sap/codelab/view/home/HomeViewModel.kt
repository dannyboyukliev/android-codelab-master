package com.sap.codelab.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val repository: IMemoRepository
) : ViewModel() {

    private var isShowAll = false
    private val _memos: MutableStateFlow<List<Memo>> = MutableStateFlow(listOf())
    val memos: StateFlow<List<Memo>> = _memos

    fun loadAllMemos() {
        isShowAll = true
        viewModelScope.launch(Dispatchers.IO) {
            _memos.value = repository.getAll()
        }
    }

    fun loadOpenMemos() {
        isShowAll = false
        viewModelScope.launch(Dispatchers.IO) {
            _memos.value = repository.getOpen()
        }
    }

    fun refreshMemos() {
        if (isShowAll) loadAllMemos() else loadOpenMemos()
    }

    fun updateMemo(memo: Memo, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isChecked) {
                repository.saveMemo(memo.copy(isDone = true))
            }
        }
    }
}
