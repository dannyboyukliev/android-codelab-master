package com.sap.codelab.view.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.utils.extensions.empty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateMemoViewModel @Inject constructor(
    private val repository: IMemoRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private var memo = Memo(0, String.empty(), String.empty(), 0, 0.0, 0.0, false)
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    /**
     * Stores the location the user picked on the map for this memo.
     */
    fun setLocation(latitude: Double, longitude: Double) {
        selectedLatitude = latitude
        selectedLongitude = longitude
    }

    fun saveMemo() {
        viewModelScope.launch(dispatcher) {
            repository.saveMemo(memo)
        }
    }

    fun updateMemo(title: String, description: String) {
        memo = memo.copy(
            title = title,
            description = description,
            reminderLatitude = selectedLatitude ?: 0.0,
            reminderLongitude = selectedLongitude ?: 0.0
        )
    }

    fun isMemoValid(): Boolean = memo.title.isNotBlank() && memo.description.isNotBlank()

    fun hasTextError() = memo.description.isBlank()

    fun hasTitleError() = memo.title.isBlank()
}
