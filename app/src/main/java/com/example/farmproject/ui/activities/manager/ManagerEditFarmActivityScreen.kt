package com.example.farmproject.ui.activities.manager

import com.example.farmproject.ui.common.DatePickerField

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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
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
import java.time.LocalDate

private fun extractExpectedHarvestDate(notes: String?): String? {
    if (notes.isNullOrBlank()) return null
    val regex = Regex("""Expected Harvest Date:\s*([0-9]{4}-[0-9]{2}-[0-9]{2})""", RegexOption.IGNORE_CASE)
    return regex.find(notes)?.groupValues?.get(1)
}

private fun stripExpectedHarvestDateFromNotes(notes: String?): String {
    if (notes.isNullOrBlank()) return ""
    return notes.replace(Regex("""Expected Harvest Date:\s*[0-9]{4}-[0-9]{2}-[0-9]{2}\n?""", RegexOption.IGNORE_CASE), "").trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerEditFarmActivityScreen(
    activityId: String = "",
    initialType: String = "",
    initialName: String = "",
    initialDueDate: String = "",
    initialNotes: String = "",
    initialRole: String = "Worker",
    initialDivisionId: String? = null,
    initialQuantity: Double? = null,
    initialQuantityUnit: String? = null,
    initialEquipmentNeeded: String? = null,
    initialWorkerProfileId: String? = null,
    initialSupervisorProfileId: String? = null,
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        if (saveState is FarmActivitySaveState.Saved) {
            viewModel.resetSaveState()
            onBackClick()
        }
    }

    val todayDateStr = try {
        LocalDate.now().toString()
    } catch (_: Exception) {
        "2026-03-30"
    }

    val initialExtractedDate = extractExpectedHarvestDate(initialNotes).orEmpty()
    val initialCleanNotes = stripExpectedHarvestDateFromNotes(initialNotes)

    var activityType by remember(initialType) { mutableStateOf(initialType.ifBlank { "Planting" }) }
    var activityNameText by remember(initialName) { mutableStateOf(initialName) }
    var dueDate by remember(initialDueDate) { mutableStateOf(initialDueDate.ifBlank { todayDateStr }) }
    var notes by remember(initialNotes) { mutableStateOf(initialCleanNotes) }
    var assignedRole by remember(initialRole) { mutableStateOf(if (initialRole.contains("Supervisor", ignoreCase = true)) "Supervisor" else "Worker") }
    var selectedWorkerProfileId by remember(initialWorkerProfileId) { mutableStateOf(initialWorkerProfileId) }
    var selectedSupervisorProfileId by remember(initialSupervisorProfileId) { mutableStateOf(initialSupervisorProfileId) }
    var assigneeDropdownExpanded by remember { mutableStateOf(false) }
    var quantityText by remember(initialQuantity) { mutableStateOf(initialQuantity?.toString().orEmpty()) }
    var quantityUnitText by remember(initialQuantityUnit) { mutableStateOf(initialQuantityUnit.orEmpty()) }
    var expectedHarvestDateText by remember(initialNotes) { mutableStateOf(initialExtractedDate) }
    var selectedMachineryName by remember(initialEquipmentNeeded) { mutableStateOf(initialEquipmentNeeded.orEmpty()) }

    var dueDateError by remember { mutableStateOf<String?>(null) }
    var harvestDateError by remember { mutableStateOf<String?>(null) }

    val isPlantingType = activityType.contains("Planting", ignoreCase = true) ||
            activityType.contains("Transplanting", ignoreCase = true)

    var isEntireFarm by remember(initialDivisionId) { mutableStateOf(initialDivisionId.isNullOrBlank()) }
    var selectedDivision by remember(initialDivisionId, divisions) {
        mutableStateOf(divisions.find { it.divisionId == initialDivisionId } ?: divisions.firstOrNull())
    }

    var typeExpanded by remember { mutableStateOf(false) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var machineryExpanded by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF1565C0),
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = Color(0xFF1565C0),
        cursorColor = Color(0xFF1565C0)
    )

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
                        text = "Edit Farm Activity",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Update farm task details",
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
                    expanded = typeExpanded && activityTypes.isNotEmpty(),
                    onDismissRequest = { typeExpanded = false }
                ) {
                    activityTypes.forEach { item ->
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
            val availableEmployees = if (assignedRole == "Worker") workers else supervisors
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
                            if (selectedDivision == null && divisions.isNotEmpty()) {
                                selectedDivision = divisions.first()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isEntireFarm,
                        onClick = {
                            isEntireFarm = false
                            if (selectedDivision == null && divisions.isNotEmpty()) {
                                selectedDivision = divisions.first()
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
                    if (divisions.isEmpty()) {
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
                                divisions.forEach { division ->
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
                    machineryList.forEach { machine ->
                        DropdownMenuItem(
                            text = { Text(machine.name) },
                            onClick = {
                                selectedMachineryName = machine.name
                                machineryExpanded = false
                            }
                        )
                    }
                }
            }

            // QUANTITY & MEASUREMENT
            SectionTitle(
                icon = Icons.Default.Numbers,
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantityUnitText,
                    onValueChange = { quantityUnitText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Unit (optional)") },
                    placeholder = { Text("e.g. Kg, Acres") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (isPlantingType) {
                DatePickerField(
                    value = expectedHarvestDateText,
                    onValueChange = {
                        expectedHarvestDateText = it
                        harvestDateError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Expected Harvest Date",
                    isError = harvestDateError != null,
                    supportingText = harvestDateError?.let { { Text(it) } },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // SCHEDULE DETAILS
            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                title = "Schedule Details"
            )

            DatePickerField(
                value = dueDate,
                onValueChange = {
                    dueDate = it
                    dueDateError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = "Due Date",
                isError = dueDateError != null,
                supportingText = dueDateError?.let { { Text(it) } },
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // NOTES
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Additional Information"
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Notes (optional)") },
                placeholder = { Text("Enter any additional details or instructions...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null
                    )
                },
                maxLines = 5,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            if (saveState is FarmActivitySaveState.Error) {
                Text(
                    text = (saveState as FarmActivitySaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SAVE BUTTON
            val isSaving = saveState is FarmActivitySaveState.Saving

            Button(
                onClick = {
                    if (isSaving) return@Button

                    val today = try { LocalDate.now() } catch (_: Exception) { null }
                    val dueParsed = try { LocalDate.parse(dueDate.trim()) } catch (_: Exception) { null }

                    if (dueParsed != null && today != null && dueParsed.isBefore(today)) {
                        dueDateError = "Due date cannot be in the past."
                        return@Button
                    }

                    if (isPlantingType && expectedHarvestDateText.isNotBlank()) {
                        val hParsed = try { LocalDate.parse(expectedHarvestDateText.trim()) } catch (_: Exception) { null }
                        if (hParsed != null && today != null && hParsed.isBefore(today)) {
                            harvestDateError = "Expected harvest date cannot be in the past."
                            return@Button
                        }
                    }

                    val finalType = activityType.ifBlank { "Planting" }
                    val finalName = activityNameText.trim().ifBlank { null }
                    val finalDate = dueDate.ifBlank { todayDateStr }
                    val targetDivisionId = if (isEntireFarm) null else selectedDivision?.divisionId
                    val parsedQuantity = quantityText.toDoubleOrNull()
                    val parsedUnit = quantityUnitText.trim().ifBlank { null }
                    val finalMachinery = selectedMachineryName.trim().ifBlank { null }

                    val userNotes = notes.trim()
                    val finalNotes = if (isPlantingType && expectedHarvestDateText.isNotBlank()) {
                        val hDateNote = "Expected Harvest Date: ${expectedHarvestDateText.trim()}"
                        if (userNotes.isNotBlank()) "$hDateNote\n$userNotes" else hDateNote
                    } else {
                        userNotes.ifBlank { null }
                    }

                    onSaveClick(
                        finalType,
                        finalName,
                        finalDate,
                        finalNotes,
                        assignedRole,
                        selectedWorkerProfileId,
                        selectedSupervisorProfileId,
                        targetDivisionId,
                        parsedQuantity,
                        parsedUnit,
                        finalMachinery
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Save Changes",
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
