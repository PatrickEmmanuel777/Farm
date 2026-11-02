package com.example.farmproject.ui.machinery.manager

import com.example.farmproject.ui.common.DatePickerField


import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAddMachineryScreen(
    viewModel: MachineryViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onSaveClick: (
        name: String,
        machineryType: String,
        model: String?,
        serialNumber: String?,
        purchaseDate: String?,
        condition: String
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        if (saveState is MachinerySaveState.Saved) {
            viewModel.resetSaveState()
            onBackClick()
        }
    }

    var machineryName by remember { mutableStateOf("") }
    var machineryType by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var typeExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }
    var fuelExpanded by remember { mutableStateOf(false) }

    val machineryTypes = listOf(
        "Tractor",
        "Harvester",
        "Planter",
        "Sprayer",
        "Irrigation Equipment",
        "Trailer",
        "Transport Vehicle",
        "Other"
    )

    val conditions = listOf(
        "Excellent",
        "Good",
        "Fair",
        "Needs Maintenance"
    )

    val fuelTypes = listOf(
        "Diesel",
        "Petrol",
        "Electric",
        "Hybrid",
        "Other"
    )

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

                IconButton(
                    onClick = onBackClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Add Machinery",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Register new farm machinery",
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

            // ---------------------------------------------------------
            // BASIC INFORMATION
            // ---------------------------------------------------------

            SectionTitle(
                icon = Icons.Default.Construction,
                title = "Basic Information"
            )

            OutlinedTextField(
                value = machineryName,
                onValueChange = { machineryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Machinery Name") },
                placeholder = { Text("e.g. John Deere 5075E") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null
                    )
                },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = {
                    typeExpanded = !typeExpanded
                }
            ) {

                OutlinedTextField(
                    value = machineryType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Machinery Type") },
                    placeholder = { Text("Select machinery type") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = typeExpanded
                        )
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = {
                        typeExpanded = false
                    }
                ) {

                    machineryTypes.forEach { type ->

                        DropdownMenuItem(
                            text = {
                                Text(type)
                            },
                            onClick = {
                                machineryType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model") },
                placeholder = { Text("e.g. 5075E") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = registrationNumber,
                onValueChange = { registrationNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Registration / Serial Number") },
                placeholder = { Text("e.g. KDF 123A") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // ---------------------------------------------------------
            // PURCHASE INFORMATION
            // ---------------------------------------------------------

            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                title = "Purchase Information"
            )

            DatePickerField(
                value = purchaseDate,
                onValueChange = { purchaseDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Purchase Date",
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // ---------------------------------------------------------
            // MACHINERY STATUS
            // ---------------------------------------------------------

            SectionTitle(
                icon = Icons.Default.CheckCircle,
                title = "Machinery Status"
            )

            ExposedDropdownMenuBox(
                expanded = conditionExpanded,
                onExpandedChange = {
                    conditionExpanded = !conditionExpanded
                }
            ) {

                OutlinedTextField(
                    value = condition,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Current Condition") },
                    placeholder = { Text("Select condition") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = conditionExpanded
                        )
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = conditionExpanded,
                    onDismissRequest = {
                        conditionExpanded = false
                    }
                ) {

                    conditions.forEach { item ->

                        DropdownMenuItem(
                            text = {
                                Text(item)
                            },
                            onClick = {
                                condition = item
                                conditionExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = fuelExpanded,
                onExpandedChange = {
                    fuelExpanded = !fuelExpanded
                }
            ) {

                OutlinedTextField(
                    value = fuelType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Fuel Type") },
                    placeholder = { Text("Select fuel type") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = fuelExpanded
                        )
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = fuelExpanded,
                    onDismissRequest = {
                        fuelExpanded = false
                    }
                ) {

                    fuelTypes.forEach { item ->

                        DropdownMenuItem(
                            text = {
                                Text(item)
                            },
                            onClick = {
                                fuelType = item
                                fuelExpanded = false
                            }
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // NOTES
            // ---------------------------------------------------------

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
                label = { Text("Notes") },
                placeholder = {
                    Text("Enter any additional information...")
                },
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

            Spacer(modifier = Modifier.height(4.dp))

            // ---------------------------------------------------------
            // SAVE BUTTON
            // ---------------------------------------------------------

            Button(
                onClick = {
                    if (machineryName.isNotBlank()) {
                        val finalType = machineryType.ifBlank { "Tractor" }
                        val finalCondition = condition.ifBlank { "Good" }
                        onSaveClick(
                            machineryName.trim(),
                            finalType,
                            model.trim().ifBlank { null },
                            registrationNumber.trim().ifBlank { null },
                            purchaseDate.trim().ifBlank { null },
                            finalCondition
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Save Machinery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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