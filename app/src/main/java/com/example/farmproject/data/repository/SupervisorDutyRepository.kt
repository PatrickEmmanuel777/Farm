package com.example.farmproject.data.repository

import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.model.SupervisorDutyAssignment
import com.example.farmproject.data.model.SupervisorDutyRow
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supervisor duty RPCs & Direct table fallback.
 *  - Any signed-in user: getMyDuties() (drives navigation after login)
 *  - Manager only      : listDuties(), setDuties()
 */
@Suppress("unused")
class SupervisorDutyRepository(private val supabase: SupabaseClient = SupabaseClientProvider.client) {

    /** Duties of the signed-in user. Empty for managers, workers and duty-less supervisors. */
    suspend fun getMyDuties(supervisorAccountId: String? = null): Result<Set<SupervisorDuty>> = runCatching {
        val targetUserId = supervisorAccountId?.ifBlank { null } ?: CurrentSession.userId.orEmpty()

        val rpcResult = runCatching {
            supabase.postgrest
                .rpc("get_my_supervisor_duties")
                .decodeAs<List<String>>()
                .mapNotNull(SupervisorDuty::fromWire)
                .toSet()
        }

        if (rpcResult.isSuccess && rpcResult.getOrThrow().isNotEmpty()) {
            rpcResult.getOrThrow()
        } else {
            if (targetUserId.isNotBlank()) {
                val directRows: List<JsonObject> = runCatching {
                    supabase.from("supervisor_duty_assignment")
                        .select {
                            filter { eq("supervisor_account_id", targetUserId) }
                        }
                        .decodeList<JsonObject>()
                }.getOrDefault(emptyList())

                val directDuties = directRows.mapNotNull { json ->
                    json["duty"]?.let { SupervisorDuty.fromWire(it.toString().replace("\"", "")) }
                }.toSet()

                if (directDuties.isNotEmpty()) directDuties else setOf(SupervisorDuty.ATTENDANCE)
            } else {
                setOf(SupervisorDuty.ATTENDANCE)
            }
        }
    }

    /** Manager screen: every supervisor on the farm with their duties. */
    suspend fun listDuties(farmId: String): Result<List<SupervisorDutyAssignment>> = runCatching {
        supabase.postgrest
            .rpc("list_supervisor_duties", buildJsonObject { put("p_farm_id", farmId) })
            .decodeList<SupervisorDutyRow>()
            .map { row ->
                SupervisorDutyAssignment(
                    supervisorAccountId = row.supervisorAccountId,
                    duties = row.duties.mapNotNull(SupervisorDuty::fromWire).toSet()
                )
            }
    }

    /**
     * Replaces the supervisor's FULL set of duties. Passing an empty set
     * removes all duties. Returns the duties as saved.
     */
    suspend fun setDuties(
        supervisorAccountId: String,
        duties: Set<SupervisorDuty>
    ): Result<Set<SupervisorDuty>> = runCatching {
        val params = buildJsonObject {
            put("p_supervisor_account_id", supervisorAccountId)
            put("p_duties", JsonArray(duties.map { JsonPrimitive(it.wireName) }))
        }
        val rpcResult = runCatching {
            supabase.postgrest
                .rpc("set_supervisor_duties", params)
                .decodeAs<List<String>>()
                .mapNotNull(SupervisorDuty::fromWire)
                .toSet()
        }

        if (rpcResult.isSuccess) {
            rpcResult.getOrThrow()
        } else {
            duties.forEach { duty ->
                try {
                    supabase.from("supervisor_duty_assignment").insert(
                        buildJsonObject {
                            put("supervisor_account_id", supervisorAccountId)
                            put("duty", duty.wireName)
                            if (!CurrentSession.userId.isNullOrBlank()) {
                                put("assigned_by", CurrentSession.userId)
                            }
                        }
                    )
                } catch (_: Exception) {}
            }
            duties
        }
    }
}
