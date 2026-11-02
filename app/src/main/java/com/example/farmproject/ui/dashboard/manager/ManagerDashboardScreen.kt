package com.example.farmproject.ui.dashboard.manager

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.farmproject.data.session.CurrentSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onCropsClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onWorkersClick: () -> Unit = {},
    onMachineryClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onDivisionsClick: () -> Unit = {},
    onActivitiesClick: () -> Unit = {},
    onYieldsClick: () -> Unit = {},
    onAddCropClick: () -> Unit = {},
    onLogActivityClick: () -> Unit = {},
    onAddWorkerClick: () -> Unit = {},
    onReportsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDashboardData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val softCreamBg = Color(0xFFF8F6EF)

    Scaffold(
        containerColor = softCreamBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // HERO HEADER
            item {
                val state = uiState as? DashboardUiState.Success
                FarmOverviewHeroHeader(
                    ownerName = state?.ownerName ?: CurrentSession.fullName ?: "Farm Owner",
                    farmName = state?.farmName ?: "Farm Overview",
                    totalFarmSize = state?.totalFarmSize ?: 0.0,
                    farmUnit = state?.farmUnit ?: "Acres",
                    totalDivisions = state?.totalDivisions ?: 0,
                    pendingApprovals = state?.pendingApprovals ?: 0,
                    onDivisionsClick = onDivisionsClick,
                    onActivitiesClick = onActivitiesClick
                )
            }

            // BODY CONTENT
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1B5E20))
                        }
                    }
                }

                is DashboardUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadDashboardData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is DashboardUiState.Success -> {
                    // Pinned Needs Attention Today Card
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            NeedsAttentionTodayCard(
                                pendingActivities = state.pendingActivities,
                                lowStockCount = state.lowStockCount,
                                maintenanceDueCount = state.maintenanceDueCount,
                                onActivitiesClick = onActivitiesClick,
                                onInventoryClick = onInventoryClick
                            )
                        }
                    }

                    // Quick Actions
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            QuickActionsSection(
                                onAddCropClick = onAddCropClick,
                                onLogActivityClick = onLogActivityClick,
                                onAddWorkerClick = onAddWorkerClick,
                                onReportsClick = onReportsClick
                            )
                        }
                    }

                    // Performance Summary
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Performance Summary",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1D1A)
                                )

                                SeasonSelectorPill()
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Total Harvest",
                                    value = if (state.totalHarvestKg > 0) "${"%,.0f".format(state.totalHarvestKg)} kg" else "0 kg",
                                    changeText = state.harvestChangePct,
                                    isPositive = state.totalHarvestKg > 0
                                )

                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Average Yield",
                                    value = if (state.avgYieldKgPerAc > 0) "${"%,.1f".format(state.avgYieldKgPerAc)} kg/ac" else "No data yet",
                                    changeText = state.yieldChangePct,
                                    isPositive = state.avgYieldKgPerAc > 0
                                )
                            }

                            // Total Expenses Card
                            ExpensesSummaryCard(
                                totalExpenses = state.totalExpenses,
                                changeText = state.expensesChangePct,
                                proportions = state.expenseProportions
                            )
                        }
                    }

                    // Farm Operations
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FarmOperationsSection(
                                cropsCount = state.totalCrops,
                                resourcesCount = state.totalInventory,
                                harvestsCount = state.totalHarvestsCount,
                                storageCount = state.totalStorage,
                                onCropsClick = onCropsClick,
                                onInventoryClick = onInventoryClick,
                                onYieldsClick = onYieldsClick,
                                onStorageClick = onStorageClick
                            )
                        }
                    }

                    // Yield by Crop
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Yield by Crop",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1D1A)
                            )

                            YieldByCropCard(items = state.yieldByCrop)
                        }
                    }

                    // Recent Harvests
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
                                    text = "Recent Harvests",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1D1A)
                                )

                                TextButton(onClick = onYieldsClick) {
                                    Text(
                                        text = "View all",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }

                            RecentHarvestsCard(harvests = state.recentHarvests)
                        }
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
private fun NeedsAttentionTodayCard(
    pendingActivities: Int,
    lowStockCount: Int,
    maintenanceDueCount: Int,
    onActivitiesClick: () -> Unit,
    onInventoryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Needs Attention Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                val totalAttention = pendingActivities + lowStockCount + maintenanceDueCount
                Box(
                    modifier = Modifier
                        .background(
                            color = if (totalAttention > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (totalAttention > 0) "$totalAttention items" else "Clear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAttention > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }
            }

            if (pendingActivities == 0 && lowStockCount == 0 && maintenanceDueCount == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All farm operations & inventory in optimal order today.",
                        fontSize = 13.sp,
                        color = Color(0xFF6E7068)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingActivities > 0) {
                        AttentionRowItem(
                            text = "$pendingActivities pending activities awaiting review or completion",
                            onClick = onActivitiesClick
                        )
                    }

                    if (lowStockCount > 0) {
                        AttentionRowItem(
                            text = "$lowStockCount inventory items low in stock",
                            onClick = onInventoryClick
                        )
                    }

                    if (maintenanceDueCount > 0) {
                        AttentionRowItem(
                            text = "$maintenanceDueCount machinery items due for service",
                            onClick = onInventoryClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttentionRowItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFFF9F9F6), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFF2C312D),
                fontWeight = FontWeight.Medium
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF8D6E63),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FarmOverviewHeroHeader(
    ownerName: String,
    farmName: String,
    totalFarmSize: Double,
    farmUnit: String,
    totalDivisions: Int,
    pendingApprovals: Int,
    onDivisionsClick: () -> Unit,
    onActivitiesClick: () -> Unit
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    text = "Welcome back, $ownerName 👋",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (farmName.isNotBlank() && !farmName.equals("My Farm", ignoreCase = true)) farmName else "Farm Overview",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val sizeFormatted = if (totalFarmSize % 1.0 == 0.0) totalFarmSize.toInt().toString() else "%.1f".format(totalFarmSize)

                HeaderStatCard(
                    modifier = Modifier.weight(1f),
                    value = sizeFormatted,
                    label = farmUnit,
                    onClick = {}
                )

                HeaderStatCard(
                    modifier = Modifier.weight(1f),
                    value = totalDivisions.toString(),
                    label = "Divisions",
                    onClick = onDivisionsClick
                )

                HeaderStatCard(
                    modifier = Modifier.weight(1f),
                    value = pendingApprovals.toString(),
                    label = "Pending Activities",
                    onClick = onActivitiesClick
                )
            }
        }
    }
}

@Composable
private fun HeaderStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF2D6A3E),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
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
private fun QuickActionsSection(
    onAddCropClick: () -> Unit,
    onLogActivityClick: () -> Unit,
    onAddWorkerClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Quick Actions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1D1A)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Add Crop",
                onClick = onAddCropClick
            )

            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = "Log Activity",
                onClick = onLogActivityClick
            )

            QuickActionButton(
                icon = Icons.Default.PersonAdd,
                label = "Add Worker",
                onClick = onAddWorkerClick
            )

            QuickActionButton(
                icon = Icons.Default.BarChart,
                label = "Reports",
                onClick = onReportsClick
            )
        }
    }
}

@Composable
private fun QuickActionButton(
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
private fun SeasonSelectorPill() {
    Box(
        modifier = Modifier
            .background(Color.White, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "This Season",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF1B5E20),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    changeText: String,
    isPositive: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF6E7068)
            )

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1D1A)
            )

            Text(
                text = changeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPositive) Color(0xFF1B5E20) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun ExpensesSummaryCard(
    totalExpenses: Double,
    changeText: String,
    proportions: List<ExpenseCategoryProportion>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    text = "Total Expenses",
                    fontSize = 13.sp,
                    color = Color(0xFF6E7068)
                )

                Text(
                    text = changeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA53A2A)
                )
            }

            Text(
                text = if (totalExpenses > 0) "KES ${"%,.0f".format(totalExpenses)}" else "KES 0",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1D1A)
            )

            if (proportions.isNotEmpty()) {
                val barColors = listOf(
                    Color(0xFF277A3C),
                    Color(0xFFE65100),
                    Color(0xFF1976D2),
                    Color(0xFF9E9E9E)
                )
                val totalPct = proportions.sumOf { it.percentage }.coerceAtLeast(1)

                // Segmented Color Proportion Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(4.dp))
                ) {
                    proportions.forEachIndexed { index, prop ->
                        val weightVal = (prop.percentage.toFloat() / totalPct.toFloat()).coerceAtLeast(0.01f)
                        Box(
                            modifier = Modifier
                                .weight(weightVal)
                                .fillMaxSize()
                                .background(barColors.getOrElse(index) { Color(0xFF9E9E9E) })
                        )
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    proportions.forEachIndexed { _, prop ->
                        Text(
                            text = "● ${prop.name} ${prop.percentage}%",
                            fontSize = 10.sp,
                            color = Color(0xFF6E7068)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FarmOperationsSection(
    cropsCount: Int,
    resourcesCount: Int,
    harvestsCount: Int,
    storageCount: Int,
    onCropsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onYieldsClick: () -> Unit,
    onStorageClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Farm Operations",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1D1A)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OperationGridCard(
                modifier = Modifier.weight(1f),
                label = "Registered Crops",
                value = cropsCount.toString(),
                onClick = onCropsClick
            )

            OperationGridCard(
                modifier = Modifier.weight(1f),
                label = "Resources",
                value = resourcesCount.toString(),
                onClick = onInventoryClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OperationGridCard(
                modifier = Modifier.weight(1f),
                label = "Harvests",
                value = harvestsCount.toString(),
                onClick = onYieldsClick
            )

            OperationGridCard(
                modifier = Modifier.weight(1f),
                label = "Storage & Produce",
                value = storageCount.toString(),
                onClick = onStorageClick
            )
        }
    }
}

@Composable
private fun OperationGridCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF6E7068)
            )

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1D1A)
            )
        }
    }
}

@Composable
private fun YieldByCropCard(
    items: List<CropYieldProgress>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (items.isNotEmpty()) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.cropName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A1D1A),
                            modifier = Modifier.width(80.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp),
                            color = Color(0xFF1B5E20),
                            trackColor = Color(0xFFEFEBE9),
                            drawStopIndicator = {}
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "${"%,.0f".format(item.yieldKg)} kg",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
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
                        text = "No crop yield records found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6E7068)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentHarvestsCard(
    harvests: List<RecentHarvestItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (harvests.isNotEmpty()) {
                harvests.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A1D1A)
                        )

                        Text(
                            text = "${item.quantityText} · ${item.dateText}",
                            fontSize = 13.sp,
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
                        text = "No recent harvest records found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6E7068)
                    )
                }
            }
        }
    }
}
