package com.example.farmproject.ui.activities.manager

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.FarmActivity
import com.example.farmproject.data.model.FarmActivityDisplay
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFarmActivitiesScreen(
    viewModel: FarmActivitiesViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onActivityClick: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActivities()
    }

    Scaffold(
        topBar = {
            Column {
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
                                text = "Farm Activities",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Track tasks across the farm",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                    }
                )

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pending") },
                        icon = { Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("In Progress") },
                        icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Completed") },
                        icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null) }
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

                    val filteredActivities = when (selectedTab) {
                        1 -> allActivities.filter {
                            val st = it.activity.safeStatus
                            st.equals("In Progress", ignoreCase = true) ||
                            st.equals("in_progress", ignoreCase = true) ||
                            st.equals("in progress", ignoreCase = true) ||
                            st.equals("in-progress", ignoreCase = true) ||
                            st.equals("ongoing", ignoreCase = true)
                        }
                        2 -> allActivities.filter {
                            val st = it.activity.safeStatus
                            st.equals("Completed", ignoreCase = true) ||
                            st.equals("completed", ignoreCase = true) ||
                            st.equals("done", ignoreCase = true) ||
                            st.equals("finished", ignoreCase = true) ||
                            st.equals("closed", ignoreCase = true)
                        }
                        else -> allActivities.filter {
                            val st = it.activity.safeStatus
                            val isInProgress = st.equals("In Progress", ignoreCase = true) ||
                                    st.equals("in_progress", ignoreCase = true) ||
                                    st.equals("in progress", ignoreCase = true) ||
                                    st.equals("in-progress", ignoreCase = true) ||
                                    st.equals("ongoing", ignoreCase = true)
                            val isCompleted = st.equals("Completed", ignoreCase = true) ||
                                    st.equals("completed", ignoreCase = true) ||
                                    st.equals("done", ignoreCase = true) ||
                                    st.equals("finished", ignoreCase = true) ||
                                    st.equals("closed", ignoreCase = true)
                            !isInProgress && !isCompleted
                        }
                    }

                    val overdueCount = allActivities.count {
                        it.activity.status.equals("Overdue", ignoreCase = true)
                    }

                    val tabTitle = when (selectedTab) {
                        1 -> "In Progress Tasks"
                        2 -> "Completed Tasks"
                        else -> "Pending Tasks"
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

                            ActivitySummaryCard(
                                totalActivities = filteredActivities.size,
                                labelText = "$tabTitle count",
                                overdueCount = if (selectedTab == 0) overdueCount else 0
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tabTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                // Only show Add button on the Pending tab (selectedTab == 0)
                                if (selectedTab == 0) {
                                    OutlinedButton(onClick = onAddClick) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Add")
                                    }
                                }
                            }
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
                                        text = "No ${tabTitle.lowercase()} recorded yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredActivities) { activityDisplay ->
                                val activityId = activityDisplay.activity.activityId.orEmpty()
                                FarmActivityCard(
                                    activityDisplay = activityDisplay,
                                    onClick = {
                                        if (activityId.isNotBlank()) onActivityClick(activityId)
                                    },
                                    onStartClick = null,
                                    onCompleteClick = null
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
private fun ActivitySummaryCard(
    totalActivities: Int,
    labelText: String,
    overdueCount: Int
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
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Column {
                    Text(
                        text = totalActivities.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (overdueCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text(
                        text = "$overdueCount activity(ies) overdue",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun calculateDisplayDuration(activity: FarmActivity): String {
    if (activity.durationHours != null && activity.durationHours > 0) {
        val hrs = activity.durationHours
        return if (hrs == 1.0) "1.0 hr" else String.format(Locale.US, "%.1f hrs", hrs)
    }

    val startStr = activity.startedAt ?: activity.startTime ?: activity.createdAt
    val endStr = activity.completedAt ?: activity.endTime ?: activity.updatedAt

    if (!startStr.isNullOrBlank() && !endStr.isNullOrBlank()) {
        try {
            val start = LocalDateTime.parse(startStr)
            val end = LocalDateTime.parse(endStr)
            val mins = Duration.between(start, end).toMinutes()
            if (mins > 0) {
                val hrs = mins / 60.0
                return if (hrs == 1.0) "1.0 hr" else String.format(Locale.US, "%.1f hrs", hrs)
            }
        } catch (_: Exception) {}
    }

    return "1.0 hr"
}

@Composable
private fun FarmActivityCard(
    activityDisplay: FarmActivityDisplay,
    onClick: () -> Unit,
    onStartClick: (() -> Unit)? = null,
    onCompleteClick: (() -> Unit)? = null
) {
    val isOverdue = activityDisplay.activity.status.equals("Overdue", ignoreCase = true)
    val isCompleted = activityDisplay.activity.safeStatus.equals("Completed", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activityDisplay.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = activityDisplay.activityTypeName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                StatusBadge(
                    status = activityDisplay.activity.safeStatus,
                    isOverdue = isOverdue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityInfo(
                    icon = Icons.Default.Person,
                    label = "Assigned to",
                    value = activityDisplay.assignedToName ?: "Unassigned"
                )

                if (isCompleted) {
                    ActivityInfo(
                        icon = Icons.Default.Schedule,
                        label = "Duration",
                        value = calculateDisplayDuration(activityDisplay.activity)
                    )
                } else {
                    ActivityInfo(
                        icon = Icons.Default.CalendarMonth,
                        label = "Due date",
                        value = activityDisplay.activity.safeActivityDate
                    )
                }
            }

            // Begin activity button for Pending tasks
            if (onStartClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Begin activity",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Complete task button for In Progress tasks
            if (onCompleteClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Complete task",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityInfo(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.size(6.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: String,
    isOverdue: Boolean
) {
    val displayStatus = when {
        status.equals("in_progress", ignoreCase = true) || status.equals("in progress", ignoreCase = true) -> "In Progress"
        status.equals("completed", ignoreCase = true) || status.equals("done", ignoreCase = true) -> "Completed"
        status.equals("pending", ignoreCase = true) || status.equals("scheduled", ignoreCase = true) -> "Pending"
        status.equals("overdue", ignoreCase = true) -> "Overdue"
        else -> status.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    val containerColor = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {
        Text(
            text = displayStatus,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
