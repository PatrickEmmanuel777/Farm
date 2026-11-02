package com.example.farmproject.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.ExpenseCategory
import com.example.farmproject.data.model.ExpenseDetail
import com.example.farmproject.data.model.ExpenseInput
import com.example.farmproject.data.model.ExpenseListRow
import com.example.farmproject.ui.common.DatePickerField
import java.time.LocalDate
import java.util.Locale

private fun formatCurrency(amount: Double): String =
    String.format(Locale.US, "KES %,.2f", amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerExpenseScreen(
    viewModel: ExpenseViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var selectedRowForDetail by remember { mutableStateOf<ExpenseListRow?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExpenseEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.text)
                }

                is ExpenseEvent.Saved -> {
                    showFormDialog = false
                    editingExpenseId = null
                    snackbarHostState.showSnackbar(
                        if (event.wasEdit) "Expense updated successfully." else "Expense recorded successfully."
                    )
                }

                is ExpenseEvent.Deleted -> {
                    showDeleteConfirm = false
                    selectedRowForDetail = null
                    snackbarHostState.showSnackbar("Expense deleted.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Farm Expenses",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Track, record & manage operational costs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (state.canRecord) {
                        IconButton(onClick = {
                            editingExpenseId = null
                            viewModel.clearFormErrors()
                            showFormDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Record Expense",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20)
                )
            )
        },
        floatingActionButton = {
            if (state.canRecord) {
                FloatingActionButton(
                    onClick = {
                        editingExpenseId = null
                        viewModel.clearFormErrors()
                        showFormDialog = true
                    },
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Record Expense"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Dashboard Summary Card
            val dashboard = state.dashboard
            ExpenseDashboardCard(
                selectedMonth = state.selectedMonth,
                totalExpenditure = dashboard?.monthExpenditure ?: dashboard?.totalExpenditure ?: state.totalAmount,
                count = dashboard?.monthExpenseCount ?: dashboard?.expenseCount ?: state.totalCount,
                isLoading = state.isDashboardLoading,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )

            // Search Bar & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchText,
                    onValueChange = { viewModel.onSearchTextChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by description or vendor...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchTextChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.filters.categoryId == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All Categories") }
                        )
                    }

                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.filters.categoryId == category.categoryId,
                            onClick = { viewModel.setCategoryFilter(category.categoryId) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            // Category Filter Subtotal Card
            if (state.filters.categoryId != null || state.searchText.isNotBlank()) {
                val filteredSubtotal = state.items.sumOf { it.amount }
                val selectedCatName = state.categories.find { it.categoryId == state.filters.categoryId }?.name ?: "Filtered"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedCatName Subtotal (${state.items.size} items)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = formatCurrency(filteredSubtotal),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // Error or Loading or Items List
            if (state.isListLoading && state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1B5E20))
                }
            } else if (state.listError != null && state.items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.listError ?: "Failed to load expenses",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            } else if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.filters.categoryId != null) "No expenses in this category this month."
                               else if (state.hasActiveFilters) "No expenses match search filter."
                               else "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.expenseId }) { row ->
                        ExpenseRowCard(
                            row = row,
                            onClick = {
                                selectedRowForDetail = row
                                viewModel.loadDetail(row.expenseId)
                            }
                        )
                    }

                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    OutlinedButton(onClick = { viewModel.loadNextPage() }) {
                                        Text("Load More")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // FORM DIALOG (CREATE / EDIT)
    if (showFormDialog) {
        val detail = state.detail
        val initialInput = if (editingExpenseId != null && detail != null) {
            ExpenseInput(
                categoryId = detail.categoryId,
                description = detail.description,
                amount = detail.amount,
                expenseDate = detail.expenseDate,
                paymentMethod = detail.paymentMethod,
                supplierName = detail.supplierName,
                receiptReference = detail.receiptReference,
                notes = detail.notes
            )
        } else {
            ExpenseInput(
                categoryId = state.categories.firstOrNull()?.categoryId ?: "",
                description = "",
                amount = 0.0,
                expenseDate = LocalDate.now().toString(),
                paymentMethod = "M-Pesa",
                supplierName = "",
                receiptReference = "",
                notes = ""
            )
        }

        ExpenseFormDialog(
            isEdit = editingExpenseId != null,
            initialInput = initialInput,
            categories = state.categories,
            formErrors = state.formErrors,
            isSaving = state.isSaving,
            onDismiss = { showFormDialog = false },
            onSave = { input ->
                viewModel.saveExpense(input, editingExpenseId)
            }
        )
    }

    // DETAIL DIALOG
    selectedRowForDetail?.let { selectedRow ->
        ExpenseDetailDialog(
            row = selectedRow,
            detail = state.detail,
            isLoading = state.isDetailLoading,
            error = state.detailError,
            canModify = state.detail?.canModify ?: state.canRecord,
            onDismiss = {
                selectedRowForDetail = null
                viewModel.clearDetail()
            },
            onEdit = {
                val detail = state.detail
                if (detail != null) {
                    editingExpenseId = detail.expenseId
                    showFormDialog = true
                }
            },
            onDelete = {
                showDeleteConfirm = true
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirm && selectedRowForDetail != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this expense record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedRowForDetail?.expenseId?.let { id ->
                            viewModel.deleteExpense(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExpenseDashboardCard(
    selectedMonth: String,
    totalExpenditure: Double,
    count: Long,
    isLoading: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Month Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedMonth.take(7),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                }
            } else {
                Text(
                    text = "Total Monthly Expenditure",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Text(
                    text = formatCurrency(totalExpenditure),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$count expenses recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ExpenseRowCard(
    row: ExpenseListRow,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${row.categoryName} • ${row.expenseDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!row.supplierName.isNullOrBlank()) {
                    Text(
                        text = "Payee: ${row.supplierName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(row.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                if (!row.paymentMethod.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = row.paymentMethod,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(
    isEdit: Boolean,
    initialInput: ExpenseInput,
    categories: List<ExpenseCategory>,
    formErrors: ExpenseFormErrors,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExpenseInput) -> Unit
) {
    var categoryId by remember { mutableStateOf(initialInput.categoryId) }
    var description by remember { mutableStateOf(initialInput.description) }
    var amountText by remember { mutableStateOf(if (initialInput.amount > 0) initialInput.amount.toString() else "") }
    var expenseDate by remember { mutableStateOf(initialInput.expenseDate) }
    var supplierName by remember { mutableStateOf(initialInput.supplierName.orEmpty()) }
    var paymentMethod by remember { mutableStateOf(initialInput.paymentMethod.orEmpty().ifBlank { "M-Pesa" }) }
    var receiptReference by remember { mutableStateOf(initialInput.receiptReference.orEmpty()) }
    var notes by remember { mutableStateOf(initialInput.notes.orEmpty()) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    val paymentMethods = listOf("M-Pesa", "Cash", "Bank Transfer", "Cheque", "Credit Card")
    val selectedCategoryName = categories.find { it.categoryId == categoryId }?.name ?: "Select Category"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Expense" else "Record Farm Expense",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text("Category *") },
                        isError = formErrors.category != null,
                        supportingText = formErrors.category?.let { { Text(it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryId = cat.categoryId
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description *") },
                    placeholder = { Text("e.g. Purchased 10 bags of NPK fertilizer") },
                    isError = formErrors.description != null,
                    supportingText = formErrors.description?.let { { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount (KES)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (KES) *") },
                    placeholder = { Text("e.g. 15000") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    isError = formErrors.amount != null,
                    supportingText = formErrors.amount?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Expense Date
                DatePickerField(
                    value = expenseDate,
                    onValueChange = { expenseDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Expense Date *",
                    shape = RoundedCornerShape(12.dp)
                )

                // Supplier / Payee
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Payee / Supplier Name") },
                    placeholder = { Text("e.g. AgroVet Kenya Ltd") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text("Payment Method") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Receipt / Reference
                OutlinedTextField(
                    value = receiptReference,
                    onValueChange = { receiptReference = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receipt / Ref Number") },
                    placeholder = { Text("e.g. QX1234567") },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Additional Notes") },
                    placeholder = { Text("e.g. Invoice attached") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                    onSave(
                        ExpenseInput(
                            categoryId = categoryId,
                            description = description,
                            amount = amountVal,
                            expenseDate = expenseDate,
                            paymentMethod = paymentMethod.ifBlank { null },
                            supplierName = supplierName.ifBlank { null },
                            receiptReference = receiptReference.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                    )
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(if (isEdit) "Save Changes" else "Record Expense")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExpenseDetailDialog(
    row: ExpenseListRow,
    detail: ExpenseDetail?,
    isLoading: Boolean,
    error: String?,
    canModify: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = row.description,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val d = detail
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = formatCurrency(d?.amount ?: row.amount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )

                    DetailField("Category", d?.categoryName ?: row.categoryName)
                    DetailField("Expense Date", d?.expenseDate ?: row.expenseDate)
                    DetailField("Payee / Supplier", d?.supplierName ?: row.supplierName ?: "Not specified")
                    DetailField("Payment Method", d?.paymentMethod ?: row.paymentMethod ?: "M-Pesa")
                    DetailField("Receipt / Ref", d?.receiptReference ?: row.receiptReference ?: "None")
                    DetailField("Recorded By", d?.recordedBy ?: row.recordedBy)
                    if (!d?.notes.isNullOrBlank()) {
                        DetailField("Notes", d?.notes ?: "")
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canModify) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }

                    Button(onClick = onEdit) {
                        Text("Edit")
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        },
        dismissButton = {
            if (canModify) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
