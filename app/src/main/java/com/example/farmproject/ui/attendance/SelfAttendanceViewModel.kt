package com.example.farmproject.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.AttendanceRecord
import com.example.farmproject.data.model.ClockRequest
import com.example.farmproject.data.repository.AttendanceRepository
import com.example.farmproject.data.repository.ClockRequestRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SelfAttendanceUiState {
    data object Loading : SelfAttendanceUiState
    data class Success(
        val todayRecord: AttendanceRecord?,
        val pendingClockRequest: ClockRequest?,
        val history: List<AttendanceRecord>,
        val name: String,
        val role: String
    ) : SelfAttendanceUiState
    data class Error(val message: String) : SelfAttendanceUiState
}

class SelfAttendanceViewModel(
    private val repository: AttendanceRepository = AttendanceRepository(),
    private val clockRequestRepository: ClockRequestRepository = ClockRequestRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SelfAttendanceUiState>(SelfAttendanceUiState.Loading)
    val uiState: StateFlow<SelfAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadMyAttendance()
    }

    fun loadMyAttendance() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SelfAttendanceUiState.Loading
            try {
                val farmId = CurrentSession.farmId
                val todayStr = LocalDate.now().toString()
                val history = repository.getMyAttendanceHistory(farmId)
                val todayRecord = repository.getMyAttendanceToday() ?: history.find { it.workDate == todayStr }
                val latestClockRequest = clockRequestRepository.getMyLatestRequest().getOrNull()

                _uiState.value = SelfAttendanceUiState.Success(
                    todayRecord = todayRecord,
                    pendingClockRequest = latestClockRequest,
                    history = history,
                    name = CurrentSession.fullName ?: "My Account",
                    role = CurrentSession.userRole ?: "Employee"
                )
            } catch (e: Exception) {
                _uiState.value = SelfAttendanceUiState.Error(e.message ?: "Failed to load your attendance record")
            }
        }
    }

    fun clockInSelf() {
        viewModelScope.launch(Dispatchers.IO) {
            val farmId = CurrentSession.farmId
            val isWorker = CurrentSession.isWorker() || CurrentSession.userRole?.lowercase() == "worker"

            val result: Result<Unit> = if (isWorker) {
                clockRequestRepository.requestClockIn().map { }
            } else {
                repository.clockInSelf(farmId).map { }
            }

            if (result.isSuccess) {
                loadMyAttendance()
            } else {
                _uiState.value = SelfAttendanceUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to request clock in"
                )
            }
        }
    }

    fun clockOutSelf(attendanceId: String? = null, workerProfileId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.clockOut(
                attendanceId = attendanceId,
                workerProfileId = workerProfileId
            )
            if (result.isSuccess) {
                loadMyAttendance()
            } else {
                _uiState.value = SelfAttendanceUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to clock out"
                )
            }
        }
    }

    fun markAway(status: String, notes: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.markAway(status, notes)
            if (result.isSuccess) {
                loadMyAttendance()
            } else {
                _uiState.value = SelfAttendanceUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to mark status"
                )
            }
        }
    }

    fun undoAway() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.undoAway()
            if (result.isSuccess) {
                loadMyAttendance()
            } else {
                _uiState.value = SelfAttendanceUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to reset status"
                )
            }
        }
    }
}
