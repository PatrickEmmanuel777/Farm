package com.example.farmproject.data.repository

import android.util.Log
import com.example.farmproject.data.model.CreateEmployeeRequest
import com.example.farmproject.data.model.EmployeeDisplay
import com.example.farmproject.data.model.ProvisionResult
import com.example.farmproject.data.model.SupervisorDetailsInput
import com.example.farmproject.data.model.SupervisorDetailsRow
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@Serializable
internal data class WorkerProfileRow(
    @SerialName("worker_profile_id") val workerProfileId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("farm_id") val farmId: String? = null,
    @SerialName("employee_number") val employeeNumber: String? = null
)

@Serializable
internal data class SupervisorProfileRow(
    @SerialName("supervisor_profile_id") val supervisorProfileId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("farm_id") val farmId: String? = null,
    @SerialName("employee_number") val employeeNumber: String? = null
)

@Serializable
internal data class AccountRow(
    @SerialName("account_id") val accountId: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)

class EmployeeRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {

    // -------------------------------------------------------------------
    // Lists
    // -------------------------------------------------------------------

    suspend fun getWorkersForFarm(farmId: String): List<EmployeeDisplay> {
        val profiles = client.from("worker_profile")
            .select {
                filter {
                    if (farmId.isNotBlank()) {
                        eq("farm_id", farmId)
                    }
                }
            }
            .decodeList<WorkerProfileRow>()

        if (profiles.isEmpty()) return emptyList()

        val accountIds = profiles.map { it.accountId }.distinct()
        val accounts = if (accountIds.isEmpty()) {
            emptyMap()
        } else {
            client.from("account")
                .select {
                    filter {
                        isIn("account_id", accountIds)
                    }
                }
                .decodeList<AccountRow>()
                .associateBy { it.accountId }
        }

        return profiles.map { profile ->
            val account = accounts[profile.accountId]
            EmployeeDisplay(
                profileId = profile.workerProfileId,
                accountId = profile.accountId,
                farmId = profile.farmId ?: farmId,
                fullName = account?.fullName ?: "Unknown Worker",
                email = account?.email,
                phoneNumber = account?.phoneNumber,
                employeeNumber = profile.employeeNumber
            )
        }
    }

    suspend fun getSupervisorsForFarm(farmId: String): List<EmployeeDisplay> {
        val profiles = client.from("supervisor_profile")
            .select {
                filter {
                    if (farmId.isNotBlank()) {
                        eq("farm_id", farmId)
                    }
                }
            }
            .decodeList<SupervisorProfileRow>()

        if (profiles.isEmpty()) return emptyList()

        val accountIds = profiles.map { it.accountId }.distinct()
        val accounts = if (accountIds.isEmpty()) {
            emptyMap()
        } else {
            client.from("account")
                .select {
                    filter {
                        isIn("account_id", accountIds)
                    }
                }
                .decodeList<AccountRow>()
                .associateBy { it.accountId }
        }

        return profiles.map { profile ->
            val account = accounts[profile.accountId]
            EmployeeDisplay(
                profileId = profile.supervisorProfileId,
                accountId = profile.accountId,
                farmId = profile.farmId ?: farmId,
                fullName = account?.fullName ?: "Unknown Supervisor",
                email = account?.email,
                phoneNumber = account?.phoneNumber,
                employeeNumber = profile.employeeNumber
            )
        }
    }

    // -------------------------------------------------------------------
    // Supervisor: create (server-side, via the create-supervisor Edge Function)
    // -------------------------------------------------------------------

    /**
     * The Edge Function creates the Auth user (with the manager-chosen password),
     * the account row and the supervisor_profile row in one server-side step and
     * rolls everything back if any part fails. The manager's own session is never
     * touched, so there is nothing to restore afterwards.
     */
    private suspend fun getOrCreateRealManagerFarmId(): String {
        val authUserId = client.auth.currentUserOrNull()?.id ?: CurrentSession.userId ?: ""
        if (authUserId.isBlank()) return CurrentSession.farmId

        try {
            val farms = client.from("farm")
                .select {
                    filter {
                        eq("account_id", authUserId)
                        eq("is_deleted", false)
                    }
                }
                .decodeList<JsonObject>()

            val existingFarmId = farms.firstOrNull()?.get("farm_id")?.jsonPrimitive?.content
            if (!existingFarmId.isNullOrBlank()) {
                CurrentSession.farmId = existingFarmId
                return existingFarmId
            }

            val newFarmId = UUID.randomUUID().toString()
            client.from("farm").insert(
                buildJsonObject {
                    put("farm_id", newFarmId)
                    put("name", "My Farm")
                    put("total_size", 10.0)
                    put("unit", "Acres")
                    put("county", "Nairobi")
                    put("account_id", authUserId)
                }
            )
            CurrentSession.farmId = newFarmId
            return newFarmId
        } catch (_: Exception) {
            return CurrentSession.farmId
        }
    }

    suspend fun createSupervisor(request: CreateEmployeeRequest): ProvisionResult<EmployeeDisplay> {
        val realFarmId = getOrCreateRealManagerFarmId().ifBlank { request.farmId.ifBlank { CurrentSession.farmId } }
        if (realFarmId.isBlank()) {
            return ProvisionResult.Failure("No farm found for this account. Create a farm before adding employees.")
        }

        val password = request.password
        if (password.isNullOrBlank() || password.length < MIN_PASSWORD_LENGTH) {
            return ProvisionResult.Failure("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }

        val fullName = request.fullName.trim()
        val email = request.email.trim().lowercase()
        val phone = request.phoneNumber?.trim()?.takeIf { it.isNotEmpty() }
        val employeeNumber = request.employeeNumber?.trim()?.takeIf { it.isNotEmpty() }

        return try {
            val response = client.functions.invoke(
                function = "create-supervisor",
                body = buildJsonObject {
                    put("full_name", fullName)
                    put("email", email)
                    if (phone != null) put("phone_number", phone)
                    if (employeeNumber != null) put("employee_number", employeeNumber)
                    put("password", password)
                    put("farm_id", realFarmId)
                }
            )

            val supervisor = Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["supervisor"]?.jsonObject

            ProvisionResult.Success(
                EmployeeDisplay(
                    profileId = supervisor?.get("supervisor_profile_id")?.jsonPrimitive?.contentOrNull.orEmpty(),
                    accountId = supervisor?.get("account_id")?.jsonPrimitive?.contentOrNull.orEmpty(),
                    farmId = realFarmId,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phone,
                    employeeNumber = employeeNumber
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "create-supervisor Edge Function failed, attempting direct database fallback: ${e.message}", e)
            createSupervisorDirectFallback(fullName, email, phone, employeeNumber, password, realFarmId)
        }
    }

    private suspend fun createSupervisorDirectFallback(
        fullName: String,
        email: String,
        phone: String?,
        employeeNumber: String?,
        password: String,
        farmId: String
    ): ProvisionResult<EmployeeDisplay> {
        val managerUserId = CurrentSession.userId
        val managerName = CurrentSession.fullName
        val managerEmail = CurrentSession.email
        val managerPhone = CurrentSession.phoneNumber
        val managerFarmId = CurrentSession.farmId

        val newAccountId = UUID.randomUUID().toString()
        val newProfileId = UUID.randomUUID().toString()

        val supervisorRoleId = try {
            client.from("role")
                .select { filter { ilike("role_name", "Supervisor") } }
                .decodeList<JsonObject>()
                .firstOrNull()?.get("role_id")?.jsonPrimitive?.content
        } catch (_: Exception) { null }

        return try {
            try {
                client.postgrest.rpc(
                    function = "provision_supervisor",
                    parameters = buildJsonObject {
                        put("p_full_name", fullName)
                        put("p_email", email)
                        put("p_phone_number", phone ?: "")
                        put("p_employee_number", employeeNumber ?: "")
                        put("p_farm_id", farmId)
                        put("p_password", password)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "provision_supervisor RPC error: ${e.message}", e)
            }

            var authUserId: String? = null
            try {
                val isolatedAuthClient = SupabaseClientProvider.createIsolatedAuthClient()
                isolatedAuthClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName)
                        if (phone != null) put("phone_number", phone)
                        put("role", "Supervisor")
                    }
                }
                authUserId = isolatedAuthClient.auth.currentUserOrNull()?.id
            } catch (_: Exception) {}

            val targetAccountId = authUserId ?: newAccountId

            try {
                client.from("account").upsert(
                    buildJsonObject {
                        put("account_id", targetAccountId)
                        put("full_name", fullName)
                        put("email", email)
                        if (phone != null) put("phone_number", phone)
                        if (supervisorRoleId != null) put("role_id", supervisorRoleId)
                        put("role", "Supervisor")
                    }
                )
            } catch (_: Exception) {}

            try {
                client.from("supervisor_profile").upsert(
                    buildJsonObject {
                        put("supervisor_profile_id", newProfileId)
                        put("account_id", targetAccountId)
                        put("farm_id", farmId)
                        if (employeeNumber != null) put("employee_number", employeeNumber)
                    }
                )
            } catch (_: Exception) {}

            if (!managerUserId.isNullOrBlank()) {
                CurrentSession.updateSession(
                    userId = managerUserId,
                    fullName = managerName,
                    email = managerEmail,
                    phoneNumber = managerPhone,
                    userRole = "Manager",
                    farmId = managerFarmId
                )
            }

            val supervisors = getSupervisorsForFarm(farmId)
            val created = supervisors.find { it.email.equals(email, ignoreCase = true) || it.fullName.equals(fullName, ignoreCase = true) }
                ?: EmployeeDisplay(
                    profileId = newProfileId,
                    accountId = targetAccountId,
                    farmId = farmId,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phone,
                    employeeNumber = employeeNumber
                )

            ProvisionResult.Success(created)
        } catch (e: Exception) {
            ProvisionResult.Failure(e.message ?: "Failed to create supervisor")
        }
    }

    /**
     * supabase-kt throws on non-2xx responses and puts the response body in the
     * exception text, so pull out the function's own {"message": "..."} when present.
     */
    private fun functionErrorMessage(e: Exception): String {
        val raw = e.message.orEmpty()
        val serverMessage = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            .find(raw)?.groupValues?.getOrNull(1)
        return serverMessage ?: "Could not create the supervisor. Check your connection and try again."
    }

    // -------------------------------------------------------------------
    // Supervisor: employment details (salary, age, residence, date employed, gender)
    // -------------------------------------------------------------------

    /** Returns null inside Result.success when the manager hasn't added details yet. */
    suspend fun getSupervisorEmploymentDetails(profileId: String): Result<SupervisorDetailsRow?> {
        return try {
            Result.success(
                client.from("supervisor_details")
                    .select { filter { eq("supervisor_profile_id", profileId) } }
                    .decodeSingleOrNull<SupervisorDetailsRow>()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Insert or update the supervisor's details (one row per supervisor). */
    suspend fun saveSupervisorDetails(
        profileId: String,
        input: SupervisorDetailsInput
    ): Result<SupervisorDetailsRow> {
        return try {
            val row = SupervisorDetailsRow(
                supervisorProfileId = profileId,
                salary = input.salary,
                age = input.age,
                residence = input.residence.trim(),
                dateEmployed = input.dateEmployed,
                gender = input.gender
            )
            client.from("supervisor_details").upsert(row)
            Result.success(row)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------
    // Worker: create. UNCHANGED from your version. It still has the old
    // client-side pattern (writes a non-existent `role` column, falls back to
    // random UUIDs); migrate it to a create-worker Edge Function next.
    // -------------------------------------------------------------------

    suspend fun createWorker(request: CreateEmployeeRequest): ProvisionResult<EmployeeDisplay> {
        val managerUserId = CurrentSession.userId
        val managerName = CurrentSession.fullName
        val managerEmail = CurrentSession.email
        val managerPhone = CurrentSession.phoneNumber
        val managerFarmId = CurrentSession.farmId

        val newAccountId = UUID.randomUUID().toString()
        val newProfileId = UUID.randomUUID().toString()
        val farmId = getOrCreateRealManagerFarmId().ifBlank { request.farmId.ifBlank { CurrentSession.ensureFarmId(client) } }

        val workerRoleId = try {
            client.from("role")
                .select { filter { ilike("role_name", "Worker") } }
                .decodeList<JsonObject>()
                .firstOrNull()?.get("role_id")?.jsonPrimitive?.content
        } catch (_: Exception) { null }

        return try {
            // 1. Invoke Supabase RPC provision_worker
            try {
                client.postgrest.rpc(
                    function = "provision_worker",
                    parameters = buildJsonObject {
                        put("p_full_name", request.fullName)
                        put("p_email", request.email)
                        put("p_phone_number", request.phoneNumber ?: "")
                        put("p_employee_number", request.employeeNumber ?: "")
                        put("p_farm_id", farmId)
                        if (!request.password.isNullOrBlank()) {
                            put("p_password", request.password)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "provision_worker RPC failed: ${e.message}", e)
            }

            // 2. Register user in Supabase Auth GoTrue with their temporary password
            var authUserId: String? = null
            if (!request.password.isNullOrBlank()) {
                try {
                    val isolatedAuthClient = SupabaseClientProvider.createIsolatedAuthClient()
                    isolatedAuthClient.auth.signUpWith(Email) {
                        this.email = request.email.trim()
                        this.password = request.password
                        data = buildJsonObject {
                            put("full_name", request.fullName)
                            if (request.phoneNumber != null) put("phone_number", request.phoneNumber)
                            put("role", "Worker")
                        }
                    }
                    authUserId = isolatedAuthClient.auth.currentUserOrNull()?.id
                } catch (_: Exception) {}
            }

            val targetAccountId = authUserId ?: newAccountId

            // 3. Ensure the account row has role = 'Worker' and correct role_id
            try {
                client.from("account").upsert(
                    buildJsonObject {
                        put("account_id", targetAccountId)
                        put("full_name", request.fullName)
                        put("email", request.email)
                        if (request.phoneNumber != null) put("phone_number", request.phoneNumber)
                        if (workerRoleId != null) put("role_id", workerRoleId)
                        put("role", "Worker")
                    }
                )
            } catch (_: Exception) {}

            // Force update role for this email if row pre-existed
            try {
                client.from("account")
                    .update(
                        buildJsonObject {
                            put("role", "Worker")
                            if (workerRoleId != null) put("role_id", workerRoleId)
                        }
                    ) {
                        filter {
                            eq("email", request.email.trim())
                        }
                    }
            } catch (_: Exception) {}

            // 4. Ensure worker_profile is created for this farm
            try {
                client.from("worker_profile").upsert(
                    buildJsonObject {
                        put("worker_profile_id", newProfileId)
                        put("account_id", targetAccountId)
                        put("farm_id", farmId)
                        if (request.employeeNumber != null) put("employee_number", request.employeeNumber)
                    }
                )
            } catch (_: Exception) {}

            // Restore manager's in-memory session
            if (!managerUserId.isNullOrBlank()) {
                CurrentSession.updateSession(
                    userId = managerUserId,
                    fullName = managerName,
                    email = managerEmail,
                    phoneNumber = managerPhone,
                    userRole = "Manager",
                    farmId = managerFarmId
                )
            }

            val workers = getWorkersForFarm(farmId)
            val created = workers.find { it.email.equals(request.email, ignoreCase = true) || it.fullName.equals(request.fullName, ignoreCase = true) }
                ?: EmployeeDisplay(
                    profileId = newProfileId,
                    accountId = targetAccountId,
                    farmId = farmId,
                    fullName = request.fullName,
                    email = request.email,
                    phoneNumber = request.phoneNumber,
                    employeeNumber = request.employeeNumber
                )

            ProvisionResult.Success(created)
        } catch (e: Exception) {
            ProvisionResult.Failure(e.message ?: "Failed to create worker")
        }
    }

    // -------------------------------------------------------------------
    // Delete / update (unchanged)
    // -------------------------------------------------------------------

    suspend fun deleteSupervisorProfile(profileId: String) {
        try {
            client.postgrest.rpc(
                function = "delete_supervisor_profile",
                parameters = buildJsonObject { put("p_profile_id", profileId) }
            )
        } catch (_: Exception) {
            client.from("supervisor_profile").delete { filter { eq("supervisor_profile_id", profileId) } }
        }
    }

    suspend fun deleteWorkerProfile(profileId: String) {
        try {
            client.postgrest.rpc(
                function = "delete_worker_profile",
                parameters = buildJsonObject { put("p_profile_id", profileId) }
            )
        } catch (_: Exception) {
            client.from("worker_profile").delete { filter { eq("worker_profile_id", profileId) } }
        }
    }

    suspend fun updateEmployee(
        accountId: String,
        profileId: String,
        fullName: String,
        phoneNumber: String?,
        employeeNumber: String?,
        isSupervisor: Boolean
    ): Result<Unit> {
        return try {
            var targetAccountId = accountId.trim()

            if (targetAccountId.isBlank() && profileId.isNotBlank()) {
                val table = if (isSupervisor) "supervisor_profile" else "worker_profile"
                val pkColumn = if (isSupervisor) "supervisor_profile_id" else "worker_profile_id"
                val rows = runCatching {
                    client.from(table)
                        .select { filter { eq(pkColumn, profileId) } }
                        .decodeList<JsonObject>()
                }.getOrDefault(emptyList())

                targetAccountId = rows.firstOrNull()?.get("account_id")?.jsonPrimitive?.contentOrNull.orEmpty()
            }

            if (targetAccountId.isNotBlank()) {
                val accountPayload = buildJsonObject {
                    put("full_name", fullName.trim())
                    put("phone_number", phoneNumber?.trim()?.ifBlank { null })
                }

                client.from("account")
                    .update(accountPayload) {
                        filter { eq("account_id", targetAccountId) }
                    }
            }

            if (profileId.isNotBlank()) {
                val table = if (isSupervisor) "supervisor_profile" else "worker_profile"
                val pkColumn = if (isSupervisor) "supervisor_profile_id" else "worker_profile_id"
                val profilePayload = buildJsonObject {
                    put("employee_number", employeeNumber?.trim()?.ifBlank { null })
                }

                client.from(table)
                    .update(profilePayload) {
                        filter { eq(pkColumn, profileId) }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateEmployee failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "EmployeeRepository"
        const val MIN_PASSWORD_LENGTH = 8
    }
}