@file:Suppress("unused")

package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tolerant JSON config used to decode report summaries. */
val ReportJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

// ---------------------------------------------------------------------
// Rows from the report_category_status view (one card on the screen)
// ---------------------------------------------------------------------
@Serializable
data class ReportCategoryStatus(
    @SerialName("farm_id") val farmId: String,
    @SerialName("category_id") val categoryId: String,
    val code: String,                       // crop_yield, inventory, expenses, storage
    val name: String,
    val description: String,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("last_report_id") val lastReportId: String? = null,
    @SerialName("last_generated_at") val lastGeneratedAt: String? = null,
)

/** A row from the report table (also what generate_report returns). */
@Serializable
data class ReportRecord(
    @SerialName("report_id") val reportId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("generated_by") val generatedBy: String? = null,
    @SerialName("period_start") val periodStart: String? = null,   // yyyy-MM-dd
    @SerialName("period_end") val periodEnd: String? = null,
    val status: String = "completed",
    val summary: JsonObject = JsonObject(emptyMap()),
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("generated_at") val generatedAt: String,
)

/** "Last generated: 12 Sep 2026" / "Not generated yet" */
fun ReportCategoryStatus.lastGeneratedLabel(zone: ZoneId = ZoneId.of("Africa/Nairobi")): String {
    val iso = lastGeneratedAt ?: return "Not generated yet"
    return runCatching {
        val formatted = OffsetDateTime.parse(iso)
            .atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        "Last generated: $formatted"
    }.getOrDefault("Not generated yet")
}

// ---------------------------------------------------------------------
// Typed summaries (mirror the jsonb built by the SQL functions)
// ---------------------------------------------------------------------
sealed interface ReportSummary

// ----- Crop Yield (Matches build_crop_yield_summary SQL function) -----
@Serializable data class CropYieldCards(
    @SerialName("total_harvest_kg") val totalHarvestKg: Double = 0.0,
    @SerialName("avg_yield_per_acre") val avgYieldPerAcre: Double = 0.0,
    @SerialName("total_sold_kg") val totalSoldKg: Double? = null,
    @SerialName("in_storage_kg") val inStorageKg: Double = 0.0,
)

@Serializable data class HarvestRow(
    @SerialName("harvest_id") val harvestId: String? = null,
    val crop: String? = null,
    val division: String? = null,
    @SerialName("harvest_date") val harvestDate: String? = null,
    val quantity: Double = 0.0,
    val unit: String? = null,
    @SerialName("quantity_kg") val quantityKg: Double = 0.0,
    val grade: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    val notes: String? = null,
)

@Serializable data class CropProductivityRow(
    val crop: String,
    val harvests: Int = 0,
    @SerialName("actual_yield_kg") val actualYieldKg: Double = 0.0,
    @SerialName("area_harvested_acres") val areaHarvestedAcres: Double = 0.0,
    @SerialName("yield_per_acre") val yieldPerAcre: Double = 0.0,
)

@Serializable data class StatusCount(val status: String = "unknown", val plantings: Int = 0)
@Serializable data class HarvestTotal(val unit: String? = null, val quantity: Double = 0.0, val harvests: Int = 0)
@Serializable data class CropYieldRow(
    @SerialName("crop_name") val cropName: String? = null,
    val unit: String? = null,
    val plantings: Int = 0,
    val harvested: Double = 0.0,
)
@Serializable data class DivisionYieldRow(
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("division_name") val divisionName: String? = null,
    val unit: String? = null,
    val plantings: Int = 0,
    val harvested: Double = 0.0,
)
@Serializable data class GradeRow(val grade: String = "ungraded", val unit: String? = null, val quantity: Double = 0.0)
@Serializable data class PlantingRow(
    @SerialName("planting_id") val plantingId: String,
    @SerialName("crop_name") val cropName: String? = null,
    @SerialName("division_name") val divisionName: String? = null,
    @SerialName("planting_date") val plantingDate: String? = null,
    @SerialName("planting_status") val plantingStatus: String? = null,
    @SerialName("area_planted") val areaPlanted: Double? = null,
    @SerialName("area_unit") val areaUnit: String? = null,
    @SerialName("expected_first_harvest_date") val expectedFirstHarvestDate: String? = null,
    val unit: String? = null,
    val harvested: Double = 0.0,
    val harvests: Int = 0,
    @SerialName("target_qty") val targetQty: Double? = null,
    @SerialName("target_achieved_pct") val targetAchievedPct: Double? = null,
)

@Serializable
data class CropYieldSummary(
    val cards: CropYieldCards? = null,
    @SerialName("harvest_count") val harvestCount: Int = 0,
    @SerialName("total_area_acres") val totalAreaAcres: Double = 0.0,
    @SerialName("crops_harvested") val cropsHarvested: Int = 0,
    @SerialName("unconverted_units") val unconvertedUnits: List<String> = emptyList(),
    val harvests: List<HarvestRow> = emptyList(),
    val productivity: List<CropProductivityRow> = emptyList(),

    // Legacy fields for backward compatibility
    @SerialName("total_plantings") val totalPlantings: Int = 0,
    @SerialName("plantings_by_status") val plantingsByStatus: List<StatusCount> = emptyList(),
    @SerialName("harvest_totals") val harvestTotals: List<HarvestTotal> = emptyList(),
    @SerialName("by_crop") val byCrop: List<CropYieldRow> = emptyList(),
    @SerialName("by_division") val byDivision: List<DivisionYieldRow> = emptyList(),
    @SerialName("by_quality_grade") val byQualityGrade: List<GradeRow> = emptyList(),
    val plantings: List<PlantingRow> = emptyList(),
) : ReportSummary

// ----- Inventory -----
@Serializable data class InventoryCategoryRow(
    val category: String,
    val items: Int = 0,
    @SerialName("low_stock") val lowStock: Int = 0,
    @SerialName("out_of_stock") val outOfStock: Int = 0,
)
@Serializable data class ReorderAlert(
    val category: String,
    @SerialName("item_id") val itemId: String,
    val name: String,
    val quantity: Double = 0.0,
    val unit: String? = null,
    @SerialName("reorder_level") val reorderLevel: Double? = null,
)
@Serializable data class ExpiryAlert(
    val category: String,
    @SerialName("item_id") val itemId: String,
    val name: String,
    val quantity: Double = 0.0,
    val unit: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("is_expired") val isExpired: Boolean = false,
)
@Serializable data class InventoryUsageRow(
    val category: String,
    val unit: String? = null,
    @SerialName("quantity_used") val quantityUsed: Double = 0.0,
    val applications: Int = 0,
)
@Serializable data class WaterUsageRow(
    val unit: String? = null,
    @SerialName("quantity_used") val quantityUsed: Double = 0.0,
    val records: Int = 0,
)
@Serializable
data class InventorySummary(
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("out_of_stock_count") val outOfStockCount: Int = 0,
    @SerialName("low_stock_count") val lowStockCount: Int = 0,
    @SerialName("expiring_soon_count") val expiringSoonCount: Int = 0,
    @SerialName("expired_count") val expiredCount: Int = 0,
    @SerialName("by_category") val byCategory: List<InventoryCategoryRow> = emptyList(),
    @SerialName("reorder_alerts") val reorderAlerts: List<ReorderAlert> = emptyList(),
    @SerialName("expiry_alerts") val expiryAlerts: List<ExpiryAlert> = emptyList(),
    @SerialName("usage_by_category") val usageByCategory: List<InventoryUsageRow> = emptyList(),
    @SerialName("water_usage") val waterUsage: List<WaterUsageRow> = emptyList(),
) : ReportSummary

// ----- Expenses (Matches build_expenses_summary SQL function) -----
@Serializable data class ExpenseCategoryBreakdown(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val category: String? = null,
    val count: Int = 0,
    val total: Double = 0.0,
    val percentage: Double = 0.0,
) {
    val name: String get() = categoryName ?: category ?: "Uncategorised"
}

@Serializable data class ExpenseDivisionBreakdown(
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("division_name") val divisionName: String? = null,
    val count: Int = 0,
    val total: Double = 0.0,
)

@Serializable data class ExpenseMonthBreakdown(
    val month: String,
    val total: Double = 0.0,
)

@Serializable data class ExpensePaymentMethodBreakdown(
    val method: String,
    val total: Double = 0.0,
)

@Serializable data class ExpenseRow(
    @SerialName("expense_id") val expenseId: String? = null,
    val description: String,
    val category: String? = null,
    val amount: Double = 0.0,
    @SerialName("expense_date") val expenseDate: String? = null,
    val supplier: String? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
) {
    val payee: String? get() = supplierName ?: supplier
}

@Serializable
data class ExpenseSummary(
    @SerialName("total_expenses") val totalExpenses: Double = 0.0,
    @SerialName("expense_count") val expenseCount: Int = 0,
    @SerialName("average_expense") val averageExpense: Double = 0.0,
    @SerialName("largest_expense") val largestExpense: Double = 0.0,
    @SerialName("by_category") val byCategory: List<ExpenseCategoryBreakdown> = emptyList(),
    @SerialName("by_division") val byDivision: List<ExpenseDivisionBreakdown> = emptyList(),
    @SerialName("by_month") val byMonth: List<ExpenseMonthBreakdown> = emptyList(),
    @SerialName("by_payment_method") val byPaymentMethod: List<ExpensePaymentMethodBreakdown> = emptyList(),
    @SerialName("top_expenses") val topExpenses: List<ExpenseRow> = emptyList(),
    val expenses: List<ExpenseRow> = emptyList(),
) : ReportSummary

// ----- Storage -----
@Serializable data class FacilityRow(
    @SerialName("facility_id") val facilityId: String,
    val name: String,
    @SerialName("facility_type") val facilityType: String? = null,
    @SerialName("capacity_kg") val capacityKg: Double = 0.0,
    val condition: String? = null,
    @SerialName("stored_kg") val storedKg: Double = 0.0,
    @SerialName("utilisation_pct") val utilisationPct: Double = 0.0,
    @SerialName("at_risk_lots") val atRiskLots: Int = 0,
)
@Serializable data class ProduceByCropRow(
    @SerialName("crop_name") val cropName: String,
    @SerialName("stored_kg") val storedKg: Double = 0.0,
    val lots: Int = 0,
)
@Serializable data class NearExpiryRow(
    @SerialName("stock_id") val stockId: String,
    @SerialName("crop_name") val cropName: String,
    @SerialName("quantity_kg") val quantityKg: Double = 0.0,
    val condition: String? = null,
    @SerialName("expires_on") val expiresOn: String? = null,
)
@Serializable
data class StorageSummary(
    val facilities: List<FacilityRow> = emptyList(),
    @SerialName("produce_by_crop") val produceByCrop: List<ProduceByCropRow> = emptyList(),
    @SerialName("near_expiry_lots") val nearExpiryLots: List<NearExpiryRow> = emptyList(),
) : ReportSummary

/** Decode the raw jsonb summary into the right typed model for a category code. */
fun parseReportSummary(code: String, summary: JsonObject): ReportSummary? = runCatching {
    when (code) {
        "crop_yield" -> ReportJson.decodeFromJsonElement(CropYieldSummary.serializer(), summary)
        "inventory"  -> ReportJson.decodeFromJsonElement(InventorySummary.serializer(), summary)
        "expenses"   -> ReportJson.decodeFromJsonElement(ExpenseSummary.serializer(), summary)
        "storage"    -> ReportJson.decodeFromJsonElement(StorageSummary.serializer(), summary)
        else -> null
    }
}.getOrNull()
