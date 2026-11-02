package com.example.farmproject.ui.dashboard.supervisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.session.CurrentSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(
    viewModel: SupervisorDashboardViewModel = viewModel(),
    onReviewRequestsClick: () -> Unit = {},
    onAttendanceLogClick: () -> Unit = {},
    onActivitiesClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onMachineryClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onYieldsClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val isManager = CurrentSession.isManager()
    val hasAttendanceDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.ATTENDANCE)
    val hasActivitiesDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.ACTIVITIES_RESOURCES)
    val hasExpenseDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.EXPENSE)
    val hasYieldDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.YIELD_HARVEST)

    val softCreamBg = Color(0xFFF8F6EF)

    Scaffold(
        containerColor = softCreamBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // HERO HEADER
            item {
                SupervisorHeaderHero(
                    dutyLabel = state.dutyLabel,
                    teamCount = state.teamCount,
                    presentCount = state.presentCount,
                    pendingCount = state.pendingCount
                )
            }

            // MY ATTENDANCE CARD
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SupervisorAttendanceCard(
                        statusText = state.myClockStatusText,
                        isClockedIn = state.isClockedInToday,
                        onClockIn = { viewModel.clockInSelf() }
                    )
                }
            }

            // CLOCK-IN REQUESTS (ATTENDANCE SUPERVISOR)
            if (hasAttendanceDuty) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clock-in Requests",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1D1A)
                            )

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFF8E1), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${state.pendingRequests.size} pending",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }

                        ClockInRequestsCard(
                            requests = state.pendingRequests,
                            onApprove = { viewModel.approveRequest(it) },
                            onReject = { viewModel.rejectRequest(it) }
                        )
                    }
                }
            }

            // QUICK ACTIONS (DYNAMIC BASED ON ASSIGNED DUTIES)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1D1A)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasAttendanceDuty) {
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.Check,
                                    label = "Review Requests",
                                    onClick = onReviewRequestsClick
                                )
                            }
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Attendance Log",
                                    onClick = onAttendanceLogClick
                                )
                            }
                        }

                        if (hasActivitiesDuty) {
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    label = "Activities",
                                    onClick = onActivitiesClick
                                )
                            }
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.Inventory2,
                                    label = "Resources",
                                    onClick = onInventoryClick
                                )
                            }
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.PrecisionManufacturing,
                                    label = "Machinery",
                                    onClick = onMachineryClick
                                )
                            }
                        }

                        if (hasExpenseDuty) {
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.AttachMoney,
                                    label = "Add Expense",
                                    onClick = onExpensesClick
                                )
                            }
                        }

                        if (hasYieldDuty) {
                            item {
                                SupervisorQuickActionButton(
                                    icon = Icons.Default.Agriculture,
                                    label = "Record Yield",
                                    onClick = onYieldsClick
                                )
                            }
                        }

                        item {
                            SupervisorQuickActionButton(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                label = "Team Chat",
                                onClick = onChatClick
                            )
                        }
                    }
                }
            }

            // TODAY'S ACTIVITIES (ACTIVITIES & RESOURCES SUPERVISOR)
            if (hasActivitiesDuty) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Today's Activities",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
                        )

                        TodayActivitiesCard(
                            activities = state.todayActivities,
                            onClick = onActivitiesClick
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SupervisorHeaderHero(
    dutyLabel: String,
    teamCount: Int,
    presentCount: Int,
    pendingCount: Int
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B5E20),
            Color(0xFF277A3C)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = gradientBrush,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Supervisor Dashboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = dutyLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeaderStatCard(modifier = Modifier.weight(1f), value = "$teamCount", label = "Team")
                HeaderStatCard(modifier = Modifier.weight(1f), value = "$presentCount", label = "Present")
                HeaderStatCard(modifier = Modifier.weight(1f), value = "$pendingCount", label = "Pending")
            }
        }
    }
}

@Composable
private fun HeaderStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF2D6A3E),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SupervisorAttendanceCard(
    statusText: String,
    isClockedIn: Boolean,
    onClockIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Attendance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = Color(0xFF6E7068)
                )
            }

            Button(
                onClick = onClockIn,
                enabled = !isClockedIn,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20),
                    disabledContainerColor = Color(0xFFE8F5E9),
                    disabledContentColor = Color(0xFF2E7D32)
                )
            ) {
                Text(
                    text = if (isClockedIn) "Clocked In" else "Clock In",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ClockInRequestsCard(
    requests: List<PendingClockRequestUi>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending clock-in requests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                requests.forEach { req ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = req.workerName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1D1A)
                            )
                            Text(
                                text = req.codeAndDateText,
                                fontSize = 12.sp,
                                color = Color(0xFF6E7068)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onReject(req.requestId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8D6E63))
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onApprove(req.requestId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Text("Approve", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupervisorQuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = Color(0xFFE8F5E9),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF1B5E20),
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C312D)
        )
    }
}

@Composable
private fun TodayActivitiesCard(
    activities: List<FarmActivityDisplay>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activities.isNotEmpty()) {
                activities.forEach { act ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${act.displayTitle} · ${act.divisionName ?: "Division A"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
                        )
                        Text(
                            text = "${act.assignedToName} worker",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7068)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities assigned for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
