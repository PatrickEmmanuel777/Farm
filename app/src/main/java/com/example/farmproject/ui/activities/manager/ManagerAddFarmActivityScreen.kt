package com.example.farmproject.ui.activities.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.Division
import com.example.farmproject.data.model.EmployeeDisplay
import com.example.farmproject.data.model.Machinery
import com.example.farmproject.ui.common.DatePickerField
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAddFarmActivityScreen(
    farmId: String = "",
    viewModel: FarmActivitiesViewModel = viewModel(),
    activityTypes: List<String> = listOf(
        "Planting",
        "Transplanting",
        "Spraying",
        "Irrigation",
        "Fertilizing",
        "Harvesting",
        "Maintenance"
    ),
    divisions: List<Division> = emptyList(),
    machineryList: List<Machinery> = emptyList(),
    workers: List<EmployeeDisplay> = emptyList(),
    supervisors: List<EmployeeDisplay> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    onSaveClick: (
        activityTypeName: String,
        activityName: String?,
        dueDate: String,
        notes: String?,
        assignedRole: String,
        workerProfileId: String?,
        supervisorProfileId: String?,
        divisionId: String?,
        quantity: Double?,
        quantityUnit: String?,
        equipmentNeeded: String?
    ) -> Unit = { activityTypeName, activityName, dueDate, notes, assignedRole, workerProfileId, supervisorProfileId, divisionId, quantity, quantityUnit, equipmentNeeded ->
        viewModel.addActivity(
            activityTypeName = activityTypeName,
            activityName = activityName,
            activityDate = dueDate,
            status = "Pending",
            notes = notes,
            assignedRole = assignedRole,
            workerProfileId = workerProfileId,
            supervisorProfileId = supervisorProfileId,
            divisionId = divisionId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            equipmentNeeded = equipmentNeeded
        )
    }
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val formOptions by viewModel.formOptions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFormOptions()
    }

    LaunchedEffect(saveState) {
        if (saveState is FarmActivitySaveState.Saved) {
            viewModel.resetSaveState()
            onSaveSuccess()
            onBackClick()
        }
    }

    val liveActivityTypes = formOptions.activityTypeNames.ifEmpty { activityTypes }
    val liveDivisions = formOptions.divisions.ifEmpty { divisions }
    val liveMachineryList = formOptions.machinery.ifEmpty { machineryList }
    val liveWorkers = formOptions.workers.ifEmpty { workers }
    val liveSupervisors = formOptions.supervisors.ifEmpty { supervisors }

    val todayDateStr = try {
        LocalDate.now().toString()
    } catch (_: Exception) {
        "2026-10-27"
    }

    var activityType by remember { mutableStateOf(liveActivityTypes.firstOrNull() ?: "Planting") }
    var activityNameText by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(todayDateStr) }
    var notes by remember { mutableStateOf("") }
    var assignedRole by remember { mutableStateOf("Worker") } // "Worker" or "Supervisor"
    var selectedWorkerProfileId by remember { mutableStateOf<String?>(null) }
    var selectedSupervisorProfileId by remember { mutableStateOf<String?>(null) }
    var assigneeDropdownExpanded by remember { mutableStateOf(false) }

    var isEntireFarm by remember { mutableStateOf(true) }
    var selectedDivision by remember { mutableStateOf<Division?>(null) }
    var divisionExpanded by remember { mutableStateOf(false) }

    var selectedMachineryName by remember { mutableStateOf("") }
    var machineryExpanded by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("") }
    var quantityUnitText by remember { mutableStateOf("") }

    var typeExpanded by remember { mutableStateOf(false) }

    val isSaving = saveState is FarmActivitySaveState.Saving
    val serverError = (saveState as? FarmActivitySaveState.Error)?.message

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF1B5E20),
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = Color(0xFF1B5E20),
        cursorColor = Color(0xFF1B5E20)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, enabled = !isSaving) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Add Farm Activity",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Schedule a new farm task",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ACTIVITY TYPE
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "Activity Type"
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = activityType,
                    onValueChange = {
                        activityType = it
                        typeExpanded = true
                    },
                    readOnly = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    label = { Text("Activity Type *") },
                    placeholder = { Text("Select or type activity type") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = typeExpanded && liveActivityTypes.isNotEmpty(),
                    onDismissRequest = { typeExpanded = false }
                ) {
                    liveActivityTypes.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                activityType = item
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // ACTIVITY NAME (OPTIONAL)
            OutlinedTextField(
                value = activityNameText,
                onValueChange = { activityNameText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Activity Name (optional)") },
                placeholder = { Text("e.g. Maize Spraying Block 1") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null
                    )
                },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // ASSIGNED ROLE & SPECIFIC EMPLOYEE SELECTION
            SectionTitle(
                icon = Icons.Default.Badge,
                title = "Assigned Employee & Role"
            )

            Text(
                text = "Select whether this task is intended for a Worker or a Supervisor:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = assignedRole == "Worker",
                    onClick = {
                        assignedRole = "Worker"
                        selectedSupervisorProfileId = null
                    },
                    label = { Text("Worker") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = assignedRole == "Supervisor",
                    onClick = {
                        assignedRole = "Supervisor"
                        selectedWorkerProfileId = null
                    },
                    label = { Text("Supervisor") },
                    modifier = Modifier.weight(1f)
                )
            }

            // SPECIFIC EMPLOYEE DROPDOWN
            val availableEmployees = if (assignedRole == "Worker") liveWorkers else liveSupervisors
            val selectedProfileId = if (assignedRole == "Worker") selectedWorkerProfileId else selectedSupervisorProfileId
            val selectedEmployeeName = availableEmployees.find { it.profileId == selectedProfileId }?.let {
                "${it.fullName}${if (!it.employeeNumber.isNullOrBlank()) " (${it.employeeNumber})" else ""}"
            } ?: "Unassigned / Any $assignedRole"

            ExposedDropdownMenuBox(
                expanded = assigneeDropdownExpanded,
                onExpandedChange = { assigneeDropdownExpanded = !assigneeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedEmployeeName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Select Specific $assignedRole") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeDropdownExpanded) },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = assigneeDropdownExpanded,
                    onDismissRequest = { assigneeDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassigned / Any $assignedRole") },
                        onClick = {
                            if (assignedRole == "Worker") selectedWorkerProfileId = null else selectedSupervisorProfileId = null
                            assigneeDropdownExpanded = false
                        }
                    )

                    availableEmployees.forEach { emp ->
                        val labelText = "${emp.fullName}${if (!emp.employeeNumber.isNullOrBlank()) " (${emp.employeeNumber})" else ""}"
                        DropdownMenuItem(
                            text = { Text(labelText) },
                            onClick = {
                                if (assignedRole == "Worker") {
                                    selectedWorkerProfileId = emp.profileId
                                    selectedSupervisorProfileId = null
                                } else {
                                    selectedSupervisorProfileId = emp.profileId
                                    selectedWorkerProfileId = null
                                }
                                assigneeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // FARM LOCATION / DIVISION SELECTION
            SectionTitle(
                icon = Icons.Default.Place,
                title = "Farm Location"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEntireFarm = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isEntireFarm,
                        onClick = { isEntireFarm = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entire Farm",
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isEntireFarm = false
                            if (selectedDivision == null && liveDivisions.isNotEmpty()) {
                                selectedDivision = liveDivisions.first()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isEntireFarm,
                        onClick = {
                            isEntireFarm = false
                            if (selectedDivision == null && liveDivisions.isNotEmpty()) {
                                selectedDivision = liveDivisions.first()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Specific Division",
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!isEntireFarm) {
                    if (liveDivisions.isEmpty()) {
                        Text(
                            text = "No farm divisions found. Task will apply to entire farm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = divisionExpanded,
                            onExpandedChange = { divisionExpanded = !divisionExpanded },
                            modifier = Modifier.padding(start = 32.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedDivision?.name ?: "Select division",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                label = { Text("Division") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded)
                                },
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = divisionExpanded,
                                onDismissRequest = { divisionExpanded = false }
                            ) {
                                liveDivisions.forEach { division ->
                                    DropdownMenuItem(
                                        text = { Text(division.name) },
                                        onClick = {
                                            selectedDivision = division
                                            divisionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MACHINERY REQUIRED (OPTIONAL)
            SectionTitle(
                icon = Icons.Default.Agriculture,
                title = "Machinery Required"
            )

            ExposedDropdownMenuBox(
                expanded = machineryExpanded,
                onExpandedChange = { machineryExpanded = !machineryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMachineryName.ifBlank { "None / Not Required" },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Machinery Item (optional)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = machineryExpanded)
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = machineryExpanded,
                    onDismissRequest = { machineryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None / Not Required") },
                        onClick = {
                            selectedMachineryName = ""
                            machineryExpanded = false
                        }
                    )
                    liveMachineryList.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.name) },
                            onClick = {
                                selectedMachineryName = m.name
                                machineryExpanded = false
                            }
                        )
                    }
                }
            }

            // QUANTITY & MEASUREMENT (OPTIONAL)
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "Quantity & Measurement"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity") },
                    placeholder = { Text("e.g. 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantityUnitText,
                    onValueChange = { quantityUnitText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Unit (optional)") },
                    placeholder = { Text("e.g. kg, acres, litres") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // SCHEDULE DETAILS
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "Schedule Details"
            )

            DatePickerField(
                label = "Due Date *",
                value = dueDate,
                onValueChange = { dueDate = it }
            )

            // ADDITIONAL INFORMATION
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Assignment,
                title = "Additional Information"
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes (optional)") },
                placeholder = { Text("Add instructions or notes for assigned workers...") },
                minLines = 3,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            if (!serverError.isNullOrBlank()) {
                Text(
                    text = serverError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val finalDivId = if (isEntireFarm) null else selectedDivision?.divisionId
                    val qtyVal = quantityText.trim().toDoubleOrNull()
                    val qtyUnit = quantityUnitText.trim().ifBlank { null }
                    val equip = selectedMachineryName.trim().ifBlank { null }

                    onSaveClick(
                        activityType,
                        activityNameText.trim().ifBlank { null },
                        dueDate,
                        notes.trim().ifBlank { null },
                        assignedRole,
                        selectedWorkerProfileId,
                        selectedSupervisorProfileId,
                        finalDivId,
                        qtyVal,
                        qtyUnit,
                        equip
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saving Task...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
            tint = Color(0xFF1B5E20),
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
