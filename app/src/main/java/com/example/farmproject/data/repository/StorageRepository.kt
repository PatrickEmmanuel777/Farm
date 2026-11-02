package com.example.farmproject.data.repository

import com.example.farmproject.data.model.StorageFacility
import com.example.farmproject.data.model.StoredProduce
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

class StorageRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {
    suspend fun getStorageForFarm(farmId: String): List<StoredProduce> {
        val validFarmId = farmId.ifBlank { CurrentSession.farmId }
        if (validFarmId.isBlank()) return emptyList()

        // 1. Try list_stored_produce RPC
        val rpcResult = runCatching {
            client.postgrest
                .rpc("list_stored_produce", buildJsonObject { put("p_farm_id", validFarmId) })
                .decodeList<StoredProduce>()
        }

        if (rpcResult.isSuccess && rpcResult.getOrThrow().isNotEmpty()) {
            return rpcResult.getOrThrow()
        }

        // 2. Direct Postgrest query fallback
        return try {
            client.from("stored_produce")
                .select {
                    filter {
                        eq("farm_id", validFarmId)
                    }
                    order("date_stored", Order.DESCENDING)
                }
                .decodeList<StoredProduce>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFacilitiesForFarm(farmId: String): List<StorageFacility> {
        val validFarmId = farmId.ifBlank { CurrentSession.farmId }
        if (validFarmId.isBlank()) return emptyList()

        return try {
            client.from("storage_facility")
                .select {
                    filter {
                        eq("farm_id", validFarmId)
                        eq("is_active", true)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<StorageFacility>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createFacility(facility: StorageFacility): Result<StorageFacility> {
        return try {
            client.from("storage_facility").insert(facility)
            Result.success(facility)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStoredProduce(produce: StoredProduce): Result<StoredProduce> {
        val targetFarmId = produce.farmId.ifBlank { CurrentSession.farmId }
        val facilities = getFacilitiesForFarm(targetFarmId)
        val defaultFacilityId = facilities.firstOrNull()?.facilityId

        // Resolve or create facility_id
        val resolvedFacilityId = produce.facilityId?.ifBlank { null }
            ?: defaultFacilityId
            ?: try {
                val newFacId = UUID.randomUUID().toString()
                val fac = StorageFacility(
                    facilityId = newFacId,
                    farmId = targetFarmId,
                    name = produce.safeLocation,
                    facilityType = "store",
                    capacityKg = 10000.0,
                    condition = "good"
                )
                createFacility(fac)
                newFacId
            } catch (_: Exception) {
                UUID.randomUUID().toString()
            }

        val stockId = produce.safeStockId
        val qty = produce.safeQuantityKg
        val cond = when (produce.condition.lowercase().replace(" ", "_")) {
            "spoiled", "poor" -> "spoiled"
            "spoiling", "needs_repair" -> "spoiling"
            "fair" -> "fair"
            else -> "good"
        }

        val todayDate = try { LocalDate.now().toString() } catch (_: Exception) { "2026-10-27" }
        val dateVal = produce.dateStored.ifBlank { todayDate }
        val unitVal = produce.unit?.ifBlank { "kg" } ?: "kg"

        // 1. Try create_stored_produce RPC
        val rpcParams = buildJsonObject {
            put("p_facility_id", resolvedFacilityId)
            put("p_farm_id", targetFarmId)
            put("p_crop_name", produce.cropName.ifBlank { "Produce" })
            put("p_quantity", qty)
            put("p_unit", unitVal)
            put("p_condition", cond)
            put("p_date_stored", dateVal)
            produce.notes?.ifBlank { null }?.let { put("p_notes", it) }
        }

        val rpcResult = runCatching {
            client.postgrest
                .rpc("create_stored_produce", rpcParams)
                .decodeSingle<StoredProduce>()
        }

        if (rpcResult.isSuccess) {
            return Result.success(rpcResult.getOrThrow())
        }

        // 2. Direct Postgrest insert fallback
        val jsonPayload = buildJsonObject {
            put("stock_id", stockId)
            put("facility_id", resolvedFacilityId)
            put("farm_id", targetFarmId)
            put("crop_name", produce.cropName.ifBlank { "Produce" })
            put("quantity_kg", qty)
            put("condition", cond)
            put("date_stored", dateVal)
            produce.notes?.ifBlank { null }?.let { put("notes", it) }
        }

        return try {
            client.from("stored_produce").insert(jsonPayload)
            Result.success(
                produce.copy(
                    stockId = stockId,
                    facilityId = resolvedFacilityId,
                    farmId = targetFarmId,
                    quantityKg = qty,
                    condition = cond,
                    dateStored = dateVal
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to add stored produce."))
        }
    }
}
