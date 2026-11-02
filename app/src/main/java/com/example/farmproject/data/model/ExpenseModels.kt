package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =========================================================
// Response models (match the SQL RPC results)
// Dates are ISO strings: "yyyy-MM-dd" for dates, ISO-8601 for timestamps.
// =========================================================

/** list_expense_categories() */
@Serializable
data class ExpenseCategory(
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val description: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

/**
 * One row of list_farm_expenses().
 * totalAmount / totalCount repeat on every row and cover ALL matching
 * expenses, not just the current page.
 */
@Serializable
data class ExpenseListRow(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    val description: String,
    val amount: Double,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("receipt_reference") val receiptReference: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("total_count") val totalCount: Long = 0
)

/** get_farm_expense() */
@Serializable
data class ExpenseDetail(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    val description: String,
    val amount: Double,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("receipt_reference") val receiptReference: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    /** True only for managers. Use it to show/hide Edit and Delete. */
    @SerialName("can_modify") val canModify: Boolean = false
)

/** The raw farm_expense row returned by create_farm_expense / update_farm_expense. */
@Serializable
data class FarmExpense(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("category_id") val categoryId: String,
    val description: String,
    val amount: Double,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("receipt_reference") val receiptReference: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    val notes: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

/** get_expense_dashboard() */
@Serializable
data class ExpenseDashboard(
    @SerialName("month_start") val monthStart: String,
    @SerialName("total_expenditure") val totalExpenditure: Double = 0.0,
    @SerialName("expense_count") val expenseCount: Long = 0,
    @SerialName("month_expenditure") val monthExpenditure: Double = 0.0,
    @SerialName("month_expense_count") val monthExpenseCount: Long = 0,
    @SerialName("by_category") val byCategory: List<CategoryTotal> = emptyList()
)

@Serializable
data class CategoryTotal(
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val total: Double = 0.0,
    val count: Long = 0
)

// =========================================================
// UI-side models (not serialized)
// =========================================================

/** Fields the Add / Edit expense form collects. expenseDate is "yyyy-MM-dd". */
data class ExpenseInput(
    val categoryId: String,
    val description: String,
    val amount: Double,
    val expenseDate: String,
    val divisionId: String? = null,
    val paymentMethod: String? = null,
    val supplierName: String? = null,
    val receiptReference: String? = null,
    val notes: String? = null
)

/** Search and filter state for the history screen. Dates are "yyyy-MM-dd". */
data class ExpenseFilters(
    val fromDate: String? = null,
    val toDate: String? = null,
    val categoryId: String? = null,
    val search: String? = null,
    val divisionId: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/** One page of history plus totals across ALL matches. */
data class ExpensePage(
    val items: List<ExpenseListRow>,
    val totalAmount: Double,
    val totalCount: Long,
    val hasMore: Boolean
)
