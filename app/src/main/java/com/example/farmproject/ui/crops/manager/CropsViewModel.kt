package com.example.farmproject.ui.crops.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.Crop
import com.example.farmproject.data.repository.CropRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CropsUiState {
    data object Loading : CropsUiState
    data class Success(val crops: List<Crop>) : CropsUiState
    data class Error(val message: String) : CropsUiState
}

sealed interface CropSaveState {
    data object Idle : CropSaveState
    data object Saving : CropSaveState
    data object Saved : CropSaveState
    data class Error(val message: String) : CropSaveState
}

class CropsViewModel(
    private val repository: CropRepository = CropRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CropsUiState>(CropsUiState.Loading)
    val uiState: StateFlow<CropsUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<CropSaveState>(CropSaveState.Idle)
    val saveState: StateFlow<CropSaveState> = _saveState.asStateFlow()

    init {
        loadCrops()
    }

    fun loadCrops() {
        viewModelScope.launch {
            _uiState.value = CropsUiState.Loading
            try {
                val farmId = CurrentSession.farmId
                val crops = repository.getCropsForFarm(farmId).getOrDefault(emptyList())
                _uiState.value = CropsUiState.Success(crops)
            } catch (e: Exception) {
                _uiState.value = CropsUiState.Error(e.message ?: "Failed to load crops")
            }
        }
    }

    fun addCrop(
        name: String,
        scientificName: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _saveState.value = CropSaveState.Saving
            try {
                val result = repository.createCrop(
                    name = name,
                    scientificName = scientificName,
                    description = notes
                )
                result.fold(
                    onSuccess = {
                        _saveState.value = CropSaveState.Saved
                        loadCrops()
                    },
                    onFailure = { error ->
                        _saveState.value = CropSaveState.Error(error.message ?: "Failed to save crop")
                    }
                )
            } catch (e: Exception) {
                _saveState.value = CropSaveState.Error(e.message ?: "Failed to save crop")
            }
        }
    }

    fun deleteCrop(cropId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCrop(cropId)
                loadCrops()
            } catch (e: Exception) {
                _saveState.value = CropSaveState.Error(e.message ?: "Failed to delete crop")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = CropSaveState.Idle
    }
}
