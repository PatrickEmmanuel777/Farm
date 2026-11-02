package com.example.farmproject.ui.employees.manager

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.farmproject.data.model.SupervisorDetailsInput
import com.example.farmproject.data.model.SupervisorDetailsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerEmployeeDetailsScreen(
    employee: EmployeeDisplay?,
    roleLabel: String = "Supervisor",
    dutiesLabel: String? = null,
    onBackClick: () -> Unit = {},
    onUpdateSave: (fullName: String, phone: String?, employeeNumber: String?) -> Unit = { _, _, _ -> },
    onDeleteClick: () -> Unit = {},
    // ---- Employment details (supervisors only; workers keep the defaults) ----
    showEmploymentDetails: Boolean = false,
    employmentDetails: SupervisorDetailsRow? = null,
    isEmploymentLoading: Boolean = false,
    employmentLoadError: String? = null,
    isSavingEmployment: Boolean = false,
    employmentSaveError: String? = null,
    onRetryEmployment: () -> Unit = {},
    onSaveEmployment: (SupervisorDetailsInput) -> Unit = {},
    onEmploymentDialogDismiss: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEmploymentDialog by remember { mutableStateOf(false) }

    // Close the employment form when a save finishes without an error.
    var wasSavingEmployment by remember { mutableStateOf(false) }
    LaunchedEffect(isSavingEmployment) {
        if (wasSavingEmployment && !isSavingEmployment && employmentSaveError == null) {
            showEmploymentDialog = false
        }
        wasSavingEmployment = isSavingEmployment
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee?.fullName ?: "$roleLabel Details",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$roleLabel Profile & Information",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                if (employee != null) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit employee",
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
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (employee == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$roleLabel details not found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // OVERVIEW CARD
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
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = employee.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = roleLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // DETAILS CARD
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
                        DetailItemRow(
                            icon = Icons.Default.Email,
                            label = "Email Address",
                            value = employee.email ?: "Not set"
                        )

                        DetailItemRow(
                            icon = Icons.Default.Phone,
                            label = "Phone Number",
                            value = employee.phoneNumber ?: "Not set"
                        )

                        if (roleLabel.equals("Supervisor", ignoreCase = true)) {
                            DetailItemRow(
                                icon = Icons.Default.Badge,
                                label = "Assigned Duties",
                                value = dutiesLabel?.ifBlank { null } ?: "Attendance Supervisor"
                            )
                        } else {
                            DetailItemRow(
                                icon = Icons.Default.Badge,
                                label = "Employee Number",
                                value = employee.employeeNumber ?: "Not set"
                            )
                        }

                        DetailItemRow(
                            icon = Icons.Default.SupervisorAccount,
                            label = "Role",
                            value = roleLabel
                        )
                    }
                }

                // EMPLOYMENT DETAILS CARD (supervisors)
                if (showEmploymentDetails) {
                    EmploymentDetailsCard(
                        details = employmentDetails,
                        isLoading = isEmploymentLoading,
                        loadError = employmentLoadError,
                        onRetry = onRetryEmployment,
                        onActionClick = { showEmploymentDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }

                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }

    // EDIT DIALOG
    if (showEditDialog && employee != null) {
        EditEmployeeDialog(
            currentName = employee.fullName,
            currentPhone = employee.phoneNumber ?: "",
            currentEmployeeNum = employee.employeeNumber ?: "",
            roleLabel = roleLabel,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, empNum ->
                onUpdateSave(name, phone, empNum)
                showEditDialog = false
            }
        )
    }

    // EMPLOYMENT DETAILS FORM
    if (showEmploymentDialog && employee != null) {
        EmploymentDetailsDialog(
            initial = employmentDetails,
            isSaving = isSavingEmployment,
            saveError = employmentSaveError,
            onDismiss = {
                showEmploymentDialog = false
                onEmploymentDialogDismiss()
            },
            onSave = onSaveEmployment
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete $roleLabel") },
            text = { Text("Are you sure you want to delete ${employee?.fullName ?: roleLabel}? This action cannot be undone.") },
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
}

@Composable
private fun EmploymentDetailsCard(
    details: SupervisorDetailsRow?,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onActionClick: () -> Unit
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
            Text(
                text = "Employment Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }

                loadError != null -> {
                    Text(
                        text = loadError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }

                details == null -> {
                    Text(
                        text = "No employment details added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    DetailItemRow(Icons.Default.Payments, "Salary", formatSalary(details.salary))
                    DetailItemRow(Icons.Default.Cake, "Age", details.age.toString())
                    DetailItemRow(Icons.Default.Home, "Residence", details.residence)
                    DetailItemRow(Icons.Default.CalendarMonth, "Date Employed", details.dateEmployed)
                    DetailItemRow(Icons.Default.Wc, "Gender", details.gender)
                }
            }

            // Hidden while loading or on a load error so an existing record is never overwritten blindly.
            if (!isLoading && loadError == null) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (details == null) "Add details" else "Edit details")
                }
            }
        }
    }
}

@Composable
private fun EditEmployeeDialog(
    currentName: String,
    currentPhone: String,
    currentEmployeeNum: String,
    roleLabel: String,
    onDismiss: () -> Unit,
    onSave: (fullName: String, phone: String?, empNum: String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var empNum by remember { mutableStateOf(currentEmployeeNum) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $roleLabel Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = empNum,
                    onValueChange = { empNum = it },
                    label = { Text("Employee Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), phone.trim().ifBlank { null }, empNum.trim().ifBlank { null })
                    }
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
private fun DetailItemRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
}
