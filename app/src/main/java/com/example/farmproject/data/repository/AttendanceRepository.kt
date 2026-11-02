package com.example.farmproject.data.repository

import android.util.Log
import com.example.farmproject.data.model.AttendanceDisplay
import com.example.farmproject.data.model.AttendanceRecord
import com.example.farmproject.data.model.FarmAttendanceRow
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Attendance data access.
 *
 *  - READS  : plain table selects (manager list, own history) or RPC (own row today).
 *  - WRITES : only through the Postgres functions in attendance_functions.sql. The database
 *             decides who the caller is, stamps the time, sets present/late and enforces the rules,
 *             so a manager can no longer write attendance for someone else.
 */
class AttendanceRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val employeeRepository: EmployeeRepository = EmployeeRepository(client)
) {

    // ───────────────────────── Identity ─────────────────────────

    /** worker_profile_id or supervisor_profile_id of the logged-in user; null if they have neither. */
    suspend fun resolveMyProfileId(): String? {
        if (CurrentSession.userId == null && client.auth.currentUserOrNull() == null) return null
        return try {
            client.postgrest.rpc("get_my_attendance_profile")
                .decodeList<JsonObject>()
                .firstOrNull()
                ?.str("staff_id")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "resolveMyProfileId failed: ${e.message}")
            null
        }
    }

    // ───────────────────────── SELECT ─────────────────────────

    /** Manager list: every worker + supervisor on the farm, with their attendance for the day (if any). */
    suspend fun getAttendanceForDate(farmId: String, workDate: String): List<AttendanceDisplay> {
        val targetFarmId = farmId.ifBlank { CurrentSession.farmId }

        // 1. Try get_farm_attendance RPC
        val rpcResult = runCatching {
            val params = buildJsonObject {
                put("p_farm_id", targetFarmId)
                put("p_date", workDate)
            }
            client.postgrest.rpc("get_farm_attendance", params)
                .decodeList<FarmAttendanceRow>()
        }

        if (rpcResult.isSuccess) {
            val rows = rpcResult.getOrThrow()
            if (rows.isNotEmpty()) {
                return rows.map { row ->
                    val rec = if (!row.attendanceId.isNullOrBlank() || !row.status.isNullOrBlank() || row.clockIn != null) {
                        AttendanceRecord(
                            attendanceId = row.attendanceId,
                            farmId = targetFarmId,
                            workerProfileId = row.staffId,
                            workDate = workDate,
                            clockIn = row.clockIn,
                            clockOut = row.clockOut,
                            status = row.status ?: "present",
                            notes = row.notes
                        )
                    } else null

                    AttendanceDisplay(
                        record = rec,
                        workerProfileId = row.staffId,
                        workerName = row.fullName,
                        employeeNumber = row.employeeCode,
                        phoneNumber = null,
                        role = if (row.staffRole.equals("supervisor", ignoreCase = true)) "Supervisor" else "Worker"
                    )
                }
            }
        }

        // 2. Direct table select fallback
        val records = runCatching {
            client.from("attendance")
                .select {
                    filter {
                        if (targetFarmId.isNotBlank()) eq("farm_id", targetFarmId)
                        eq("work_date", workDate)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JsonObject>()
                .map { it.toRecord(targetFarmId, workDate) }
        }.getOrDefault(emptyList())

        val workers = employeeRepository.getWorkersForFarm(targetFarmId)
        val supervisors = employeeRepository.getSupervisorsForFarm(targetFarmId)

        val workersDisplay = workers.map { w ->
            val match = records.find {
                it.workerProfileId == w.profileId || it.workerProfileId == w.accountId
            }
            AttendanceDisplay(
                record = match,
                workerProfileId = w.profileId,
                workerName = w.fullName,
                employeeNumber = w.employeeNumber,
                phoneNumber = w.phoneNumber,
                role = "Worker"
            )
        }

        val supervisorsDisplay = supervisors.map { s ->
            val match = records.find {
                it.workerProfileId == s.profileId || it.workerProfileId == s.accountId
            }
            AttendanceDisplay(
                record = match,
                workerProfileId = s.profileId,
                workerName = s.fullName,
                employeeNumber = s.employeeNumber,
                phoneNumber = s.phoneNumber,
                role = "Supervisor"
            )
        }

        return supervisorsDisplay + workersDisplay
    }

    /** My own row for today (server decides "today" in Nairobi time), or null = not marked. */
    suspend fun getMyAttendanceToday(): AttendanceRecord? =
        client.postgrest.rpc("get_my_attendance")
            .decodeList<JsonObject>()
            .firstOrNull()
            ?.toRecord()

    suspend fun getMyAttendanceHistory(farmId: String): List<AttendanceRecord> {
        val myProfileId = resolveMyProfileId() ?: return emptyList()
        val targetFarmId = farmId.ifBlank { CurrentSession.farmId }

        return client.from("attendance")
            .select {
                filter {
                    if (targetFarmId.isNotBlank()) eq("farm_id", targetFarmId)
                    or {
                        eq("worker_profile_id", myProfileId)
                        eq("supervisor_profile_id", myProfileId)
                    }
                }
                order("work_date", Order.DESCENDING)
            }
            .decodeList<JsonObject>()
            .map { it.toRecord(targetFarmId) }
    }

    // ───────────────────────── CREATE / UPDATE (RPC) ─────────────────────────

    /** Creates today's row. Status becomes present (or late after 08:00); time is stamped by the server. */
    suspend fun clockInSelf(@Suppress("UNUSED_PARAMETER") farmId: String = ""): Result<AttendanceRecord> =
        attempt("clockIn") { rpcRecord("attendance_clock_in") }

    /** Fills clock_out (and duration). Arguments kept only so existing callers still compile. */
    suspend fun clockOut(
        @Suppress("UNUSED_PARAMETER") attendanceId: String? = null,
        @Suppress("UNUSED_PARAMETER") workerProfileId: String? = null,
        @Suppress("UNUSED_PARAMETER") workDate: String = ""
    ): Result<AttendanceRecord> =
        attempt("clockOut") { rpcRecord("attendance_clock_out") }

    /** The worker/supervisor marks THEMSELVES absent or on leave (only before clocking in). */
    suspend fun markAway(status: String, notes: String? = null): Result<AttendanceRecord> {
        val dbStatus = status.lowercase().replace(' ', '_')
        if (dbStatus != "absent" && dbStatus != "on_leave") {
            return Result.failure(Exception("Present and late are set automatically when you clock in."))
        }
        return attempt("markAway") {
            rpcRecord("attendance_mark_away", buildJsonObject {
                put("p_status", dbStatus)
                put("p_notes", notes?.trim()?.ifBlank { null })
            })
        }
    }

    /** Removes today's absent/leave row so the person can clock in instead. */
    suspend fun undoAway(): Result<Unit> =
        attempt("undoAway") {
            client.postgrest.rpc("attendance_undo_away")
        }

    // ── Old entry points, kept so existing screens compile ─────────────────────

    @Deprecated("Managers no longer record attendance for others. Use clockInSelf().")
    suspend fun clockIn(
        @Suppress("UNUSED_PARAMETER") farmId: String,
        @Suppress("UNUSED_PARAMETER") workerProfileId: String,
        @Suppress("UNUSED_PARAMETER") workDate: String = ""
    ): Result<AttendanceRecord> = clockInSelf(farmId)

    @Deprecated("Staff mark themselves: use markAway(). Present/late are set by clocking in.")
    suspend fun markAttendance(
        @Suppress("UNUSED_PARAMETER") farmId: String,
        @Suppress("UNUSED_PARAMETER") workerProfileId: String,
        @Suppress("UNUSED_PARAMETER") workDate: String,
        status: String,
        notes: String? = null
    ): Result<AttendanceRecord> = markAway(status, notes)

    // ───────────────────────── helpers ─────────────────────────

    private suspend fun rpcRecord(function: String, params: JsonObject? = null): AttendanceRecord {
        val result = if (params == null) client.postgrest.rpc(function)
        else client.postgrest.rpc(function, params)
        return result.decodeAs<JsonObject>().toRecord()
    }

    private suspend inline fun <T> attempt(tag: String, block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$tag failed: ${e.message}", e)
            Result.failure(Exception(friendlyError(e)))
        }

    /**
     * Shows the database's own message when it raised one ("You have already clocked in today"),
     * and a generic one otherwise. Failures are never reported as success.
     */
    private fun friendlyError(e: Exception): String {
        val dbMessage = (e as? RestException)?.error?.takeIf { it.isNotBlank() }
        val raw = dbMessage ?: e.message?.lineSequence()?.firstOrNull().orEmpty()
        return when {
            raw.isBlank() -> "An unexpected error occurred."
            raw.contains("PGRST", ignoreCase = true) || raw.contains("Headers:", ignoreCase = true) ->
                "Failed to save attendance. Please check your connection and try again."
            else -> raw
        }
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    /** Maps a DB row to your existing AttendanceRecord model (clock_in/clock_out -> checkIn/checkOut). */
    private fun JsonObject.toRecord(fallbackFarmId: String = "", fallbackDate: String = ""): AttendanceRecord =
        AttendanceRecord(
            attendanceId = str("attendance_id"),
            farmId = str("farm_id") ?: fallbackFarmId,
            workerProfileId = str("worker_profile_id") ?: str("supervisor_profile_id") ?: "",
            workDate = str("work_date") ?: fallbackDate,
            clockIn = str("clock_in"),
            clockOut = str("clock_out"),
            rawCheckIn = str("check_in"),
            rawCheckOut = str("check_out"),
            status = str("status") ?: "present",
            notes = str("notes"),
            recordedBy = str("recorded_by"),
            createdAt = str("created_at")
        )

    private companion object {
        const val TAG = "AttendanceRepository"
    }
}
