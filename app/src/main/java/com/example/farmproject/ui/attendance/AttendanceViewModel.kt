package com.example.farmproject.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.AttendanceDisplay
import com.example.farmproject.data.repository.AttendanceRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface AttendanceUiState {
    data object Loading : AttendanceUiState
    data class Success(
        val workDate: String,
        val items: List<AttendanceDisplay>,
        val presentCount: Int,
        val lateCount: Int,
        val absentCount: Int,
        val leaveCount: Int,
    ) : AttendanceUiState
    data class Error(val message: String) : AttendanceUiState
}

class AttendanceViewModel(
    private val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var currentDate: LocalDate = LocalDate.now()

    init {
        loadAttendance()
    }

    fun loadAttendance() {
        val dateStr = currentDate.toString()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AttendanceUiState.Loading
            try {
                val farmId = CurrentSession.farmId
                val displays = repository.getAttendanceForDate(farmId, dateStr)

                val present = displays.count { it.record?.status?.equals("present", ignoreCase = true) == true }
                val late = displays.count { it.record?.status?.equals("late", ignoreCase = true) == true }
                val absent = displays.count { it.record?.status?.equals("absent", ignoreCase = true) == true }
                val leave = displays.count {
                    val st = it.record?.status?.lowercase().orEmpty()
                    st == "on_leave" || st == "on leave"
                }

                _uiState.value = AttendanceUiState.Success(
                    workDate = dateStr,
                    items = displays,
                    presentCount = present,
                    lateCount = late,
                    absentCount = absent,
                    leaveCount = leave,
                )
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Error(e.message ?: "Failed to load attendance records")
            }
        }
    }

    @Suppress("unused")
    fun selectDate(date: LocalDate) {
        currentDate = date
        loadAttendance()
    }

    fun selectPreviousDay() {
        currentDate = currentDate.minusDays(1)
        loadAttendance()
    }

    fun selectNextDay() {
        currentDate = currentDate.plusDays(1)
        loadAttendance()
    }

    @Suppress("DEPRECATION", "unused")
    fun markAttendance(workerProfileId: String, status: String, notes: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val farmId = CurrentSession.farmId
            val dateStr = currentDate.toString()

            val result = repository.markAttendance(
                farmId = farmId,
                workerProfileId = workerProfileId,
                workDate = dateStr,
                status = status,
                notes = notes
            )

            if (result.isSuccess) {
                loadAttendance()
            }
        }
    }

    @Suppress("DEPRECATION", "unused")
    fun clockInWorker(workerProfileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val farmId = CurrentSession.farmId
            val dateStr = currentDate.toString()

            val result = repository.clockIn(
                farmId = farmId,
                workerProfileId = workerProfileId,
                workDate = dateStr
            )

            if (result.isSuccess) {
                loadAttendance()
            }
        }
    }

    @Suppress("unused")
    fun clockOutWorker(attendanceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.clockOut(attendanceId)

            if (result.isSuccess) {
                loadAttendance()
            }
        }
    }
}
