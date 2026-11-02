package com.example.farmproject.ui.activities.manager

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmproject.data.model.EmployeeDisplay

private fun extractExpectedHarvestDate(notes: String?): String? {
    if (notes.isNullOrBlank()) return null
    val regex = Regex("""Expected Harvest Date:\s*([0-9]{4}-[0-9]{2}-[0-9]{2})""", RegexOption.IGNORE_CASE)
    return regex.find(notes)?.groupValues?.get(1)
}

private fun cleanNotes(notes: String?): String {
    if (notes.isNullOrBlank()) return "No additional notes recorded for this activity."
    val cleaned = notes.replace(Regex("""Expected Harvest Date:\s*[0-9]{4}-[0-9]{2}-[0-9]{2}\n?""", RegexOption.IGNORE_CASE), "").trim()
    return cleaned.ifBlank { "No additional notes recorded for this activity." }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFarmActivityDetailsScreen(
    title: String = "Spray Maize",
    type: String = "Spraying",
    assignedTo: String = "Unassigned",
    dueDate: String = "14 Sep 2026",
    durationText: String = "1.0 hr",
    status: String = "Pending",
    isOverdue: Boolean = false,
    notes: String = "No additional notes recorded for this activity.",
    quantity: Double? = null,
    quantityUnit: String? = null,
    equipmentNeeded: String? = null,
    workers: List<EmployeeDisplay> = emptyList(),
    supervisors: List<EmployeeDisplay> = emptyList(),
    isManagerRole: Boolean = true,
    onBackClick: () -> Unit = {},
    onStartClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    onAssignSave: (isSupervisor: Boolean, profileId: String) -> Unit = { _, _ -> },
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var showAssignDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isAlreadyAssignedToSpecificPerson = assignedTo.isNotBlank() &&
            !assignedTo.equals("Unassigned", ignoreCase = true) &&
            !assignedTo.equals("Worker", ignoreCase = true) &&
            !assignedTo.equals("Supervisor", ignoreCase = true)

    val isCompleted = status.equals("Completed", ignoreCase = true) ||
            status.equals("completed", ignoreCase = true) ||
            status.equals("Done", ignoreCase = true)

    val isInProgress = status.equals("In Progress", ignoreCase = true) ||
            status.equals("in_progress", ignoreCase = true) ||
            status.equals("Ongoing", ignoreCase = true)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1565C0))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Farm activity details",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isManagerRole && !isCompleted) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Activity",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    StatusBadge(
                        status = status.ifBlank { "Pending" },
                        isOverdue = isOverdue
                    )
                }
            }

            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                title = "Activity Details"
            )

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Assigned To",
                            value = assignedTo
                        )

                        if (isManagerRole && !isCompleted && !isAlreadyAssignedToSpecificPerson) {
                            OutlinedButton(
                                onClick = { showAssignDialog = true },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Assign To",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    if (quantity != null && quantity > 0.0) {
                        DetailRow(
                            icon = Icons.Default.Numbers,
                            label = "Quantity",
                            value = if (quantityUnit.isNullOrBlank()) "$quantity" else "$quantity $quantityUnit"
                        )
                    }

                    if (!equipmentNeeded.isNullOrBlank()) {
                        DetailRow(
                            icon = Icons.Default.Agriculture,
                            label = "Machinery Required",
                            value = equipmentNeeded
                        )
                    }

                    val extractedHarvestDate = extractExpectedHarvestDate(notes)
                    if (!extractedHarvestDate.isNullOrBlank()) {
                        DetailRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Expected Harvest Date",
                            value = extractedHarvestDate
                        )
                    }

                    if (isCompleted) {
                        DetailRow(
                            icon = Icons.Default.Schedule,
                            label = "Duration",
                            value = durationText.ifBlank { "1.0 hr" }
                        )
                    } else {
                        DetailRow(
                            icon = Icons.Default.CalendarMonth,
                            label = "Due Date",
                            value = dueDate
                        )
                    }

                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        label = "Type",
                        value = type
                    )

                    DetailRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Status",
                        value = status.ifBlank { "Pending" }
                    )
                }
            }

            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Additional Information"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = cleanNotes(notes),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isCompleted) {
                // COMPLETED BADGE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "This activity is completed.",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (!isManagerRole) {
                // SUPERVISOR AND WORKER ACTION BUTTONS
                if (isInProgress) {
                    Button(
                        onClick = onCompleteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Activity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Begin Activity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // MANAGER EDIT & DELETE ACTIONS
            if (isManagerRole) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }

                    if (!isCompleted) {
                        Button(
                            onClick = onEditClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Activity")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Confirm Delete",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete this farm activity? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
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
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAssignDialog) {
        AssignActivityDialog(
            supervisors = supervisors,
            workers = workers,
            onDismiss = { showAssignDialog = false },
            onAssign = { isSupervisor, profileId ->
                onAssignSave(isSupervisor, profileId)
                showAssignDialog = false
            }
        )
    }
}

@Composable
private fun AssignActivityDialog(
    supervisors: List<EmployeeDisplay>,
    workers: List<EmployeeDisplay>,
    onDismiss: () -> Unit,
    onAssign: (isSupervisor: Boolean, profileId: String) -> Unit
) {
    var isSupervisorSelected by remember { mutableStateOf(false) }
    val currentEmployees = if (isSupervisorSelected) supervisors else workers
    var selectedEmployee by remember { mutableStateOf(currentEmployees.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Assign Activity",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Assign task to a supervisor or worker:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = !isSupervisorSelected,
                        onClick = {
                            isSupervisorSelected = false
                            selectedEmployee = workers.firstOrNull()
                        },
                        label = { Text("Worker") }
                    )

                    FilterChip(
                        selected = isSupervisorSelected,
                        onClick = {
                            isSupervisorSelected = true
                            selectedEmployee = supervisors.firstOrNull()
                        },
                        label = { Text("Supervisor") }
                    )
                }

                if (currentEmployees.isEmpty()) {
                    Text(
                        text = "No added ${if (isSupervisorSelected) "supervisors" else "workers"} found. Please add employees first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentEmployees.forEach { employee ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedEmployee = employee }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedEmployee?.profileId == employee.profileId,
                                    onClick = { selectedEmployee = employee }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = employee.fullName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!employee.employeeNumber.isNullOrBlank()) {
                                        Text(
                                            text = "#${employee.employeeNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedEmployee?.let { onAssign(isSupervisorSelected, it.profileId) }
                },
                enabled = selectedEmployee != null
            ) {
                Text("Assign")
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
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: String,
    isOverdue: Boolean
) {
    val containerColor = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1565C0),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
    }
}
