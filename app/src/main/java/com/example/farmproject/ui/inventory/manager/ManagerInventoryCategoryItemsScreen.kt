package com.example.farmproject.ui.inventory.manager

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.Division
import com.example.farmproject.data.model.InventoryCategory
import com.example.farmproject.data.model.InventoryItem
import com.example.farmproject.data.model.StockIssuance
import com.example.farmproject.ui.common.DatePickerField
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerInventoryCategoryItemsScreen(
    category: InventoryCategory,
    viewModel: InventoryViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItemForIssue by remember { mutableStateOf<InventoryItem?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val divisions by viewModel.divisions.collectAsState()

    LaunchedEffect(category) {
        viewModel.loadInventory()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = pluralCategoryName(category),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Manage stock items and issuance history",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Stock Items") },
                        icon = { Icon(imageVector = Icons.Default.Inventory, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Issued Stock") },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is InventoryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is InventoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadInventory() }) {
                            Text("Retry")
                        }
                    }
                }

                is InventoryUiState.Success -> {
                    val categoryItems = state.items.filter { it.category == category }
                    val categoryItemIds = categoryItems.map { it.id }.toSet()
                    val categoryIssuances = state.issuances.filter { issuance ->
                        (issuance.itemId != null && categoryItemIds.contains(issuance.itemId)) ||
                        issuance.safeCategory.equals(category.name, ignoreCase = true) ||
                        issuance.safeCategory.equals(category.displayName, ignoreCase = true) ||
                        issuance.safeItemName.contains(category.displayName, ignoreCase = true)
                    }
                    val lowStockCount = categoryItems.count { it.isLowStock }

                    when (selectedTab) {
                        0 -> {
                            // TAB 0: STOCK ITEMS
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    CategorySummaryCard(
                                        categoryName = pluralCategoryName(category),
                                        totalItems = categoryItems.size,
                                        lowStockCount = lowStockCount
                                    )
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pluralCategoryName(category)} Stock Items",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        OutlinedButton(onClick = onAddClick) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text("Add")
                                        }
                                    }
                                }

                                if (categoryItems.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No ${pluralCategoryName(category).lowercase()} items recorded yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(categoryItems) { item ->
                                        InventoryCategoryItemCard(
                                            item = item,
                                            onClick = { onItemClick(item.id) },
                                            onIssueClick = { selectedItemForIssue = item }
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: ISSUED STOCK HISTORY
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    IssuanceSummaryCard(
                                        categoryName = pluralCategoryName(category),
                                        totalIssuances = categoryIssuances.size
                                    )
                                }

                                if (categoryIssuances.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No stock issuances recorded for ${pluralCategoryName(category).lowercase()} yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(categoryIssuances) { issuance ->
                                        StockIssuanceCard(issuance = issuance)
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ISSUE STOCK DIALOG
    selectedItemForIssue?.let { item ->
        IssueStockDialog(
            item = item,
            divisions = divisions,
            onDismiss = { selectedItemForIssue = null },
            onIssue = { quantity, divisionId, issuedTo, issueDate, notes ->
                viewModel.issueStock(
                    category = category,
                    itemId = item.id,
                    quantityIssued = quantity,
                    divisionId = divisionId,
                    issuedTo = issuedTo,
                    issueDate = issueDate,
                    notes = notes
                )
                selectedItemForIssue = null
            }
        )
    }
}

@Composable
private fun CategorySummaryCard(
    categoryName: String,
    totalItems: Int,
    lowStockCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Column {
                    Text(
                        text = totalItems.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$categoryName items in stock",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (lowStockCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.size(6.dp))

                    Text(
                        text = "$lowStockCount item(s) below reorder level",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun IssuanceSummaryCard(
    categoryName: String,
    totalIssuances: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Outbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(
                    text = totalIssuances.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$categoryName Stock Issuances Recorded",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InventoryCategoryItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onIssueClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (item.subtype != null) {
                        Text(
                            text = item.subtype,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (item.isLowStock) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Low stock",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Available Quantity",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Reorder Level",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Text(
                        text = item.reorderLevel?.let { "$it ${item.unit}" } ?: "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onIssueClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Outbox,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Issue Stock")
            }
        }
    }
}

@Composable
private fun StockIssuanceCard(
    issuance: StockIssuance
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Outbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = issuance.safeItemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Issued: ${issuance.safeQuantityIssued} ${issuance.safeUnit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = issuance.safeIssueDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Issued To: ${issuance.safeIssuedTo}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!issuance.notes.isNullOrBlank()) {
                Text(
                    text = issuance.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueStockDialog(
    item: InventoryItem,
    divisions: List<Division>,
    onDismiss: () -> Unit,
    onIssue: (quantity: Double, divisionId: String?, issuedTo: String?, issueDate: String, notes: String?) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var selectedDivision by remember { mutableStateOf<Division?>(null) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var customRecipientText by remember { mutableStateOf("") }
    var issueDateText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Issue Stock - ${item.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Available: ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity to Issue * (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (divisions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = divisionExpanded,
                        onExpandedChange = { divisionExpanded = !divisionExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDivision?.name ?: "Select Division (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Division / Field") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Place, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = divisionExpanded,
                            onDismissRequest = { divisionExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None / General Farm") },
                                onClick = {
                                    selectedDivision = null
                                    divisionExpanded = false
                                }
                            )
                            divisions.forEach { div ->
                                DropdownMenuItem(
                                    text = { Text(div.name) },
                                    onClick = {
                                        selectedDivision = div
                                        divisionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customRecipientText,
                    onValueChange = { customRecipientText = it },
                    label = { Text("Recipient / Person (optional)") },
                    placeholder = { Text("e.g. John Mwangi or Field Team") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = issueDateText,
                    onValueChange = { issueDateText = it },
                    label = "Issue Date",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Purpose (optional)") },
                    placeholder = { Text("e.g. Spraying Maize crop") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull()
                    val finalDate = issueDateText.trim().ifBlank {
                        try {
                            LocalDate.now().toString()
                        } catch (_: Exception) {
                            "2026-03-30"
                        }
                    }
                    val today = try { LocalDate.now() } catch (_: Exception) { null }
                    val dateParsed = try { LocalDate.parse(finalDate) } catch (_: Exception) { null }

                    when {
                        qty == null || qty <= 0.0 -> errorMsg = "Enter a valid quantity to issue."
                        qty > item.quantity -> errorMsg = "Quantity exceeds available stock (${item.quantity} ${item.unit})."
                        dateParsed != null && today != null && dateParsed.isBefore(today) -> errorMsg = "Issue date cannot be in the past."
                        else -> {
                            onIssue(
                                qty,
                                selectedDivision?.divisionId,
                                customRecipientText.trim().ifBlank { selectedDivision?.name },
                                finalDate,
                                notesText.trim().ifBlank { null }
                            )
                        }
                    }
                }
            ) {
                Text("Issue Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun pluralCategoryName(category: InventoryCategory): String = when (category) {
    InventoryCategory.FERTILIZER -> "Fertilizers"
    InventoryCategory.HERBICIDE -> "Herbicides"
    InventoryCategory.PESTICIDE -> "Pesticides"
    InventoryCategory.FUNGICIDE -> "Fungicides"
    InventoryCategory.INSECTICIDE -> "Insecticides"
    InventoryCategory.WATER -> "Water"
}
