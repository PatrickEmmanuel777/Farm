package com.example.farmproject.ui.more.supervisor

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.session.CurrentSession

private data class SupervisorMoreOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorMoreScreen(
    onBackClick: () -> Unit = {},
    onActivitiesClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onMachineryClick: () -> Unit = {},
    onAttendanceClick: () -> Unit = {},
    onYieldsClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val options = mutableListOf<SupervisorMoreOption>()

    val isManager = CurrentSession.isManager()
    val hasActivitiesDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.ACTIVITIES_RESOURCES)
    val hasAttendanceDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.ATTENDANCE)
    val hasYieldDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.YIELD_HARVEST)
    val hasExpenseDuty = isManager || CurrentSession.hasDuty(SupervisorDuty.EXPENSE)

    if (hasActivitiesDuty) {
        options.add(
            SupervisorMoreOption(
                title = "Field Activities",
                description = "Create and update farm field activities & tasks",
                icon = Icons.AutoMirrored.Filled.Assignment,
                onClick = onActivitiesClick
            )
        )
        options.add(
            SupervisorMoreOption(
                title = "Resources & Inventory",
                description = "Manage farm inputs, fertilizers, and requisitions",
                icon = Icons.Default.Inventory2,
                onClick = onInventoryClick
            )
        )
        options.add(
            SupervisorMoreOption(
                title = "Machinery & Equipment",
                description = "Monitor farm machinery, usage and maintenance",
                icon = Icons.Default.PrecisionManufacturing,
                onClick = onMachineryClick
            )
        )
    }

    if (hasAttendanceDuty) {
        options.add(
            SupervisorMoreOption(
                title = "Worker Attendance Log",
                description = "Clock-in workers, view logs & review pending requests",
                icon = Icons.Default.CalendarMonth,
                onClick = onAttendanceClick
            )
        )
    }

    if (hasYieldDuty) {
        options.add(
            SupervisorMoreOption(
                title = "Record Crop Harvest",
                description = "Record crop harvest yields on manager's behalf",
                icon = Icons.Default.Agriculture,
                onClick = onYieldsClick
            )
        )
    }

    if (hasExpenseDuty) {
        options.add(
            SupervisorMoreOption(
                title = "Record Expenses (Add Only)",
                description = "Add farm expenses (read & create only)",
                icon = Icons.Default.AttachMoney,
                onClick = onExpensesClick
            )
        )
    }

    options.add(
        SupervisorMoreOption(
            title = "Supervisor Profile",
            description = "View and manage your personal profile",
            icon = Icons.Default.Person,
            onClick = onProfileClick
        )
    )

    options.add(
        SupervisorMoreOption(
            title = "Settings",
            description = "Manage supervisor preferences and settings",
            icon = Icons.Default.Settings,
            onClick = onSettingsClick
        )
    )

    options.add(
        SupervisorMoreOption(
            title = "Log Out",
            description = "Sign out of your supervisor account",
            icon = Icons.AutoMirrored.Filled.Logout,
            isDestructive = true,
            onClick = onLogoutClick
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
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
                        text = "More Options",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Supervisor role-based tools & settings",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Role Features & Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(options) { option ->
                SupervisorMoreOptionCard(option = option)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SupervisorMoreOptionCard(
    option: SupervisorMoreOption
) {
    val iconContainerColor = if (option.isDestructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val iconTint = if (option.isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = option.onClick,
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
                    .size(46.dp)
                    .background(
                        color = iconContainerColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (option.isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
                )

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
