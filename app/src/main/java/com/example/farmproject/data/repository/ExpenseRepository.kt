package com.example.farmproject.data.repository

import com.example.farmproject.data.model.ExpenseCategory
import com.example.farmproject.data.model.ExpenseDashboard
import com.example.farmproject.data.model.ExpenseDetail
import com.example.farmproject.data.model.ExpenseFilters
import com.example.farmproject.data.model.ExpenseInput
import com.example.farmproject.data.model.ExpenseListRow
import com.example.farmproject.data.model.ExpensePage
import com.example.farmproject.data.model.FarmExpense
import com.example.farmproject.data.remote.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Talks to the expense RPCs. Every function returns a Result so the
 * ViewModel can show the server's message (e.g. "Only a farm manager ...").
 *
 * Pass in your existing SupabaseClient. Permissions are enforced in the
 * database; the UI should only mirror them (canModify, supervisor duties).
 */
class ExpenseRepository(private val supabase: SupabaseClient = SupabaseClientProvider.client) {

    // ---------------------------------------------------------
    // Categories (the nine predefined, ordered 1-9)
    // ---------------------------------------------------------
    suspend fun getCategories(): Result<List<ExpenseCategory>> = runCatching {
        supabase.postgrest
            .rpc("list_expense_categories")
            .decodeList<ExpenseCategory>()
    }

    // ---------------------------------------------------------
    // Dashboard: total, monthly, count, per-category breakdown
    // month: any date inside the month, "yyyy-MM-dd" (null = current month)
    // ---------------------------------------------------------
    suspend fun getDashboard(farmId: String, month: String? = null): Result<ExpenseDashboard> =
        runCatching {
            val params = buildJsonObject {
                put("p_farm_id", farmId)
                putIfNotNull("p_month", month)
            }
            supabase.postgrest
                .rpc("get_expense_dashboard", params)
                .decodeAs<ExpenseDashboard>()
        }

    // ---------------------------------------------------------
    // History with search and filters (paged)
    // ---------------------------------------------------------
    suspend fun listExpenses(
        farmId: String,
        filters: ExpenseFilters = ExpenseFilters()
    ): Result<ExpensePage> = runCatching {
        val params = buildJsonObject {
            put("p_farm_id", farmId)
            putIfNotNull("p_from_date", filters.fromDate)
            putIfNotNull("p_to_date", filters.toDate)
            putIfNotNull("p_category_id", filters.categoryId)
            putIfNotNull("p_search", filters.search?.trim()?.takeIf { it.isNotEmpty() })
            putIfNotNull("p_division_id", filters.divisionId)
            put("p_limit", filters.limit)
            put("p_offset", filters.offset)
        }
        val rows = supabase.postgrest
            .rpc("list_farm_expenses", params)
            .decodeList<ExpenseListRow>()

        val totalCount = rows.firstOrNull()?.totalCount ?: 0L
        ExpensePage(
            items = rows,
            totalAmount = rows.firstOrNull()?.totalAmount ?: 0.0,
            totalCount = totalCount,
            hasMore = (filters.offset + rows.size) < totalCount
        )
    }

    // ---------------------------------------------------------
    // Details
    // ---------------------------------------------------------
    suspend fun getExpense(expenseId: String): Result<ExpenseDetail> = runCatching {
        val rows = supabase.postgrest
            .rpc("get_farm_expense", buildJsonObject { put("p_expense_id", expenseId) })
            .decodeList<ExpenseDetail>()
        rows.firstOrNull() ?: error("Expense not found")
    }

    // ---------------------------------------------------------
    // Record (manager or expense supervisor)
    // ---------------------------------------------------------
    suspend fun createExpense(farmId: String, input: ExpenseInput): Result<FarmExpense> =
        runCatching {
            val params = buildJsonObject {
                put("p_farm_id", farmId)
                putExpenseFields(input)
            }
            supabase.postgrest
                .rpc("create_farm_expense", params)
                .decodeAs<FarmExpense>()
        }

    // ---------------------------------------------------------
    // Edit (manager only). Full replace: send every field, a null
    // optional field clears that value.
    // ---------------------------------------------------------
    suspend fun updateExpense(expenseId: String, input: ExpenseInput): Result<FarmExpense> =
        runCatching {
            val params = buildJsonObject {
                put("p_expense_id", expenseId)
                putExpenseFields(input)
            }
            supabase.postgrest
                .rpc("update_farm_expense", params)
                .decodeAs<FarmExpense>()
        }

    // ---------------------------------------------------------
    // Delete (manager only, soft delete)
    // ---------------------------------------------------------
    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        supabase.postgrest
            .rpc("delete_farm_expense", buildJsonObject { put("p_expense_id", expenseId) })
        Unit
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------
    private fun JsonObjectBuilder.putExpenseFields(input: ExpenseInput) {
        put("p_category_id", input.categoryId)
        put("p_description", input.description.trim())
        put("p_amount", input.amount)
        put("p_expense_date", input.expenseDate)
        putIfNotNull("p_division_id", input.divisionId)
        putIfNotNull("p_payment_method", input.paymentMethod)
        putIfNotNull("p_supplier_name", input.supplierName)
        putIfNotNull("p_receipt_reference", input.receiptReference)
        putIfNotNull("p_notes", input.notes)
    }

    /** Omitted parameters fall back to the SQL defaults (NULL). */
    private fun JsonObjectBuilder.putIfNotNull(key: String, value: String?) {
        if (value != null) put(key, value)
    }
}

/** Turn a failure into text for a snackbar or dialog. Refine once you see real errors. */
fun Throwable.toUserMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
