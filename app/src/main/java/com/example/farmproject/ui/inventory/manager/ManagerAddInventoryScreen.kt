package com.example.farmproject.ui.inventory.manager

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmproject.data.model.InventoryCategory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAddInventoryScreen(
    initialCategory: String = "Fertilizer",
    onBackClick: () -> Unit = {},
    onSaveClick: (
        category: InventoryCategory,
        name: String,
        subtype: String?,
        quantity: Double,
        unit: String,
        reorderLevel: Double?,
        description: String?
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val defaultCategory = InventoryCategory.entries.find {
        it.name.equals(initialCategory, ignoreCase = true) ||
        it.displayName.equals(initialCategory, ignoreCase = true)
    }?.displayName ?: "Fertilizer"

    var categoryText by remember(initialCategory) { mutableStateOf(defaultCategory) }
    var itemName by remember { mutableStateOf("") }

    // Fertilizer specific
    var fertilizerType by remember { mutableStateOf("Inorganic") } // "Inorganic" or "Organic"
    var npkN by remember { mutableStateOf("") }
    var npkP by remember { mutableStateOf("") }
    var npkK by remember { mutableStateOf("") }

    // Herbicide specific
    var herbicideActionType by remember { mutableStateOf("Selective") } // "Selective" or "Non-selective"
    var applicationTiming by remember { mutableStateOf("Post-emergence") }
    var activeIngredient by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf("") }
    var targetWeeds by remember { mutableStateOf("") }
    var applicationRate by remember { mutableStateOf("") }
    var rateUnit by remember { mutableStateOf("L per acre") }
    var phiDays by remember { mutableStateOf("") }

    // Pesticide / Fungicide / Insecticide specific
    var subCategory by remember(initialCategory) {
        mutableStateOf(initialCategory.ifBlank { "Insecticide" })
    }
    var formulation by remember(initialCategory) {
        val defaultFormulation = when {
            initialCategory.equals("Fungicide", ignoreCase = true) -> "Wettable powder (WP)"
            initialCategory.equals("Insecticide", ignoreCase = true) -> "Suspension concentrate (SC)"
            else -> "Emulsifiable concentrate (EC)"
        }
        mutableStateOf(defaultFormulation)
    }
    var targetPests by remember { mutableStateOf("") }
    var reEntryIntervalHours by remember { mutableStateOf("24") }

    // Shared
    var brand by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember(initialCategory) {
        val defaultUnit = if (initialCategory.equals("Fungicide", ignoreCase = true)) "kg" else "litres"
        mutableStateOf(defaultUnit)
    }
    var reorderLevel by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var storageLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }

    // Dropdowns
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var storageExpanded by remember { mutableStateOf(false) }
    var timingExpanded by remember { mutableStateOf(false) }
    var rateUnitExpanded by remember { mutableStateOf(false) }
    var formulationExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Fertilizer",
        "Herbicide",
        "Pesticide",
        "Fungicide",
        "Insecticide",
        "Water"
    )

    val formulationsList = listOf(
        "Wettable powder (WP)",
        "Suspension concentrate (SC)",
        "Emulsifiable concentrate (EC)",
        "Soluble concentrate (SL)",
        "Granules (GR)",
        "Dustable powder (DP)"
    )

    val units = listOf(
        "kg",
        "g",
        "litres",
        "mL",
        "bags",
        "tonnes",
        "units"
    )

    val timings = listOf(
        "Post-emergence",
        "Pre-emergence",
        "Pre-planting",
        "Desiccant"
    )

    val rateUnits = listOf(
        "mL per 20 L",
        "g per 20 L",
        "L per acre",
        "L per hectare",
        "mL per L",
        "kg per acre"
    )

    val stores = listOf(
        "Main Store",
        "Fertilizer Shed",
        "Chemical Store",
        "Cold Storage",
        "General Store"
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF1B5E20),
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = Color(0xFF1B5E20),
        cursorColor = Color(0xFF1B5E20)
    )

    val isFertilizer = categoryText.equals("Fertilizer", ignoreCase = true)
    val isHerbicide = categoryText.equals("Herbicide", ignoreCase = true)
    val isFungicide = categoryText.equals("Fungicide", ignoreCase = true)
    val isInsecticide = categoryText.equals("Insecticide", ignoreCase = true)
    val isPesticideGeneral = categoryText.equals("Pesticide", ignoreCase = true)
    val isPesticideGroup = isPesticideGeneral || isFungicide || isInsecticide

    val titleText = when {
        isFertilizer -> "Add fertilizer"
        isHerbicide -> "Add herbicide"
        isFungicide -> "Add fungicide"
        isInsecticide -> "Add insecticide"
        isPesticideGeneral -> "Add pesticide"
        else -> "Add resource item"
    }

    val subtitleText = when {
        isFertilizer -> "Register a new fertilizer input"
        isHerbicide -> "Register a new herbicide chemical"
        isFungicide -> "Register a new fungicide chemical"
        isInsecticide -> "Register a new insecticide chemical"
        isPesticideGeneral -> "Register a new pesticide chemical"
        else -> "Register a new farm resource input"
    }

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
                        text = titleText,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = subtitleText,
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
            // CATEGORY SELECTOR
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Category *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                categoryText = item
                                categoryExpanded = false
                                if (item == "Fungicide" && unit == "litres") unit = "kg"
                                if ((item == "Herbicide" || item == "Insecticide" || item == "Pesticide") && unit == "kg") unit = "litres"
                            }
                        )
                    }
                }
            }

            // ITEM NAME
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        when {
                            isFertilizer -> "Fertilizer name *"
                            isHerbicide -> "Herbicide name *"
                            isFungicide -> "Fungicide name *"
                            isInsecticide -> "Insecticide name *"
                            isPesticideGeneral -> "Pesticide name *"
                            else -> "Item name *"
                        }
                    )
                },
                placeholder = {
                    Text(
                        when {
                            isFertilizer -> "DAP planting fertilizer"
                            isHerbicide -> "Roundup 360 SL"
                            isFungicide -> "Dithane M-45"
                            isInsecticide -> "Belt 480 SC"
                            isPesticideGeneral -> "Duduthrin 1.75 EC"
                            else -> "e.g. Item Name"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null
                    )
                },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // FERTILIZER FIELDS
            if (isFertilizer) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Type *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = fertilizerType == "Inorganic",
                            onClick = { fertilizerType = "Inorganic" },
                            label = { Text("Inorganic") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = fertilizerType == "Organic",
                            onClick = { fertilizerType = "Organic" },
                            label = { Text("Organic") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // NUTRIENT CONTENT (NPK %)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nutrient content (NPK %)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = npkN,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) npkN = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("N") },
                            placeholder = { Text("18") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = npkP,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) npkP = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("P") },
                            placeholder = { Text("46") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = npkK,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) npkK = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("K") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Brand or manufacturer") },
                    placeholder = { Text("Yara") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // HERBICIDE FIELDS
            if (isHerbicide) {
                // ACTION TYPE * (Selective vs Non-selective)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Action type *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = herbicideActionType == "Selective",
                            onClick = { herbicideActionType = "Selective" },
                            label = { Text("Selective") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = herbicideActionType == "Non-selective",
                            onClick = { herbicideActionType = "Non-selective" },
                            label = { Text("Non-selective") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // APPLICATION TIMING
                ExposedDropdownMenuBox(
                    expanded = timingExpanded,
                    onExpandedChange = { timingExpanded = !timingExpanded }
                ) {
                    OutlinedTextField(
                        value = applicationTiming,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text("Application timing") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = timingExpanded)
                        },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = timingExpanded,
                        onDismissRequest = { timingExpanded = false }
                    ) {
                        timings.forEach { timing ->
                            DropdownMenuItem(
                                text = { Text(timing) },
                                onClick = {
                                    applicationTiming = timing
                                    timingExpanded = false
                                }
                            )
                        }
                    }
                }

                // ACTIVE INGREDIENT & CONCENTRATION (SIDE-BY-SIDE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = activeIngredient,
                        onValueChange = { activeIngredient = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Active ingredient") },
                        placeholder = { Text("Glyphosate") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = concentration,
                        onValueChange = { concentration = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Concentration") },
                        placeholder = { Text("360 g/L") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // TARGET WEEDS
                OutlinedTextField(
                    value = targetWeeds,
                    onValueChange = { targetWeeds = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target weeds") },
                    placeholder = { Text("Couch grass, Oxalis") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                // BRAND OR MANUFACTURER
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Brand or manufacturer") },
                    placeholder = { Text("Bayer") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                // APPLICATION RATE & RATE UNIT (SIDE-BY-SIDE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = applicationRate,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) applicationRate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Application rate") },
                        placeholder = { Text("2.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = rateUnitExpanded,
                        onExpandedChange = { rateUnitExpanded = !rateUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = rateUnit,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            label = { Text("Rate unit") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateUnitExpanded)
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = rateUnitExpanded,
                            onDismissRequest = { rateUnitExpanded = false }
                        ) {
                            rateUnits.forEach { rUnit ->
                                DropdownMenuItem(
                                    text = { Text(rUnit) },
                                    onClick = {
                                        rateUnit = rUnit
                                        rateUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // PRE-HARVEST INTERVAL (DAYS)
                OutlinedTextField(
                    value = phiDays,
                    onValueChange = { if (it.all { c -> c.isDigit() }) phiDays = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pre-harvest interval (days)") },
                    placeholder = { Text("14") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // PESTICIDE / FUNGICIDE / INSECTICIDE FIELDS
            if (isPesticideGroup) {

                // FORMULATION DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = formulationExpanded,
                    onExpandedChange = { formulationExpanded = !formulationExpanded }
                ) {
                    OutlinedTextField(
                        value = formulation,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text("Formulation") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = formulationExpanded)
                        },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = formulationExpanded,
                        onDismissRequest = { formulationExpanded = false }
                    ) {
                        formulationsList.forEach { formItem ->
                            DropdownMenuItem(
                                text = { Text(formItem) },
                                onClick = {
                                    formulation = formItem
                                    formulationExpanded = false
                                }
                            )
                        }
                    }
                }

                // ACTIVE INGREDIENT & CONCENTRATION (SIDE-BY-SIDE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = activeIngredient,
                        onValueChange = { activeIngredient = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Active ingredient") },
                        placeholder = { Text(if (isFungicide) "Mancozeb" else if (isInsecticide) "Flubendiamide" else "Lambda-cyhalothrin") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = concentration,
                        onValueChange = { concentration = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Concentration") },
                        placeholder = { Text(if (isFungicide) "800 g/kg" else if (isInsecticide) "480 g/L" else "17.5 g/L") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // TARGET DISEASES / TARGET INSECTS / TARGET PESTS
                val targetLabel = when {
                    isFungicide -> "Target diseases"
                    isInsecticide -> "Target insects"
                    else -> "Target pests or diseases"
                }
                val targetPlaceholder = when {
                    isFungicide -> "Late blight, downy mildew, rust"
                    isInsecticide -> "Fall armyworm, stalk borers, aphids"
                    else -> "Fall armyworm, aphids, late blight"
                }

                OutlinedTextField(
                    value = targetPests,
                    onValueChange = { targetPests = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(targetLabel) },
                    placeholder = { Text(targetPlaceholder) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                // BRAND OR MANUFACTURER
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Brand or manufacturer") },
                    placeholder = { Text(if (isFungicide) "Syngenta" else "Bayer") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                // APPLICATION RATE & RATE UNIT (SIDE-BY-SIDE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = applicationRate,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) applicationRate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Application rate") },
                        placeholder = { Text(if (isFungicide) "40" else "10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = rateUnitExpanded,
                        onExpandedChange = { rateUnitExpanded = !rateUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = rateUnit,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            label = { Text("Rate unit") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateUnitExpanded)
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = rateUnitExpanded,
                            onDismissRequest = { rateUnitExpanded = false }
                        ) {
                            rateUnits.forEach { rUnit ->
                                DropdownMenuItem(
                                    text = { Text(rUnit) },
                                    onClick = {
                                        rateUnit = rUnit
                                        rateUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // PRE-HARVEST INTERVAL & RE-ENTRY INTERVAL (SIDE-BY-SIDE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phiDays,
                        onValueChange = { if (it.all { c -> c.isDigit() }) phiDays = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Pre-harvest interval (days)") },
                        placeholder = { Text("7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = reEntryIntervalHours,
                        onValueChange = { if (it.all { c -> c.isDigit() }) reEntryIntervalHours = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Re-entry interval (hours)") },
                        placeholder = { Text("24") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            } else if (!isFertilizer && !isHerbicide && categoryText != "Water") {
                // OTHER CATEGORIES
                OutlinedTextField(
                    value = activeIngredient,
                    onValueChange = { activeIngredient = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Active Ingredient / Type") },
                    placeholder = { Text("e.g. Active component") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // QUANTITY * & UNIT * (SIDE-BY-SIDE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quantity = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity *") },
                    placeholder = { Text(if (isFungicide) "5" else "10") },
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

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        label = { Text("Unit *") },
                        placeholder = { Text(if (isFungicide) "kg" else "litres") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    unit = item
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // LOW-STOCK ALERT AT
            OutlinedTextField(
                value = reorderLevel,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) reorderLevel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Low-stock alert at") },
                placeholder = { Text("1") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // SUPPLIER
            OutlinedTextField(
                value = supplier,
                onValueChange = { supplier = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Supplier") },
                placeholder = { Text("Agro-vet name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null
                    )
                },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp)
            )

            // PURCHASE DATE & EXPIRY DATE (SIDE-BY-SIDE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePickerField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    modifier = Modifier.weight(1f),
                    label = "Purchase date",
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                DatePickerField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    modifier = Modifier.weight(1f),
                    label = "Expiry date",
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // STORAGE LOCATION
            ExposedDropdownMenuBox(
                expanded = storageExpanded,
                onExpandedChange = { storageExpanded = !storageExpanded }
            ) {
                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    label = { Text("Storage location") },
                    placeholder = { Text("Select store") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = storageExpanded)
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = storageExpanded,
                    onDismissRequest = { storageExpanded = false }
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store) },
                            onClick = {
                                storageLocation = store
                                storageExpanded = false
                            }
                        )
                    }
                }
            }

            // NOTES / SAFETY NOTES
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                label = { Text(if (isHerbicide || isPesticideGroup) "Safety and handling notes" else "Notes") },
                placeholder = {
                    Text(
                        when {
                            isFungicide -> "Wear gloves and mask. Keep away from food and water."
                            isPesticideGroup || isHerbicide -> "Wear gloves, mask and overalls. Do not spray near water."
                            else -> "Apply at planting, 1 tablespoon per hole"
                        }
                    )
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

            dateError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ACTION BUTTONS (CANCEL & SAVE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        dateError = null

                        val today = try { LocalDate.now() } catch (_: Exception) { null }
                        val pDate = if (purchaseDate.isNotBlank()) try { LocalDate.parse(purchaseDate.trim()) } catch (_: Exception) { null } else null
                        val eDate = if (expiryDate.isNotBlank()) try { LocalDate.parse(expiryDate.trim()) } catch (_: Exception) { null } else null

                        if (pDate != null && today != null && pDate.isBefore(today)) {
                            dateError = "Purchase date cannot be in the past."
                            return@Button
                        }

                        if (eDate != null && today != null && eDate.isBefore(today)) {
                            dateError = "Expiry date cannot be in the past."
                            return@Button
                        }
                        val categoryEnum = InventoryCategory.entries
                            .firstOrNull { it.displayName.equals(categoryText, ignoreCase = true) }
                            ?: InventoryCategory.PESTICIDE

                        val quantityVal = quantity.toDoubleOrNull() ?: 0.0
                        val reorderVal = reorderLevel.toDoubleOrNull()

                        val npkStr = if (npkN.isNotBlank() || npkP.isNotBlank() || npkK.isNotBlank()) {
                            "NPK ${npkN.ifBlank { "0" }}-${npkP.ifBlank { "0" }}-${npkK.ifBlank { "0" }}"
                        } else null

                        val finalSubtype = when {
                            isFertilizer -> if (npkStr != null) "$fertilizerType ($npkStr)" else fertilizerType
                            isHerbicide -> activeIngredient.ifBlank { herbicideActionType }
                            isPesticideGroup -> activeIngredient.ifBlank { subCategory }
                            else -> brand.ifBlank { null }
                        }

                        val detailsList = mutableListOf<String>()
                        if (isPesticideGroup) {
                            if (isPesticideGeneral) detailsList.add("Sub-Category: $subCategory")
                            if (formulation.isNotBlank()) detailsList.add("Formulation: $formulation")
                            if (concentration.isNotBlank()) detailsList.add("Concentration: $concentration")
                            if (targetPests.isNotBlank()) detailsList.add("Target Pests/Diseases: $targetPests")
                            if (applicationRate.isNotBlank()) detailsList.add("App Rate: $applicationRate $rateUnit")
                            if (phiDays.isNotBlank()) detailsList.add("PHI: $phiDays days")
                            if (reEntryIntervalHours.isNotBlank()) detailsList.add("Re-entry: $reEntryIntervalHours hrs")
                        } else if (isHerbicide) {
                            detailsList.add("Action Type: $herbicideActionType")
                            if (applicationTiming.isNotBlank()) detailsList.add("Timing: $applicationTiming")
                            if (concentration.isNotBlank()) detailsList.add("Concentration: $concentration")
                            if (targetWeeds.isNotBlank()) detailsList.add("Target Weeds: $targetWeeds")
                            if (applicationRate.isNotBlank()) detailsList.add("App Rate: $applicationRate $rateUnit")
                            if (phiDays.isNotBlank()) detailsList.add("PHI: $phiDays days")
                        }
                        if (brand.isNotBlank()) detailsList.add("Brand: ${brand.trim()}")
                        if (supplier.isNotBlank()) detailsList.add("Supplier: ${supplier.trim()}")
                        if (purchaseDate.isNotBlank()) detailsList.add("Purchase Date: ${purchaseDate.trim()}")
                        if (expiryDate.isNotBlank()) detailsList.add("Expiry Date: ${expiryDate.trim()}")
                        if (storageLocation.isNotBlank()) detailsList.add("Storage Location: ${storageLocation.trim()}")
                        if (notes.isNotBlank()) detailsList.add("Notes: ${notes.trim()}")

                        val finalDescription = if (detailsList.isNotEmpty()) {
                            detailsList.joinToString(" | ")
                        } else null

                        if (itemName.isNotBlank()) {
                            onSaveClick(
                                categoryEnum,
                                itemName.trim(),
                                finalSubtype,
                                quantityVal,
                                unit.trim().ifBlank { if (isFungicide) "kg" else "litres" },
                                reorderVal,
                                finalDescription
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = when {
                            isFertilizer -> "Save fertilizer"
                            isHerbicide -> "Save herbicide"
                            isFungicide -> "Save fungicide"
                            isInsecticide -> "Save insecticide"
                            isPesticideGeneral -> "Save pesticide"
                            else -> "Save item"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
