package com.example.farmproject.ui.navigation.manager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.farmproject.ui.activities.manager.ManagerFarmActivitiesScreen
import com.example.farmproject.ui.activities.manager.ManagerFarmActivityDetailsScreen
import com.example.farmproject.ui.crops.manager.CropsUiState
import com.example.farmproject.ui.crops.manager.CropsViewModel
import com.example.farmproject.ui.crops.manager.ManagerAddCropScreen
import com.example.farmproject.ui.crops.manager.ManagerCropDetailsScreen
import com.example.farmproject.ui.crops.manager.ManagerCropsScreen
import com.example.farmproject.ui.crops.manager.ManagerYieldScreen
import com.example.farmproject.ui.crops.manager.YieldViewModel
import com.example.farmproject.ui.dashboard.manager.DashboardViewModel
import com.example.farmproject.ui.dashboard.manager.ManagerDashboardScreen
import com.example.farmproject.ui.divisions.manager.DivisionsViewModel
import com.example.farmproject.ui.divisions.manager.ManagerAddDivisionScreen
import com.example.farmproject.ui.divisions.manager.ManagerDivisionsScreen
import com.example.farmproject.ui.employees.manager.EmployeesUiState
import com.example.farmproject.ui.employees.manager.EmployeesViewModel
import com.example.farmproject.ui.employees.manager.ManagerAddSupervisorScreen
import com.example.farmproject.ui.employees.manager.ManagerAddWorkerScreen
import com.example.farmproject.ui.employees.manager.ManagerEmployeeDetailsScreen
import com.example.farmproject.ui.employees.manager.ManagerEmployeesScreen
import com.example.farmproject.ui.employees.manager.SupervisorDetailsRoute
import com.example.farmproject.ui.inventory.manager.InventoryUiState
import com.example.farmproject.ui.inventory.manager.InventoryViewModel
import com.example.farmproject.ui.inventory.manager.ManagerAddInventoryScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryCategoryItemsScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryDetailsScreen
import com.example.farmproject.ui.inventory.manager.ManagerInventoryScreen
import com.example.farmproject.ui.machinery.manager.MachineryDetailUiState
import com.example.farmproject.ui.machinery.manager.MachineryViewModel
import com.example.farmproject.ui.machinery.manager.ManagerAddMachineryScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryDetailsScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryInUseScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryMaintenanceScreen
import com.example.farmproject.ui.machinery.manager.ManagerMachineryScreen
import com.example.farmproject.ui.more.manager.ManagerMoreScreen
import com.example.farmproject.ui.notifications.manager.ManagerNotificationsScreen
import com.example.farmproject.ui.reports.manager.ManagerGenerateReportScreen
import com.example.farmproject.ui.reports.manager.ManagerReportDetailsScreen
import com.example.farmproject.ui.reports.manager.ManagerReportsScreen
import com.example.farmproject.ui.reports.manager.ReportsViewModel
import com.example.farmproject.ui.notifications.manager.NotificationViewModel
import com.example.farmproject.ui.recommendations.manager.ManagerRecommendationsScreen
import com.example.farmproject.ui.settings.manager.ManagerFarmDetailsScreen
import com.example.farmproject.ui.settings.manager.ManagerProfileScreen
import com.example.farmproject.ui.settings.manager.ManagerSettingsScreen
import com.example.farmproject.ui.attendance.manager.ManagerAttendanceScreen
import com.example.farmproject.ui.chat.ChatRoutes
import com.example.farmproject.ui.chat.chatGraph
import com.example.farmproject.ui.expense.ExpenseViewModel
import com.example.farmproject.ui.expense.ManagerExpenseScreen
import com.example.farmproject.ui.storage.manager.ManagerAddStorageScreen
import com.example.farmproject.ui.storage.manager.ManagerStorageDetailsScreen
import com.example.farmproject.ui.storage.manager.ManagerStorageScreen
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

/**
 * Sealed class representing bottom navigation tabs available in the Manager section.
 *
 * @property route Unique route string identifying the top-level tab destination.
 * @property title Human-readable label displayed in the bottom navigation bar.
 * @property icon Image vector icon rendered for the tab in the bottom navigation bar.
 */
private sealed class ManagerTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    /** Dashboard overview tab showing farm metrics and navigation shortcuts. */
    data object Dashboard : ManagerTab(
        route = "manager_dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard
    )

    /** Crops management tab for viewing, adding, and tracking crop plantings. */
    data object Crops : ManagerTab(
        route = "manager_crops",
        title = "Crops",
        icon = Icons.Default.Eco
    )

    /** Farm Activities tab for viewing, adding, and tracking farm activities. */
    data object FarmActivities : ManagerTab(
        route = "manager_farm_activities",
        title = "Activities",
        icon = Icons.AutoMirrored.Filled.Assignment
    )

    /** Employees management tab for managing supervisors and workers. */
    data object Employees : ManagerTab(
        route = "manager_employees",
        title = "Employees",
        icon = Icons.Default.Groups
    )

    /** More tab for secondary manager features like settings, inventory, and machinery. */
    data object More : ManagerTab(
        route = "manager_more",
        title = "More",
        icon = Icons.Default.Menu
    )
}

/**
 * Collection of route pattern constants used for non-tab screens within the Manager navigation graph.
 */
private object ManagerRoutes {
    const val DIVISIONS = "manager_divisions"
    const val ADD_DIVISION = "manager_add_division"
    const val INVENTORY = "manager_inventory"
    const val ADD_INVENTORY = "manager_add_inventory?category={categoryName}"
    const val INVENTORY_DETAILS = "manager_inventory_details/{itemId}"
    const val MACHINERY = "manager_machinery"
    const val MACHINERY_DETAILS = "manager_machinery_details/{machineryId}"
    const val ADD_MACHINERY = "manager_add_machinery"
    const val ADD_CROP = "manager_add_crop"
    const val CROP_DETAILS = "manager_crop_details/{plantingId}"
    const val YIELD = "manager_yield"
    const val ADD_FARM_ACTIVITY = "manager_add_farm_activity"
    const val EDIT_FARM_ACTIVITY = "manager_edit_farm_activity/{activityId}"
    const val FARM_ACTIVITY_DETAILS = "manager_farm_activity_details/{activityId}"
    const val ADD_SUPERVISOR = "manager_add_supervisor"
    const val ADD_WORKER = "manager_add_worker"
    const val SUPERVISOR_DETAILS = "manager_supervisor_details/{profileId}"
    const val WORKER_DETAILS = "manager_worker_details/{profileId}"
    const val STORAGE = "manager_storage"
    const val ADD_STORAGE = "manager_add_storage"
    const val STORAGE_DETAILS = "manager_storage_details"
    const val CROP_RECOMMENDATIONS = "manager_crop_recommendations"
    const val SETTINGS = "manager_settings"
    const val PROFILE = "manager_profile"
    const val FARM_DETAILS = "manager_farm_details"
    const val NOTIFICATIONS = "manager_notifications"
    const val MACHINERY_IN_USE = "manager_machinery_in_use"
    const val MACHINERY_MAINTENANCE = "manager_machinery_maintenance"
    const val REPORTS = "manager_reports"
    const val GENERATE_REPORT = "manager_generate_report"
    const val REPORT_DETAILS = "manager_report_details/{reportTitle}"
    const val ATTENDANCE = "manager_attendance"
    const val EXPENSES = "manager_expenses"
}

/**
 * Obtains a ViewModel scoped to another route's back-stack entry instead
 * of the current one — this is how an Add screen and its List screen
 * share the SAME ViewModel instance (and therefore the same StateFlow).
 */
@Composable
private inline fun <reified VM : ViewModel> sharedViewModel(
    navController: NavHostController,
    route: String,
    backStackEntry: NavBackStackEntry
): VM {
    val parentEntry = remember(backStackEntry) {
        try {
            navController.getBackStackEntry(route)
        } catch (_: Exception) {
            backStackEntry
        }
    }
    return viewModel(parentEntry)
}

/**
 * Top-level navigation entry point for the Farm Manager experience.
 *
 * Configures the primary [NavHost] and bottom [NavigationBar], handling destination
 * transitions across all Manager workflows.
 *
 * @param onLogout Callback executed when the manager opts to log out of the application.
 */
@Composable
fun ManagerNavigation(
    onLogout: () -> Unit = {}
) {

    val navController = rememberNavController()
    val chatRepository = remember { ChatRepository() }

    val tabs = listOf(
        ManagerTab.Dashboard,
        ManagerTab.Crops,
        ManagerTab.FarmActivities,
        ManagerTab.Employees,
        ManagerTab.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val formRoutes = setOf(
        ManagerRoutes.ADD_DIVISION,
        ManagerRoutes.ADD_MACHINERY,
        ManagerRoutes.ADD_INVENTORY,
        ManagerRoutes.ADD_CROP,
        ManagerRoutes.ADD_FARM_ACTIVITY,
        ManagerRoutes.EDIT_FARM_ACTIVITY,
        ManagerRoutes.ADD_SUPERVISOR,
        ManagerRoutes.ADD_WORKER,
        ManagerRoutes.PROFILE
    )

    val isFormScreen = currentRoute in formRoutes || currentRoute?.startsWith("manager_add_inventory") == true

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
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
                            onClick = {
                                navigateToTab(tab.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(tab.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            NavHost(
                navController = navController,
                startDestination = ManagerTab.Dashboard.route
            ) {

                composable(ManagerTab.Dashboard.route) {
                    val viewModel: DashboardViewModel = viewModel()
                    ManagerDashboardScreen(
                        viewModel = viewModel,
                        onCropsClick = { navigateToTab(ManagerTab.Crops.route) },
                        onInventoryClick = { navController.navigate(ManagerRoutes.INVENTORY) },
                        onWorkersClick = { navigateToTab(ManagerTab.Employees.route) },
                        onMachineryClick = { navController.navigate(ManagerRoutes.MACHINERY) },
                        onStorageClick = { navController.navigate(ManagerRoutes.STORAGE) },
                        onDivisionsClick = { navController.navigate(ManagerRoutes.DIVISIONS) },
                        onActivitiesClick = { navigateToTab(ManagerTab.FarmActivities.route) },
                        onYieldsClick = { navController.navigate(ManagerRoutes.YIELD) },
                        onAddCropClick = { navController.navigate(ManagerRoutes.ADD_CROP) },
                        onLogActivityClick = { navController.navigate(ManagerRoutes.ADD_FARM_ACTIVITY) },
                        onAddWorkerClick = { navController.navigate(ManagerRoutes.ADD_WORKER) },
                        onReportsClick = { navController.navigate(ManagerRoutes.REPORTS) }
                    )
                }

                composable(ManagerRoutes.DIVISIONS) { backStackEntry ->
                    val viewModel: DivisionsViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Dashboard.route,
                        backStackEntry = backStackEntry
                    )

                    ManagerDivisionsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onAddClick = {
                            navController.navigate(ManagerRoutes.ADD_DIVISION)
                        }
                    )
                }

                composable(ManagerRoutes.ADD_DIVISION) { backStackEntry ->
                    val viewModel: DivisionsViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Dashboard.route,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddDivisionScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                // ---------------------------------------------------------
                // CROPS
                // ---------------------------------------------------------

                composable(ManagerTab.Crops.route) {
                    val viewModel: CropsViewModel = viewModel()

                    ManagerCropsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navigateToTab(ManagerTab.Dashboard.route)
                        },
                        onAddClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_CROP
                            )
                        },
                        onYieldsClick = {
                            navController.navigate(
                                ManagerRoutes.YIELD
                            )
                        },
                        onCropClick = { plantingId ->
                            navController.navigate(
                                "manager_crop_details/$plantingId"
                            )
                        }
                    )
                }

                composable(ManagerRoutes.YIELD) {
                    val viewModel: YieldViewModel = viewModel()
                    ManagerYieldScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                // Add Crop screen
                composable(ManagerRoutes.ADD_CROP) { backStackEntry ->
                    val viewModel: CropsViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Crops.route,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddCropScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveClick = { cropName, scientificName, notes ->
                            viewModel.addCrop(
                                name = cropName,
                                scientificName = scientificName,
                                notes = notes
                            )
                        }
                    )
                }

                // Crop details screen
                composable(
                    route = ManagerRoutes.CROP_DETAILS,
                    arguments = listOf(navArgument("plantingId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val cropId = backStackEntry.arguments?.getString("plantingId").orEmpty()

                    val viewModel: CropsViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Crops.route,
                        backStackEntry = backStackEntry
                    )

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val crop = (uiState as? CropsUiState.Success)?.crops
                        ?.find { it.cropId == cropId }

                    ManagerCropDetailsScreen(
                        cropName = crop?.name ?: "Unknown crop",
                        scientificName = crop?.scientificName,
                        status = "Active",
                        notes = crop?.description
                            ?: "No additional notes recorded for this crop.",
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEditClick = {
                            // No edit flow yet.
                        },
                        onDeleteClick = {
                            viewModel.deleteCrop(cropId)
                            navController.popBackStack()
                        }
                    )
                }

                // ---------------------------------------------------------
                // FARM ACTIVITIES (Bottom Tab)
                // ---------------------------------------------------------

                composable(ManagerTab.FarmActivities.route) {
                    val viewModel: FarmActivitiesViewModel = viewModel()
                    ManagerFarmActivitiesScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navigateToTab(ManagerTab.Dashboard.route)
                        },
                        onAddClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_FARM_ACTIVITY
                            )
                        },
                        onActivityClick = { activityId ->
                            navController.navigate(
                                "manager_farm_activity_details/$activityId"
                            )
                        }
                    )
                }

                // Add Farm Activity screen
                composable(ManagerRoutes.ADD_FARM_ACTIVITY) { backStackEntry ->
                    val viewModel: FarmActivitiesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.FarmActivities.route,
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
                        onBackClick = {
                            navController.popBackStack()
                        },
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

                // Farm Activity details screen
                composable(
                    route = ManagerRoutes.FARM_ACTIVITY_DETAILS,
                    arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()

                    val viewModel: FarmActivitiesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.FarmActivities.route,
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
                        } else {
                            val startStr = act.startedAt ?: act.startTime ?: act.createdAt
                            val endStr = act.completedAt ?: act.endTime ?: act.updatedAt
                            if (!startStr.isNullOrBlank() && !endStr.isNullOrBlank()) {
                                try {
                                    val start = LocalDateTime.parse(startStr)
                                    val end = LocalDateTime.parse(endStr)
                                    val mins = Duration.between(start, end).toMinutes()
                                    if (mins > 0) {
                                        val hrs = mins / 60.0
                                        if (hrs == 1.0) "1.0 hr" else String.format(Locale.US, "%.1f hrs", hrs)
                                    } else "1.0 hr"
                                } catch (_: Exception) { "1.0 hr" }
                            } else "1.0 hr"
                        }
                    } else "1.0 hr"

                    ManagerFarmActivityDetailsScreen(
                        title = display?.displayTitle ?: "Unknown activity",
                        type = display?.activityTypeName ?: "",
                        assignedTo = display?.assignedToName ?: "Unassigned",
                        dueDate = display?.activity?.safeActivityDate ?: "",
                        durationText = durationText,
                        status = status,
                        isOverdue = status.equals("Overdue", ignoreCase = true),
                        notes = display?.activity?.notes
                            ?: "No additional notes recorded for this activity.",
                        quantity = display?.activity?.quantity,
                        quantityUnit = display?.activity?.quantityUnit,
                        equipmentNeeded = display?.activity?.equipmentNeeded,
                        workers = formOptions.workers,
                        supervisors = formOptions.supervisors,
                        isManagerRole = true,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onStartClick = {
                            viewModel.startActivity(activityId)
                        },
                        onCompleteClick = {
                            viewModel.completeActivity(activityId)
                        },
                        onAssignSave = { isSupervisor, profileId ->
                            viewModel.assignActivity(activityId, isSupervisor, profileId)
                        },
                        onEditClick = {
                            navController.navigate("manager_edit_farm_activity/$activityId")
                        },
                        onDeleteClick = {
                            viewModel.deleteActivity(activityId)
                            navController.popBackStack()
                        }
                    )
                }

                // Edit Farm Activity screen
                composable(
                    route = ManagerRoutes.EDIT_FARM_ACTIVITY,
                    arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()

                    val viewModel: FarmActivitiesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.FarmActivities.route,
                        backStackEntry = backStackEntry
                    )

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val formOptions by viewModel.formOptions.collectAsStateWithLifecycle()

                    val display = (uiState as? FarmActivitiesUiState.Success)?.activities
                        ?.find { it.activity.activityId == activityId }

                    ManagerEditFarmActivityScreen(
                        activityId = activityId,
                        initialType = display?.activityTypeName.orEmpty(),
                        initialName = display?.activity?.activityName.orEmpty(),
                        initialDueDate = display?.activity?.safeActivityDate.orEmpty(),
                        initialNotes = display?.activity?.notes.orEmpty(),
                        initialRole = display?.assignedToName.orEmpty(),
                        initialWorkerProfileId = display?.activity?.workerProfileId,
                        initialSupervisorProfileId = display?.activity?.supervisorProfileId,
                        initialDivisionId = display?.activity?.divisionId,
                        initialQuantity = display?.activity?.quantity,
                        initialQuantityUnit = display?.activity?.quantityUnit,
                        initialEquipmentNeeded = display?.activity?.equipmentNeeded,
                        viewModel = viewModel,
                        activityTypes = formOptions.activityTypeNames,
                        divisions = formOptions.divisions,
                        machineryList = formOptions.machinery,
                        workers = formOptions.workers,
                        supervisors = formOptions.supervisors,
                        onBackClick = {
                            navController.popBackStack()
                        },
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

                // ---------------------------------------------------------
                // EMPLOYEES (Bottom Tab)
                // ---------------------------------------------------------

                composable(ManagerTab.Employees.route) {
                    val viewModel: EmployeesViewModel = viewModel()
                    ManagerEmployeesScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navigateToTab(ManagerTab.Dashboard.route)
                        },
                        onAddSupervisorClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_SUPERVISOR
                            )
                        },
                        onAddWorkerClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_WORKER
                            )
                        },
                        onSupervisorClick = { profileId ->
                            navController.navigate(
                                "manager_supervisor_details/$profileId"
                            )
                        },
                        onWorkerClick = { profileId ->
                            navController.navigate(
                                "manager_worker_details/$profileId"
                            )
                        }
                    )
                }

                // Add Supervisor screen
                composable(ManagerRoutes.ADD_SUPERVISOR) { backStackEntry ->
                    val viewModel: EmployeesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Employees.route,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddSupervisorScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveClick = { fullName, email, phoneNumber, employeeNumber, password, duties ->
                            viewModel.addSupervisor(
                                fullName = fullName,
                                email = email,
                                phoneNumber = phoneNumber,
                                employeeNumber = employeeNumber,
                                password = password,
                                duties = duties
                            )
                        }
                    )
                }

                // Add Worker screen
                composable(ManagerRoutes.ADD_WORKER) { backStackEntry ->
                    val viewModel: EmployeesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Employees.route,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddWorkerScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveClick = { fullName, email, phoneNumber, employeeNumber, password ->
                            viewModel.addWorker(
                                fullName = fullName,
                                email = email,
                                phoneNumber = phoneNumber,
                                employeeNumber = employeeNumber,
                                password = password
                            )
                        }
                    )
                }

                // Supervisor Details screen
                composable(
                    route = ManagerRoutes.SUPERVISOR_DETAILS,
                    arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val profileId = backStackEntry.arguments?.getString("profileId").orEmpty()

                    val viewModel: EmployeesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Employees.route,
                        backStackEntry = backStackEntry
                    )

                    SupervisorDetailsRoute(
                        profileId = profileId,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        employeesViewModel = viewModel
                    )
                }

                // Worker Details screen
                composable(
                    route = ManagerRoutes.WORKER_DETAILS,
                    arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val profileId = backStackEntry.arguments?.getString("profileId").orEmpty()

                    val viewModel: EmployeesViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerTab.Employees.route,
                        backStackEntry = backStackEntry
                    )

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val worker = (uiState as? EmployeesUiState.Success)?.workers
                        ?.find { it.profileId == profileId }

                    ManagerEmployeeDetailsScreen(
                        employee = worker,
                        roleLabel = "Worker",
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onUpdateSave = { name, phone, empNum ->
                            worker?.let {
                                viewModel.updateEmployee(
                                    accountId = it.accountId.orEmpty(),
                                    profileId = it.profileId,
                                    fullName = name,
                                    phoneNumber = phone,
                                    employeeNumber = empNum,
                                    isSupervisor = false
                                )
                            }
                        },
                        onDeleteClick = {
                            worker?.let {
                                viewModel.deleteWorkerProfile(it.profileId)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                // ---------------------------------------------------------
                // INVENTORY (In More)
                // ---------------------------------------------------------

                composable(ManagerRoutes.INVENTORY) {
                    val viewModel: InventoryViewModel = viewModel()

                    ManagerInventoryScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onCategoryClick = { category ->
                            navController.navigate(
                                "manager_inventory_category/${category.name}"
                            )
                        }
                    )
                }

                composable(
                    route = "manager_inventory_category/{categoryName}",
                    arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName").orEmpty()
                    val category = InventoryCategory.entries.find { it.name == categoryName } ?: InventoryCategory.FERTILIZER

                    val viewModel: InventoryViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerRoutes.INVENTORY,
                        backStackEntry = backStackEntry
                    )

                    ManagerInventoryCategoryItemsScreen(
                        category = category,
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onAddClick = {
                            navController.navigate(
                                "manager_add_inventory?category=${category.name}"
                            )
                        },
                        onItemClick = { itemId ->
                            navController.navigate(
                                "manager_inventory_details/$itemId"
                            )
                        }
                    )
                }

                composable(
                    route = "manager_add_inventory?category={categoryName}",
                    arguments = listOf(
                        navArgument("categoryName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = "Fertilizer"
                        }
                    )
                ) { backStackEntry ->
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "Fertilizer"

                    val viewModel: InventoryViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerRoutes.INVENTORY,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddInventoryScreen(
                        initialCategory = categoryName,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveClick = { category, name, subtype, quantity, unit, reorderLevel, description ->
                            viewModel.addInventoryItem(
                                category = category,
                                name = name,
                                subtype = subtype,
                                quantity = quantity,
                                unit = unit,
                                reorderLevel = reorderLevel,
                                description = description
                            )
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = ManagerRoutes.INVENTORY_DETAILS,
                    arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()

                    val viewModel: InventoryViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerRoutes.INVENTORY,
                        backStackEntry = backStackEntry
                    )

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val item = (uiState as? InventoryUiState.Success)?.items
                        ?.find { it.id == itemId }

                    ManagerInventoryDetailsScreen(
                        itemName = item?.name ?: "Unknown item",
                        category = item?.category?.displayName ?: "",
                        quantity = item?.quantity?.toString() ?: "0",
                        unit = item?.unit ?: "",
                        reorderLevel = item?.reorderLevel?.toString() ?: "Not set",
                        isLowStock = item?.isLowStock ?: false,
                        notes = item?.description ?: "No additional notes recorded for this item.",
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEditClick = {
                            // No edit flow yet.
                        },
                        onDeleteClick = {
                            item?.let {
                                viewModel.deleteInventoryItem(it.category, itemId)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                // ---------------------------------------------------------
                // MACHINERY (In More)
                // ---------------------------------------------------------

                composable(ManagerRoutes.MACHINERY) {
                    val viewModel: MachineryViewModel = viewModel()

                    ManagerMachineryScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onMachineryClick = { machineryId ->
                            navController.navigate(
                                "manager_machinery_details/$machineryId"
                            )
                        },
                        onAddMachineryClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_MACHINERY
                            )
                        }
                    )
                }

                composable(ManagerRoutes.MACHINERY_IN_USE) {
                    ManagerMachineryInUseScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onMachineryClick = { machineryId ->
                            navController.navigate("manager_machinery_details/$machineryId")
                        }
                    )
                }

                composable(ManagerRoutes.MACHINERY_MAINTENANCE) {
                    ManagerMachineryMaintenanceScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = ManagerRoutes.MACHINERY_DETAILS,
                    arguments = listOf(navArgument("machineryId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val machineryId = backStackEntry.arguments?.getString("machineryId").orEmpty()

                    val viewModel: MachineryViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerRoutes.MACHINERY,
                        backStackEntry = backStackEntry
                    )

                    LaunchedEffect(machineryId) {
                        viewModel.loadMachineryDetail(machineryId)
                    }

                    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
                    val success = detailState as? MachineryDetailUiState.Success

                    val purchaseCostFormatted = success?.machinery?.purchaseCost?.let {
                        String.format(Locale.US, "KES %,.2f", it)
                    } ?: ""

                    ManagerMachineryDetailsScreen(
                        machineName = success?.machinery?.name ?: "Loading...",
                        machineType = success?.machinery?.machineryType ?: "",
                        status = success?.machinery?.status ?: "",
                        condition = success?.machinery?.condition ?: "",
                        location = success?.machinery?.location ?: "Not set",
                        purchaseDate = success?.machinery?.purchaseDate.orEmpty(),
                        purchaseCost = purchaseCostFormatted,
                        rawPurchaseCost = success?.machinery?.purchaseCost?.toString().orEmpty(),
                        totalHoursUsed = success?.let { "${it.totalHoursUsed} hrs" } ?: "0 hrs",
                        totalFuelUsed = success?.let { "${it.totalFuelUsed} litres" } ?: "0 litres",
                        lastMaintenance = success?.lastMaintenanceDate ?: "No maintenance recorded",
                        nextMaintenance = success?.nextMaintenanceDate ?: "Not scheduled",
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEditSave = { name, purchaseDate, purchaseCost ->
                            viewModel.updateMachineryDetails(machineryId, name, purchaseDate, purchaseCost)
                        },
                        onDeleteClick = {
                            viewModel.deleteMachinery(machineryId)
                            navController.popBackStack()
                        },
                        onUsageHistoryClick = {
                            navController.navigate(ManagerRoutes.MACHINERY_IN_USE)
                        },
                        onMaintenanceHistoryClick = {
                            navController.navigate(ManagerRoutes.MACHINERY_MAINTENANCE)
                        },
                        onRecordUsageSave = { hrs, fuel, date, notes ->
                            viewModel.recordUsage(machineryId, hrs, fuel, date, notes)
                        }
                    )
                }

                composable(ManagerRoutes.ADD_MACHINERY) { backStackEntry ->
                    val viewModel: MachineryViewModel = sharedViewModel(
                        navController = navController,
                        route = ManagerRoutes.MACHINERY,
                        backStackEntry = backStackEntry
                    )

                    ManagerAddMachineryScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveClick = { name, machineryType, model, serialNumber, purchaseDate, condition ->
                            viewModel.addMachinery(
                                name = name,
                                machineryType = machineryType,
                                model = model,
                                serialNumber = serialNumber,
                                purchaseDate = purchaseDate,
                                condition = condition
                            )
                        }
                    )
                }

                // ---------------------------------------------------------
                // MORE
                // ---------------------------------------------------------

                composable(ManagerTab.More.route) {
                    ManagerMoreScreen(
                        onBackClick = {
                            navigateToTab(ManagerTab.Dashboard.route)
                        },
                        onChatClick = {
                            navController.navigate(
                                ChatRoutes.LIST
                            )
                        },
                        onAttendanceClick = {
                            navController.navigate(
                                ManagerRoutes.ATTENDANCE
                            )
                        },
                        onInventoryClick = {
                            navController.navigate(
                                ManagerRoutes.INVENTORY
                            )
                        },
                        onMachineryClick = {
                            navController.navigate(
                                ManagerRoutes.MACHINERY
                            )
                        },
                        onStorageClick = {
                            navController.navigate(
                                ManagerRoutes.STORAGE
                            )
                        },
                        onYieldsClick = {
                            navController.navigate(
                                ManagerRoutes.YIELD
                            )
                        },
                        onReportsClick = {
                            navController.navigate(
                                ManagerRoutes.REPORTS
                            )
                        },
                        onCropRecommendationsClick = {
                            navController.navigate(
                                ManagerRoutes.CROP_RECOMMENDATIONS
                            )
                        },
                        onExpensesClick = {
                            navController.navigate(
                                ManagerRoutes.EXPENSES
                            )
                        },
                        onSettingsClick = {
                            navController.navigate(
                                ManagerRoutes.SETTINGS
                            )
                        },
                        onLogoutClick = onLogout
                    )
                }

                composable(ManagerRoutes.REPORTS) {
                    val reportsViewModel: ReportsViewModel = viewModel()
                    val uiState by reportsViewModel.state.collectAsStateWithLifecycle()
                    ManagerReportsScreen(
                        uiState = uiState,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onGenerateReportClick = {
                            navController.navigate(ManagerRoutes.GENERATE_REPORT)
                        },
                        onReportClick = { code ->
                            reportsViewModel.openByTitleOrCode(code)
                            navController.navigate("manager_report_details/$code")
                        }
                    )
                }

                composable(ManagerRoutes.GENERATE_REPORT) { backStackEntry ->
                    val reportsViewModel: ReportsViewModel = sharedViewModel(navController, ManagerRoutes.REPORTS, backStackEntry)
                    ManagerGenerateReportScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onGenerateClick = { selectedType ->
                            reportsViewModel.generateByCode(selectedType)
                            navController.popBackStack()
                            navController.navigate("manager_report_details/$selectedType")
                        }
                    )
                }

                composable(
                    route = ManagerRoutes.REPORT_DETAILS,
                    arguments = listOf(navArgument("reportTitle") { type = NavType.StringType })
                ) { backStackEntry ->
                    val reportTitle = backStackEntry.arguments?.getString("reportTitle") ?: "crop_yield"
                    val reportsViewModel: ReportsViewModel = sharedViewModel(navController, ManagerRoutes.REPORTS, backStackEntry)
                    val uiState by reportsViewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(reportTitle) {
                        if (uiState.detail == null || !uiState.detail!!.category.code.equals(reportTitle, ignoreCase = true)) {
                            reportsViewModel.openByTitleOrCode(reportTitle)
                        }
                    }

                    ManagerReportDetailsScreen(
                        detail = uiState.detail,
                        titleFallback = reportTitle,
                        isLoading = uiState.isLoading,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onRegenerateClick = {
                            navController.navigate(ManagerRoutes.GENERATE_REPORT)
                        }
                    )
                }

                composable(ManagerRoutes.CROP_RECOMMENDATIONS) {
                    ManagerRecommendationsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.SETTINGS) {
                    ManagerSettingsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onProfileClick = {
                            navController.navigate(ManagerRoutes.PROFILE)
                        },
                        onFarmDetailsClick = {
                            navController.navigate(ManagerRoutes.FARM_DETAILS)
                        },
                        onNotificationsClick = {
                            navController.navigate(ManagerRoutes.NOTIFICATIONS)
                        }
                    )
                }

                composable(ManagerRoutes.PROFILE) {
                    ManagerProfileScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.FARM_DETAILS) {
                    ManagerFarmDetailsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.NOTIFICATIONS) {
                    val viewModel: NotificationViewModel = viewModel()
                    ManagerNotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                // ---------------------------------------------------------
                // STORAGE
                // ---------------------------------------------------------

                composable(ManagerRoutes.STORAGE) {
                    ManagerStorageScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onAddClick = {
                            navController.navigate(
                                ManagerRoutes.ADD_STORAGE
                            )
                        },
                        onProduceClick = {
                            navController.navigate(
                                ManagerRoutes.STORAGE_DETAILS
                            )
                        }
                    )
                }

                composable(ManagerRoutes.ADD_STORAGE) {
                    ManagerAddStorageScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSaveSuccess = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.STORAGE_DETAILS) {
                    ManagerStorageDetailsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEditClick = {
                            // No edit flow yet.
                        },
                        onDeleteClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.ATTENDANCE) {
                    ManagerAttendanceScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ManagerRoutes.EXPENSES) {
                    val viewModel: ExpenseViewModel = viewModel()
                    ManagerExpenseScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                chatGraph(navController, chatRepository)
            }
        }
    }
}

