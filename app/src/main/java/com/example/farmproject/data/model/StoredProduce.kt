package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StoredProduce(
    @SerialName("stock_id") val stockId: String? = null,
    @SerialName("facility_id") val facilityId: String? = null,
    @SerialName("farm_id") val farmId: String,
    @SerialName("crop_name") val cropName: String,
    @SerialName("quantity_kg") val quantityKg: Double = 0.0,
    val condition: String = "good",
    @SerialName("date_stored") val dateStored: String,
    @SerialName("shelf_life_days") val shelfLifeDays: Int? = null,
    @SerialName("removed_on") val removedOn: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Backward-compatibility getters for UI
    @SerialName("storage_id") val storageId: String? = null,
    val location: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    @SerialName("needs_attention") val needsAttention: Boolean = false
) {
    val safeStockId: String get() = stockId ?: storageId ?: UUID.randomUUID().toString()
    val safeQuantityKg: Double get() = if (quantityKg > 0) quantityKg else quantity ?: 0.0
    val safeUnit: String get() = unit.orEmpty().ifBlank { "kg" }
    val safeLocation: String get() = location.orEmpty().ifBlank { "Main Facility" }
    val safeCondition: String get() = when (condition.lowercase()) {
        "spoiling" -> "Spoiling"
        "spoiled" -> "Spoiled"
        "fair" -> "Fair"
        else -> "Good"
    }
}
