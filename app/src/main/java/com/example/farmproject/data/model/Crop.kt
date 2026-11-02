package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Crop(
    @SerialName("crop_id") val cropId: String? = null,
    val name: String,
    @SerialName("scientific_name") val scientificName: String? = null,
    val description: String? = null,
    val type: String? = null,
    @SerialName("farm_id") val farmId: String? = null,
    @SerialName("crop_type_id") val cropTypeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)
