package com.example.farmproject.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.AttendanceRecord
import com.example.farmproject.data.model.ClockRequest
import com.example.farmproject.data.model.ClockRequestStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfAttendanceScreen(
    viewModel: SelfAttendanceViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "My Attendance & Clock In",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily field clock-in and attendance history",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SelfAttendanceUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SelfAttendanceUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyAttendance() }) {
                            Text("Retry")
                        }
                    }
                }

                is SelfAttendanceUiState.Success -> {
                    val todayRecord = state.todayRecord
                    val pendingClockRequest = state.pendingClockRequest
                    val history = state.history

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Today's Clock In/Out Card
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TodayClockInCard(
                                name = state.name,
                                role = state.role,
                                record = todayRecord,
                                pendingClockRequest = pendingClockRequest,
                                onClockIn = { viewModel.clockInSelf() },
                                onClockOut = {
                                    viewModel.clockOutSelf(
                                        attendanceId = todayRecord?.attendanceId,
                                        workerProfileId = todayRecord?.workerProfileId
                                    )
                                },
                                onMarkAbsent = { viewModel.markAway("absent") },
                                onMarkLeave = { viewModel.markAway("on_leave") },
                                onUndoAway = { viewModel.undoAway() }
                            )
                        }

                        item {
                            Text(
                                text = "My Attendance History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (history.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No attendance history recorded yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(history) { record ->
                                SelfAttendanceHistoryCard(record = record)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayClockInCard(
    name: String,
    role: String,
    record: AttendanceRecord?,
    pendingClockRequest: ClockRequest?,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMarkLeave: () -> Unit,
    onUndoAway: () -> Unit
) {
    val todayDateStr = LocalDate.now().toString()

    val isPendingApproval = record == null && pendingClockRequest?.status == ClockRequestStatus.PENDING
    val isRejected = record == null && pendingClockRequest?.status == ClockRequestStatus.REJECTED

    val statusText = when {
        record != null -> record.displayStatus
        isPendingApproval -> "Pending Approval"
        isRejected -> "Rejected"
        else -> "Not Clocked In"
    }

    val isAway = statusText.lowercase() == "absent" || statusText.lowercase() == "on leave"

    val statusBgColor = when (statusText.lowercase()) {
        "present", "clocked in" -> Color(0xFFE8F5E9)
        "pending approval" -> Color(0xFFFFF8E1)
        "late" -> Color(0xFFFFF3E0)
        "absent", "rejected" -> Color(0xFFFFEBEE)
        "on leave" -> Color(0xFFF3E5F5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusTextColor = when (statusText.lowercase()) {
        "present", "clocked in" -> Color(0xFF2E7D32)
        "pending approval" -> Color(0xFFF57F17)
        "late" -> Color(0xFFE65100)
        "absent", "rejected" -> Color(0xFFC62828)
        "on leave" -> Color(0xFF6A1B9A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$role • Today ($todayDateStr)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusBgColor, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val inTime = record?.checkIn?.take(16)?.replace("T", " ") ?: "--:--"
            val outTime = record?.checkOut?.take(16)?.replace("T", " ") ?: "--:--"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Check In: $inTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Check Out: $outTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (isAway) {
                    Button(
                        onClick = onUndoAway,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clock In Instead")
                    }
                } else if (isPendingApproval) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFFFFF8E1),
                            disabledContentColor = Color(0xFFF57F17)
                        )
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pending Approval", fontWeight = FontWeight.Bold)
                    }
                } else if (record?.checkIn == null) {
                    Button(
                        onClick = onClockIn,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clock In Now")
                    }
                } else if (record.checkOut == null) {
                    Button(
                        onClick = onClockOut,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clock Out")
                    }
                }
            }

            if (record?.checkIn == null && !isAway) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPendingApproval) "Clock-in requested. Waiting for approval." else "Not working today?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    if (!isPendingApproval) {
                        Row {
                            OutlinedButton(
                                onClick = onMarkAbsent,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Absent", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = onMarkLeave,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("On Leave", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfAttendanceHistoryCard(
    record: AttendanceRecord
) {
    val statusText = record.displayStatus

    val statusBgColor = when (statusText.lowercase()) {
        "present" -> Color(0xFFE8F5E9)
        "late" -> Color(0xFFFFF3E0)
        "absent" -> Color(0xFFFFEBEE)
        "on leave" -> Color(0xFFF3E5F5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusTextColor = when (statusText.lowercase()) {
        "present" -> Color(0xFF2E7D32)
        "late" -> Color(0xFFE65100)
        "absent" -> Color(0xFFC62828)
        "on leave" -> Color(0xFF6A1B9A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val inTime = record.checkIn?.take(16)?.replace("T", " ") ?: "--:--"
    val outTime = record.checkOut?.take(16)?.replace("T", " ") ?: "--:--"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.workDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "In: $inTime • Out: $outTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .background(statusBgColor, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor
                )
            }
        }
    }
}
