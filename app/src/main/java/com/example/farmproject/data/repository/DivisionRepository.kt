package com.example.farmproject.data.repository

import com.example.farmproject.data.model.Division
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class DivisionRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {

    suspend fun getDivisionsForFarm(farmId: String): List<Division> {
        val targetFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }

        return try {
            client.from("division")
                .select {
                    filter {
                        eq("farm_id", targetFarmId)
                        eq("is_deleted", false)
                    }
                }
                .decodeList<Division>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createDivision(
        name: String,
        size: Double,
        units: String,
        soilPh: Double? = null,
        soilHealth: String? = null
    ): Result<Division> {
        val farmId = CurrentSession.ensureFarmId(client)
        val newDivisionId = UUID.randomUUID().toString()

        val newDivision = Division(
            divisionId = newDivisionId,
            farmId = farmId,
            name = name.trim(),
            size = size,
            units = units.ifBlank { "Acres" },
            soilPh = soilPh,
            soilHealth = soilHealth?.trim()?.ifBlank { null }
        )

        return try {
            // 1. Try create_division RPC procedure
            var created: Division? = null
            try {
                client.postgrest.rpc(
                    function = "create_division",
                    parameters = buildJsonObject {
                        put("p_farm_id", farmId)
                        put("p_name", name.trim())
                        put("p_size", size)
                        put("p_units", units)
                        if (soilPh != null) put("p_soil_ph", soilPh)
                        if (!soilHealth.isNullOrBlank()) put("p_soil_health", soilHealth.trim())
                    }
                )
                created = getDivisionsForFarm(farmId).find { it.name.equals(name.trim(), ignoreCase = true) }
            } catch (_: Exception) {}

            // 2. Direct insert fallback
            if (created == null) {
                created = client.from("division")
                    .insert(newDivision) {
                        select()
                    }
                    .decodeSingle<Division>()
            }

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to create division. Please try again."))
        }
    }

    suspend fun deleteDivision(divisionId: String) {
        try {
            client.from("division")
                .update(mapOf("is_deleted" to true)) {
                    filter {
                        eq("division_id", divisionId)
                    }
                }
        } catch (_: Exception) {}
    }
}
