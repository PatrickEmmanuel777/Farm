package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CropPlanting(
    @SerialName("planting_id") val plantingId: String? = null,
    @SerialName("crop_id") val cropId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("worker_profile_id") val workerProfileId: String? = null,
    @SerialName("supervisor_profile_id") val supervisorProfileId: String? = null,
    @SerialName("manager_profile_id") val managerProfileId: String? = null,
    @SerialName("planting_date") val plantingDate: String,
    @SerialName("planting_time") val plantingTime: String? = null,
    @SerialName("quantity_planted") val quantityPlanted: Double? = null,
    @SerialName("quantity_unit") val quantityUnit: String? = null,
    @SerialName("area_planted") val areaPlanted: Double? = null,
    @SerialName("area_unit") val areaUnit: String,
    @SerialName("number_of_seeds") val numberOfSeeds: Int? = null,
    val notes: String? = null,
    @SerialName("expected_first_harvest_date") val expectedFirstHarvestDate: String? = null,
    @SerialName("planting_status") val plantingStatus: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)
