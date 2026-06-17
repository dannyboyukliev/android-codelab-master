package com.sap.codelab.view.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.geofence.IGeofenceManager
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.utils.extensions.empty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed class CreateMemoUiState {
    object Idle : CreateMemoUiState()
    object Saved : CreateMemoUiState()
    data class ValidationFailed(val titleError: Boolean, val textError: Boolean) : CreateMemoUiState()
}

@HiltViewModel
internal class CreateMemoViewModel @Inject constructor(
    private val repository: IMemoRepository,
    private val geofenceManager: IGeofenceManager,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private var memo = Memo(0, String.empty(), String.empty(), 0, null, null, false)
    var selectedLatitude: Double? = null
        private set
    var selectedLongitude: Double? = null
        private set

    private val _uiState = MutableStateFlow<CreateMemoUiState>(CreateMemoUiState.Idle)
    val uiState: StateFlow<CreateMemoUiState> = _uiState

    /**
     * Stores the location the user picked on the map for this memo.
     */
    fun setLocation(latitude: Double, longitude: Double) {
        selectedLatitude = latitude
        selectedLongitude = longitude
    }

    fun hasSelectedLocation(): Boolean = selectedLatitude != null && selectedLongitude != null

    fun clearLocation() {
        selectedLatitude = null
        selectedLongitude = null
    }

    fun saveMemo() {
        val titleError = memo.title.isBlank()
        val textError = memo.description.isBlank()
        if (titleError || textError) {
            _uiState.value = CreateMemoUiState.ValidationFailed(titleError, textError)
            return
        }
        viewModelScope.launch(dispatcher) {
            val memoId = repository.saveMemo(memo)
            val lat = selectedLatitude
            val lng = selectedLongitude
            if (lat != null && lng != null) {
                geofenceManager.add(memoId, lat, lng)
            }
            _uiState.value = CreateMemoUiState.Saved
        }
    }

    fun updateMemo(title: String, description: String) {
        memo = memo.copy(
            title = title,
            description = description,
            reminderLatitude = selectedLatitude,
            reminderLongitude = selectedLongitude
        )
    }
}
