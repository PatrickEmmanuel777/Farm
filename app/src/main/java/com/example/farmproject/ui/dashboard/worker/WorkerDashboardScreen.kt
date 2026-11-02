package com.example.farmproject.ui.dashboard.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.FarmActivityDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboardScreen(
    viewModel: WorkerDashboardViewModel = viewModel(),
    onTasksClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
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
                WorkerHeaderHero(
                    dateText = state.formattedDate,
                    workerName = state.workerName,
                    employeeCode = state.employeeCode
                )
            }

            // ATTENDANCE CARD
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    WorkerAttendanceCard(
                        statusText = state.clockStatusText,
                        onClockIn = { viewModel.requestClockIn() },
                        onMarkAbsent = { viewModel.markAway("absent") },
                        onMarkLeave = { viewModel.markAway("on_leave") }
                    )
                }
            }

            // THIS MONTH STATS
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "This Month",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1D1A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MonthlyStatCard(modifier = Modifier.weight(1f), value = "${state.presentCount}", label = "Present")
                        MonthlyStatCard(modifier = Modifier.weight(1f), value = "${state.absentCount}", label = "Absent")
                        MonthlyStatCard(modifier = Modifier.weight(1f), value = "${state.leaveCount}", label = "On leave")
                    }
                }
            }

            // MY TASKS TODAY
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
                            text = "My Tasks Today",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
                        )

                        TextButton(onClick = onTasksClick) {
                            Text(
                                text = "View all",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    TasksTodayCard(
                        tasks = state.todayTasks,
                        onTaskClick = { onTasksClick() }
                    )
                }
            }

            // FARM CHAT
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Farm Chat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1D1A)
                    )

                    FarmChatCard(
                        previewText = state.chatPreviewText,
                        unreadCount = state.unreadChatCount,
                        onClick = onChatClick
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WorkerHeaderHero(
    dateText: String,
    workerName: String,
    employeeCode: String
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = dateText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Text(
                text = "My Day",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$workerName · $employeeCode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

@Composable
private fun WorkerAttendanceCard(
    statusText: String,
    onClockIn: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMarkLeave: () -> Unit
) {
    val statusBg = when {
        statusText.contains("Clocked", ignoreCase = true) -> Color(0xFFE8F5E9)
        statusText.contains("Pending", ignoreCase = true) -> Color(0xFFFFF8E1)
        else -> Color(0xFFEFEBE9)
    }

    val statusTint = when {
        statusText.contains("Clocked", ignoreCase = true) -> Color(0xFF2E7D32)
        statusText.contains("Pending", ignoreCase = true) -> Color(0xFFF57F17)
        else -> Color(0xFF5D4037)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                Box(
                    modifier = Modifier
                        .background(statusBg, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTint
                    )
                }
            }

            Button(
                onClick = onClockIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text(
                    text = "Clock In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkAbsent,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark Absent", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onMarkLeave,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("On Leave", fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "Clock-ins stay pending until your supervisor or the manager approves them.",
                fontSize = 12.sp,
                color = Color(0xFF6E7068),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MonthlyStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1D1A)
            )

            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6E7068)
            )
        }
    }
}

@Composable
private fun TasksTodayCard(
    tasks: List<FarmActivityDisplay>,
    onTaskClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (tasks.isNotEmpty()) {
                tasks.forEach { display ->
                    val statusText = display.activity.safeStatus
                    val statusBg = if (statusText.equals("In Progress", ignoreCase = true)) Color(0xFFFFF8E1) else Color(0xFFE8F5E9)
                    val statusTint = if (statusText.equals("In Progress", ignoreCase = true)) Color(0xFFF57F17) else Color(0xFF2E7D32)

                    TaskRowItem(
                        title = display.displayTitle,
                        subtitle = "${display.divisionName ?: "Division A"}",
                        statusText = statusText,
                        statusBg = statusBg,
                        statusTint = statusTint
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active tasks assigned for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRowItem(
    title: String,
    subtitle: String,
    statusText: String,
    statusBg: Color,
    statusTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1D1A)
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6E7068)
            )
        }

        Box(
            modifier = Modifier
                .background(statusBg, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = statusTint
            )
        }
    }
}

@Composable
private fun FarmChatCard(
    previewText: String,
    unreadCount: Int,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Farm Team",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                Text(
                    text = previewText,
                    fontSize = 12.sp,
                    color = Color(0xFF6E7068)
                )
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1B5E20), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
