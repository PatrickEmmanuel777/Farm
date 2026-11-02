package com.example.farmproject.ui.dashboard.supervisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.repository.AccountSignUpResult
import com.example.farmproject.data.repository.AttendanceRepository
import com.example.farmproject.data.repository.ClockRequestRepository
import com.example.farmproject.data.repository.EmployeeRepository
import com.example.farmproject.data.repository.FarmActivityRepository
import com.example.farmproject.data.repository.FarmRepository
import com.example.farmproject.data.repository.SupervisorDutyRepository
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PendingClockRequestUi(
    val requestId: String,
    val workerName: String,
    val codeAndDateText: String
)

data class SupervisorDashboardUiState(
    val isLoading: Boolean = true,
    val supervisorName: String = "Supervisor",
    val dutyLabel: String = "Attendance Supervisor",
    val teamCount: Int = 0,
    val presentCount: Int = 0,
    val pendingCount: Int = 0,
    val myClockStatusText: String = "Not clocked in today",
    val isClockedInToday: Boolean = false,
    val pendingRequests: List<PendingClockRequestUi> = emptyList(),
    val todayActivities: List<FarmActivityDisplay> = emptyList(),
    val message: String? = null
)

class SupervisorDashboardViewModel(
    private val clockRequestRepository: ClockRequestRepository = ClockRequestRepository(),
    private val supervisorDutyRepository: SupervisorDutyRepository = SupervisorDutyRepository(),
    private val employeeRepository: EmployeeRepository = EmployeeRepository(),
    private val activityRepository: FarmActivityRepository = FarmActivityRepository(),
    private val attendanceRepository: AttendanceRepository = AttendanceRepository(),
    private val farmRepository: FarmRepository = FarmRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorDashboardUiState())
    val uiState: StateFlow<SupervisorDashboardUiState> = _uiState.asStateFlow()

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
                var farmId = CurrentSession.farmId
                val name = CurrentSession.fullName?.takeIf { it.isNotBlank() } ?: "Supervisor"
                val myUserId = CurrentSession.userId.orEmpty()

                if (farmId.isBlank() && myUserId.isNotBlank()) {
                    val farm = farmRepository.getFarmForUser(myUserId)
                    if (farm?.farmId != null && farm.farmId.isNotBlank()) {
                        CurrentSession.farmId = farm.farmId
                        farmId = farm.farmId
                    }
                }

                coroutineScope {
                    val dutiesDeferred = async { supervisorDutyRepository.getMyDuties().getOrDefault(emptySet()) }
                    val rawPendingDeferred = async { clockRequestRepository.listPendingRequestsRaw().getOrDefault(emptyList()) }
                    val directPendingDeferred = async { clockRequestRepository.listPendingRequestsDirect().getOrDefault(emptyList()) }
                    val pendingListDeferred = async { clockRequestRepository.listPending(farmId).getOrDefault(emptyList()) }
                    val attendanceBoardDeferred = async { clockRequestRepository.getFarmAttendanceBoard(farmId).getOrDefault(emptyList()) }
                    val workersDeferred = async { employeeRepository.getWorkersForFarm(farmId) }
                    val activitiesDeferred = async { activityRepository.getFarmActivitiesForFarm(farmId) }
                    val myAttendanceDeferred = async { attendanceRepository.getMyAttendanceToday() }

                    val duties = dutiesDeferred.await()
                    if (duties.isNotEmpty()) {
                        CurrentSession.supervisorDuties = duties
                    }

                    val rawPending = rawPendingDeferred.await()
                    val directPending = directPendingDeferred.await()
                    val pendingFromRpc = pendingListDeferred.await()
                    val board = attendanceBoardDeferred.await()
                    val workers = workersDeferred.await()
                    val activities = activitiesDeferred.await()
                    val myTodayRecord = myAttendanceDeferred.await()

                    val missingAccIds = (rawPending.map { it.requesterAccountId } + directPending.map { it.requesterAccountId })
                        .filter { id -> id.isNotBlank() && workers.none { it.accountId == id } }
                        .distinct()

                    val fallbackAccounts = if (missingAccIds.isNotEmpty()) {
                        runCatching {
                            SupabaseClientProvider.client.from("account")
                                .select { filter { isIn("account_id", missingAccIds) } }
                                .decodeList<AccountSignUpResult>()
                        }.getOrDefault(emptyList())
                    } else emptyList()

                    val dutyTitle = CurrentSession.supervisorDuties.joinToString(" · ") { it.label }
                        .ifBlank { "Attendance Supervisor" }

                    val pendingMap = mutableMapOf<String, PendingClockRequestUi>()

                    // 0. Raw pending (guaranteed JSON parsing)
                    rawPending.forEach { raw ->
                        val reqId = raw.requestId
                        if (reqId.isNotBlank()) {
                            val workerMatch = workers.find { it.accountId == raw.requesterAccountId }
                            val fallbackAcc = fallbackAccounts.find { it.accountId == raw.requesterAccountId }
                            val wName = workerMatch?.fullName ?: fallbackAcc?.fullName ?: "Worker"
                            val empCode = workerMatch?.employeeNumber ?: fallbackAcc?.email?.substringBefore("@") ?: "Worker"
                            val timeStr = raw.requestedAt.takeLast(8).take(5).ifBlank { "Today" }
                            pendingMap[reqId] = PendingClockRequestUi(
                                requestId = reqId,
                                workerName = wName,
                                codeAndDateText = "$empCode · $timeStr"
                            )
                        }
                    }

                    // 1. Direct table query
                    directPending.forEach { req ->
                        val reqId = req.requestId
                        if (reqId.isNotBlank() && !pendingMap.containsKey(reqId)) {
                            val workerMatch = workers.find { it.accountId == req.requesterAccountId }
                            val fallbackAcc = fallbackAccounts.find { it.accountId == req.requesterAccountId }
                            val wName = workerMatch?.fullName ?: fallbackAcc?.fullName ?: "Worker"
                            val empCode = workerMatch?.employeeNumber ?: fallbackAcc?.email?.substringBefore("@") ?: "Worker"
                            val timeStr = req.requestedAt.takeLast(8).take(5).ifBlank { "Today" }
                            pendingMap[reqId] = PendingClockRequestUi(
                                requestId = reqId,
                                workerName = wName,
                                codeAndDateText = "$empCode · $timeStr"
                            )
                        }
                    }

                    // 2. Attendance board
                    board.filter { it.hasPendingRequest && !it.pendingRequestId.isNullOrBlank() }.forEach { row ->
                        val reqId = row.pendingRequestId.orEmpty()
                        if (!pendingMap.containsKey(reqId)) {
                            val timeStr = row.clockIn?.takeLast(8)?.take(5) ?: "Today"
                            pendingMap[reqId] = PendingClockRequestUi(
                                requestId = reqId,
                                workerName = row.fullName,
                                codeAndDateText = "${row.employeeCode ?: "Worker"} · $timeStr"
                            )
                        }
                    }

                    // 3. Pending RPC
                    pendingFromRpc.forEach { pending ->
                        val reqId = pending.requestId
                        if (!pendingMap.containsKey(reqId)) {
                            val workerMatch = workers.find { it.accountId == pending.requesterAccountId }
                            val fallbackAcc = fallbackAccounts.find { it.accountId == pending.requesterAccountId }
                            val wName = workerMatch?.fullName ?: fallbackAcc?.fullName ?: "Worker"
                            val empCode = pending.employeeNumber ?: workerMatch?.employeeNumber ?: fallbackAcc?.email?.substringBefore("@") ?: "Worker"
                            val timeStr = pending.requestedAt.takeLast(8).take(5).ifBlank { "Today" }
                            pendingMap[reqId] = PendingClockRequestUi(
                                requestId = reqId,
                                workerName = wName,
                                codeAndDateText = "$empCode · $timeStr"
                            )
                        }
                    }

                    val finalPendingList = pendingMap.values.toList()
                    val presentCnt = board.count { it.status?.lowercase() in listOf("present", "late") }
                    val teamCnt = workers.size
                    val pendingCnt = finalPendingList.size

                    val myTasks = activities.filter {
                        it.activity.supervisorProfileId == myUserId || it.activity.supervisorProfileId != null
                    }.take(3)

                    val myCheckIn = myTodayRecord?.checkIn
                    val isClockedIn = myCheckIn != null
                    val clockStatus = if (myCheckIn != null) {
                        "Clocked in at ${myCheckIn.takeLast(5)}"
                    } else {
                        "Not clocked in today"
                    }

                    _uiState.value = SupervisorDashboardUiState(
                        isLoading = false,
                        supervisorName = name,
                        dutyLabel = dutyTitle,
                        teamCount = teamCnt,
                        presentCount = presentCnt,
                        pendingCount = pendingCnt,
                        myClockStatusText = clockStatus,
                        isClockedInToday = isClockedIn,
                        pendingRequests = finalPendingList,
                        todayActivities = myTasks
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }

    fun clockInSelf() {
        viewModelScope.launch(Dispatchers.IO) {
            val farmId = CurrentSession.farmId
            val result = attendanceRepository.clockInSelf(farmId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(message = "Clocked in successfully.")
                    loadData()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(message = error.message ?: "Clock-in failed.")
                }
            )
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = clockRequestRepository.approve(requestId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(message = "Clock-in request approved.")
                    loadData()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(message = error.message ?: "Approval failed.")
                }
            )
        }
    }

    fun rejectRequest(requestId: String, reason: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = clockRequestRepository.reject(requestId, reason)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(message = "Clock-in request rejected.")
                    loadData()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(message = error.message ?: "Rejection failed.")
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
