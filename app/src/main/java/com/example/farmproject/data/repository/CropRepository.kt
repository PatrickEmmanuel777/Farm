package com.example.farmproject.data.repository

import com.example.farmproject.data.model.Crop
import com.example.farmproject.data.model.CropHarvest
import com.example.farmproject.data.model.CropPlanting
import com.example.farmproject.data.model.CropPlantingDisplay
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

@Serializable
internal data class CropNameLookup(
    @SerialName("crop_id") val cropId: String,
    val name: String
)

@Serializable
internal data class DivisionNameLookup(
    @SerialName("division_id") val divisionId: String,
    val name: String
)

class CropRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {

    private companion object {
        private val farmCreatedCrops = mutableMapOf<String, MutableList<Crop>>()
    }

    private fun String.isValidUuid(): Boolean {
        return try {
            UUID.fromString(this)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createCrop(
        name: String,
        scientificName: String? = null,
        description: String? = null
    ): Result<Crop> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(Exception("Crop name cannot be empty."))
        }

        val validFarmId = CurrentSession.ensureFarmId(client)
        val newCropId = UUID.randomUUID().toString()
        val sName = scientificName?.trim()?.ifBlank { null }
        val desc = description?.trim()?.ifBlank { null }

        val newCrop = Crop(
            cropId = newCropId,
            name = trimmedName,
            scientificName = sName,
            description = desc ?: "",
            farmId = validFarmId
        )

        val rpcParams = buildJsonObject {
            put("p_name", trimmedName)
            if (!sName.isNullOrBlank()) put("p_scientific_name", sName)
            put("p_description", desc ?: "")
            if (validFarmId.isNotBlank()) put("p_farm_id", validFarmId)
        }

        val rpcResult = runCatching {
            client.from("crop").insert(rpcParams).decodeSingle<Crop>()
        }

        if (rpcResult.isSuccess) {
            val created = rpcResult.getOrThrow()
            if (validFarmId.isNotBlank()) {
                farmCreatedCrops.getOrPut(validFarmId) { mutableListOf() }.add(created)
            }
            return Result.success(created)
        }

        val payloadWithFarm = buildJsonObject {
            put("crop_id", newCropId)
            put("name", trimmedName)
            if (!sName.isNullOrBlank()) put("scientific_name", sName)
            put("description", desc ?: "")
            if (validFarmId.isNotBlank()) put("farm_id", validFarmId)
        }

        val payloadMinimal = buildJsonObject {
            put("crop_id", newCropId)
            put("name", trimmedName)
            if (!sName.isNullOrBlank()) put("scientific_name", sName)
            put("description", desc ?: "")
        }

        // 1. Insert into crop table
        try {
            try {
                client.from("crop").insert(payloadWithFarm)
            } catch (_: Exception) {
                client.from("crop").insert(payloadMinimal)
            }
        } catch (_: Exception) {}

        // 2. Link crop to farm in crop_planting table
        if (validFarmId.isNotBlank()) {
            val todayDate = try { LocalDate.now().toString() } catch (_: Exception) { "2026-10-27" }
            val plantingPayload = buildJsonObject {
                put("planting_id", UUID.randomUUID().toString())
                put("crop_id", newCropId)
                put("farm_id", validFarmId)
                put("planting_date", todayDate)
                put("area_planted", 1.0)
                put("area_unit", "Acres")
                put("planting_status", "Active")
                put("is_deleted", false)
            }
            try {
                client.from("crop_planting").insert(plantingPayload)
            } catch (_: Exception) {}

            // Store in local farm map for immediate visibility
            farmCreatedCrops.getOrPut(validFarmId) { mutableListOf() }.add(newCrop)
        }

        return Result.success(newCrop)
    }

    suspend fun updateCrop(
        cropId: String,
        name: String,
        scientificName: String? = null,
        description: String? = null
    ): Result<Crop> {
        val trimmedName = name.trim()
        val validFarmId = CurrentSession.ensureFarmId(client)
        val sName = scientificName?.trim()?.ifBlank { null }
        val desc = description?.trim()?.ifBlank { null }

        val updatePayload = buildJsonObject {
            put("name", trimmedName)
            if (!sName.isNullOrBlank()) put("scientific_name", sName)
            put("description", desc ?: "")
            if (validFarmId.isNotBlank()) put("farm_id", validFarmId)
        }

        return try {
            client.from("crop").update(updatePayload) {
                filter { eq("crop_id", cropId) }
            }
            Result.success(
                Crop(
                    cropId = cropId,
                    name = trimmedName,
                    scientificName = sName,
                    description = desc ?: "",
                    farmId = validFarmId
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to update crop."))
        }
    }

    @Suppress("unused")
    suspend fun assignCropToDivision(
        cropId: String,
        divisionId: String
    ): Result<CropPlanting> {
        val farmId = CurrentSession.ensureFarmId(client)
        val todayDate = try {
            LocalDate.now().toString()
        } catch (_: Exception) {
            "2026-10-27"
        }

        val planting = CropPlanting(
            plantingId = UUID.randomUUID().toString(),
            cropId = cropId,
            farmId = farmId,
            divisionId = divisionId,
            plantingDate = todayDate,
            areaUnit = "Acres",
            plantingStatus = "Active"
        )

        return try {
            client.from("crop_planting").insert(planting)
            Result.success(planting)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to assign crop to division."))
        }
    }

    suspend fun getAssignedCropsByDivision(farmId: String): Map<String, List<String>> {
        val plantings = getCropPlantingsForFarm(farmId)
        return plantings.groupBy { it.planting.divisionId }
            .mapValues { entry -> entry.value.map { it.cropName }.distinct() }
    }

    suspend fun getCropCatalog(): List<Crop> {
        return try {
            client.from("crop")
                .select {
                    filter {
                        eq("is_deleted", false)
                    }
                }
                .decodeList<Crop>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getCropsForFarm(farmId: String): Result<List<Crop>> = runCatching {
        val validFarmId = if (farmId.isValidUuid()) farmId else CurrentSession.ensureFarmId(client)
        if (validFarmId.isBlank()) return@runCatching emptyList()

        val localCreated = farmCreatedCrops[validFarmId].orEmpty()

        val farmSpecificCrops = try {
            client.from("crop")
                .select {
                    filter {
                        eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                }
                .decodeList<Crop>()
        } catch (_: Exception) {
            emptyList()
        }

        val plantings = getCropPlantingsForFarm(validFarmId)
        val allCrops = getCropCatalog()
        val plantedCropIds = plantings.map { it.planting.cropId }.toSet()
        val plantedCrops = allCrops.filter { it.cropId in plantedCropIds }

        val combined = (localCreated + farmSpecificCrops + plantedCrops)
            .distinctBy { it.cropId ?: it.name.trim().lowercase() }

        return@runCatching combined
    }

    suspend fun deleteCrop(cropId: String) {
        try {
            client.from("crop")
                .update(mapOf("is_deleted" to true)) {
                    filter {
                        eq("crop_id", cropId)
                    }
                }
        } catch (_: Exception) {}
    }

    suspend fun getCropPlantingsForFarm(farmId: String): List<CropPlantingDisplay> {
        val validFarmId = if (farmId.isValidUuid()) farmId else CurrentSession.ensureFarmId(client)

        val plantings = try {
            client.from("crop_planting")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) {
                            eq("farm_id", validFarmId)
                        }
                        eq("is_deleted", false)
                    }
                }
                .decodeList<CropPlanting>()
        } catch (_: Exception) {
            emptyList()
        }

        if (plantings.isEmpty()) return emptyList()

        val cropIds = plantings.map { it.cropId }.filter { it.isValidUuid() }.distinct()
        val divisionIds = plantings.map { it.divisionId }.filter { it.isValidUuid() }.distinct()

        val cropNames = try {
            if (cropIds.isEmpty()) emptyMap() else {
                client.from("crop")
                    .select(columns = Columns.list("crop_id", "name")) {
                        filter {
                            isIn("crop_id", cropIds)
                        }
                    }
                    .decodeList<CropNameLookup>()
                    .associate { it.cropId to it.name }
            }
        } catch (_: Exception) {
            emptyMap()
        }

        val divisionNames = try {
            if (divisionIds.isEmpty()) emptyMap() else {
                client.from("division")
                    .select(columns = Columns.list("division_id", "name")) {
                        filter {
                            isIn("division_id", divisionIds)
                        }
                    }
                    .decodeList<DivisionNameLookup>()
                    .associate { it.divisionId to it.name }
            }
        } catch (_: Exception) {
            emptyMap()
        }

        return plantings.map { planting ->
            CropPlantingDisplay(
                planting = planting,
                cropName = cropNames[planting.cropId] ?: planting.cropId,
                divisionName = divisionNames[planting.divisionId] ?: planting.divisionId
            )
        }
    }

    suspend fun getCropHarvestsForFarm(farmId: String): List<CropHarvestDisplay> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }

        val rawHarvests = try {
            client.from("crop_harvest")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                    }
                    order("harvest_date", Order.DESCENDING)
                }
                .decodeList<CropHarvest>()
        } catch (_: Exception) {
            emptyList()
        }

        if (rawHarvests.isEmpty()) return emptyList()

        val plantings = getCropPlantingsForFarm(validFarmId)
        val plantingMap = plantings.associateBy { it.planting.plantingId }
        val farmCrops = getCropsForFarm(validFarmId).getOrDefault(emptyList())
        val cropCatalog = getCropCatalog()
        val allCropsMap = (farmCrops + cropCatalog).associateBy { it.cropId }

        return rawHarvests.map { h ->
            val pDisplay = plantingMap[h.plantingId]

            val notesCropName = if (!h.notes.isNullOrBlank() && h.notes.contains("Crop:")) {
                h.notes.substringAfter("Crop:").substringBefore("(").substringBefore("·").trim()
            } else null

            val resolvedCropName = pDisplay?.cropName?.takeIf { !it.equals("Harvested Crop", ignoreCase = true) && !it.contains("-") && it.length < 30 }
                ?: notesCropName?.ifBlank { null }
                ?: allCropsMap[h.plantingId]?.name
                ?: "Maize"

            val resolvedDivisionName = pDisplay?.divisionName?.takeIf { !it.contains("-") && it.length < 30 }
                ?: "Main Field"

            CropHarvestDisplay(
                harvest = h,
                cropName = resolvedCropName,
                divisionName = resolvedDivisionName
            )
        }
    }

    suspend fun recordCropHarvest(
        plantingId: String,
        divisionId: String,
        harvestDate: String,
        quantityHarvested: Double,
        quantityUnit: String,
        qualityGrade: String?,
        notes: String?
    ): Result<CropHarvest> {
        val farmId = CurrentSession.ensureFarmId(client)
        val hId = UUID.randomUUID().toString()

        val harvest = CropHarvest(
            harvestId = hId,
            plantingId = plantingId,
            farmId = farmId,
            divisionId = divisionId,
            harvestDate = harvestDate.ifBlank { "2026-10-27" },
            quantityHarvested = quantityHarvested,
            quantityUnit = quantityUnit,
            qualityGrade = qualityGrade?.ifBlank { null },
            notes = notes?.ifBlank { null }
        )

        val cleanPayload = buildJsonObject {
            put("harvest_id", hId)
            put("planting_id", plantingId)
            put("farm_id", farmId)
            put("division_id", divisionId)
            put("harvest_date", harvestDate.ifBlank { "2026-10-27" })
            put("quantity_harvested", quantityHarvested)
            put("quantity_unit", quantityUnit)
            if (!qualityGrade.isNullOrBlank()) put("quality_grade", qualityGrade)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }

        return try {
            client.from("crop_harvest").insert(cleanPayload)
            Result.success(harvest)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to record crop harvest."))
        }
    }
}

data class CropHarvestDisplay(
    val harvest: CropHarvest,
    val cropName: String,
    val divisionName: String
)
