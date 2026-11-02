package com.example.farmproject.ui.activities.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.Division
import com.example.farmproject.data.model.EmployeeDisplay
import com.example.farmproject.data.model.FarmActivity
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.model.Machinery
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.repository.DivisionRepository
import com.example.farmproject.data.repository.EmployeeRepository
import com.example.farmproject.data.repository.FarmActivityRepository
import com.example.farmproject.data.repository.MachineryRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FarmActivitiesUiState {
    data object Loading : FarmActivitiesUiState
    data class Success(val activities: List<FarmActivityDisplay>) : FarmActivitiesUiState
    data class Error(val message: String) : FarmActivitiesUiState
}

sealed interface FarmActivitySaveState {
    data object Idle : FarmActivitySaveState
    data object Saving : FarmActivitySaveState
    data object Saved : FarmActivitySaveState
    data class Error(val message: String) : FarmActivitySaveState
}

data class FarmActivityFormOptions(
    val activityTypeNames: List<String> = emptyList(),
    val workers: List<EmployeeDisplay> = emptyList(),
    val supervisors: List<EmployeeDisplay> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val machinery: List<Machinery> = emptyList()
)

class FarmActivitiesViewModel(
    private val activityRepository: FarmActivityRepository = FarmActivityRepository(),
    private val employeeRepository: EmployeeRepository = EmployeeRepository(),
    private val divisionRepository: DivisionRepository = DivisionRepository(),
    private val machineryRepository: MachineryRepository = MachineryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FarmActivitiesUiState>(FarmActivitiesUiState.Loading)
    val uiState: StateFlow<FarmActivitiesUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<FarmActivitySaveState>(FarmActivitySaveState.Idle)
    val saveState: StateFlow<FarmActivitySaveState> = _saveState.asStateFlow()

    private val _formOptions = MutableStateFlow(FarmActivityFormOptions())
    val formOptions: StateFlow<FarmActivityFormOptions> = _formOptions.asStateFlow()

    private var activityTypeIdsByName: Map<String, String> = emptyMap()

    init {
        loadActivities()
        loadFormOptions()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = FarmActivitiesUiState.Loading
            try {
                val farmId = CurrentSession.ensureFarmId(SupabaseClientProvider.client)
                val activities = activityRepository.getFarmActivitiesForFarm(farmId)
                _uiState.value = FarmActivitiesUiState.Success(activities)
            } catch (e: Exception) {
                _uiState.value = FarmActivitiesUiState.Error(e.message ?: "Failed to load activities")
            }
        }
    }

    fun loadFormOptions() {
        viewModelScope.launch {
            try {
                val farmId = CurrentSession.ensureFarmId(SupabaseClientProvider.client)
                val activityTypes = activityRepository.getActivityTypes()
                val workers = employeeRepository.getWorkersForFarm(farmId)
                val supervisors = employeeRepository.getSupervisorsForFarm(farmId)
                val divisions = divisionRepository.getDivisionsForFarm(farmId)
                val machinery = machineryRepository.getMachineryForFarm(farmId)

                activityTypeIdsByName = activityTypes
                    .filter { !it.activityTypeId.isNullOrBlank() }
                    .associate { it.name to it.activityTypeId!! }

                val defaultTypes = listOf(
                    "Planting",
                    "Transplanting",
                    "Spraying",
                    "Irrigation",
                    "Fertilizing",
                    "Harvesting",
                    "Maintenance",
                    "Weeding",
                    "Land Preparation"
                )

                val combinedTypeNames = (defaultTypes + activityTypeIdsByName.keys).distinct()

                _formOptions.value = FarmActivityFormOptions(
                    activityTypeNames = combinedTypeNames,
                    workers = workers,
                    supervisors = supervisors,
                    divisions = divisions,
                    machinery = machinery
                )
            } catch (_: Exception) {
                _formOptions.value = FarmActivityFormOptions(
                    activityTypeNames = listOf(
                        "Planting",
                        "Transplanting",
                        "Spraying",
                        "Irrigation",
                        "Fertilizing",
                        "Harvesting",
                        "Maintenance"
                    )
                )
            }
        }
    }

    fun addActivity(
        activityTypeName: String,
        activityName: String? = null,
        activityDate: String,
        status: String = "Pending",
        notes: String?,
        assignedRole: String = "Worker",
        workerProfileId: String? = null,
        supervisorProfileId: String? = null,
        divisionId: String? = null,
        quantity: Double? = null,
        quantityUnit: String? = null,
        equipmentNeeded: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = FarmActivitySaveState.Saving
            try {
                val farmId = CurrentSession.ensureFarmId(SupabaseClientProvider.client)
                val activityTypeId = activityTypeIdsByName[activityTypeName] ?: activityTypeName.ifBlank { "General Activity" }

                val newActivity = FarmActivity(
                    activityTypeId = activityTypeId,
                    farmId = farmId,
                    divisionId = divisionId,
                    workerProfileId = workerProfileId,
                    supervisorProfileId = supervisorProfileId,
                    activityDate = activityDate,
                    status = status.ifBlank { "Pending" },
                    method = if (workerProfileId != null || supervisorProfileId != null) null else "Role: $assignedRole",
                    notes = notes,
                    quantity = quantity,
                    quantityUnit = quantityUnit,
                    activityName = activityName,
                    equipmentNeeded = equipmentNeeded
                )

                activityRepository.insertFarmActivity(newActivity)

                _saveState.value = FarmActivitySaveState.Saved
                loadActivities()
            } catch (e: Exception) {
                _saveState.value = FarmActivitySaveState.Error(e.message ?: "Failed to save activity")
            }
        }
    }

    fun updateActivity(
        activityId: String,
        activityTypeName: String,
        activityName: String? = null,
        activityDate: String,
        notes: String?,
        assignedRole: String = "Worker",
        workerProfileId: String? = null,
        supervisorProfileId: String? = null,
        divisionId: String? = null,
        quantity: Double? = null,
        quantityUnit: String? = null,
        equipmentNeeded: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = FarmActivitySaveState.Saving
            try {
                val farmId = CurrentSession.ensureFarmId(SupabaseClientProvider.client)
                val activityTypeId = activityTypeIdsByName[activityTypeName] ?: activityTypeName.ifBlank { "General Activity" }

                val existing = activityRepository.getFarmActivityById(activityId)
                val base = existing ?: FarmActivity(activityId = activityId, farmId = farmId, activityDate = activityDate)

                val updatedActivity = base.copy(
                    activityTypeId = activityTypeId,
                    divisionId = divisionId,
                    workerProfileId = workerProfileId,
                    supervisorProfileId = supervisorProfileId,
                    activityDate = activityDate,
                    method = if (workerProfileId != null || supervisorProfileId != null) null else "Role: $assignedRole",
                    notes = notes,
                    quantity = quantity,
                    quantityUnit = quantityUnit,
                    activityName = activityName,
                    equipmentNeeded = equipmentNeeded
                )

                activityRepository.updateFarmActivity(updatedActivity)
                _saveState.value = FarmActivitySaveState.Saved
                loadActivities()
            } catch (e: Exception) {
                _saveState.value = FarmActivitySaveState.Error(e.message ?: "Failed to update activity")
            }
        }
    }

    fun assignActivity(activityId: String, isSupervisor: Boolean, profileId: String) {
        viewModelScope.launch {
            try {
                activityRepository.assignFarmActivity(
                    activityId = activityId,
                    isSupervisor = isSupervisor,
                    profileId = profileId
                )
                loadActivities()
            } catch (e: Exception) {
                _uiState.value = FarmActivitiesUiState.Error(e.message ?: "Failed to assign activity")
            }
        }
    }

    fun startActivity(activityId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FarmActivitiesUiState.Success) {
                val updatedList = currentState.activities.map { display ->
                    if (display.activity.activityId == activityId) {
                        display.copy(activity = display.activity.copy(status = "in_progress"))
                    } else display
                }
                _uiState.value = FarmActivitiesUiState.Success(updatedList)
            }

            try {
                activityRepository.startFarmActivity(activityId)
                loadActivities()
            } catch (e: Exception) {
                _uiState.value = FarmActivitiesUiState.Error(e.message ?: "Failed to start activity")
            }
        }
    }

    fun completeActivity(activityId: String, notes: String? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FarmActivitiesUiState.Success) {
                val updatedList = currentState.activities.map { display ->
                    if (display.activity.activityId == activityId) {
                        display.copy(activity = display.activity.copy(status = "completed"))
                    } else display
                }
                _uiState.value = FarmActivitiesUiState.Success(updatedList)
            }

            try {
                activityRepository.completeFarmActivity(activityId, notes)
                loadActivities()
            } catch (e: Exception) {
                _uiState.value = FarmActivitiesUiState.Error(e.message ?: "Failed to complete activity")
            }
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            try {
                activityRepository.deleteFarmActivity(activityId)
                loadActivities()
            } catch (e: Exception) {
                _uiState.value = FarmActivitiesUiState.Error(e.message ?: "Failed to delete activity")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = FarmActivitySaveState.Idle
    }
}
