package com.example.farmproject.ui.dashboard.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.ClockRequest
import com.example.farmproject.data.model.ClockRequestStatus
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.repository.AttendanceRepository
import com.example.farmproject.data.repository.ChatRepository
import com.example.farmproject.data.repository.ClockRequestRepository
import com.example.farmproject.data.repository.EmployeeRepository
import com.example.farmproject.data.repository.FarmActivityRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WorkerDashboardUiState(
    val isLoading: Boolean = true,
    val workerName: String = "Worker",
    val employeeCode: String = "Worker",
    val formattedDate: String = "Today",
    val clockRequest: ClockRequest? = null,
    val clockStatusText: String = "Not clocked in",
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val leaveCount: Int = 0,
    val todayTasks: List<FarmActivityDisplay> = emptyList(),
    val chatPreviewText: String = "No messages yet",
    val unreadChatCount: Int = 0,
    val message: String? = null
)

class WorkerDashboardViewModel(
    private val clockRequestRepository: ClockRequestRepository = ClockRequestRepository(),
    private val attendanceRepository: AttendanceRepository = AttendanceRepository(),
    private val activityRepository: FarmActivityRepository = FarmActivityRepository(),
    private val employeeRepository: EmployeeRepository = EmployeeRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerDashboardUiState())
    val uiState: StateFlow<WorkerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val currentSuccess = if (!_uiState.value.isLoading) _uiState.value else null
        viewModelScope.launch(Dispatchers.IO) {
            if (currentSuccess == null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                val farmId = CurrentSession.farmId
                val myUserId = CurrentSession.userId.orEmpty()
                val name = CurrentSession.fullName?.takeIf { it.isNotBlank() } ?: "Worker"

                coroutineScope {
                    val latestRequestDeferred = async { clockRequestRepository.getMyLatestRequest().getOrNull() }
                    val todayRecordDeferred = async { attendanceRepository.getMyAttendanceToday() }
                    val historyDeferred = async { attendanceRepository.getMyAttendanceHistory(farmId) }
                    val workersDeferred = async { employeeRepository.getWorkersForFarm(farmId) }
                    val activitiesDeferred = async { activityRepository.getFarmActivitiesForFarm(farmId) }
                    val conversationsDeferred = async { runCatching { chatRepository.conversations() }.getOrDefault(emptyList()) }

                    val latestRequest = latestRequestDeferred.await()
                    val todayRecord = todayRecordDeferred.await()
                    val history = historyDeferred.await()
                    val workers = workersDeferred.await()
                    val activities = activitiesDeferred.await()
                    val conversations = conversationsDeferred.await()

                    val workerMatch = workers.find { it.accountId == myUserId }
                    val myWorkerProfileId = workerMatch?.profileId.orEmpty()
                    val empCode = workerMatch?.employeeNumber?.takeIf { it.isNotBlank() } ?: "Worker"

                    val statusText = when {
                        todayRecord?.checkIn != null -> "Clocked in"
                        todayRecord?.status?.equals("absent", ignoreCase = true) == true -> "Absent"
                        todayRecord?.status?.lowercase() in listOf("on_leave", "on leave") -> "On leave"
                        latestRequest?.status == ClockRequestStatus.PENDING -> "Pending approval"
                        latestRequest?.status == ClockRequestStatus.APPROVED -> "Clocked in"
                        latestRequest?.status == ClockRequestStatus.REJECTED -> "Rejected"
                        else -> "Not clocked in"
                    }

                    val now = LocalDate.now()
                    val currentMonth = now.monthValue
                    val currentYear = now.year

                    val monthHistory = history.filter { rec ->
                        runCatching {
                            val parsed = LocalDate.parse(rec.workDate)
                            parsed.monthValue == currentMonth && parsed.year == currentYear
                        }.getOrDefault(false)
                    }

                    val presentCnt = monthHistory.count {
                        val st = it.status.lowercase()
                        st == "present" || st == "late" || it.checkIn != null
                    }
                    val absentCnt = monthHistory.count { it.status.lowercase() == "absent" }
                    val leaveCnt = monthHistory.count {
                        val st = it.status.lowercase()
                        st == "on_leave" || st == "on leave"
                    }

                    val myTasks = activities.filter { display ->
                        val act = display.activity
                        val st = act.safeStatus
                        val isCompleted = st.equals("Completed", ignoreCase = true) || st.equals("Done", ignoreCase = true)

                        val isAssignedToMe = (myUserId.isNotBlank() && act.assignedTo == myUserId) ||
                                (myWorkerProfileId.isNotBlank() && act.workerProfileId == myWorkerProfileId) ||
                                (myUserId.isNotBlank() && act.workerProfileId == myUserId) ||
                                (display.assignedToName != null && display.assignedToName.equals(name, ignoreCase = true)) ||
                                (act.method != null && act.method.contains("Worker", ignoreCase = true)) ||
                                display.assignedToName.isNullOrBlank() ||
                                display.assignedToName.equals("Worker", ignoreCase = true) ||
                                display.assignedToName.equals("Unassigned", ignoreCase = true)

                        isAssignedToMe && !isCompleted
                    }.take(5)

                    val chatPreview = conversations.firstOrNull()?.lastMessageBody?.ifBlank { null }
                        ?: "Team Chat Available"
                    val unreadCount = conversations.sumOf { it.unreadCount }.toInt()

                    val dateStr = try {
                        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
                    } catch (_: Exception) {
                        "Tuesday, 27 October"
                    }

                    _uiState.value = WorkerDashboardUiState(
                        isLoading = false,
                        workerName = name,
                        employeeCode = empCode,
                        formattedDate = dateStr,
                        clockRequest = latestRequest,
                        clockStatusText = statusText,
                        presentCount = presentCnt,
                        absentCount = absentCnt,
                        leaveCount = leaveCnt,
                        todayTasks = myTasks,
                        chatPreviewText = chatPreview,
                        unreadChatCount = unreadCount
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    fun requestClockIn() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = clockRequestRepository.requestClockIn()
            result.fold(
                onSuccess = {
                    loadData()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(message = error.message ?: "Clock-in request failed")
                }
            )
        }
    }

    fun markAway(status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = attendanceRepository.markAway(status)
            result.fold(
                onSuccess = {
                    loadData()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(message = error.message ?: "Action failed")
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
