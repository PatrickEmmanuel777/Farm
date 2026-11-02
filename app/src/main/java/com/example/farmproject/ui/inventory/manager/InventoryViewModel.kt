package com.example.farmproject.ui.inventory.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmproject.data.model.Division
import com.example.farmproject.data.model.Fertilizer
import com.example.farmproject.data.model.Fungicide
import com.example.farmproject.data.model.Herbicide
import com.example.farmproject.data.model.Insecticide
import com.example.farmproject.data.model.InventoryCategory
import com.example.farmproject.data.model.InventoryItem
import com.example.farmproject.data.model.Pesticide
import com.example.farmproject.data.model.StockIssuance
import com.example.farmproject.data.model.Water
import com.example.farmproject.data.repository.DivisionRepository
import com.example.farmproject.data.repository.InventoryRepository
import com.example.farmproject.data.session.CurrentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InventoryUiState {
    data object Loading : InventoryUiState
    data class Success(
        val items: List<InventoryItem>,
        val issuances: List<StockIssuance> = emptyList()
    ) : InventoryUiState
    data class Error(val message: String) : InventoryUiState
}

sealed interface InventorySaveState {
    data object Idle : InventorySaveState
    data object Saving : InventorySaveState
    data object Saved : InventorySaveState
    data class Error(val message: String) : InventorySaveState
}

class InventoryViewModel(
    private val repository: InventoryRepository = InventoryRepository(),
    private val divisionRepository: DivisionRepository = DivisionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _divisions = MutableStateFlow<List<Division>>(emptyList())
    val divisions: StateFlow<List<Division>> = _divisions.asStateFlow()

    private val _saveState = MutableStateFlow<InventorySaveState>(InventorySaveState.Idle)
    val saveState: StateFlow<InventorySaveState> = _saveState.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                val farmId = CurrentSession.farmId
                val divisions = runCatching { divisionRepository.getDivisionsForFarm(farmId) }.getOrDefault(emptyList())
                _divisions.value = divisions

                val items = repository.getInventoryForFarm(farmId)
                val issuances = repository.getStockIssuanceForFarm(farmId, divisions, items)
                _uiState.value = InventoryUiState.Success(items = items, issuances = issuances)
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.message ?: "Failed to load inventory")
            }
        }
    }

    fun issueStock(
        category: InventoryCategory,
        itemId: String,
        quantityIssued: Double,
        divisionId: String? = null,
        issuedTo: String? = null,
        issueDate: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _saveState.value = InventorySaveState.Saving
            try {
                repository.issueStockItem(
                    category = category,
                    itemId = itemId,
                    quantityIssued = quantityIssued,
                    divisionId = divisionId,
                    issuedTo = issuedTo,
                    issueDate = issueDate,
                    notes = notes
                )
                _saveState.value = InventorySaveState.Saved
                loadInventory()
            } catch (e: Exception) {
                _saveState.value = InventorySaveState.Error(e.message ?: "Failed to issue stock")
            }
        }
    }

    fun addInventoryItem(
        category: InventoryCategory,
        name: String,
        subtype: String?,
        quantity: Double,
        unit: String,
        reorderLevel: Double?,
        description: String?
    ) {
        viewModelScope.launch {
            _saveState.value = InventorySaveState.Saving
            try {
                val farmId = CurrentSession.farmId

                when (category) {
                    InventoryCategory.FERTILIZER -> repository.insertFertilizer(
                        Fertilizer(
                            farmId = farmId,
                            name = name,
                            fertilizerType = subtype,
                            quantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )

                    InventoryCategory.HERBICIDE -> repository.insertHerbicide(
                        Herbicide(
                            farmId = farmId,
                            name = name,
                            activeIngredient = subtype,
                            quantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )

                    InventoryCategory.PESTICIDE -> repository.insertPesticide(
                        Pesticide(
                            farmId = farmId,
                            name = name,
                            activeIngredient = subtype,
                            quantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )

                    InventoryCategory.FUNGICIDE -> repository.insertFungicide(
                        Fungicide(
                            farmId = farmId,
                            name = name,
                            activeIngredient = subtype,
                            quantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )

                    InventoryCategory.INSECTICIDE -> repository.insertInsecticide(
                        Insecticide(
                            farmId = farmId,
                            name = name,
                            activeIngredient = subtype,
                            quantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )

                    InventoryCategory.WATER -> repository.insertWater(
                        Water(
                            farmId = farmId,
                            sourceName = name,
                            availableQuantity = quantity,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            description = description
                        )
                    )
                }

                _saveState.value = InventorySaveState.Saved
                loadInventory()
            } catch (e: Exception) {
                _saveState.value = InventorySaveState.Error(e.message ?: "Failed to save item")
            }
        }
    }

    fun deleteInventoryItem(category: InventoryCategory, itemId: String) {
        viewModelScope.launch {
            try {
                when (category) {
                    InventoryCategory.FERTILIZER -> repository.deleteFertilizer(itemId)
                    InventoryCategory.HERBICIDE -> repository.deleteHerbicide(itemId)
                    InventoryCategory.PESTICIDE -> repository.deletePesticide(itemId)
                    InventoryCategory.FUNGICIDE -> repository.deleteFungicide(itemId)
                    InventoryCategory.INSECTICIDE -> repository.deleteInsecticide(itemId)
                    InventoryCategory.WATER -> repository.deleteWater(itemId)
                }
                loadInventory()
            } catch (e: Exception) {
                _saveState.value = InventorySaveState.Error(e.message ?: "Failed to delete item")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = InventorySaveState.Idle
    }
}
