package com.example.farmproject.data.repository

import com.example.farmproject.data.model.ActivityType
import com.example.farmproject.data.model.FarmActivity
import com.example.farmproject.data.model.FarmActivityDisplay
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable
internal data class ActivityTypeNameLookup(
    @SerialName("activity_type_id") val activityTypeId: String,
    val name: String
)

@Serializable
internal data class ActivityDivisionLookup(
    @SerialName("division_id") val divisionId: String,
    val name: String
)

@Serializable
internal data class WorkerAccountLookup(
    @SerialName("worker_profile_id") val workerProfileId: String,
    @SerialName("account_id") val accountId: String
)

@Serializable
internal data class SupervisorAccountLookup(
    @SerialName("supervisor_profile_id") val supervisorProfileId: String,
    @SerialName("account_id") val accountId: String
)

@Serializable
internal data class AccountNameLookup(
    @SerialName("account_id") val accountId: String,
    @SerialName("full_name") val fullName: String
)

class FarmActivityRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val employeeRepository: EmployeeRepository = EmployeeRepository(client)
) {

    private fun String.isValidUuid(): Boolean {
        return try {
            UUID.fromString(this)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun resolveActivityTypeId(input: String): String {
        if (input.isValidUuid()) return input

        val existingTypes = try {
            client.from("activity_type")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<ActivityType>()
        } catch (_: Exception) {
            emptyList()
        }

        // 1. Try exact name match
        val matched = existingTypes.find { it.name.equals(input.trim(), ignoreCase = true) }
        if (matched?.activityTypeId != null) return matched.activityTypeId

        // 2. Try partial name match
        val partial = existingTypes.find { it.name.contains(input.trim(), ignoreCase = true) || input.trim().contains(it.name, ignoreCase = true) }
        if (partial?.activityTypeId != null) return partial.activityTypeId

        // 3. Insert new activity type into database
        val newTypeId = UUID.randomUUID().toString()
        try {
            val inserted = client.from("activity_type")
                .insert(
                    ActivityType(
                        activityTypeId = newTypeId,
                        name = input.trim(),
                        category = "General",
                        description = "Created automatically"
                    )
                ) {
                    select()
                }
                .decodeSingle<ActivityType>()
            return inserted.activityTypeId ?: newTypeId
        } catch (_: Exception) {
            return existingTypes.firstOrNull()?.activityTypeId ?: newTypeId
        }
    }

    suspend fun getActivityTypes(): List<ActivityType> {
        return try {
            client.from("activity_type")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<ActivityType>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFarmActivitiesForFarm(farmId: String): List<FarmActivityDisplay> {
        val validFarmId = if (farmId.isValidUuid()) farmId else CurrentSession.ensureFarmId(client)

        val fetchedActivities = try {
            client.postgrest
                .rpc("list_farm_activities", buildJsonObject { put("p_farm_id", validFarmId) })
                .decodeList<FarmActivity>()
        } catch (_: Exception) {
            try {
                client.from("farm_activity")
                    .select {
                        filter {
                            if (validFarmId.isNotBlank()) {
                                eq("farm_id", validFarmId)
                            }
                        }
                    }
                    .decodeList<FarmActivity>()
            } catch (_: Exception) {
                emptyList()
            }
        }

        val activities = if (fetchedActivities.isEmpty() && validFarmId.isNotBlank()) {
            try {
                client.from("farm_activity").select().decodeList<FarmActivity>()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            fetchedActivities
        }

        if (activities.isEmpty()) return emptyList()

        val activityTypeIds = activities.mapNotNull { it.activityTypeId }.filter { it.isValidUuid() }.distinct()
        val divisionIds = activities.mapNotNull { it.divisionId }.filter { it.isValidUuid() }.distinct()
        val assignedAccountIds = activities.mapNotNull { it.assignedTo }.filter { it.isValidUuid() }.distinct()
        val workerProfileIds = activities.mapNotNull { it.workerProfileId }.filter { it.isValidUuid() }.distinct()
        val supervisorProfileIds = activities.mapNotNull { it.supervisorProfileId }.filter { it.isValidUuid() }.distinct()

        val activityTypeNames = if (activityTypeIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                client.from("activity_type")
                    .select(columns = Columns.list("activity_type_id", "name")) {
                        filter { isIn("activity_type_id", activityTypeIds) }
                    }
                    .decodeList<ActivityTypeNameLookup>()
                    .associate { it.activityTypeId to it.name }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        val divisionNames = if (divisionIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                client.from("division")
                    .select(columns = Columns.list("division_id", "name")) {
                        filter { isIn("division_id", divisionIds) }
                    }
                    .decodeList<ActivityDivisionLookup>()
                    .associate { it.divisionId to it.name }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        val workerAccountIds = if (workerProfileIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                client.from("worker_profile")
                    .select(columns = Columns.list("worker_profile_id", "account_id")) {
                        filter { isIn("worker_profile_id", workerProfileIds) }
                    }
                    .decodeList<WorkerAccountLookup>()
                    .associate { it.workerProfileId to it.accountId }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        val supervisorAccountIds = if (supervisorProfileIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                client.from("supervisor_profile")
                    .select(columns = Columns.list("supervisor_profile_id", "account_id")) {
                        filter { isIn("supervisor_profile_id", supervisorProfileIds) }
                    }
                    .decodeList<SupervisorAccountLookup>()
                    .associate { it.supervisorProfileId to it.accountId }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        val allAccountIds = (assignedAccountIds + workerAccountIds.values + supervisorAccountIds.values).distinct()

        val accountNames = if (allAccountIds.isEmpty()) {
            emptyMap()
        } else {
            try {
                client.from("account")
                    .select(columns = Columns.list("account_id", "full_name")) {
                        filter { isIn("account_id", allAccountIds) }
                    }
                    .decodeList<AccountNameLookup>()
                    .associate { it.accountId to it.fullName }
            } catch (_: Exception) {
                emptyMap()
            }
        }

        return activities.map { activity ->
            val assignedToName = when {
                activity.assignedTo != null ->
                    accountNames[activity.assignedTo]

                activity.workerProfileId != null ->
                    workerAccountIds[activity.workerProfileId]?.let { accountNames[it] }

                activity.supervisorProfileId != null ->
                    supervisorAccountIds[activity.supervisorProfileId]?.let { accountNames[it] }

                activity.method?.startsWith("Role:") == true ->
                    activity.method.removePrefix("Role:").trim()

                else -> null
            }

            val typeId = activity.safeActivityTypeId
            val dateStr = activity.safeActivityDate
            val statusStr = activity.safeStatus

            val cleanActivity = activity.copy(
                activityDate = dateStr,
                status = statusStr
            )

            FarmActivityDisplay(
                activity = cleanActivity,
                activityTypeName = activityTypeNames[typeId] ?: typeId,
                divisionName = activity.divisionId?.let { divisionNames[it] },
                assignedToName = assignedToName
            )
        }
    }

    suspend fun getFarmActivityById(activityId: String): FarmActivity? {
        val rpcResult = runCatching {
            client.postgrest
                .rpc("get_farm_activity", buildJsonObject { put("p_activity_id", activityId) })
                .decodeSingle<FarmActivity>()
        }

        if (rpcResult.isSuccess) {
            return rpcResult.getOrThrow()
        }

        return try {
            client.from("farm_activity")
                .select {
                    filter {
                        eq("activity_id", activityId)
                    }
                }
                .decodeSingleOrNull<FarmActivity>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun insertFarmActivity(activity: FarmActivity): FarmActivity {
        val targetFarmId = activity.safeFarmId
        val resolvedFarmId = if (targetFarmId.isValidUuid()) targetFarmId else CurrentSession.ensureFarmId(client)
        val resolvedTypeId = resolveActivityTypeId(activity.safeActivityTypeId)
        val resolvedDivisionId = if (activity.divisionId?.isValidUuid() == true) activity.divisionId else null

        // Resolve assigned_to (account_id)
        var resolvedAccountId = activity.assignedTo?.ifBlank { null }
        if (resolvedAccountId == null && !activity.workerProfileId.isNullOrBlank()) {
            val workers = employeeRepository.getWorkersForFarm(resolvedFarmId)
            resolvedAccountId = workers.find { it.profileId == activity.workerProfileId }?.accountId
        }
        if (resolvedAccountId == null && !activity.supervisorProfileId.isNullOrBlank()) {
            val supervisors = employeeRepository.getSupervisorsForFarm(resolvedFarmId)
            resolvedAccountId = supervisors.find { it.profileId == activity.supervisorProfileId }?.accountId
        }
        if (resolvedAccountId == null) {
            resolvedAccountId = CurrentSession.userId.orEmpty()
        }

        val newActivityId = activity.activityId ?: UUID.randomUUID().toString()
        val taskName = activity.displayTitle.ifBlank { "General Activity" }
        val cleanDate = activity.safeActivityDate.ifBlank {
            try { LocalDate.now().toString() } catch (_: Exception) { "2026-10-28" }
        }
        val dbStatus = when (activity.safeStatus.lowercase().replace(" ", "_")) {
            "in_progress", "in-progress", "ongoing" -> "in_progress"
            "completed", "done" -> "completed"
            else -> "pending"
        }

        // 1. Try create_farm_activity RPC
        val rpcParams = buildJsonObject {
            put("p_name", taskName)
            put("p_activity_type_id", resolvedTypeId)
            put("p_farm_id", resolvedFarmId)
            put("p_assigned_to", resolvedAccountId)
            put("p_finish_at", "${cleanDate}T17:00:00Z")
            resolvedDivisionId?.let { put("p_division_id", it) }
            put("p_status", dbStatus)
        }

        val rpcResult = runCatching {
            client.postgrest
                .rpc("create_farm_activity", rpcParams)
                .decodeSingle<FarmActivity>()
        }

        if (rpcResult.isSuccess) {
            return rpcResult.getOrThrow()
        }

        // 2. Direct Postgrest insert fallback
        val jsonPayload = buildJsonObject {
            put("activity_id", newActivityId)
            put("name", taskName)
            put("activity_type_id", resolvedTypeId)
            put("farm_id", resolvedFarmId)
            put("assigned_to", resolvedAccountId)
            put("start_at", "${cleanDate}T08:00:00Z")
            put("finish_at", "${cleanDate}T17:00:00Z")
            put("status", dbStatus)

            resolvedDivisionId?.let { put("division_id", it) }
            if (!CurrentSession.userId.isNullOrBlank()) {
                put("created_by", CurrentSession.userId)
            }
        }

        try {
            client.from("farm_activity").insert(jsonPayload)
        } catch (_: Exception) {}

        return activity.copy(
            activityId = newActivityId,
            name = taskName,
            farmId = resolvedFarmId,
            activityTypeId = resolvedTypeId,
            assignedTo = resolvedAccountId,
            startAt = "${cleanDate}T08:00:00Z",
            finishAt = "${cleanDate}T17:00:00Z",
            status = dbStatus
        )
    }

    suspend fun updateFarmActivity(activity: FarmActivity): FarmActivity {
        val id = requireNotNull(activity.activityId) {
            "Cannot update a FarmActivity with no activityId"
        }

        val dbStatus = when (activity.safeStatus.lowercase().replace(" ", "_")) {
            "in_progress", "in-progress", "ongoing" -> "in_progress"
            "completed", "done" -> "completed"
            else -> "pending"
        }

        val rpcParams = buildJsonObject {
            put("p_activity_id", id)
            activity.name?.ifBlank { null }?.let { put("p_name", it) }
            activity.activityTypeId?.let { put("p_activity_type_id", it) }
            activity.assignedTo?.let { put("p_assigned_to", it) }
            activity.startAt?.let { put("p_start_at", it) }
            activity.finishAt?.let { put("p_finish_at", it) }
            put("p_status", dbStatus)
            if (activity.divisionId != null) {
                put("p_division_id", activity.divisionId)
                put("p_set_division", true)
            }
        }

        val rpcResult = runCatching {
            client.postgrest
                .rpc("update_farm_activity", rpcParams)
                .decodeSingle<FarmActivity>()
        }

        if (rpcResult.isSuccess) {
            return rpcResult.getOrThrow()
        }

        val jsonPayload = buildJsonObject {
            activity.name?.ifBlank { null }?.let { put("name", it) }
            activity.activityTypeId?.let { put("activity_type_id", it) }
            activity.divisionId?.let { put("division_id", it) }
            activity.assignedTo?.let { put("assigned_to", it) }
            activity.startAt?.let { put("start_at", it) }
            activity.finishAt?.let { put("finish_at", it) }
            put("status", dbStatus)
        }

        try {
            client.from("farm_activity").update(jsonPayload) {
                filter { eq("activity_id", id) }
            }
        } catch (_: Exception) {}

        return activity
    }

    suspend fun deleteFarmActivity(activityId: String) {
        val rpcResult = runCatching {
            client.postgrest.rpc("delete_farm_activity", buildJsonObject { put("p_activity_id", activityId) })
        }
        if (rpcResult.isFailure) {
            try {
                client.from("farm_activity").delete { filter { eq("activity_id", activityId) } }
            } catch (_: Exception) {}
        }
    }

    suspend fun startFarmActivity(activityId: String) {
        val rpcResult = runCatching {
            client.postgrest.rpc(
                "set_farm_activity_status",
                buildJsonObject {
                    put("p_activity_id", activityId)
                    put("p_status", "in_progress")
                }
            )
        }

        if (rpcResult.isFailure) {
            val nowStr = try { Instant.now().toString() } catch (_: Exception) { "2026-10-28T08:00:00Z" }
            val futureFinishStr = try { Instant.now().plusSeconds(8 * 3600).toString() } catch (_: Exception) { "2026-10-28T17:00:00Z" }
            val updatePayload = buildJsonObject {
                put("status", "in_progress")
                put("start_at", nowStr)
                put("finish_at", futureFinishStr)
            }
            try {
                client.from("farm_activity").update(updatePayload) {
                    filter { eq("activity_id", activityId) }
                }
            } catch (_: Exception) {
                try {
                    client.from("farm_activity").update(buildJsonObject { put("status", "in_progress") }) {
                        filter { eq("activity_id", activityId) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun completeFarmActivity(activityId: String, @Suppress("UNUSED_PARAMETER") notes: String? = null) {
        val rpcResult = runCatching {
            client.postgrest.rpc(
                "set_farm_activity_status",
                buildJsonObject {
                    put("p_activity_id", activityId)
                    put("p_status", "completed")
                }
            )
        }

        if (rpcResult.isFailure) {
            val nowStr = try { Instant.now().toString() } catch (_: Exception) { "2026-10-28T17:00:00Z" }
            val updatePayload = buildJsonObject {
                put("status", "completed")
                put("finish_at", nowStr)
            }
            try {
                client.from("farm_activity").update(updatePayload) {
                    filter { eq("activity_id", activityId) }
                }
            } catch (_: Exception) {
                try {
                    client.from("farm_activity").update(buildJsonObject { put("status", "completed") }) {
                        filter { eq("activity_id", activityId) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun assignFarmActivity(
        activityId: String,
        @Suppress("UNUSED_PARAMETER") isSupervisor: Boolean,
        profileId: String
    ) {
        val employeeRepository = EmployeeRepository(client)
        val farmId = CurrentSession.farmId
        val workers = employeeRepository.getWorkersForFarm(farmId)
        val supervisors = employeeRepository.getSupervisorsForFarm(farmId)

        val targetAccountId = workers.find { it.profileId == profileId }?.accountId
            ?: supervisors.find { it.profileId == profileId }?.accountId
            ?: profileId

        val rpcParams = buildJsonObject {
            put("p_activity_id", activityId)
            put("p_assigned_to", targetAccountId)
        }

        val rpcResult = runCatching {
            client.postgrest.rpc("update_farm_activity", rpcParams)
        }

        if (rpcResult.isFailure) {
            try {
                client.from("farm_activity")
                    .update(buildJsonObject { put("assigned_to", targetAccountId) }) {
                        filter { eq("activity_id", activityId) }
                    }
            } catch (_: Exception) {}
        }
    }
}
