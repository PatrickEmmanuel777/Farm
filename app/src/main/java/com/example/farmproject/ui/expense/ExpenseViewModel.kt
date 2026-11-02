package com.example.farmproject.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.ExpenseCategory
import com.example.farmproject.data.model.ExpenseDashboard
import com.example.farmproject.data.model.ExpenseDetail
import com.example.farmproject.data.model.ExpenseFilters
import com.example.farmproject.data.model.ExpenseInput
import com.example.farmproject.data.model.ExpenseListRow
import com.example.farmproject.data.repository.ExpenseRepository
import com.example.farmproject.data.repository.toUserMessage
import com.example.farmproject.data.session.CurrentSession
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =========================================================
// UI state
// =========================================================

data class ExpenseFormErrors(
    val category: String? = null,
    val description: String? = null,
    val amount: String? = null,
    val date: String? = null
) {
    val hasErrors: Boolean
        get() = category != null || description != null || amount != null || date != null
}

data class ExpenseUiState(
    /** Manager or supervisor with the 'expense' duty. Controls the Add button. */
    val canRecord: Boolean = false,

    // Categories
    val categories: List<ExpenseCategory> = emptyList(),

    // Dashboard (selectedMonth is the first day of the month, "yyyy-MM-dd")
    val dashboard: ExpenseDashboard? = null,
    val selectedMonth: String = firstOfCurrentMonth(),
    val isDashboardLoading: Boolean = false,
    val dashboardError: String? = null,

    // History, search and filters
    val searchText: String = "",
    val filters: ExpenseFilters = ExpenseFilters(),
    val items: List<ExpenseListRow> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalCount: Long = 0,
    val hasMore: Boolean = false,
    val isListLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val listError: String? = null,

    // Details
    val detail: ExpenseDetail? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,

    // Add / edit / delete
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val formErrors: ExpenseFormErrors = ExpenseFormErrors()
) {
    val hasActiveFilters: Boolean
        get() = filters.fromDate != null || filters.toDate != null ||
            filters.categoryId != null || !filters.search.isNullOrBlank()
}

/** One-off events: show once, don't re-show on rotation. */
sealed interface ExpenseEvent {
    data class ShowMessage(val text: String) : ExpenseEvent
    /** Close the add/edit form. */
    data class Saved(val expenseId: String, val wasEdit: Boolean) : ExpenseEvent
    /** Close the details screen. */
    data object Deleted : ExpenseEvent
}

private fun firstOfCurrentMonth(): String = LocalDate.now().withDayOfMonth(1).toString()

// =========================================================
// ViewModel
// =========================================================

@OptIn(FlowPreview::class)
class ExpenseViewModel(
    private val repository: ExpenseRepository = ExpenseRepository(),
    private val farmId: String = CurrentSession.farmId,
    canRecord: Boolean = CurrentSession.userRole?.equals("Manager", ignoreCase = true) == true || CurrentSession.userRole?.equals("Supervisor", ignoreCase = true) == true
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseUiState(canRecord = canRecord))
    val state: StateFlow<ExpenseUiState> = _state.asStateFlow()

    private val _events = Channel<ExpenseEvent>(Channel.BUFFERED)
    val events: Flow<ExpenseEvent> = _events.receiveAsFlow()

    private val searchInput = MutableStateFlow("")
    private var listJob: Job? = null
    private var dashboardJob: Job? = null

    init {
        loadCategories()
        loadDashboard()
        loadFirstPage()
        observeSearch()
    }

    // ---------------------------------------------------------
    // Categories
    // ---------------------------------------------------------
    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories()
                .onSuccess { list -> _state.update { it.copy(categories = list) } }
                .onFailure { e -> handleFailure(e) { msg -> message(msg) } }
        }
    }

    // ---------------------------------------------------------
    // Dashboard
    // ---------------------------------------------------------
    fun loadDashboard() {
        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            _state.update { it.copy(isDashboardLoading = true, dashboardError = null) }
            val currentFarmId = farmId.ifBlank { CurrentSession.farmId }
            repository.getDashboard(currentFarmId, _state.value.selectedMonth)
                .onSuccess { d ->
                    _state.update { it.copy(dashboard = d, isDashboardLoading = false) }
                }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isDashboardLoading = false, dashboardError = msg) }
                    }
                }
        }
    }

    fun previousMonth() = shiftMonth(-1)
    fun nextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Long) {
        val next = LocalDate.parse(_state.value.selectedMonth).plusMonths(delta).withDayOfMonth(1)
        // Don't navigate past the current month
        if (next.isAfter(LocalDate.now().withDayOfMonth(1))) return
        _state.update { it.copy(selectedMonth = next.toString()) }
        loadDashboard()
    }

    // ---------------------------------------------------------
    // History, search and filters
    // ---------------------------------------------------------
    fun onSearchTextChanged(text: String) {
        _state.update { it.copy(searchText = text) }
        searchInput.value = text
    }

    private fun observeSearch() {
        viewModelScope.launch {
            searchInput
                .debounce(400)
                .map { it.trim() }
                .distinctUntilChanged()
                .drop(1) // skip the initial empty value
                .collect { query ->
                    _state.update {
                        it.copy(filters = it.filters.copy(search = query.ifEmpty { null }, offset = 0))
                    }
                    loadFirstPage()
                }
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _state.update { it.copy(filters = it.filters.copy(categoryId = categoryId, offset = 0)) }
        loadFirstPage()
    }

    /** Dates are "yyyy-MM-dd"; pass null to clear a bound. */
    fun setDateRange(fromDate: String?, toDate: String?) {
        _state.update {
            it.copy(filters = it.filters.copy(fromDate = fromDate, toDate = toDate, offset = 0))
        }
        loadFirstPage()
    }

    fun clearFilters() {
        searchInput.value = ""
        _state.update { it.copy(searchText = "", filters = ExpenseFilters()) }
        loadFirstPage()
    }

    fun refresh() {
        loadDashboard()
        loadFirstPage()
    }

    private fun loadFirstPage() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _state.update { it.copy(isListLoading = true, isLoadingMore = false, listError = null) }
            val currentFarmId = farmId.ifBlank { CurrentSession.farmId }
            val filters = _state.value.filters.copy(offset = 0)
            repository.listExpenses(currentFarmId, filters)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = page.items,
                            totalAmount = page.totalAmount,
                            totalCount = page.totalCount,
                            hasMore = page.hasMore,
                            isListLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isListLoading = false, listError = msg) }
                    }
                }
        }
    }

    /** Call when the user scrolls near the end of the list. */
    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isListLoading || s.isLoadingMore) return

        listJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val currentFarmId = farmId.ifBlank { CurrentSession.farmId }
            val filters = _state.value.filters.copy(offset = s.items.size)
            repository.listExpenses(currentFarmId, filters)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = it.items + page.items,
                            totalAmount = page.totalAmount,
                            totalCount = page.totalCount,
                            hasMore = page.hasMore,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isLoadingMore = false) }
                        message(msg)
                    }
                }
        }
    }

    // ---------------------------------------------------------
    // Details
    // ---------------------------------------------------------
    fun loadDetail(expenseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDetailLoading = true, detailError = null, detail = null) }
            repository.getExpense(expenseId)
                .onSuccess { d -> _state.update { it.copy(detail = d, isDetailLoading = false) } }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isDetailLoading = false, detailError = msg) }
                    }
                }
        }
    }

    fun clearDetail() {
        _state.update { it.copy(detail = null, detailError = null) }
    }

    // ---------------------------------------------------------
    // Add / edit
    // expenseId = null -> create; otherwise edit (manager only)
    // ---------------------------------------------------------
    fun saveExpense(input: ExpenseInput, expenseId: String? = null) {
        val s = _state.value
        if (s.isSaving) return

        if (expenseId == null && !s.canRecord) {
            message("You don't have permission to record expenses.")
            return
        }

        val errors = validate(input)
        _state.update { it.copy(formErrors = errors) }
        if (errors.hasErrors) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val currentFarmId = farmId.ifBlank { CurrentSession.farmId }
            val result = if (expenseId == null) {
                repository.createExpense(currentFarmId, input)
            } else {
                repository.updateExpense(expenseId, input)
            }
            result
                .onSuccess { saved ->
                    _state.update { it.copy(isSaving = false, formErrors = ExpenseFormErrors()) }
                    _events.trySend(ExpenseEvent.Saved(saved.expenseId, wasEdit = expenseId != null))
                    refresh()
                    if (_state.value.detail?.expenseId == saved.expenseId) loadDetail(saved.expenseId)
                }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isSaving = false) }
                        message(msg)
                    }
                }
        }
    }

    fun clearFormErrors() {
        _state.update { it.copy(formErrors = ExpenseFormErrors()) }
    }

    private fun validate(input: ExpenseInput): ExpenseFormErrors {
        val today = LocalDate.now()
        val date = runCatching { LocalDate.parse(input.expenseDate) }.getOrNull()
        return ExpenseFormErrors(
            category = if (input.categoryId.isBlank()) "Choose a category" else null,
            description = if (input.description.isBlank()) "Enter a description" else null,
            amount = if (input.amount.isNaN() || input.amount <= 0.0) "Enter an amount above 0" else null,
            date = when {
                date == null -> "Choose a valid date"
                date.isAfter(today) -> "Date can't be in the future"
                else -> null
            }
        )
    }

    // ---------------------------------------------------------
    // Delete (manager only)
    // ---------------------------------------------------------
    fun deleteExpense(expenseId: String) {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            repository.deleteExpense(expenseId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, detail = null) }
                    _events.trySend(ExpenseEvent.Deleted)
                    refresh()
                }
                .onFailure { e ->
                    handleFailure(e) { msg ->
                        _state.update { it.copy(isDeleting = false) }
                        message(msg)
                    }
                }
        }
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------
    private fun message(text: String) {
        _events.trySend(ExpenseEvent.ShowMessage(text))
    }


    private inline fun handleFailure(e: Throwable, onError: (String) -> Unit) {
        if (e is CancellationException) throw e
        onError(e.toUserMessage())
    }
}

// =========================================================
// Factory (the ViewModel needs constructor arguments)
// =========================================================

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository = ExpenseRepository(),
    private val farmId: String = CurrentSession.farmId,
    private val canRecord: Boolean = CurrentSession.userRole?.equals("Manager", ignoreCase = true) == true || CurrentSession.userRole?.equals("Supervisor", ignoreCase = true) == true
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ExpenseViewModel(repository, farmId, canRecord) as T
}
