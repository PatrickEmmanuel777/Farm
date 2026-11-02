package com.example.farmproject.ui.activities.worker

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.FarmActivity
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.session.CurrentSession
import com.example.farmproject.ui.activities.manager.FarmActivitiesUiState
import com.example.farmproject.ui.activities.manager.FarmActivitiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFarmActivitiesScreen(
    viewModel: FarmActivitiesViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onActivityClick: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Assigned, 1 = In Progress, 2 = Completed
    val uiState by viewModel.uiState.collectAsState()

    val workerUserId = CurrentSession.userId.orEmpty()
    val workerName = CurrentSession.fullName?.trim().orEmpty()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Worker Activities",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Assigned, In Progress & Completed Tasks",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Assigned") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("In Progress") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Completed") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is FarmActivitiesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is FarmActivitiesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadActivities() }) {
                            Text("Retry")
                        }
                    }
                }

                is FarmActivitiesUiState.Success -> {
                    val allActivities = state.activities

                    // Filter activities assigned to worker
                    val workerActivities = allActivities.filter { display ->
                        val act = display.activity
                        (act.assignedTo != null && act.assignedTo == workerUserId) ||
                        (act.workerProfileId != null && act.workerProfileId == workerUserId) ||
                        (display.assignedToName != null && display.assignedToName.equals(workerName, ignoreCase = true)) ||
                        (act.method != null && act.method.contains("Worker", ignoreCase = true)) ||
                        display.assignedToName.isNullOrBlank() ||
                        display.assignedToName.equals("Worker", ignoreCase = true) ||
                        display.assignedToName.equals("Unassigned", ignoreCase = true)
                    }

                    val filteredActivities = workerActivities.filter { display ->
                        val st = display.activity.safeStatus
                        val isInProgress = st.equals("In Progress", ignoreCase = true) ||
                                st.equals("in_progress", ignoreCase = true) ||
                                st.equals("Ongoing", ignoreCase = true)
                        val isCompleted = st.equals("Completed", ignoreCase = true) ||
                                st.equals("completed", ignoreCase = true) ||
                                st.equals("Done", ignoreCase = true)

                        when (selectedTab) {
                            0 -> !isInProgress && !isCompleted // Assigned / Pending
                            1 -> isInProgress                   // In Progress
                            2 -> isCompleted                    // Completed
                            else -> true
                        }
                    }

                    val tabTitle = when (selectedTab) {
                        0 -> "Assigned Tasks"
                        1 -> "In Progress Tasks"
                        2 -> "Completed Activities"
                        else -> "Tasks"
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            WorkerActivitySummaryCard(
                                totalActivities = filteredActivities.size,
                                labelText = "$tabTitle count"
                            )
                        }

                        item {
                            Text(
                                text = tabTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (filteredActivities.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No $tabTitle found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredActivities) { activityDisplay ->
                                val activityId = activityDisplay.activity.activityId.orEmpty()

                                WorkerActivityCard(
                                    activityDisplay = activityDisplay,
                                    tabIndex = selectedTab,
                                    onClick = {
                                        if (activityId.isNotBlank()) {
                                            onActivityClick(activityId)
                                        }
                                    },
                                    onStartClick = if (selectedTab == 0 && activityId.isNotBlank()) {
                                        {
                                            viewModel.startActivity(activityId)
                                            selectedTab = 1
                                        }
                                    } else null,
                                    onCompleteClick = if (selectedTab == 1 && activityId.isNotBlank()) {
                                        {
                                            viewModel.completeActivity(activityId)
                                            selectedTab = 2
                                        }
                                    } else null
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerActivitySummaryCard(
    totalActivities: Int,
    labelText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "$totalActivities",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerActivityCard(
    activityDisplay: FarmActivityDisplay,
    tabIndex: Int,
    onClick: () -> Unit,
    onStartClick: (() -> Unit)? = null,
    onCompleteClick: (() -> Unit)? = null
) {
    val activity = activityDisplay.activity

    val statusBg = when {
        tabIndex == 1 -> Color(0xFFFFF8E1)
        tabIndex == 2 -> Color(0xFFE8F5E9)
        else -> Color(0xFFEFEBE9)
    }

    val statusTint = when {
        tabIndex == 1 -> Color(0xFFF57F17)
        tabIndex == 2 -> Color(0xFF2E7D32)
        else -> Color(0xFF5D4037)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activityDisplay.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(statusBg, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activity.safeStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTint
                    )
                }
            }

            Text(
                text = activityDisplay.activityTypeName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!activityDisplay.divisionName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activityDisplay.divisionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Due: ${activity.safeActivityDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onStartClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Activity", fontWeight = FontWeight.Bold)
                }
            }

            if (onCompleteClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("Complete Activity", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
