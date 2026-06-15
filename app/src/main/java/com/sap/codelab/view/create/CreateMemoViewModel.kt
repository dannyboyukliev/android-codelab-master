package com.sap.codelab.view.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.utils.extensions.empty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateMemoViewModel @Inject constructor(
    private val repository: IMemoRepository
) : ViewModel() {

    private var memo = Memo(0, String.empty(), String.empty(), 0, 0, 0, false)

    fun saveMemo() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMemo(memo)
        }
    }

    fun updateMemo(title: String, description: String) {
        memo = Memo(title = title, description = description, id = 0, reminderDate = 0, reminderLatitude = 0, reminderLongitude = 0, isDone = false)
    }

    fun isMemoValid(): Boolean = memo.title.isNotBlank() && memo.description.isNotBlank()

    fun hasTextError() = memo.description.isBlank()

    fun hasTitleError() = memo.title.isBlank()
}
