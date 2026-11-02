package com.example.farmproject.data.repository

import com.example.farmproject.data.model.ClockRequest
import com.example.farmproject.data.model.FarmAttendanceRow
import com.example.farmproject.data.model.PendingClockRequest
import com.example.farmproject.data.remote.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class RawPendingClockRequest(
    val requestId: String,
    val requesterAccountId: String,
    val requestedAt: String
)

@Suppress("unused")
class ClockRequestRepository(private val supabase: SupabaseClient = SupabaseClientProvider.client) {

    // ---------------------------------------------------------
    // Worker
    // ---------------------------------------------------------

    suspend fun requestClockIn(): Result<ClockRequest> = runCatching {
        supabase.postgrest
            .rpc("request_clock_in")
            .decodeAs<ClockRequest>()
    }

    suspend fun getMyRequests(limit: Int = 30): Result<List<ClockRequest>> = runCatching {
        supabase.postgrest
            .rpc("get_my_clock_requests", buildJsonObject { put("p_limit", limit) })
            .decodeList<ClockRequest>()
    }

    suspend fun getMyLatestRequest(): Result<ClockRequest?> =
        getMyRequests(limit = 1).map { it.firstOrNull() }

    // ---------------------------------------------------------
    // Manager / attendance supervisor
    // ---------------------------------------------------------

    suspend fun listPendingRequestsRaw(): Result<List<RawPendingClockRequest>> = runCatching {
        val rows = supabase.from("attendance_clock_request")
            .select {
                filter {
                    eq("status", "pending")
                }
            }
            .decodeList<JsonObject>()

        rows.map { json ->
            val reqId = json["request_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val reqAccId = json["requester_account_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val reqAt = json["requested_at"]?.jsonPrimitive?.contentOrNull
                ?: json["created_at"]?.jsonPrimitive?.contentOrNull
                ?: ""
            RawPendingClockRequest(
                requestId = reqId,
                requesterAccountId = reqAccId,
                requestedAt = reqAt
            )
        }
    }

    suspend fun listPendingRequestsDirect(): Result<List<ClockRequest>> = runCatching {
        supabase.from("attendance_clock_request")
            .select {
                filter {
                    eq("status", "pending")
                }
            }
            .decodeList<ClockRequest>()
    }

    suspend fun listPending(farmId: String): Result<List<PendingClockRequest>> = runCatching {
        supabase.postgrest
            .rpc("list_pending_clock_requests", buildJsonObject { put("p_farm_id", farmId) })
            .decodeList<PendingClockRequest>()
    }

    suspend fun approve(requestId: String): Result<ClockRequest> = review(requestId, true, null)

    suspend fun reject(requestId: String, reason: String? = null): Result<ClockRequest> =
        review(requestId, false, reason)

    private suspend fun review(
        requestId: String,
        approve: Boolean,
        reason: String?
    ): Result<ClockRequest> = runCatching {
        val params = buildJsonObject {
            put("p_request_id", requestId)
            put("p_approve", approve)
            putIfNotNull("p_reason", reason?.trim()?.takeIf { it.isNotEmpty() })
        }
        supabase.postgrest
            .rpc("review_clock_request", params)
            .decodeAs<ClockRequest>()
    }

    suspend fun getFarmAttendanceBoard(
        farmId: String,
        date: String? = null
    ): Result<List<FarmAttendanceRow>> = runCatching {
        val params = buildJsonObject {
            put("p_farm_id", farmId)
            putIfNotNull("p_date", date)
        }
        supabase.postgrest
            .rpc("get_farm_attendance", params)
            .decodeList<FarmAttendanceRow>()
    }

    private fun JsonObjectBuilder.putIfNotNull(key: String, value: String?) {
        if (value != null) put(key, value)
    }
}
