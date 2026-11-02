package com.example.farmproject.ui.employees.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.CreateEmployeeRequest
import com.example.farmproject.data.model.EmployeeDisplay
import com.example.farmproject.data.model.ProvisionResult
import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.repository.EmployeeRepository
import com.example.farmproject.data.repository.SupervisorDutyRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EmployeesUiState {
    data object Loading : EmployeesUiState
    data class Success(
        val supervisors: List<EmployeeDisplay>,
        val workers: List<EmployeeDisplay>
    ) : EmployeesUiState
    data class Error(val message: String) : EmployeesUiState
}

sealed interface EmployeeSaveState {
    data object Idle : EmployeeSaveState
    data object Saving : EmployeeSaveState
    data object Saved : EmployeeSaveState
    data class Error(val message: String) : EmployeeSaveState
}

class EmployeesViewModel(
    private val repository: EmployeeRepository = EmployeeRepository(),
    private val dutyRepository: SupervisorDutyRepository = SupervisorDutyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmployeesUiState>(EmployeesUiState.Loading)
    val uiState: StateFlow<EmployeesUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<EmployeeSaveState>(EmployeeSaveState.Idle)
    val saveState: StateFlow<EmployeeSaveState> = _saveState.asStateFlow()

    init {
        loadEmployees()
    }

    fun loadEmployees() {
        val currentSuccess = _uiState.value as? EmployeesUiState.Success
        viewModelScope.launch {
            if (currentSuccess == null) {
                _uiState.value = EmployeesUiState.Loading
            }
            try {
                val farmId = CurrentSession.farmId
                val supervisors = repository.getSupervisorsForFarm(farmId)
                val workers = repository.getWorkersForFarm(farmId)
                _uiState.value = EmployeesUiState.Success(supervisors, workers)
            } catch (e: Exception) {
                if (currentSuccess == null) {
                    _uiState.value = EmployeesUiState.Error(e.message ?: "Failed to load employees")
                }
            }
        }
    }

    fun addSupervisor(
        fullName: String,
        email: String,
        phoneNumber: String?,
        employeeNumber: String?,
        password: String? = null,
        duties: Set<SupervisorDuty> = setOf(SupervisorDuty.ATTENDANCE)
    ) {
        createEmployee(fullName, email, phoneNumber, employeeNumber, password) { request ->
            val result = repository.createSupervisor(request)
            if (result is ProvisionResult.Success) {
                val createdAccId = (result.data as? EmployeeDisplay)?.accountId
                if (!createdAccId.isNullOrBlank()) {
                    runCatching { dutyRepository.setDuties(createdAccId, duties) }
                }
            }
            result
        }
    }

    fun addWorker(fullName: String, email: String, phoneNumber: String?, employeeNumber: String?, password: String? = null) {
        createEmployee(fullName, email, phoneNumber, employeeNumber, password) { request ->
            repository.createWorker(request)
        }
    }

    fun deleteSupervisorProfile(profileId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteSupervisorProfile(profileId)
                loadEmployees()
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = EmployeesUiState.Error(e.message ?: "Failed to delete supervisor")
            }
        }
    }

    fun deleteWorkerProfile(profileId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteWorkerProfile(profileId)
                loadEmployees()
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = EmployeesUiState.Error(e.message ?: "Failed to delete worker")
            }
        }
    }

    fun updateEmployee(
        accountId: String,
        profileId: String,
        fullName: String,
        phoneNumber: String?,
        employeeNumber: String?,
        isSupervisor: Boolean
    ) {
        viewModelScope.launch {
            repository.updateEmployee(
                accountId = accountId,
                profileId = profileId,
                fullName = fullName,
                phoneNumber = phoneNumber,
                employeeNumber = employeeNumber,
                isSupervisor = isSupervisor
            ).fold(
                onSuccess = { loadEmployees() },
                onFailure = { e ->
                    _uiState.value = EmployeesUiState.Error(e.message ?: "Failed to update employee")
                }
            )
        }
    }

    private fun createEmployee(
        fullName: String,
        email: String,
        phoneNumber: String?,
        employeeNumber: String?,
        password: String?,
        create: suspend (CreateEmployeeRequest) -> ProvisionResult<*>
    ) {
        if (_saveState.value is EmployeeSaveState.Saving) return

        viewModelScope.launch {
            _saveState.value = EmployeeSaveState.Saving

            val request = CreateEmployeeRequest(
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber,
                employeeNumber = employeeNumber,
                farmId = CurrentSession.farmId,
                password = password
            )

            when (val result = create(request)) {
                is ProvisionResult.Success -> {
                    _saveState.value = EmployeeSaveState.Saved
                    loadEmployees()
                }

                is ProvisionResult.Failure -> {
                    _saveState.value = EmployeeSaveState.Error(result.message)
                }
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = EmployeeSaveState.Idle
    }
}
