package com.example.farmproject.ui.more.manager

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class MoreMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerMoreScreen(
    onBackClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onMachineryClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onAttendanceClick: () -> Unit = {},
    onYieldsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onCropRecommendationsClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {

    val menuItems = listOf(
        MoreMenuItem(
            title = "Messages & Team Chat",
            description = "Chat with farm staff, supervisors and workers",
            icon = Icons.Default.Forum
        ),
        MoreMenuItem(
            title = "Farm Expenses",
            description = "Track, record & manage operational costs",
            icon = Icons.Default.AttachMoney
        ),
        MoreMenuItem(
            title = "Worker Attendance",
            description = "Daily field clock-in, status & attendance logs",
            icon = Icons.Default.CalendarMonth
        ),
        MoreMenuItem(
            title = "Resources",
            description = "Track fertilizers, chemicals, water & farm resources",
            icon = Icons.Default.Inventory
        ),
        MoreMenuItem(
            title = "Machinery",
            description = "Manage farm machinery & maintenance",
            icon = Icons.Default.PrecisionManufacturing
        ),
        MoreMenuItem(
            title = "Storage & Produce",
            description = "Manage harvested produce & storage locations",
            icon = Icons.Default.Warehouse
        ),
        MoreMenuItem(
            title = "Crop Yields",
            description = "Record and view crop harvest logs",
            icon = Icons.Default.Agriculture
        ),
        MoreMenuItem(
            title = "Reports & Summaries",
            description = "View crop, inventory, machinery & workforce reports",
            icon = Icons.Default.Assessment
        ),
        MoreMenuItem(
            title = "Get Tips",
            description = "Crop recommendations & agronomy advice",
            icon = Icons.Default.Lightbulb
        ),
        MoreMenuItem(
            title = "Settings",
            description = "Manage application settings",
            icon = Icons.Default.Settings
        )
    )

    val menuActions: Map<String, () -> Unit> = mapOf(
        "Messages & Team Chat" to onChatClick,
        "Farm Expenses" to onExpensesClick,
        "Worker Attendance" to onAttendanceClick,
        "Resources" to onInventoryClick,
        "Machinery" to onMachineryClick,
        "Storage & Produce" to onStorageClick,
        "Crop Yields" to onYieldsClick,
        "Reports & Summaries" to onReportsClick,
        "Get Tips" to onCropRecommendationsClick,
        "Settings" to onSettingsClick
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "More",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Farm management options",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(menuItems) { item ->
                MoreMenuCard(
                    item = item,
                    onClick = {
                        menuActions[item.title]?.invoke()
                    }
                )
            }

            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                LogoutCard(
                    onClick = onLogoutClick
                )
            }

            item {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MoreMenuCard(
    item: MoreMenuItem,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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

            Row(
                modifier = Modifier.weight(1f),
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
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(
                    modifier = Modifier.size(12.dp)
                )

                Column {

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogoutCard(
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column {

                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "Sign out of your manager account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
