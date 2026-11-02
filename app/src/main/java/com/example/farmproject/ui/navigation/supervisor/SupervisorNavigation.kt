package com.example.farmproject.ui.navigation.supervisor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.farmproject.data.model.InventoryCategory
import com.example.farmproject.data.repository.ChatRepository
import com.example.farmproject.ui.activities.manager.FarmActivitiesUiState
import com.example.farmproject.ui.activities.manager.FarmActivitiesViewModel
import com.example.farmproject.ui.activities.manager.ManagerAddFarmActivityScreen
import com.example.farmproject.ui.activities.manager.ManagerEditFarmActivityScreen
import com.example.farmproject.ui.activities.manager.ManagerFarmActivityDetailsScreen
import com.example.farmproject.ui.activities.supervisor.SupervisorFarmActivitiesScreen
import com.example.farmproject.ui.attendance.AttendanceViewModel
import com.example.farmproject.ui.attendance.supervisor.SupervisorAttendanceLogScreen
import com.example.farmproject.ui.attendance.supervisor.SupervisorAttendanceScreen
import com.example.farmproject.ui.attendance.supervisor.SupervisorReviewClockRequestsScreen
import com.example.farmproject.ui.chat.ChatRoutes
import com.example.farmproject.ui.chat.chatGraph
import com.example.farmproject.ui.dashboard.supervisor.SupervisorDashboardScreen
import com.example.farmproject.ui.dashboard.supervisor.SupervisorDashboardViewModel
import com.example.farmproject.ui.expense.ManagerExpenseScreen
import com.example.farmproject.ui.inventory.manager.InventoryUiState
import com.example.farmproject.ui.inventory.manager.InventoryViewModel
import com.example.farmproject.ui.inventory.manager.ManagerAddInventoryScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryCategoryItemsScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryDetailsScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryScreen
import com.example.farmproject.ui.machinery.manager.MachineryInUseViewModel
import com.example.farmproject.ui.machinery.manager.MachineryMaintenanceViewModel
import com.example.farmproject.ui.machinery.manager.MachineryUiState
import com.example.farmproject.ui.machinery.manager.MachineryViewModel
import com.example.farmproject.ui.machinery.manager.ManagerAddMachineryScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryDetailsScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryInUseScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryMaintenanceScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryScreen
import com.example.farmproject.ui.more.supervisor.SupervisorMoreScreen
import com.example.farmproject.ui.notifications.manager.NotificationViewModel
import com.example.farmproject.ui.notifications.supervisor.SupervisorNotificationsScreen
import com.example.farmproject.ui.settings.supervisor.SupervisorProfileScreen
import com.example.farmproject.ui.settings.supervisor.SupervisorSettingsScreen
import java.util.Locale

private sealed class SupervisorTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : SupervisorTab(
        route = "supervisor_tab_dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard
    )

    data object Messages : SupervisorTab(
        route = ChatRoutes.LIST,
        title = "Messages",
        icon = Icons.Default.Forum
    )

    data object Settings : SupervisorTab(
        route = SupervisorRoutes.SETTINGS,
        title = "Settings",
        icon = Icons.Default.Settings
    )

    data object More : SupervisorTab(
        route = "supervisor_tab_more",
        title = "More",
        icon = Icons.Default.Menu
    )
}

private object SupervisorRoutes {
    const val SETTINGS = "supervisor_settings"
    const val PROFILE = "supervisor_profile"
    const val NOTIFICATIONS = "supervisor_notifications"
    const val ACTIVITIES = "supervisor_tab_activities"
    const val INVENTORY = "supervisor_tab_inventory"
    const val MACHINERY = "supervisor_tab_machinery"
    const val EXPENSES = "supervisor_expenses"
    const val ADD_FARM_ACTIVITY = "supervisor_add_farm_activity"
    const val EDIT_FARM_ACTIVITY = "supervisor_edit_farm_activity/{activityId}"
    const val FARM_ACTIVITY_DETAILS = "supervisor_farm_activity_details/{activityId}"
    const val INVENTORY_DETAILS = "supervisor_inventory_details/{itemId}"
    const val ADD_INVENTORY = "supervisor_add_inventory?category={categoryName}"
    const val MACHINERY_DETAILS = "supervisor_machinery_details/{machineryId}"
    const val ADD_MACHINERY = "supervisor_add_machinery"
    const val MACHINERY_IN_USE = "supervisor_machinery_in_use"
    const val MACHINERY_MAINTENANCE = "supervisor_machinery_maintenance"
    const val ATTENDANCE = "supervisor_attendance"
    const val REVIEW_CLOCK_REQUESTS = "supervisor_review_clock_requests"
    const val ATTENDANCE_LOG = "supervisor_attendance_log"
}

@Composable
private inline fun <reified VM : ViewModel> sharedViewModel(
    navController: NavHostController,
    route: String,
    backStackEntry: NavBackStackEntry
): VM {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(route)
    }
    return viewModel(parentEntry)
}

@Composable
fun SupervisorNavigation(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val chatRepository = remember { ChatRepository() }

    val tabs = listOf(
        SupervisorTab.Dashboard,
        SupervisorTab.Messages,
        SupervisorTab.Settings,
        SupervisorTab.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val formRoutes = setOf(
        SupervisorRoutes.ADD_FARM_ACTIVITY,
        SupervisorRoutes.PROFILE
    )

    val isFormScreen = currentRoute in formRoutes || currentRoute?.startsWith("supervisor_add_inventory") == true

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
            startDestination = SupervisorTab.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. DASHBOARD
            composable(SupervisorTab.Dashboard.route) {
                SupervisorDashboardScreen(
                    onReviewRequestsClick = { navController.navigate(SupervisorRoutes.REVIEW_CLOCK_REQUESTS) },
                    onAttendanceLogClick = { navController.navigate(SupervisorRoutes.ATTENDANCE_LOG) },
                    onChatClick = { navigateToTab(SupervisorTab.Messages.route) }
                )
            }

            // REVIEW CLOCK REQUESTS
            composable(SupervisorRoutes.REVIEW_CLOCK_REQUESTS) {
                val viewModel: SupervisorDashboardViewModel = viewModel()
                SupervisorReviewClockRequestsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ATTENDANCE LOG
            composable(SupervisorRoutes.ATTENDANCE_LOG) {
                val viewModel: AttendanceViewModel = viewModel()
                SupervisorAttendanceLogScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // EXPENSES (FOR EXPENSE SUPERVISOR)
            composable(SupervisorRoutes.EXPENSES) {
                ManagerExpenseScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 2. ACTIVITIES
            composable(SupervisorRoutes.ACTIVITIES) {
                val viewModel: FarmActivitiesViewModel = viewModel()
                SupervisorFarmActivitiesScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navigateToTab(SupervisorTab.Dashboard.route)
                        }
                    },
                    onActivityClick = { activityId ->
                        navController.navigate("supervisor_farm_activity_details/$activityId")
                    }
                )
            }

            composable(SupervisorRoutes.ADD_FARM_ACTIVITY) { backStackEntry ->
                val viewModel: FarmActivitiesViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.ACTIVITIES,
                    backStackEntry = backStackEntry
                )

                val formOptions by viewModel.formOptions.collectAsStateWithLifecycle()

                ManagerAddFarmActivityScreen(
                    viewModel = viewModel,
                    activityTypes = formOptions.activityTypeNames,
                    divisions = formOptions.divisions,
                    machineryList = formOptions.machinery,
                    workers = formOptions.workers,
                    supervisors = formOptions.supervisors,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { activityTypeName, activityName, dueDate, notes, assignedRole, workerProfileId, supervisorProfileId, divisionId, quantity, quantityUnit, equipmentNeeded ->
                        viewModel.addActivity(
                            activityTypeName = activityTypeName,
                            activityName = activityName,
                            activityDate = dueDate,
                            status = "Pending",
                            notes = notes,
                            assignedRole = assignedRole,
                            workerProfileId = workerProfileId,
                            supervisorProfileId = supervisorProfileId,
                            divisionId = divisionId,
                            quantity = quantity,
                            quantityUnit = quantityUnit,
                            equipmentNeeded = equipmentNeeded
                        )
                    }
                )
            }

            composable(
                route = SupervisorRoutes.EDIT_FARM_ACTIVITY,
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val viewModel: FarmActivitiesViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.ACTIVITIES,
                    backStackEntry = backStackEntry
                )

                val formOptions by viewModel.formOptions.collectAsStateWithLifecycle()

                ManagerEditFarmActivityScreen(
                    viewModel = viewModel,
                    activityId = activityId,
                    activityTypes = formOptions.activityTypeNames,
                    divisions = formOptions.divisions,
                    machineryList = formOptions.machinery,
                    workers = formOptions.workers,
                    supervisors = formOptions.supervisors,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { activityTypeName, activityName, dueDate, notes, assignedRole, workerProfileId, supervisorProfileId, divisionId, quantity, quantityUnit, equipmentNeeded ->
                        viewModel.updateActivity(
                            activityId = activityId,
                            activityTypeName = activityTypeName,
                            activityName = activityName,
                            activityDate = dueDate,
                            notes = notes,
                            assignedRole = assignedRole,
                            workerProfileId = workerProfileId,
                            supervisorProfileId = supervisorProfileId,
                            divisionId = divisionId,
                            quantity = quantity,
                            quantityUnit = quantityUnit,
                            equipmentNeeded = equipmentNeeded
                        )
                    }
                )
            }

            composable(
                route = SupervisorRoutes.FARM_ACTIVITY_DETAILS,
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val viewModel: FarmActivitiesViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.ACTIVITIES,
                    backStackEntry = backStackEntry
                )

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
                    isManagerRole = true,
                    onBackClick = { navController.popBackStack() },
                    onStartClick = { viewModel.startActivity(activityId) },
                    onCompleteClick = { viewModel.completeActivity(activityId) },
                    onAssignSave = { isSupervisor, profileId ->
                        viewModel.assignActivity(activityId, isSupervisor, profileId)
                    },
                    onEditClick = {
                        navController.navigate("supervisor_edit_farm_activity/$activityId")
                    },
                    onDeleteClick = {
                        viewModel.deleteActivity(activityId)
                        navController.popBackStack()
                    }
                )
            }

            // 3. INVENTORY
            composable(SupervisorRoutes.INVENTORY) { backStackEntry ->
                val viewModel: InventoryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.INVENTORY,
                    backStackEntry = backStackEntry
                )

                ManagerInventoryScreen(
                    viewModel = viewModel,
                    onBackClick = { navigateToTab(SupervisorTab.Dashboard.route) },
                    onCategoryClick = { category ->
                        navController.navigate("supervisor_inventory_category/${category.name}")
                    }
                )
            }

            composable(
                route = "supervisor_inventory_category/{categoryName}",
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryNameStr = backStackEntry.arguments?.getString("categoryName").orEmpty()
                val category = runCatching { InventoryCategory.valueOf(categoryNameStr) }.getOrDefault(InventoryCategory.FERTILIZER)

                val viewModel: InventoryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.INVENTORY,
                    backStackEntry = backStackEntry
                )

                ManagerInventoryCategoryItemsScreen(
                    viewModel = viewModel,
                    category = category,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate("supervisor_inventory_details/$itemId")
                    },
                    onAddClick = {
                        navController.navigate("supervisor_add_inventory")
                    }
                )
            }

            composable(
                route = SupervisorRoutes.INVENTORY_DETAILS,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
                val viewModel: InventoryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.INVENTORY,
                    backStackEntry = backStackEntry
                )

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val item = (uiState as? InventoryUiState.Success)?.items?.find { it.id == itemId }

                ManagerInventoryDetailsScreen(
                    itemName = item?.name ?: "Inventory Item",
                    category = item?.category?.displayName ?: "General",
                    quantity = "${item?.quantity ?: 0.0}",
                    unit = item?.unit ?: "units",
                    reorderLevel = "${item?.reorderLevel ?: 10.0}",
                    isLowStock = item?.isLowStock == true,
                    notes = item?.description ?: "No notes recorded.",
                    onBackClick = { navController.popBackStack() },
                    onEditClick = {},
                    onDeleteClick = { navController.popBackStack() }
                )
            }

            composable(
                route = SupervisorRoutes.ADD_INVENTORY,
                arguments = listOf(navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val categoryStr = backStackEntry.arguments?.getString("category").orEmpty().ifBlank { "Fertilizer" }

                ManagerAddInventoryScreen(
                    initialCategory = categoryStr,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 4. MACHINERY
            composable(SupervisorRoutes.MACHINERY) { backStackEntry ->
                val viewModel: MachineryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.MACHINERY,
                    backStackEntry = backStackEntry
                )

                ManagerMachineryScreen(
                    viewModel = viewModel,
                    onBackClick = { navigateToTab(SupervisorTab.Dashboard.route) },
                    onMachineryClick = { id ->
                        navController.navigate("supervisor_machinery_details/$id")
                    }
                )
            }

            composable(
                route = SupervisorRoutes.MACHINERY_DETAILS,
                arguments = listOf(navArgument("machineryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val machineryId = backStackEntry.arguments?.getString("machineryId").orEmpty()
                val viewModel: MachineryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.MACHINERY,
                    backStackEntry = backStackEntry
                )

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val item = (uiState as? MachineryUiState.Success)?.machinery?.find { it.machineryId == machineryId }

                ManagerMachineryDetailsScreen(
                    machineName = item?.name ?: "Machinery",
                    machineType = item?.machineryType ?: "Equipment",
                    status = item?.status ?: "Available",
                    condition = item?.condition ?: "Good",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(SupervisorRoutes.ADD_MACHINERY) { backStackEntry ->
                val viewModel: MachineryViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.MACHINERY,
                    backStackEntry = backStackEntry
                )

                ManagerAddMachineryScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(SupervisorRoutes.MACHINERY_IN_USE) { backStackEntry ->
                val viewModel: MachineryInUseViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.MACHINERY,
                    backStackEntry = backStackEntry
                )

                ManagerMachineryInUseScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(SupervisorRoutes.MACHINERY_MAINTENANCE) { backStackEntry ->
                val viewModel: MachineryMaintenanceViewModel = sharedViewModel(
                    navController = navController,
                    route = SupervisorRoutes.MACHINERY,
                    backStackEntry = backStackEntry
                )

                ManagerMachineryMaintenanceScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 5. ATTENDANCE
            composable(SupervisorRoutes.ATTENDANCE) {
                SupervisorAttendanceScreen(
                    onBackClick = { navigateToTab(SupervisorTab.Dashboard.route) }
                )
            }

            // 6. SETTINGS
            composable(SupervisorRoutes.SETTINGS) {
                SupervisorSettingsScreen(
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navigateToTab(SupervisorTab.Dashboard.route)
                        }
                    },
                    onProfileClick = { navController.navigate(SupervisorRoutes.PROFILE) },
                    onNotificationsClick = { navController.navigate(SupervisorRoutes.NOTIFICATIONS) }
                )
            }

            // MORE
            composable(SupervisorTab.More.route) {
                SupervisorMoreScreen(
                    onBackClick = { navigateToTab(SupervisorTab.Dashboard.route) },
                    onActivitiesClick = { navController.navigate(SupervisorRoutes.ACTIVITIES) },
                    onInventoryClick = { navController.navigate(SupervisorRoutes.INVENTORY) },
                    onMachineryClick = { navController.navigate(SupervisorRoutes.MACHINERY) },
                    onAttendanceClick = { navController.navigate(SupervisorRoutes.ATTENDANCE) },
                    onExpensesClick = { navController.navigate(SupervisorRoutes.EXPENSES) },
                    onProfileClick = { navController.navigate(SupervisorRoutes.PROFILE) },
                    onSettingsClick = { navController.navigate(SupervisorRoutes.SETTINGS) },
                    onLogoutClick = onLogout
                )
            }

            // PROFILE
            composable(SupervisorRoutes.PROFILE) {
                SupervisorProfileScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // NOTIFICATIONS
            composable(SupervisorRoutes.NOTIFICATIONS) {
                val viewModel: NotificationViewModel = viewModel()
                SupervisorNotificationsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            chatGraph(navController, chatRepository)
        }
    }
}
