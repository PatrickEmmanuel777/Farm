package com.example.farmproject.ui.crops.manager

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.data.model.CropPlantingDisplay
import com.example.farmproject.data.repository.CropHarvestDisplay
import com.example.farmproject.ui.common.DatePickerField
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private fun formatNumber(valDouble: Double): String =
    String.format(Locale.US, "%,.1f", valDouble)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerYieldScreen(
    viewModel: YieldViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRecordDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGradeFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadYieldData()
    }

    LaunchedEffect(saveState) {
        if (saveState is YieldSaveState.Error) {
            snackbarHostState.showSnackbar((saveState as YieldSaveState.Error).message)
        } else if (saveState is YieldSaveState.Saved) {
            showRecordDialog = false
            viewModel.resetSaveState()
            snackbarHostState.showSnackbar("Yield recorded successfully.")
        }
    }

    val softCreamBg = Color(0xFFF7F6F0)

    Scaffold(
        containerColor = softCreamBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1A1D1A)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "Crop Yields",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1D1A)
                        )
                        Text(
                            text = "Record & track harvest yields",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7068)
                        )
                    }
                }

                Button(
                    onClick = { showRecordDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Record Yield",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            when (val uiState = state) {
                is YieldUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1B5E20))
                    }
                }

                is YieldUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadYieldData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is YieldUiState.Success -> {
                    val totalHarvested = uiState.harvests.sumOf { it.harvest.quantityHarvested }

                    // Summary Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        YieldSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Harvest",
                            value = "${formatNumber(totalHarvested)} kg",
                            subtitle = "${uiState.harvests.size} logs recorded",
                            icon = Icons.Default.Agriculture,
                            iconBg = Color(0xFFE8F5E9),
                            iconTint = Color(0xFF1B5E20)
                        )

                        YieldSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Planted Crops",
                            value = "${uiState.activePlantings.size}",
                            subtitle = "Active sections",
                            icon = Icons.Default.Eco,
                            iconBg = Color(0xFFFFF3E0),
                            iconTint = Color(0xFFE65100)
                        )
                    }

                    // Search & Filters Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by crop or division...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Grade Filter Chips
                    val gradeFilters = listOf("All", "Grade A", "Grade B", "Export Grade")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gradeFilters.forEach { grade ->
                            FilterChip(
                                selected = (grade == "All" && selectedGradeFilter == null) || selectedGradeFilter == grade,
                                onClick = {
                                    selectedGradeFilter = if (grade == "All") null else grade
                                },
                                label = { Text(grade) }
                            )
                        }
                    }

                    // Harvest Logs List
                    Text(
                        text = "Harvest Logs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1D1A)
                    )

                    val filteredHarvests = uiState.harvests.filter { h ->
                        val query = searchQuery.trim().lowercase()
                        val matchesQuery = query.isBlank() ||
                                h.cropName.lowercase().contains(query) ||
                                h.divisionName.lowercase().contains(query)

                        val matchesGrade = selectedGradeFilter == null || h.harvest.qualityGrade.equals(selectedGradeFilter, ignoreCase = true)

                        matchesQuery && matchesGrade
                    }

                    if (filteredHarvests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No crop yield records found. Tap 'Record Yield' to add a log.",
                                color = Color(0xFF6E7068),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredHarvests) { item ->
                                HarvestLogRowCard(harvestDisplay = item)
                            }
                        }
                    }

                    if (showRecordDialog) {
                        RecordYieldDialog(
                            activePlantings = uiState.activePlantings,
                            saveState = saveState,
                            onDismiss = { showRecordDialog = false },
                            onRecord = { pId, dId, hDate, qty, unit, grade, notes ->
                                viewModel.recordYield(pId, dId, hDate, qty, unit, grade, notes)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YieldSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(color = iconBg, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6E7068)
                )
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1D1A)
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF6E7068)
            )
        }
    }
}

@Composable
private fun HarvestLogRowCard(
    harvestDisplay: CropHarvestDisplay
) {
    val h = harvestDisplay.harvest

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${harvestDisplay.cropName} · ${harvestDisplay.divisionName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                Text(
                    text = "Harvested: ${h.harvestDate}",
                    fontSize = 12.sp,
                    color = Color(0xFF6E7068)
                )

                if (!h.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = h.notes,
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatNumber(h.quantityHarvested)} ${h.quantityUnit}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20)
                )

                if (!h.qualityGrade.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = h.qualityGrade,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
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
private fun RecordYieldDialog(
    activePlantings: List<CropPlantingDisplay>,
    saveState: YieldSaveState,
    onDismiss: () -> Unit,
    onRecord: (
        plantingId: String,
        divisionId: String,
        harvestDate: String,
        qty: Double,
        unit: String,
        grade: String?,
        notes: String?
    ) -> Unit
) {
    var selectedPlanting by remember { mutableStateOf(activePlantings.firstOrNull()) }
    var customCropName by remember { mutableStateOf("") }
    var customSectionName by remember { mutableStateOf("Main Field") }

    var quantityText by remember { mutableStateOf("") }
    var unitText by remember { mutableStateOf("kg") }
    var gradeText by remember { mutableStateOf("Grade A") }
    var dateText by remember {
        val today = try { LocalDate.now().toString() } catch (_: Exception) { "2026-10-27" }
        mutableStateOf(today)
    }
    var notesText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var plantingExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    val validUnits = listOf("kg", "g", "ton", "tonnes", "bags", "crates", "boxes", "pieces")
    val validGrades = listOf("Grade A", "Grade B", "Grade C", "Premium", "Export Grade")

    val isSaving = saveState is YieldSaveState.Saving

    LaunchedEffect(saveState) {
        if (saveState is YieldSaveState.Saved) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Crop Yield / Harvest",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activePlantings.isNotEmpty()) {
                    // SELECT PLANTED CROP / SECTION DROPDOWN
                    ExposedDropdownMenuBox(
                        expanded = plantingExpanded,
                        onExpandedChange = { plantingExpanded = !plantingExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlanting?.let { "${it.cropName} - ${it.divisionName}" } ?: customCropName.ifBlank { "Select or type crop name" },
                            onValueChange = { customCropName = it },
                            readOnly = false,
                            label = { Text("Planted Crop / Section *") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Eco, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plantingExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = plantingExpanded,
                            onDismissRequest = { plantingExpanded = false }
                        ) {
                            activePlantings.forEach { plantingDisplay ->
                                DropdownMenuItem(
                                    text = { Text("${plantingDisplay.cropName} (${plantingDisplay.divisionName})") },
                                    onClick = {
                                        selectedPlanting = plantingDisplay
                                        customCropName = plantingDisplay.cropName
                                        plantingExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // MANUAL CROP NAME & SECTION FIELDS
                    OutlinedTextField(
                        value = customCropName,
                        onValueChange = { customCropName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Crop Name *") },
                        placeholder = { Text("e.g. Maize, Coffee, Tomatoes") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Eco, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customSectionName,
                        onValueChange = { customSectionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Farm Section / Division") },
                        placeholder = { Text("e.g. Division A, Main Field") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Place, contentDescription = null) },
                        singleLine = true
                    )
                }

                // QUANTITY HARVESTED & UNIT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quantityText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Quantity *") },
                        placeholder = { Text("500") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Numbers, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unitText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            validUnits.forEach { uItem ->
                                DropdownMenuItem(
                                    text = { Text(uItem) },
                                    onClick = {
                                        unitText = uItem
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // QUALITY GRADE DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = gradeExpanded,
                    onExpandedChange = { gradeExpanded = !gradeExpanded }
                ) {
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quality Grade") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Eco, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        validGrades.forEach { gItem ->
                            DropdownMenuItem(
                                text = { Text(gItem) },
                                onClick = {
                                    gradeText = gItem
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }

                // HARVEST DATE
                DatePickerField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = "Harvest Date *",
                    modifier = Modifier.fillMaxWidth()
                )

                // NOTES
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Observations") },
                    placeholder = { Text("e.g. Excellent moisture and grain size") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (saveState is YieldSaveState.Error) {
                    Text(
                        text = (saveState as YieldSaveState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                    if (isSaving) return@Button

                    val pDisplay = selectedPlanting
                    val manualCrop = customCropName.trim()
                    val qty = quantityText.toDoubleOrNull()
                    val today = try { LocalDate.now() } catch (_: Exception) { null }
                    val dParsed = if (dateText.isNotBlank()) try { LocalDate.parse(dateText.trim()) } catch (_: Exception) { null } else null

                    when {
                        pDisplay == null && manualCrop.isBlank() -> errorMsg = "Please select or type a crop name."
                        qty == null || qty <= 0.0 -> errorMsg = "Enter a valid harvested quantity."
                        dParsed != null && today != null && dParsed.isBefore(today) -> errorMsg = "Harvest date cannot be in the past."
                        else -> {
                            val pId = pDisplay?.planting?.plantingId ?: UUID.randomUUID().toString()
                            val dId = pDisplay?.planting?.divisionId ?: UUID.randomUUID().toString()
                            val notesWithCropName = if (pDisplay == null && manualCrop.isNotBlank()) {
                                "Crop: $manualCrop (${customSectionName.trim()}) · ${notesText.trim()}".trimEnd(' ', '·')
                            } else notesText.trim().ifBlank { null }

                            onRecord(
                                pId,
                                dId,
                                dateText.trim(),
                                qty,
                                unitText.trim(),
                                gradeText.trim().ifBlank { null },
                                notesWithCropName
                            )
                        }
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Yield")
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
