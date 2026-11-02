package com.example.farmproject.ui.inventory.manager

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.farmproject.R
import com.example.farmproject.data.model.InventoryCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerInventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onCategoryClick: (InventoryCategory) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val softCreamBg = Color(0xFFF7F6F0)

    Scaffold(
        containerColor = softCreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Custom Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                            text = "Inventory Categories",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1D1A)
                        )
                        Text(
                            text = "Track inputs, issuances & usage by category",
                            fontSize = 13.sp,
                            color = Color(0xFF6E7068)
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFF1A1D1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is InventoryUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1B5E20))
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
                            Button(
                                onClick = { viewModel.loadInventory() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    is InventoryUiState.Success -> {
                        val allItems = state.items
                        val categoriesWithLowStock = InventoryCategory.entries.count { category ->
                            allItems.filter { it.category == category }.any { it.isLowStock }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                InventorySummaryCard(
                                    totalItems = allItems.size,
                                    categoriesWithLowStock = categoriesWithLowStock
                                )
                            }

                            item {
                                Text(
                                    text = "Select Inventory Category",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1D1A)
                                )
                            }

                            items(InventoryCategory.entries.toTypedArray()) { category ->
                                val categoryItems = allItems.filter { it.category == category }
                                val categoryLowStock = categoryItems.count { it.isLowStock }

                                InventoryCategoryCard(
                                    category = category,
                                    itemCount = categoryItems.size,
                                    lowStockCount = categoryLowStock,
                                    onClick = { onCategoryClick(category) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySummaryCard(
    totalItems: Int,
    categoriesWithLowStock: Int
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF277A3C),
            Color(0xFF1B5E20)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = totalItems.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Total inventory stock items",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (categoriesWithLowStock > 0) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFE65100)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (categoriesWithLowStock == 1) "1 category low in stock" else "$categoriesWithLowStock categories low in stock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCategoryCard(
    category: InventoryCategory,
    itemCount: Int,
    lowStockCount: Int,
    onClick: () -> Unit
) {
    val categoryIconRes = when (category) {
        InventoryCategory.FERTILIZER -> R.drawable.ic_cat_fertilizer
        InventoryCategory.HERBICIDE -> R.drawable.ic_cat_herbicide
        InventoryCategory.PESTICIDE -> R.drawable.ic_cat_pesticide
        InventoryCategory.FUNGICIDE -> R.drawable.ic_cat_pesticide
        InventoryCategory.INSECTICIDE -> R.drawable.ic_cat_pesticide
        InventoryCategory.WATER -> R.drawable.ic_cat_water
    }

    val categoryTitle = when (category) {
        InventoryCategory.FERTILIZER -> "Fertilizers"
        InventoryCategory.HERBICIDE -> "Herbicides"
        InventoryCategory.PESTICIDE -> "Pesticides"
        InventoryCategory.FUNGICIDE -> "Fungicides"
        InventoryCategory.INSECTICIDE -> "Insecticides"
        InventoryCategory.WATER -> "Water"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = categoryIconRes),
                contentDescription = categoryTitle,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categoryTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1D1A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subtitleText = if (lowStockCount > 0) {
                    "$itemCount item · $lowStockCount low in stock"
                } else {
                    "$itemCount item${if (itemCount == 1) "" else "s"} available"
                }

                Text(
                    text = subtitleText,
                    fontSize = 13.sp,
                    color = if (lowStockCount > 0) Color(0xFFC62828) else Color(0xFF6E7068),
                    fontWeight = if (lowStockCount > 0) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Right Badge Pill
            Box(
                modifier = Modifier
                    .background(
                        color = if (lowStockCount > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (lowStockCount > 0) "$lowStockCount low" else itemCount.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (lowStockCount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
