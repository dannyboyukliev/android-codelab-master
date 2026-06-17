package com.sap.codelab.view.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.geofence.IGeofenceManager
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val repository: IMemoRepository,
    private val geofenceManager: IGeofenceManager,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _isShowAll = MutableStateFlow(false)
    val isShowAll: StateFlow<Boolean> = _isShowAll
    private val _memos: MutableStateFlow<List<Memo>> = MutableStateFlow(listOf())
    val memos: StateFlow<List<Memo>> = _memos

    fun loadAllMemos() {
        _isShowAll.value = true
        viewModelScope.launch(dispatcher) {
            _memos.value = repository.getAll()
        }
    }

    fun loadOpenMemos() {
        _isShowAll.value = false
        viewModelScope.launch(dispatcher) {
            _memos.value = repository.getOpen()
        }
    }

    fun refreshMemos() {
        if (_isShowAll.value) loadAllMemos() else loadOpenMemos()
    }

    fun updateMemo(memo: Memo, isChecked: Boolean) {
        viewModelScope.launch(dispatcher) {
            if (isChecked) {
                repository.saveMemo(memo.copy(isDone = true))
                geofenceManager.remove(memo.id)
            }
        }
    }
}
