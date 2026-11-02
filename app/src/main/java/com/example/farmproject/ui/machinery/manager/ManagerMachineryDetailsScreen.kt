package com.example.farmproject.ui.machinery.manager

import com.example.farmproject.ui.common.DatePickerField

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerMachineryDetailsScreen(
    machineName: String = "John Deere 5075E",
    machineType: String = "Tractor",
    status: String = "Available",
    condition: String = "Good",
    location: String = "Not set",
    purchaseDate: String = "",
    purchaseCost: String = "",
    rawPurchaseCost: String = "",
    totalHoursUsed: String = "0 hrs",
    totalFuelUsed: String = "0 litres",
    lastMaintenance: String = "No maintenance recorded",
    nextMaintenance: String = "Not scheduled",
    onBackClick: () -> Unit = {},
    onEditSave: (name: String, purchaseDate: String?, purchaseCost: Double?) -> Unit = { _, _, _ -> },
    onDeleteClick: () -> Unit = {},
    onUsageHistoryClick: () -> Unit = {},
    onMaintenanceHistoryClick: () -> Unit = {},
    onRecordUsageSave: (hoursUsed: Double, fuelUsed: Double?, date: String, notes: String?) -> Unit = { _, _, _, _ -> }
) {
    var showRecordUsageDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
                    Text(
                        text = "Machinery Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit machinery"
                        )
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete machinery"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MachineryHeaderCard(
                    name = machineName,
                    type = machineType,
                    status = status
                )
            }

            item {
                SectionTitle("Machine Overview")
            }

            item {
                OverviewCard(
                    totalHoursUsed = totalHoursUsed,
                    totalFuelUsed = totalFuelUsed,
                    condition = condition,
                    location = location,
                    purchaseDate = purchaseDate,
                    purchaseCost = purchaseCost
                )
            }

            item {
                SectionTitle("Maintenance")
            }

            item {
                MaintenanceCard(
                    lastMaintenance = lastMaintenance,
                    nextMaintenance = nextMaintenance
                )
            }

            item {
                SectionTitle("Machine Actions")
            }

            item {
                ActionCard(
                    icon = Icons.Default.Schedule,
                    title = "Usage History",
                    description = "View previous machinery usage records",
                    onClick = onUsageHistoryClick
                )
            }

            item {
                ActionCard(
                    icon = Icons.Default.Build,
                    title = "Maintenance History",
                    description = "View maintenance records and costs",
                    onClick = onMaintenanceHistoryClick
                )
            }

            item {
                ActionCard(
                    icon = Icons.Default.Settings,
                    title = "Record Machinery Usage",
                    description = "Record a new machinery usage activity",
                    onClick = { showRecordUsageDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showEditDialog) {
        EditMachineryDialog(
            currentName = machineName,
            currentPurchaseDate = purchaseDate,
            currentPurchaseCost = rawPurchaseCost,
            onDismiss = { showEditDialog = false },
            onSave = { name, pDate, pCost ->
                onEditSave(name, pDate, pCost)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete $machineName? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRecordUsageDialog) {
        RecordUsageDialog(
            machineName = machineName,
            onDismiss = { showRecordUsageDialog = false },
            onRecord = { hours, fuel, date, notes ->
                onRecordUsageSave(hours, fuel, date, notes)
                showRecordUsageDialog = false
            }
        )
    }
}

@Composable
private fun EditMachineryDialog(
    currentName: String,
    currentPurchaseDate: String,
    currentPurchaseCost: String,
    onDismiss: () -> Unit,
    onSave: (name: String, purchaseDate: String?, purchaseCost: Double?) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var dateText by remember { mutableStateOf(currentPurchaseDate) }
    var costText by remember { mutableStateOf(currentPurchaseCost) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Machinery Details",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorMsg = null
                    },
                    label = { Text("Name *") },
                    placeholder = { Text("e.g. Massey Ferguson 375") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        errorMsg = null
                    },
                    label = "Purchase Date",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { input ->
                        if (input.all { c -> c.isDigit() || c == '.' }) {
                            costText = input
                            errorMsg = null
                        }
                    },
                    label = { Text("Purchase Cost (KES)") },
                    placeholder = { Text("e.g. 3500000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                    if (nameText.isBlank()) {
                        errorMsg = "Please enter machinery name."
                        return@Button
                    }
                    val today = try { LocalDate.now() } catch (_: Exception) { null }
                    val dParsed = if (dateText.isNotBlank()) try { LocalDate.parse(dateText.trim()) } catch (_: Exception) { null } else null

                    if (dParsed != null && today != null && dParsed.isBefore(today)) {
                        errorMsg = "Purchase date cannot be in the past."
                        return@Button
                    }

                    onSave(
                        nameText.trim(),
                        dateText.trim().ifBlank { null },
                        costText.toDoubleOrNull()
                    )
                }
            ) {
                Text("Save")
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
private fun RecordUsageDialog(
    machineName: String,
    onDismiss: () -> Unit,
    onRecord: (hoursUsed: Double, fuelUsed: Double?, date: String, notes: String?) -> Unit
) {
    var hoursText by remember { mutableStateOf("") }
    var fuelText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Usage - $machineName",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Hours Used *") },
                    placeholder = { Text("e.g. 4.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fuelText,
                    onValueChange = { fuelText = it },
                    label = { Text("Fuel Used (Litres)") },
                    placeholder = { Text("e.g. 15.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = "Usage Date",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Task / Purpose Notes") },
                    placeholder = { Text("e.g. Plowing Division A") },
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
                    val hrs = hoursText.toDoubleOrNull()
                    val fuel = fuelText.toDoubleOrNull()
                    val dateParsed = if (dateText.isNotBlank()) try { LocalDate.parse(dateText.trim()) } catch (_: Exception) { null } else null
                    val today = try { LocalDate.now() } catch (_: Exception) { null }

                    when {
                        hrs == null || hrs <= 0.0 -> errorMsg = "Please enter valid hours used."
                        dateParsed != null && today != null && dateParsed.isBefore(today) -> errorMsg = "Usage date cannot be in the past."
                        else -> {
                            val finalDate = dateText.trim().ifBlank {
                                try {
                                    LocalDate.now().toString()
                                } catch (_: Exception) {
                                    "2026-03-30"
                                }
                            }
                            onRecord(hrs, fuel, finalDate, notesText.trim().ifBlank { null })
                        }
                    }
                }
            ) {
                Text("Record Usage")
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
private fun MachineryHeaderCard(
    name: String,
    type: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    totalHoursUsed: String,
    totalFuelUsed: String,
    condition: String,
    location: String,
    purchaseDate: String,
    purchaseCost: String
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MachineryMetric(
                    icon = Icons.Default.Schedule,
                    label = "Hours used",
                    value = totalHoursUsed
                )

                MachineryMetric(
                    icon = Icons.Default.LocalGasStation,
                    label = "Fuel used",
                    value = totalFuelUsed
                )

                MachineryMetric(
                    icon = Icons.Default.Settings,
                    label = "Condition",
                    value = condition
                )
            }

            if (location.isNotBlank()) {
                MaintenanceRow(
                    label = "Location",
                    value = location
                )
            }

            if (purchaseDate.isNotBlank()) {
                MaintenanceRow(
                    label = "Purchase Date",
                    value = purchaseDate
                )
            }

            if (purchaseCost.isNotBlank()) {
                MaintenanceRow(
                    label = "Purchase Cost",
                    value = purchaseCost
                )
            }
        }
    }
}

@Composable
private fun MachineryMetric(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MaintenanceCard(
    lastMaintenance: String,
    nextMaintenance: String
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MaintenanceRow(
                label = "Last maintenance date",
                value = lastMaintenance
            )

            MaintenanceRow(
                label = "Next scheduled maintenance",
                value = nextMaintenance
            )
        }
    }
}

@Composable
private fun MaintenanceRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}
