package com.example.farmproject.ui.reports.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmproject.R
import com.example.farmproject.data.model.CropYieldSummary
import com.example.farmproject.data.model.ExpenseSummary
import com.example.farmproject.data.model.InventorySummary
import com.example.farmproject.data.model.ReportCategoryStatus
import com.example.farmproject.data.model.StorageSummary
import com.example.farmproject.data.model.lastGeneratedLabel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerReportsScreen(
    uiState: ReportsUiState = ReportsUiState(),
    onBackClick: () -> Unit = {},
    onGenerateReportClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Categories, 1 = Summaries

    val categories = uiState.categories.ifEmpty {
        defaultReportCategories()
    }

    val softCreamBg = Color(0xFFF7F6F0)

    Scaffold(
        containerColor = softCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Custom Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1A1D1A)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "Reports & Summaries",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1D1A)
                        )
                        Text(
                            text = "Executive farm performance summaries & reports",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7068)
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFF1A1D1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Tabs: Categories vs Summaries
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Categories") },
                    icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = null) }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Summaries") },
                    icon = { Icon(imageVector = Icons.Default.Assessment, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // CATEGORIES TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ReportsSummaryCard(totalCategories = categories.size)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Report Categories",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1D1A)
                            )

                            OutlinedButton(
                                onClick = onGenerateReportClick,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF1B5E20)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Generate",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    items(categories) { category ->
                        ReportCategoryCard(
                            category = category,
                            onClick = { onReportClick(category.code) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // SUMMARIES TAB (FOUR EXECUTIVE SUMMARY CARDS NAVIGATING TO DETAILS)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Executive Summaries",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
                        )
                    }

                    // 1. Crop Yield Summary Card
                    item {
                        CropYieldSummaryCard(
                            cropYieldSummary = uiState.cropYieldSummary,
                            onClick = { onReportClick("crop_yield") }
                        )
                    }

                    // 2. Resources Summary Card
                    item {
                        ResourcesSummaryCard(
                            inventorySummary = uiState.inventorySummary,
                            onClick = { onReportClick("inventory") }
                        )
                    }

                    // 3. Farm Expenses Summary Card
                    item {
                        ExpenseSummaryCard(
                            expenseSummary = uiState.expenseSummary,
                            onClick = { onReportClick("expenses") }
                        )
                    }

                    // 4. Storage Summary Card
                    item {
                        StorageSummaryCard(
                            storageSummary = uiState.storageSummary,
                            onClick = { onReportClick("storage") }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CropYieldSummaryCard(
    cropYieldSummary: CropYieldSummary?,
    onClick: () -> Unit
) {
    val totalHarvestVal = cropYieldSummary?.cards?.totalHarvestKg ?: 0.0
    val totalAreaVal = cropYieldSummary?.totalAreaAcres ?: 0.0
    val avgYieldVal = cropYieldSummary?.cards?.avgYieldPerAcre ?: (if (totalAreaVal > 0) totalHarvestVal / totalAreaVal else 0.0)
    val harvestCountVal = cropYieldSummary?.harvestCount ?: 0
    val cropsHarvestedVal = cropYieldSummary?.cropsHarvested ?: 0
    val totalSoldVal = cropYieldSummary?.cards?.totalSoldKg ?: 0.0
    val inStorageVal = cropYieldSummary?.cards?.inStorageKg ?: 0.0
    val remainingVal = (totalHarvestVal - totalSoldVal - inStorageVal).coerceAtLeast(0.0)

    SummaryCardBase(
        title = "Crop Yield Summary",
        subtitle = "Tap to view full crop yield report",
        iconRes = R.drawable.ic_cat_yield,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Total Harvest", value = "${"%,.0f".format(totalHarvestVal)} kg")
                SummaryPill(label = "Area Harvested", value = "${"%,.0f".format(totalAreaVal)} acres")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Avg Yield / Acre", value = "${"%,.0f".format(avgYieldVal)} kg/acre")
                SummaryPill(label = "Number of Harvests", value = "$harvestCountVal")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Crops Harvested", value = "$cropsHarvestedVal")
                SummaryPill(label = "Total Sold", value = "${"%,.0f".format(totalSoldVal)} kg")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "In Storage", value = "${"%,.0f".format(inStorageVal)} kg")
                SummaryPill(label = "Produce Remaining", value = "${"%,.0f".format(remainingVal)} kg")
            }
        }
    }
}

@Composable
private fun ResourcesSummaryCard(
    inventorySummary: InventorySummary?,
    onClick: () -> Unit
) {
    val totalItemsVal = inventorySummary?.totalItems ?: 0
    val lowStockVal = inventorySummary?.lowStockCount ?: 0
    val outOfStockVal = inventorySummary?.outOfStockCount ?: 0
    val expiringVal = inventorySummary?.expiringSoonCount ?: 0

    SummaryCardBase(
        title = "Resources Summary",
        subtitle = "Tap to view full inventory report",
        iconRes = R.drawable.ic_cat_fertilizer,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Total Stock Items", value = "$totalItemsVal items")
                SummaryPill(label = "Low Stock Alerts", value = "$lowStockVal items")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Out of Stock", value = "$outOfStockVal items")
                SummaryPill(label = "Expiring Soon", value = "$expiringVal items")
            }
        }
    }
}

@Composable
private fun ExpenseSummaryCard(
    expenseSummary: ExpenseSummary?,
    onClick: () -> Unit
) {
    val totalExpensesVal = expenseSummary?.totalExpenses ?: 0.0
    val expenseCountVal = expenseSummary?.expenseCount ?: 0
    val avgExpenseVal = expenseSummary?.averageExpense ?: (if (expenseCountVal > 0) totalExpensesVal / expenseCountVal else 0.0)
    val largestExpenseVal = expenseSummary?.largestExpense ?: 0.0

    SummaryCardBase(
        title = "Farm Expenses Summary",
        subtitle = "Tap to view full operational expenses report",
        iconRes = R.drawable.ic_cat_reports,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Total Expenses", value = String.format(Locale.US, "KES %,.2f", totalExpensesVal))
                SummaryPill(label = "Expense Records", value = "$expenseCountVal logs")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Average Expense", value = String.format(Locale.US, "KES %,.2f", avgExpenseVal))
                SummaryPill(label = "Largest Expense", value = String.format(Locale.US, "KES %,.2f", largestExpenseVal))
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(
    storageSummary: StorageSummary?,
    onClick: () -> Unit
) {
    val facilitiesCount = storageSummary?.facilities?.size ?: 0
    val totalStoredVal = storageSummary?.produceByCrop?.sumOf { it.storedKg } ?: 0.0
    val nearExpiryVal = storageSummary?.nearExpiryLots?.size ?: 0
    val avgUtilisation = storageSummary?.facilities?.map { it.utilisationPct }?.average()?.takeIf { !it.isNaN() } ?: 0.0

    SummaryCardBase(
        title = "Storage & Produce Summary",
        subtitle = "Tap to view full storage produce report",
        iconRes = R.drawable.ic_cat_storage,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Storage Facilities", value = "$facilitiesCount units")
                SummaryPill(label = "Total Stored Produce", value = "${"%,.0f".format(totalStoredVal)} kg")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryPill(label = "Near Expiry Lots", value = "$nearExpiryVal lots")
                SummaryPill(label = "Average Utilisation", value = "${"%,.0f".format(avgUtilisation)}%")
            }
        }
    }
}

@Composable
private fun SummaryCardBase(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1D1A)
                    )

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF6E7068)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Details",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F6F0), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6E7068)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1D1A)
        )
    }
}

@Composable
private fun ReportsSummaryCard(totalCategories: Int) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF277A3C),
            Color(0xFF1B5E20)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = totalCategories.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Report categories available",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCategoryCard(
    category: ReportCategoryStatus,
    onClick: () -> Unit
) {
    val reportIconRes = when (category.code.lowercase()) {
        "inventory" -> R.drawable.ic_cat_fertilizer
        "expenses" -> R.drawable.ic_cat_reports
        "storage" -> R.drawable.ic_cat_storage
        else -> R.drawable.ic_cat_yield
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = reportIconRes),
                contentDescription = category.name,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                Text(
                    text = category.description,
                    fontSize = 13.sp,
                    color = Color(0xFF6E7068)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF2E7D32)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = category.lastGeneratedLabel(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1D1A)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun defaultReportCategories(): List<ReportCategoryStatus> = listOf(
    ReportCategoryStatus(
        farmId = "",
        categoryId = "cat-crop-yield",
        code = "crop_yield",
        name = "Crop Yield Report",
        description = "Planting, growth stage and harvest summaries",
        lastReportId = "rep-crop-yield",
        lastGeneratedAt = "2026-10-15T10:00:00Z"
    ),
    ReportCategoryStatus(
        farmId = "",
        categoryId = "cat-inventory",
        code = "inventory",
        name = "Resources Report",
        description = "Stock levels, usage and reorder alerts",
        lastReportId = "rep-inventory",
        lastGeneratedAt = "2026-10-15T14:30:00Z"
    ),
    ReportCategoryStatus(
        farmId = "",
        categoryId = "cat-expenses",
        code = "expenses",
        name = "Farm Expenses Report",
        description = "Operational costs, category breakdowns and supplier payments",
        lastReportId = "rep-expenses",
        lastGeneratedAt = "2026-10-15T16:00:00Z"
    ),
    ReportCategoryStatus(
        farmId = "",
        categoryId = "cat-storage",
        code = "storage",
        name = "Storage Report",
        description = "Stored produce, capacity and condition",
        lastReportId = "rep-storage",
        lastGeneratedAt = "2026-10-15T11:20:00Z"
    )
)
