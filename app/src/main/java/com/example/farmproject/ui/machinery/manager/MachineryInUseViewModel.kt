package com.example.farmproject.ui.machinery.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.MachineryUsage
import com.example.farmproject.data.repository.MachineryRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MachineryInUseUiState {
    data object Loading : MachineryInUseUiState
    data class Success(
        val usageLogs: List<MachineryUsage>,
        val machineryNames: Map<String, String> = emptyMap()
    ) : MachineryInUseUiState
    data class Error(val message: String) : MachineryInUseUiState
}

class MachineryInUseViewModel(
    private val repository: MachineryRepository = MachineryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MachineryInUseUiState>(MachineryInUseUiState.Loading)
    val uiState: StateFlow<MachineryInUseUiState> = _uiState.asStateFlow()

    init {
        loadInUseData()
    }

    fun loadInUseData() {
        viewModelScope.launch {
            _uiState.value = MachineryInUseUiState.Loading
            try {
                val farmId = CurrentSession.farmId
                val allMachinery = repository.getMachineryForFarm(farmId)
                val usageLogs = repository.getAllUsageForFarm(farmId)

                val machineryNames = allMachinery
                    .filter { !it.machineryId.isNullOrBlank() }
                    .associate { it.machineryId!! to it.name }

                _uiState.value = MachineryInUseUiState.Success(
                    usageLogs = usageLogs,
                    machineryNames = machineryNames
                )
            } catch (e: Exception) {
                _uiState.value = MachineryInUseUiState.Error(e.message ?: "Failed to load usage history")
            }
        }
    }
}
