package com.example.farmproject.ui.settings.manager

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFarmDetailsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    LaunchedEffect(saveState) {
        if (saveState is ProfileSaveState.Saved) {
            viewModel.resetSaveState()
            isEditing = false
            onBackClick()
        }
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
                        text = if (isEditing) "Edit Farm Details" else "Farm Details",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Farm configuration, size, and soil health",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Farm Details",
                        tint = Color.White
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
                is SettingsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SettingsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSettings() }) {
                            Text("Retry")
                        }
                    }
                }

                is SettingsUiState.Success -> {
                    FarmDetailsForm(
                        initialFarmName = state.farmName,
                        initialFarmSize = state.farmSize,
                        initialFarmUnit = state.farmUnit,
                        initialCounty = state.county,
                        initialSoilPh = state.soilPh,
                        initialSoilHealth = state.soilHealth ?: "",
                        isEditing = isEditing,
                        isSaving = saveState is ProfileSaveState.Saving,
                        onEnableEdit = { isEditing = true },
                        onSave = { farmName, farmSize, farmUnit, county, soilPh, soilHealth ->
                            viewModel.saveFarmDetails(
                                farmName = farmName,
                                farmSize = farmSize,
                                farmUnit = farmUnit,
                                county = county,
                                soilPh = soilPh,
                                soilHealth = soilHealth
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmDetailsForm(
    initialFarmName: String,
    initialFarmSize: Double,
    initialFarmUnit: String,
    initialCounty: String,
    initialSoilPh: Double?,
    initialSoilHealth: String,
    isEditing: Boolean,
    isSaving: Boolean,
    onEnableEdit: () -> Unit,
    onSave: (farmName: String, farmSize: Double, farmUnit: String, county: String, soilPh: Double?, soilHealth: String?) -> Unit
) {
    var farmName by remember { mutableStateOf(initialFarmName) }
    var farmSizeText by remember { mutableStateOf(initialFarmSize.toString()) }
    var farmUnit by remember { mutableStateOf(initialFarmUnit) }
    var county by remember { mutableStateOf(initialCounty) }
    var soilPhText by remember { mutableStateOf(initialSoilPh?.toString() ?: "") }
    var soilHealth by remember { mutableStateOf(initialSoilHealth) }

    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("Acres", "Hectares")

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FARM GENERAL INFORMATION
        SectionTitle(
            icon = Icons.Default.Agriculture,
            title = "Farm Information"
        )

        OutlinedTextField(
            value = farmName,
            onValueChange = { farmName = it },
            readOnly = !isEditing,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Farm Name *") },
            leadingIcon = { Icon(imageVector = Icons.Default.Agriculture, contentDescription = null) },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = farmSizeText,
                onValueChange = { farmSizeText = it },
                readOnly = !isEditing,
                modifier = Modifier.weight(1f),
                label = { Text("Total Size") },
                leadingIcon = { Icon(imageVector = Icons.Default.SquareFoot, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = unitExpanded && isEditing,
                onExpandedChange = { if (isEditing) unitExpanded = !unitExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = farmUnit,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Unit") },
                    trailingIcon = {
                        if (isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = unitExpanded && isEditing,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    units.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                farmUnit = option
                                unitExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = county,
            onValueChange = { county = it },
            readOnly = !isEditing,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("County / Region") },
            leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp)
        )

        // SOIL CHARACTERISTICS
        SectionTitle(
            icon = Icons.Default.Spa,
            title = "Soil Analysis & Health"
        )

        OutlinedTextField(
            value = soilPhText,
            onValueChange = { soilPhText = it },
            readOnly = !isEditing,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Soil pH Level") },
            placeholder = { Text("e.g. 6.5") },
            leadingIcon = { Icon(imageVector = Icons.Default.Science, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = soilHealth,
            onValueChange = { soilHealth = it },
            readOnly = !isEditing,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            label = { Text("Soil Health & Organic Status") },
            placeholder = { Text("e.g. Optimal - rich in organic matter and nitrogen") },
            leadingIcon = { Icon(imageVector = Icons.Default.Spa, contentDescription = null) },
            maxLines = 4,
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditing) {
            OutlinedButton(
                onClick = onEnableEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Edit Farm Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = {
                    if (farmName.isNotBlank()) {
                        val parsedSize = farmSizeText.toDoubleOrNull() ?: 1.0
                        val parsedPh = soilPhText.toDoubleOrNull()
                        onSave(
                            farmName.trim(),
                            parsedSize,
                            farmUnit,
                            county.trim(),
                            parsedPh,
                            soilHealth.trim().ifBlank { null }
                        )
                    }
                },
                enabled = !isSaving && farmName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Save Farm Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
            tint = MaterialTheme.colorScheme.primary,
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
