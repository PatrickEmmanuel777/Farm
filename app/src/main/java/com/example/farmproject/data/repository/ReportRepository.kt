@file:Suppress("unused")

package com.example.farmproject.data.repository

import com.example.farmproject.data.model.CropProductivityRow
import com.example.farmproject.data.model.CropYieldCards
import com.example.farmproject.data.model.CropYieldSummary
import com.example.farmproject.data.model.ExpenseCategoryBreakdown
import com.example.farmproject.data.model.ExpenseFilters
import com.example.farmproject.data.model.ExpenseListRow
import com.example.farmproject.data.model.ExpenseRow
import com.example.farmproject.data.model.ExpenseSummary
import com.example.farmproject.data.model.FacilityRow
import com.example.farmproject.data.model.HarvestRow
import com.example.farmproject.data.model.InventoryCategoryRow
import com.example.farmproject.data.model.InventorySummary
import com.example.farmproject.data.model.ProduceByCropRow
import com.example.farmproject.data.model.ReportCategoryStatus
import com.example.farmproject.data.model.ReportJson
import com.example.farmproject.data.model.ReportRecord
import com.example.farmproject.data.model.ReportSummary
import com.example.farmproject.data.model.StorageSummary
import com.example.farmproject.data.remote.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.Locale

/**
 * Data access for the Reports screen.
 */
class ReportRepository(private val supabase: SupabaseClient = SupabaseClientProvider.client) {

    suspend fun getCategories(farmId: String): List<ReportCategoryStatus> = runCatching {
        supabase.from("report_category_status")
            .select {
                filter { eq("farm_id", farmId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<ReportCategoryStatus>()
            .ifEmpty { defaultCategories(farmId) }
    }.getOrElse { defaultCategories(farmId) }

    suspend fun generate(
        farmId: String,
        categoryCode: String,
        periodStart: String? = null,
        periodEnd: String? = null,
    ): ReportRecord = runCatching {
        supabase.postgrest.rpc(
            function = "generate_report",
            parameters = buildJsonObject {
                put("p_farm_id", farmId)
                put("p_category", categoryCode)
                periodStart?.let { put("p_period_start", it) }
                periodEnd?.let { put("p_period_end", it) }
            },
        ).decodeAs<ReportRecord>()
    }.getOrElse {
        val summaryModel = fetchLiveSummary(farmId, categoryCode)
        val summaryJson = summaryModel?.let { encodeSummaryToJson(it) } ?: buildJsonObject {}

        ReportRecord(
            reportId = "rep-$categoryCode",
            farmId = farmId,
            categoryId = "cat-$categoryCode",
            generatedBy = "Manager",
            periodStart = periodStart,
            periodEnd = periodEnd,
            status = "completed",
            summary = summaryJson,
            fileUrl = null,
            generatedAt = "2026-09-12T10:00:00Z"
        )
    }

    suspend fun getReport(reportId: String): ReportRecord = runCatching {
        supabase.from("report")
            .select { filter { eq("report_id", reportId) } }
            .decodeSingle<ReportRecord>()
    }.getOrElse {
        val code = when {
            reportId.contains("inventory", ignoreCase = true) -> "inventory"
            reportId.contains("expense", ignoreCase = true) -> "expenses"
            reportId.contains("storage", ignoreCase = true) -> "storage"
            else -> "crop_yield"
        }
        val summaryModel = fetchLiveSummary("", code)
        val summaryJson = summaryModel?.let { encodeSummaryToJson(it) } ?: buildJsonObject {}

        ReportRecord(
            reportId = reportId,
            farmId = "",
            categoryId = "cat-$code",
            generatedBy = "Manager",
            periodStart = null,
            periodEnd = null,
            status = "completed",
            summary = summaryJson,
            fileUrl = null,
            generatedAt = "2026-09-12T10:00:00Z"
        )
    }

    suspend fun fetchLiveSummary(
        farmId: String,
        categoryCode: String
    ): ReportSummary? {
        return when (categoryCode.lowercase()) {
            "crop_yield" -> fetchLiveCropYieldSummary(farmId)
            "expenses" -> fetchLiveExpenseSummary(farmId)
            "inventory" -> fetchLiveInventorySummary(farmId)
            "storage" -> fetchLiveStorageSummary(farmId)
            else -> null
        }
    }

    private suspend fun fetchLiveCropYieldSummary(farmId: String): CropYieldSummary {
        val harvests = runCatching { CropRepository(supabase).getCropHarvestsForFarm(farmId) }.getOrDefault(emptyList())
        val plantings = runCatching { CropRepository(supabase).getCropPlantingsForFarm(farmId) }.getOrDefault(emptyList())
        val storage = runCatching { StorageRepository(supabase).getStorageForFarm(farmId) }.getOrDefault(emptyList())

        val totalHarvestKg = harvests.sumOf { it.harvest.quantityHarvested }
        val harvestCount = harvests.size
        val cropsHarvested = harvests.map { it.cropName }.distinct().size
        val totalAreaAcres = plantings.sumOf { it.planting.areaPlanted ?: 0.0 }
        val avgYield = if (totalAreaAcres > 0) totalHarvestKg / totalAreaAcres else 0.0
        val inStorage = storage.sumOf { it.safeQuantityKg }
        val totalSold = 0.0

        val harvestRows = harvests.map { h ->
            HarvestRow(
                harvestId = h.harvest.harvestId,
                crop = h.cropName,
                division = h.divisionName,
                harvestDate = h.harvest.harvestDate,
                quantity = h.harvest.quantityHarvested,
                unit = h.harvest.quantityUnit,
                quantityKg = h.harvest.quantityHarvested,
                grade = h.harvest.qualityGrade,
                notes = h.harvest.notes
            )
        }

        val cropGrouped = harvests.groupBy { it.cropName }
        val productivityRows = cropGrouped.map { (cropName, hList) ->
            val yieldKg = hList.sumOf { it.harvest.quantityHarvested }
            val cropPlantings = plantings.filter { it.cropName.equals(cropName, ignoreCase = true) }
            val area = cropPlantings.sumOf { it.planting.areaPlanted ?: 0.0 }.let { if (it > 0) it else 1.0 }
            CropProductivityRow(
                crop = cropName,
                harvests = hList.size,
                actualYieldKg = yieldKg,
                areaHarvestedAcres = area,
                yieldPerAcre = yieldKg / area
            )
        }

        return CropYieldSummary(
            cards = CropYieldCards(
                totalHarvestKg = totalHarvestKg,
                avgYieldPerAcre = avgYield,
                totalSoldKg = totalSold,
                inStorageKg = inStorage
            ),
            harvestCount = harvestCount,
            totalAreaAcres = totalAreaAcres,
            cropsHarvested = cropsHarvested,
            harvests = harvestRows,
            productivity = productivityRows
        )
    }

    private suspend fun fetchLiveExpenseSummary(farmId: String): ExpenseSummary {
        val page = runCatching { ExpenseRepository(supabase).listExpenses(farmId, ExpenseFilters(limit = 200)) }.getOrNull()?.getOrNull()
        val expenses: List<ExpenseListRow> = page?.items ?: emptyList()
        val totalAmount: Double = page?.totalAmount ?: expenses.sumOf { it.amount }
        val totalCount: Int = page?.totalCount?.toInt() ?: expenses.size
        val avgExpense = if (totalCount > 0) totalAmount / totalCount else 0.0
        val largestExpense = expenses.maxOfOrNull { it.amount } ?: 0.0

        val categoryGrouped = expenses.groupBy { it.categoryName }
        val categoryBreakdown = categoryGrouped.map { (catName: String, eList: List<ExpenseListRow>) ->
            val catTotal = eList.sumOf { it.amount }
            val pct = if (totalAmount > 0) (catTotal / totalAmount * 100) else 0.0
            ExpenseCategoryBreakdown(
                categoryName = catName,
                count = eList.size,
                total = catTotal,
                percentage = String.format(Locale.US, "%.1f", pct).toDoubleOrNull() ?: 0.0
            )
        }.sortedByDescending { it.total }

        val expenseRows = expenses.map { e ->
            ExpenseRow(
                expenseId = e.expenseId,
                description = e.description,
                category = e.categoryName,
                amount = e.amount,
                expenseDate = e.expenseDate,
                supplierName = e.supplierName,
                paymentMethod = e.paymentMethod
            )
        }

        return ExpenseSummary(
            totalExpenses = totalAmount,
            expenseCount = totalCount,
            averageExpense = avgExpense,
            largestExpense = largestExpense,
            byCategory = categoryBreakdown,
            topExpenses = expenseRows.sortedByDescending { it.amount }.take(5),
            expenses = expenseRows
        )
    }

    private suspend fun fetchLiveInventorySummary(farmId: String): InventorySummary {
        val items = runCatching { InventoryRepository(supabase).getInventoryForFarm(farmId) }.getOrDefault(emptyList())
        val lowStock = items.count { it.isLowStock }
        val outOfStock = items.count { it.quantity <= 0 }

        val categoryGrouped = items.groupBy { it.category.displayName }
        val byCategoryRows = categoryGrouped.map { (catName, list) ->
            InventoryCategoryRow(
                category = catName,
                items = list.size,
                lowStock = list.count { it.isLowStock },
                outOfStock = list.count { it.quantity <= 0 }
            )
        }

        return InventorySummary(
            totalItems = items.size,
            outOfStockCount = outOfStock,
            lowStockCount = lowStock,
            expiringSoonCount = 0,
            expiredCount = 0,
            byCategory = byCategoryRows
        )
    }

    private suspend fun fetchLiveStorageSummary(farmId: String): StorageSummary {
        val storageItems = runCatching { StorageRepository(supabase).getStorageForFarm(farmId) }.getOrDefault(emptyList())
        val totalStoredKg = storageItems.sumOf { it.safeQuantityKg }

        val cropGrouped = storageItems.groupBy { it.cropName }
        val produceByCropRows = cropGrouped.map { (cropName, list) ->
            ProduceByCropRow(
                cropName = cropName,
                storedKg = list.sumOf { it.safeQuantityKg },
                lots = list.size
            )
        }

        val facilityRows = listOf(
            FacilityRow(
                facilityId = "main-store",
                name = "Main Storage Facility",
                capacityKg = if (totalStoredKg > 0.0) totalStoredKg * 1.5 else 1000.0,
                storedKg = totalStoredKg,
                utilisationPct = if (totalStoredKg > 0) 67.0 else 0.0,
                condition = "Good"
            )
        )

        return StorageSummary(
            facilities = facilityRows,
            produceByCrop = produceByCropRows,
            nearExpiryLots = emptyList()
        )
    }

    private fun encodeSummaryToJson(summary: ReportSummary): JsonObject = when (summary) {
        is CropYieldSummary -> ReportJson.encodeToJsonElement(CropYieldSummary.serializer(), summary).jsonObject
        is InventorySummary -> ReportJson.encodeToJsonElement(InventorySummary.serializer(), summary).jsonObject
        is ExpenseSummary -> ReportJson.encodeToJsonElement(ExpenseSummary.serializer(), summary).jsonObject
        is StorageSummary -> ReportJson.encodeToJsonElement(StorageSummary.serializer(), summary).jsonObject
    }

    suspend fun getHistory(
        farmId: String,
        categoryId: String,
        limit: Long = 20,
    ): List<ReportRecord> = runCatching {
        supabase.from("report")
            .select {
                filter {
                    eq("farm_id", farmId)
                    eq("category_id", categoryId)
                }
                order("generated_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<ReportRecord>()
    }.getOrElse { emptyList() }
}

private fun defaultCategories(farmId: String): List<ReportCategoryStatus> = listOf(
    ReportCategoryStatus(
        farmId = farmId,
        categoryId = "cat-crop-yield",
        code = "crop_yield",
        name = "Crop Yield Report",
        description = "Planting, growth stage and harvest summaries",
        sortOrder = 1,
        lastReportId = "rep-crop-yield",
        lastGeneratedAt = "2026-09-12T10:00:00Z",
    ),
    ReportCategoryStatus(
        farmId = farmId,
        categoryId = "cat-inventory",
        code = "inventory",
        name = "Resources Report",
        description = "Stock levels, usage and reorder alerts",
        sortOrder = 2,
        lastReportId = "rep-inventory",
        lastGeneratedAt = "2026-09-10T14:30:00Z",
    ),
    ReportCategoryStatus(
        farmId = farmId,
        categoryId = "cat-expenses",
        code = "expenses",
        name = "Farm Expenses Report",
        description = "Operational costs, category breakdowns and supplier payments",
        sortOrder = 3,
        lastReportId = "rep-expenses",
        lastGeneratedAt = "2026-09-11T16:00:00Z",
    ),
    ReportCategoryStatus(
        farmId = farmId,
        categoryId = "cat-storage",
        code = "storage",
        name = "Storage Report",
        description = "Stored produce, capacity and condition",
        sortOrder = 4,
        lastReportId = "rep-storage",
        lastGeneratedAt = "2026-09-09T11:20:00Z",
    ),
)
