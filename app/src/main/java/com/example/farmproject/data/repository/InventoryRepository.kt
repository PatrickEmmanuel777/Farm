@file:Suppress("unused")

package com.example.farmproject.data.repository

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
import com.example.farmproject.data.model.toInventoryItem
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

class InventoryRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {

    /**
     * Every active item across all six category tables for one farm.
     */
    suspend fun getInventoryForFarm(farmId: String): List<InventoryItem> = coroutineScope {
        val fertilizers = async { getFertilizers(farmId) }
        val herbicides = async { getHerbicides(farmId) }
        val pesticides = async { getPesticides(farmId) }
        val fungicides = async { getFungicides(farmId) }
        val insecticides = async { getInsecticides(farmId) }
        val water = async { getWater(farmId) }

        fertilizers.await().map { it.toInventoryItem() } +
                herbicides.await().map { it.toInventoryItem() } +
                pesticides.await().map { it.toInventoryItem() } +
                fungicides.await().map { it.toInventoryItem() } +
                insecticides.await().map { it.toInventoryItem() } +
                water.await().map { it.toInventoryItem() }
    }

    // ---------------------------------------------------------------
    // Per-category reads
    // ---------------------------------------------------------------

    suspend fun getFertilizers(farmId: String): List<Fertilizer> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("fertilizers")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Fertilizer>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getHerbicides(farmId: String): List<Herbicide> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("herbicide")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Herbicide>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getPesticides(farmId: String): List<Pesticide> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("pesticide")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Pesticide>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFungicides(farmId: String): List<Fungicide> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("fungicide")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Fungicide>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getInsecticides(farmId: String): List<Insecticide> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("insecticide")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Insecticide>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getWater(farmId: String): List<Water> {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("water")
                .select {
                    filter {
                        if (validFarmId.isNotBlank()) eq("farm_id", validFarmId)
                        eq("is_deleted", false)
                    }
                    order("source_name", Order.ASCENDING)
                }
                .decodeList<Water>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------
    // Inserts
    // ---------------------------------------------------------------

    suspend fun insertFertilizer(item: Fertilizer): Fertilizer {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            fertilizerId = item.fertilizerId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("fertilizers").insert(clean) { select() }.decodeSingle<Fertilizer>()
        } catch (_: Exception) {
            client.from("fertilizers").insert(clean)
            clean
        }
    }

    suspend fun insertHerbicide(item: Herbicide): Herbicide {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            herbicideId = item.herbicideId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("herbicide").insert(clean) { select() }.decodeSingle<Herbicide>()
        } catch (_: Exception) {
            client.from("herbicide").insert(clean)
            clean
        }
    }

    suspend fun insertPesticide(item: Pesticide): Pesticide {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            pesticideId = item.pesticideId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("pesticide").insert(clean) { select() }.decodeSingle<Pesticide>()
        } catch (_: Exception) {
            client.from("pesticide").insert(clean)
            clean
        }
    }

    suspend fun insertFungicide(item: Fungicide): Fungicide {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            fungicideId = item.fungicideId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("fungicide").insert(clean) { select() }.decodeSingle<Fungicide>()
        } catch (_: Exception) {
            client.from("fungicide").insert(clean)
            clean
        }
    }

    suspend fun insertInsecticide(item: Insecticide): Insecticide {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            insecticideId = item.insecticideId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("insecticide").insert(clean) { select() }.decodeSingle<Insecticide>()
        } catch (_: Exception) {
            client.from("insecticide").insert(clean)
            clean
        }
    }

    suspend fun insertWater(item: Water): Water {
        val farmId = item.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val clean = item.copy(
            waterId = item.waterId ?: UUID.randomUUID().toString(),
            farmId = farmId
        )
        return try {
            client.from("water").insert(clean) { select() }.decodeSingle<Water>()
        } catch (_: Exception) {
            client.from("water").insert(clean)
            clean
        }
    }

    // ---------------------------------------------------------------
    // Deletes (Soft delete via is_deleted = true)
    // ---------------------------------------------------------------

    suspend fun deleteFertilizer(id: String) = softDelete("fertilizers", "fertilizer_id", id)
    suspend fun deleteHerbicide(id: String) = softDelete("herbicide", "herbicide_id", id)
    suspend fun deletePesticide(id: String) = softDelete("pesticide", "pesticide_id", id)
    suspend fun deleteFungicide(id: String) = softDelete("fungicide", "fungicide_id", id)
    suspend fun deleteInsecticide(id: String) = softDelete("insecticide", "insecticide_id", id)
    suspend fun deleteWater(id: String) = softDelete("water", "water_id", id)

    private suspend fun softDelete(table: String, idColumn: String, idValue: String) {
        try {
            client.from(table)
                .update(mapOf("is_deleted" to true)) {
                    filter {
                        eq(idColumn, idValue)
                    }
                }
        } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------
    // Stock Issuance
    // ---------------------------------------------------------------

    suspend fun issueStockItem(
        category: InventoryCategory,
        itemId: String,
        quantityIssued: Double,
        divisionId: String? = null,
        issuedTo: String? = null,
        issueDate: String,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val farmId = CurrentSession.ensureFarmId(client)
            val authUserId = client.auth.currentUserOrNull()?.id ?: CurrentSession.userId ?: ""
            val issuedBy = authUserId.ifBlank { UUID.randomUUID().toString() }

            // 1. Fetch current item details
            val items = getInventoryForFarm(farmId)
            val item = items.find { it.id == itemId }
            val unit = item?.unit ?: "units"

            // 2. Update stock quantity in category table
            if (item != null) {
                val newQuantity = (item.quantity - quantityIssued).coerceAtLeast(0.0)
                try {
                    when (category) {
                        InventoryCategory.FERTILIZER -> client.from("fertilizers").update(mapOf("quantity" to newQuantity)) { filter { eq("fertilizer_id", itemId) } }
                        InventoryCategory.HERBICIDE -> client.from("herbicide").update(mapOf("quantity" to newQuantity)) { filter { eq("herbicide_id", itemId) } }
                        InventoryCategory.PESTICIDE -> client.from("pesticide").update(mapOf("quantity" to newQuantity)) { filter { eq("pesticide_id", itemId) } }
                        InventoryCategory.FUNGICIDE -> client.from("fungicide").update(mapOf("quantity" to newQuantity)) { filter { eq("fungicide_id", itemId) } }
                        InventoryCategory.INSECTICIDE -> client.from("insecticide").update(mapOf("quantity" to newQuantity)) { filter { eq("insecticide_id", itemId) } }
                        InventoryCategory.WATER -> client.from("water").update(mapOf("available_quantity" to newQuantity)) { filter { eq("water_id", itemId) } }
                    }
                } catch (_: Exception) {}
            }

            // 3. Insert record into exact category issuance table
            val dateStr = issueDate.trim().ifBlank {
                try { LocalDate.now().toString() } catch (_: Exception) { "2026-03-30" }
            }

            val validDivisionId = divisionId?.takeIf { it.isNotBlank() }
            val combinedNotes = listOfNotNull(notes?.ifBlank { null }, issuedTo?.takeIf { it.isNotBlank() }?.let { "Recipient: $it" }).joinToString(" | ")

            val payload = buildJsonObject {
                when (category) {
                    InventoryCategory.FERTILIZER -> put("fertilizer_id", itemId)
                    InventoryCategory.HERBICIDE -> put("herbicide_id", itemId)
                    InventoryCategory.PESTICIDE -> put("pesticide_id", itemId)
                    InventoryCategory.FUNGICIDE -> put("fungicide_id", itemId)
                    InventoryCategory.INSECTICIDE -> put("insecticide_id", itemId)
                    InventoryCategory.WATER -> put("water_id", itemId)
                }
                put("farm_id", farmId)
                if (validDivisionId != null) put("division_id", validDivisionId)
                if (issuedBy.isNotBlank()) put("issued_by", issuedBy)
                put("quantity_issued", quantityIssued)
                put("unit", unit)
                put("issuance_date", dateStr)
                put("status", "Issued")
                if (combinedNotes.isNotBlank()) put("notes", combinedNotes)
            }

            val issuanceTable = when (category) {
                InventoryCategory.FERTILIZER -> "fertilizer_issuance"
                InventoryCategory.HERBICIDE -> "herbicide_issuance"
                InventoryCategory.PESTICIDE -> "pesticide_issuance"
                InventoryCategory.FUNGICIDE -> "fungicide_issuance"
                InventoryCategory.INSECTICIDE -> "insecticide_issuance"
                InventoryCategory.WATER -> "water_issuance"
            }

            try {
                client.from(issuanceTable).insert(payload)
            } catch (_: Exception) {
                // Fallback attempt without division_id if division foreign key is not present
                val payloadNoDiv = buildJsonObject {
                    when (category) {
                        InventoryCategory.FERTILIZER -> put("fertilizer_id", itemId)
                        InventoryCategory.HERBICIDE -> put("herbicide_id", itemId)
                        InventoryCategory.PESTICIDE -> put("pesticide_id", itemId)
                        InventoryCategory.FUNGICIDE -> put("fungicide_id", itemId)
                        InventoryCategory.INSECTICIDE -> put("insecticide_id", itemId)
                        InventoryCategory.WATER -> put("water_id", itemId)
                    }
                    put("farm_id", farmId)
                    if (issuedBy.isNotBlank()) put("issued_by", issuedBy)
                    put("quantity_issued", quantityIssued)
                    put("unit", unit)
                    put("issuance_date", dateStr)
                    put("status", "Issued")
                    if (combinedNotes.isNotBlank()) put("notes", combinedNotes)
                }
                client.from(issuanceTable).insert(payloadNoDiv)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchIssuanceFromTable(
        tableName: String,
        category: InventoryCategory,
        farmId: String,
        divisions: List<Division>,
        items: List<InventoryItem>
    ): List<StockIssuance> {
        val pkColumn = when (category) {
            InventoryCategory.FERTILIZER -> "fertilizer_issuance_id"
            InventoryCategory.HERBICIDE -> "herbicide_issuance_id"
            InventoryCategory.PESTICIDE -> "pesticide_issuance_id"
            InventoryCategory.FUNGICIDE -> "fungicide_issuance_id"
            InventoryCategory.INSECTICIDE -> "insecticide_issuance_id"
            InventoryCategory.WATER -> "water_issuance_id"
        }
        val productFkColumn = when (category) {
            InventoryCategory.FERTILIZER -> "fertilizer_id"
            InventoryCategory.HERBICIDE -> "herbicide_id"
            InventoryCategory.PESTICIDE -> "pesticide_id"
            InventoryCategory.FUNGICIDE -> "fungicide_id"
            InventoryCategory.INSECTICIDE -> "insecticide_id"
            InventoryCategory.WATER -> "water_id"
        }

        return try {
            val rawList = try {
                client.from(tableName)
                    .select {
                        filter {
                            if (farmId.isNotBlank()) eq("farm_id", farmId)
                        }
                    }
                    .decodeList<JsonObject>()
            } catch (_: Exception) {
                client.from(tableName).select().decodeList<JsonObject>()
            }

            rawList.mapNotNull { json ->
                try {
                    val issuanceId = json[pkColumn]?.jsonPrimitive?.contentOrNull
                        ?: json["issuance_id"]?.jsonPrimitive?.contentOrNull
                        ?: json["id"]?.jsonPrimitive?.contentOrNull
                        ?: UUID.randomUUID().toString()

                    val itemId = json[productFkColumn]?.jsonPrimitive?.contentOrNull
                        ?: json["item_id"]?.jsonPrimitive?.contentOrNull
                        ?: ""

                    val itemObj = items.find { it.id == itemId }
                    val itemName = itemObj?.name
                        ?: json["item_name"]?.jsonPrimitive?.contentOrNull
                        ?: json["name"]?.jsonPrimitive?.contentOrNull
                        ?: "${category.displayName} Item"

                    val qty = json["quantity_issued"]?.jsonPrimitive?.doubleOrNull
                        ?: json["quantity"]?.jsonPrimitive?.doubleOrNull
                        ?: 0.0

                    val unit = json["unit"]?.jsonPrimitive?.contentOrNull ?: itemObj?.unit ?: "units"
                    val divisionId = json["division_id"]?.jsonPrimitive?.contentOrNull
                    val divObj = divisions.find { it.divisionId == divisionId }
                    val divisionName = divObj?.name

                    val issuedBy = json["issued_by"]?.jsonPrimitive?.contentOrNull
                    val issueDate = json["issuance_date"]?.jsonPrimitive?.contentOrNull
                        ?: json["issue_date"]?.jsonPrimitive?.contentOrNull
                        ?: "2026-03-30"

                    val notes = json["notes"]?.jsonPrimitive?.contentOrNull
                    val status = json["status"]?.jsonPrimitive?.contentOrNull ?: "Issued"

                    val recipientFromNotes = notes?.let {
                        if (it.contains("Recipient: ")) it.substringAfter("Recipient: ").substringBefore(" |") else null
                    }
                    val issuedTo = divisionName ?: recipientFromNotes ?: "General Field"

                    StockIssuance(
                        issuanceId = issuanceId,
                        itemId = itemId,
                        category = category.name,
                        itemName = itemName,
                        quantityIssued = qty,
                        unit = unit,
                        issuedTo = issuedTo,
                        issuedBy = issuedBy,
                        divisionId = divisionId,
                        divisionName = divisionName,
                        issueDate = issueDate,
                        notes = notes,
                        farmId = farmId,
                        status = status
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getStockIssuanceForFarm(
        farmId: String,
        divisions: List<Division> = emptyList(),
        items: List<InventoryItem> = emptyList()
    ): List<StockIssuance> = coroutineScope {
        val validFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }

        val fertList = async { fetchIssuanceFromTable("fertilizer_issuance", InventoryCategory.FERTILIZER, validFarmId, divisions, items) }
        val herbList = async { fetchIssuanceFromTable("herbicide_issuance", InventoryCategory.HERBICIDE, validFarmId, divisions, items) }
        val pestList = async { fetchIssuanceFromTable("pesticide_issuance", InventoryCategory.PESTICIDE, validFarmId, divisions, items) }
        val fungList = async { fetchIssuanceFromTable("fungicide_issuance", InventoryCategory.FUNGICIDE, validFarmId, divisions, items) }
        val insecList = async { fetchIssuanceFromTable("insecticide_issuance", InventoryCategory.INSECTICIDE, validFarmId, divisions, items) }
        val waterList = async { fetchIssuanceFromTable("water_issuance", InventoryCategory.WATER, validFarmId, divisions, items) }

        val combined = fertList.await() + herbList.await() + pestList.await() +
                fungList.await() + insecList.await() + waterList.await()

        combined.distinctBy { it.safeIssuanceId }
    }
}
