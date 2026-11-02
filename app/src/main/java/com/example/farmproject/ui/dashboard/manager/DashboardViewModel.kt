package com.example.farmproject.ui.dashboard.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.repository.CropRepository
import com.example.farmproject.data.repository.DivisionRepository
import com.example.farmproject.data.repository.EmployeeRepository
import com.example.farmproject.data.repository.ExpenseRepository
import com.example.farmproject.data.repository.FarmActivityRepository
import com.example.farmproject.data.repository.FarmRepository
import com.example.farmproject.data.repository.InventoryRepository
import com.example.farmproject.data.repository.MachineryRepository
import com.example.farmproject.data.repository.StorageRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CropYieldProgress(
    val cropName: String,
    val yieldKg: Double,
    val progress: Float
)

data class RecentHarvestItem(
    val title: String,
    val quantityText: String,
    val dateText: String
)

data class ExpenseCategoryProportion(
    val name: String,
    val percentage: Int,
    val amount: Double
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val ownerName: String = "Farm Owner",
        val farmName: String,
        val totalFarmSize: Double,
        val farmUnit: String,
        val totalDivisions: Int,
        val pendingApprovals: Int,
        val lowStockCount: Int,
        val maintenanceDueCount: Int,
        val totalHarvestKg: Double,
        val harvestChangePct: String,
        val avgYieldKgPerAc: Double,
        val yieldChangePct: String,
        val totalExpenses: Double,
        val expensesChangePct: String,
        val expenseProportions: List<ExpenseCategoryProportion>,
        val totalCrops: Int,
        val pendingActivities: Int,
        val totalHarvestsCount: Int,
        val totalStorage: Int,
        val totalWorkers: Int,
        val totalInventory: Int,
        val totalMachinery: Int,
        val yieldByCrop: List<CropYieldProgress>,
        val recentHarvests: List<RecentHarvestItem>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val farmRepository: FarmRepository = FarmRepository(),
    private val divisionRepository: DivisionRepository = DivisionRepository(),
    private val cropRepository: CropRepository = CropRepository(),
    private val employeeRepository: EmployeeRepository = EmployeeRepository(),
    private val farmActivityRepository: FarmActivityRepository = FarmActivityRepository(),
    private val inventoryRepository: InventoryRepository = InventoryRepository(),
    private val machineryRepository: MachineryRepository = MachineryRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val currentSuccess = _uiState.value as? DashboardUiState.Success
        viewModelScope.launch(Dispatchers.IO) {
            if (currentSuccess == null) {
                _uiState.value = DashboardUiState.Loading
            }
            try {
                val userId = CurrentSession.userId ?: ""
                var farmId = CurrentSession.farmId

                coroutineScope {
                    val farmDeferred = async {
                        if (farmId.isNotBlank()) farmRepository.getFarmById(farmId) else farmRepository.getFarmForUser(userId)
                    }
                    val divisionsDeferred = async { divisionRepository.getDivisionsForFarm(farmId) }
                    val farmCropsDeferred = async { cropRepository.getCropsForFarm(farmId) }
                    val plantingsDeferred = async { cropRepository.getCropPlantingsForFarm(farmId) }
                    val harvestsDeferred = async { cropRepository.getCropHarvestsForFarm(farmId) }
                    val workersDeferred = async { employeeRepository.getWorkersForFarm(farmId) }
                    val supervisorsDeferred = async { employeeRepository.getSupervisorsForFarm(farmId) }
                    val inventoryDeferred = async { inventoryRepository.getInventoryForFarm(farmId) }
                    val machineryDeferred = async { machineryRepository.getMachineryForFarm(farmId) }
                    val storageDeferred = async { storageRepository.getStorageForFarm(farmId) }
                    val activitiesDeferred = async { farmActivityRepository.getFarmActivitiesForFarm(farmId) }
                    val expenseDashboardDeferred = async { expenseRepository.getDashboard(farmId).getOrNull() }

                    val farm = farmDeferred.await()
                    if (farm?.farmId != null && farm.farmId.isNotBlank()) {
                        CurrentSession.farmId = farm.farmId
                        farmId = farm.farmId
                    }

                    val rawOwnerName = CurrentSession.fullName?.takeIf { it.isNotBlank() }
                        ?: farm?.name?.ifBlank { null }
                        ?: "Farm Owner"

                    val formattedOwnerName = rawOwnerName.trim().lowercase().split("\\s+".toRegex()).joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }

                    val divisions = divisionsDeferred.await()
                    val farmCrops = farmCropsDeferred.await()
                    val plantings = plantingsDeferred.await()
                    val harvests = harvestsDeferred.await()
                    val workers = workersDeferred.await()
                    val supervisors = supervisorsDeferred.await()
                    val inventory = inventoryDeferred.await()
                    val machinery = machineryDeferred.await()
                    val storage = storageDeferred.await()
                    val activities = activitiesDeferred.await()
                    val expenseDashboard = expenseDashboardDeferred.await()

                    val farmName = farm?.name?.ifBlank { null } ?: "My Farm"
                    val farmSize = farm?.totalSize ?: plantings.sumOf { it.planting.areaPlanted ?: 0.0 }
                    val farmUnit = farm?.unit?.ifBlank { null } ?: "Acres"

                    val cropsCount = farmCrops.getOrDefault(emptyList()).size
                    val divisionsCount = divisions.size
                    val totalEmployeesCount = workers.size + supervisors.size
                    val inventoryCount = inventory.size
                    val machineryCount = machinery.size
                    val storageCount = storage.size
                    val lowStockCountVal = inventory.count { it.isLowStock }
                    val maintenanceDueVal = machinery.count {
                        it.status.equals("maintenance", ignoreCase = true) || it.condition.equals("maintenance", ignoreCase = true)
                    }

                    val pendingCount = activities.count { display ->
                        val st = display.activity.safeStatus
                        !st.equals("In Progress", ignoreCase = true) && !st.equals("Completed", ignoreCase = true)
                    }

                    val totalHarvestKgVal = harvests.sumOf { it.harvest.quantityHarvested }
                    val avgYieldVal = if (farmSize > 0 && totalHarvestKgVal > 0) totalHarvestKgVal / farmSize else 0.0

                    // Expenses from Database
                    val liveTotalExpenses = expenseDashboard?.totalExpenditure
                        ?: expenseDashboard?.monthExpenditure
                        ?: 0.0

                    val liveProportions = if (expenseDashboard != null && expenseDashboard.byCategory.isNotEmpty() && liveTotalExpenses > 0) {
                        val sorted = expenseDashboard.byCategory.sortedByDescending { it.total }
                        val top3 = sorted.take(3)
                        val otherTotal = sorted.drop(3).sumOf { it.total }

                        val list = top3.map { cat ->
                            val pct = ((cat.total / liveTotalExpenses) * 100).toInt()
                            ExpenseCategoryProportion(cat.name.take(12), pct, cat.total)
                        }.toMutableList()

                        if (otherTotal > 0) {
                            val otherPct = ((otherTotal / liveTotalExpenses) * 100).toInt()
                            list.add(ExpenseCategoryProportion("Other", otherPct, otherTotal))
                        }
                        list
                    } else {
                        emptyList()
                    }

                    val cropGrouped = harvests.groupBy { it.cropName }
                    val maxYield = cropGrouped.map { it.value.sumOf { h -> h.harvest.quantityHarvested } }.maxOrNull() ?: 1.0
                    val liveCropProgress = cropGrouped.map { entry ->
                        val totalKg = entry.value.sumOf { it.harvest.quantityHarvested }
                        CropYieldProgress(
                            cropName = entry.key,
                            yieldKg = totalKg,
                            progress = (totalKg / maxYield).toFloat().coerceIn(0.1f, 1.0f)
                        )
                    }

                    val liveRecentHarvests = harvests.take(5).map { h ->
                        RecentHarvestItem(
                            title = "${h.cropName} · ${h.divisionName}",
                            quantityText = "${"%,.0f".format(h.harvest.quantityHarvested)} ${h.harvest.quantityUnit}",
                            dateText = h.harvest.harvestDate.takeLast(6).ifBlank { "Recent" }
                        )
                    }

                    _uiState.value = DashboardUiState.Success(
                        ownerName = formattedOwnerName,
                        farmName = farmName,
                        totalFarmSize = farmSize,
                        farmUnit = farmUnit,
                        totalDivisions = divisionsCount,
                        pendingApprovals = pendingCount,
                        lowStockCount = lowStockCountVal,
                        maintenanceDueCount = maintenanceDueVal,
                        totalHarvestKg = totalHarvestKgVal,
                        harvestChangePct = if (harvests.isNotEmpty()) "▲ 8% vs last season" else "No data yet",
                        avgYieldKgPerAc = avgYieldVal,
                        yieldChangePct = if (harvests.isNotEmpty()) "▲ 3% vs last season" else "No data yet",
                        totalExpenses = liveTotalExpenses,
                        expensesChangePct = if (liveTotalExpenses > 0) "▲ 12% vs last season" else "No data yet",
                        expenseProportions = liveProportions,
                        totalCrops = cropsCount,
                        pendingActivities = pendingCount,
                        totalHarvestsCount = harvests.size,
                        totalStorage = storageCount,
                        totalWorkers = totalEmployeesCount,
                        totalInventory = inventoryCount,
                        totalMachinery = machineryCount,
                        yieldByCrop = liveCropProgress,
                        recentHarvests = liveRecentHarvests
                    )
                }
            } catch (e: Exception) {
                if (currentSuccess == null) {
                    _uiState.value = DashboardUiState.Error(e.message ?: "Failed to load dashboard data")
                }
            }
        }
    }
}
