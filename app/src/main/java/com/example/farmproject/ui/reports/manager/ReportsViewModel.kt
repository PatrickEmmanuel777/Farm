@file:Suppress("unused")

package com.example.farmproject.ui.reports.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.CropYieldSummary
import com.example.farmproject.data.model.ExpenseSummary
import com.example.farmproject.data.model.InventorySummary
import com.example.farmproject.data.model.ReportCategoryStatus
import com.example.farmproject.data.model.ReportRecord
import com.example.farmproject.data.model.ReportSummary
import com.example.farmproject.data.model.StorageSummary
import com.example.farmproject.data.model.parseReportSummary
import com.example.farmproject.data.repository.ReportRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportDetail(
    val category: ReportCategoryStatus,
    val record: ReportRecord,
    val summary: ReportSummary?,       // null if decoding failed
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val categories: List<ReportCategoryStatus> = emptyList(),
    val cropYieldSummary: CropYieldSummary? = null,
    val expenseSummary: ExpenseSummary? = null,
    val inventorySummary: InventorySummary? = null,
    val storageSummary: StorageSummary? = null,
    val generatingCodes: Set<String> = emptySet(),
    val isGeneratingAll: Boolean = false,
    val detail: ReportDetail? = null,
    val message: String? = null,       // one-shot error/info for a snackbar
) {
    /** "4 Report categories available" */
    val categoryCount: Int get() = categories.size
}

class ReportsViewModel(
    private val repository: ReportRepository = ReportRepository(),
    private val farmIdProvider: () -> String = { CurrentSession.farmId },
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private val activeFarmId: String
        get() = farmIdProvider().ifBlank { CurrentSession.farmId }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val farmId = activeFarmId

            val categoriesResult = runCatching { repository.getCategories(farmId) }.getOrDefault(emptyList())

            val cropYieldSummary = runCatching { repository.fetchLiveSummary(farmId, "crop_yield") as? CropYieldSummary }.getOrNull()
            val expenseSummary = runCatching { repository.fetchLiveSummary(farmId, "expenses") as? ExpenseSummary }.getOrNull()
            val inventorySummary = runCatching { repository.fetchLiveSummary(farmId, "inventory") as? InventorySummary }.getOrNull()
            val storageSummary = runCatching { repository.fetchLiveSummary(farmId, "storage") as? StorageSummary }.getOrNull()

            _state.update {
                it.copy(
                    isLoading = false,
                    categories = categoriesResult.ifEmpty { it.categories },
                    cropYieldSummary = cropYieldSummary,
                    expenseSummary = expenseSummary,
                    inventorySummary = inventorySummary,
                    storageSummary = storageSummary
                )
            }
        }
    }

    /** Generate one category (per-card action, or the Generate button after the user picks one). */
    fun generate(
        category: ReportCategoryStatus,
        periodStart: String? = null,
        periodEnd: String? = null,
        openAfter: Boolean = false,
    ) {
        if (category.code in _state.value.generatingCodes) return
        viewModelScope.launch {
            _state.update { it.copy(generatingCodes = it.generatingCodes + category.code) }
            val farmId = activeFarmId
            runCatching { repository.generate(farmId, category.code, periodStart, periodEnd) }
                .onSuccess { record ->
                    val refreshed = runCatching { repository.getCategories(farmId) }.getOrNull()
                    _state.update {
                        it.copy(
                            categories = refreshed ?: it.categories,
                            generatingCodes = it.generatingCodes - category.code,
                            detail = if (openAfter) record.toDetail(category) else it.detail,
                            message = "${category.name} generated",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            generatingCodes = it.generatingCodes - category.code,
                            message = e.message ?: "Could not generate ${category.name}",
                        )
                    }
                }
        }
    }

    fun generateByCode(
        titleOrCode: String,
        periodStart: String? = null,
        periodEnd: String? = null,
        openAfter: Boolean = true,
    ) {
        viewModelScope.launch {
            val farmId = activeFarmId
            val categories = _state.value.categories.ifEmpty {
                runCatching { repository.getCategories(farmId) }.getOrDefault(emptyList())
            }
            val code = normalizeCategoryCode(titleOrCode)
            val category = categories.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: createFallbackCategory(farmId, code)

            generate(category, periodStart, periodEnd, openAfter)
        }
    }

    /** Top-right "Generate" button: run all categories one after another. */
    fun generateAll(periodStart: String? = null, periodEnd: String? = null) {
        if (_state.value.isGeneratingAll) return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingAll = true) }
            val farmId = activeFarmId
            var failed = 0
            for (category in _state.value.categories) {
                runCatching { repository.generate(farmId, category.code, periodStart, periodEnd) }
                    .onFailure { failed++ }
            }
            val refreshed = runCatching { repository.getCategories(farmId) }.getOrNull()
            _state.update {
                it.copy(
                    isGeneratingAll = false,
                    categories = refreshed ?: it.categories,
                    message = if (failed == 0) "All reports generated"
                    else "$failed report(s) failed to generate",
                )
            }
        }
    }

    /** Tap on a card: open the latest report, or generate one if none exists yet. */
    fun open(category: ReportCategoryStatus) {
        val reportId = category.lastReportId
        if (reportId == null) {
            generate(category, openAfter = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { repository.getReport(reportId) }
                .onSuccess { record -> _state.update { it.copy(isLoading = false, detail = record.toDetail(category)) } }
                .onFailure {
                    // Fall back to generating report if direct fetch fails
                    _state.update { it.copy(isLoading = false) }
                    generate(category, openAfter = true)
                }
        }
    }

    fun openByTitleOrCode(titleOrCode: String) {
        viewModelScope.launch {
            val farmId = activeFarmId
            val categories = _state.value.categories.ifEmpty {
                runCatching { repository.getCategories(farmId) }.getOrDefault(emptyList())
            }
            val code = normalizeCategoryCode(titleOrCode)
            val category = categories.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: createFallbackCategory(farmId, code)

            open(category)
        }
    }

    fun closeDetail() = _state.update { it.copy(detail = null) }
    fun messageShown() = _state.update { it.copy(message = null) }

    private fun ReportRecord.toDetail(category: ReportCategoryStatus) =
        ReportDetail(category, this, parseReportSummary(category.code, summary))

    private fun normalizeCategoryCode(input: String): String = when {
        input.contains("Inventory", ignoreCase = true) || input.contains("Resource", ignoreCase = true) || input.equals("inventory", ignoreCase = true) -> "inventory"
        input.contains("Expense", ignoreCase = true) || input.equals("expenses", ignoreCase = true) -> "expenses"
        input.contains("Storage", ignoreCase = true) || input.equals("storage", ignoreCase = true) -> "storage"
        else -> "crop_yield"
    }

    private fun createFallbackCategory(farmId: String, code: String): ReportCategoryStatus {
        val (name, desc) = when (code) {
            "inventory" -> "Resources Report" to "Stock levels, usage and reorder alerts"
            "expenses" -> "Farm Expenses Report" to "Operational costs, category breakdowns and supplier payments"
            "storage" -> "Storage Report" to "Stored produce, capacity and condition"
            else -> "Crop Yield Report" to "Planting, growth stage and harvest summaries"
        }
        return ReportCategoryStatus(
            farmId = farmId,
            categoryId = "cat-$code",
            code = code,
            name = name,
            description = desc,
            lastReportId = "rep-$code",
        )
    }
}
