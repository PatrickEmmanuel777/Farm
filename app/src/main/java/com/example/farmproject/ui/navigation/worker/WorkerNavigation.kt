package com.example.farmproject.ui.navigation.worker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.farmproject.data.repository.ChatRepository
import com.example.farmproject.ui.activities.manager.FarmActivitiesUiState
import com.example.farmproject.ui.activities.manager.FarmActivitiesViewModel
import com.example.farmproject.ui.activities.manager.ManagerFarmActivityDetailsScreen
import com.example.farmproject.ui.activities.worker.WorkerFarmActivitiesScreen
import com.example.farmproject.ui.attendance.worker.WorkerAttendanceScreen
import com.example.farmproject.ui.chat.ChatRoutes
import com.example.farmproject.ui.chat.chatGraph
import com.example.farmproject.ui.dashboard.worker.WorkerDashboardScreen
import com.example.farmproject.ui.dashboard.worker.WorkerDashboardViewModel
import com.example.farmproject.ui.more.worker.WorkerMoreScreen
import com.example.farmproject.ui.notifications.manager.NotificationViewModel
import com.example.farmproject.ui.notifications.worker.WorkerNotificationsScreen
import com.example.farmproject.ui.settings.worker.WorkerProfileScreen
import com.example.farmproject.ui.settings.worker.WorkerSettingsScreen
import java.util.Locale

private sealed class WorkerTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : WorkerTab(
        route = "worker_tab_dashboard",
        title = "Home",
        icon = Icons.Default.Home
    )

    data object Tasks : WorkerTab(
        route = WorkerRoutes.ACTIVITIES,
        title = "Tasks",
        icon = Icons.AutoMirrored.Filled.Assignment
    )

    data object Attendance : WorkerTab(
        route = WorkerRoutes.ATTENDANCE,
        title = "Attendance",
        icon = Icons.Default.CalendarMonth
    )

    data object Chat : WorkerTab(
        route = ChatRoutes.LIST,
        title = "Chat",
        icon = Icons.Default.Forum
    )

    data object More : WorkerTab(
        route = "worker_tab_more",
        title = "More",
        icon = Icons.Default.Menu
    )
}

private object WorkerRoutes {
    const val SETTINGS = "worker_settings"
    const val PROFILE = "worker_profile"
    const val NOTIFICATIONS = "worker_notifications"
    const val ACTIVITIES = "worker_tab_activities"
    const val FARM_ACTIVITY_DETAILS = "worker_farm_activity_details/{activityId}"
    const val ATTENDANCE = "worker_attendance"
}

@Composable
fun WorkerNavigation(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val chatRepository = remember { ChatRepository() }

    val tabs = listOf(
        WorkerTab.Home,
        WorkerTab.Tasks,
        WorkerTab.Attendance,
        WorkerTab.Chat,
        WorkerTab.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val formRoutes = setOf(
        WorkerRoutes.PROFILE
    )

    val isFormScreen = currentRoute in formRoutes

    fun navigateToTab(route: String) {
        val startRoute = navController.graph.findStartDestination().route
        val isStart = route == startRoute
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = !isStart
            }
            launchSingleTop = true
            restoreState = !isStart
        }
    }

    Scaffold(
        bottomBar = {
            if (!isFormScreen) {
                NavigationBar(
                    containerColor = Color(0xFFF2F6F1)
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WorkerTab.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. HOME (DASHBOARD)
            composable(WorkerTab.Home.route) {
                val viewModel: WorkerDashboardViewModel = viewModel()
                WorkerDashboardScreen(
                    viewModel = viewModel,
                    onTasksClick = { navigateToTab(WorkerTab.Tasks.route) },
                    onChatClick = { navigateToTab(WorkerTab.Chat.route) },
                    onLogoutClick = onLogout
                )
            }

            // 2. ACTIVITIES / TASKS
            composable(WorkerRoutes.ACTIVITIES) {
                val viewModel: FarmActivitiesViewModel = viewModel()
                WorkerFarmActivitiesScreen(
                    viewModel = viewModel,
                    onBackClick = { navigateToTab(WorkerTab.Home.route) },
                    onActivityClick = { activityId ->
                        navController.navigate("worker_farm_activity_details/$activityId")
                    }
                )
            }

            composable(
                route = WorkerRoutes.FARM_ACTIVITY_DETAILS,
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val viewModel: FarmActivitiesViewModel = viewModel()

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val formOptions by viewModel.formOptions.collectAsStateWithLifecycle()

                val display = (uiState as? FarmActivitiesUiState.Success)?.activities
                    ?.find { it.activity.activityId == activityId }

                val status = display?.activity?.status ?: "Pending"

                val durationText = if (display?.activity != null) {
                    val act = display.activity
                    if (act.durationHours != null && act.durationHours > 0) {
                        val hrs = act.durationHours
                        if (hrs == 1.0) "1.0 hr" else String.format(Locale.US, "%.1f hrs", hrs)
                    } else "1.0 hr"
                } else "1.0 hr"

                ManagerFarmActivityDetailsScreen(
                    title = display?.displayTitle ?: "Unknown activity",
                    type = display?.activityTypeName ?: "",
                    assignedTo = display?.assignedToName ?: "Worker",
                    dueDate = display?.activity?.safeActivityDate ?: "",
                    durationText = durationText,
                    status = status,
                    isOverdue = status.equals("Overdue", ignoreCase = true),
                    notes = display?.activity?.notes ?: "No additional notes recorded for this activity.",
                    quantity = display?.activity?.quantity,
                    quantityUnit = display?.activity?.quantityUnit,
                    equipmentNeeded = display?.activity?.equipmentNeeded,
                    workers = formOptions.workers,
                    supervisors = formOptions.supervisors,
                    isManagerRole = false,
                    onBackClick = { navController.popBackStack() },
                    onStartClick = { viewModel.startActivity(activityId) },
                    onCompleteClick = { viewModel.completeActivity(activityId) },
                    onAssignSave = { _, _ -> },
                    onEditClick = {},
                    onDeleteClick = { navController.popBackStack() }
                )
            }

            // 3. ATTENDANCE
            composable(WorkerRoutes.ATTENDANCE) {
                WorkerAttendanceScreen(
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navigateToTab(WorkerTab.Home.route)
                        }
                    }
                )
            }

            // 4. MORE
            composable(WorkerTab.More.route) {
                WorkerMoreScreen(
                    onBackClick = { navigateToTab(WorkerTab.Home.route) },
                    onChatClick = { navigateToTab(WorkerTab.Chat.route) },
                    onAttendanceClick = { navController.navigate(WorkerRoutes.ATTENDANCE) },
                    onSettingsClick = { navController.navigate(WorkerRoutes.SETTINGS) },
                    onLogoutClick = onLogout
                )
            }

            // SETTINGS
            composable(WorkerRoutes.SETTINGS) {
                WorkerSettingsScreen(
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navigateToTab(WorkerTab.Home.route)
                        }
                    },
                    onProfileClick = { navController.navigate(WorkerRoutes.PROFILE) },
                    onNotificationsClick = { navController.navigate(WorkerRoutes.NOTIFICATIONS) },
                    onLogoutClick = onLogout
                )
            }

            // PROFILE
            composable(WorkerRoutes.PROFILE) {
                WorkerProfileScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // NOTIFICATIONS
            composable(WorkerRoutes.NOTIFICATIONS) {
                val viewModel: NotificationViewModel = viewModel()
                WorkerNotificationsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            chatGraph(navController, chatRepository)
        }
    }
}
