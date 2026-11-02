package com.example.farmproject.data.repository

import android.util.Log
import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class AccountSignUpResult(
    @SerialName("account_id") val accountId: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val role: String? = null,
)

@Serializable
internal data class RoleLookup(
    @SerialName("role_id") val roleId: String,
    @SerialName("role_name") val roleName: String
)

class AuthRepository(private val supabaseClient: SupabaseClient = SupabaseClientProvider.client) {

    private val client: SupabaseClient get() = supabaseClient

    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

    suspend fun ensureManagerAccount(userId: String, fullName: String, email: String, phoneNumber: String?) {
        val managerRoleId = try {
            client.from("role")
                .select {
                    filter {
                        ilike("role_name", "Manager")
                    }
                }
                .decodeList<RoleLookup>()
                .firstOrNull()?.roleId
        } catch (_: Exception) {
            null
        }

        try {
            client.from("account").upsert(
                buildJsonObject {
                    put("account_id", userId)
                    if (managerRoleId != null) put("role_id", managerRoleId)
                    put("full_name", fullName)
                    put("email", email)
                    if (phoneNumber != null) put("phone_number", phoneNumber)
                }
            )
        } catch (_: Exception) {}

        try {
            client.from("manager_profile").upsert(
                buildJsonObject {
                    put("manager_profile_id", UUID.randomUUID().toString())
                    put("account_id", userId)
                }
            )
        } catch (_: Exception) {}
    }

    private fun sanitizeAuthErrorMessage(rawError: String?, isSignUp: Boolean = false): String {
        if (rawError.isNullOrBlank()) {
            return if (isSignUp) "Sign up failed. Please check your details and try again."
                   else "Invalid credentials. Please check your email and password."
        }

        val lower = rawError.lowercase()

        return when {
            lower.contains("unable to resolve host") || lower.contains("connectexception") || lower.contains("sockettimeoutexception") || lower.contains("unknownhostexception") || lower.contains("timeout") ->
                "Network connection error. Please check your internet connection and try again."
            lower.contains("user_already_exists") || lower.contains("already registered") || lower.contains("already exists") ->
                "An account with this email already exists. Please sign in instead."
            lower.contains("email_not_confirmed") || lower.contains("email not confirmed") || lower.contains("unconfirmed") ->
                "Email not confirmed. Please check your inbox or confirm user in Supabase Dashboard."
            lower.contains("invalid_credentials") || lower.contains("invalid login credentials") || lower.contains("invalid_grant") ->
                if (isSignUp) "Could not complete sign up. An account with this email may already exist."
                else "Invalid email or password. Please check your credentials and try again."
            lower.contains("user_not_found") ->
                "No account found for this email. Please sign up first."
            lower.contains("password") && lower.contains("should be at least") ->
                "Password must be at least 6 characters long."
            else -> {
                val cleanLine = rawError.lines().firstOrNull { line ->
                    !line.contains("URL:", ignoreCase = true) &&
                    !line.contains("Headers:", ignoreCase = true) &&
                    !line.contains("Http Method:", ignoreCase = true)
                }?.trim()

                if (!cleanLine.isNullOrBlank() && cleanLine.length < 100) {
                    cleanLine
                } else if (isSignUp) {
                    "Sign up failed. Please check your details and try again."
                } else {
                    "Invalid email or password. Please check your credentials and try again."
                }
            }
        }
    }

    suspend fun loginUser(
        email: String,
        password: String,
        expectedRole: String? = null
    ): Result<AccountSignUpResult> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) return Result.failure(Exception("Please enter your email address."))
        if (password.isBlank()) return Result.failure(Exception("Please enter your password."))

        return try {
            withTimeoutOrNull(1500.milliseconds) {
                runCatching { client.auth.awaitInitialization() }
            }

            // 1. Query public.account table first (case-insensitive)
            val accounts = try {
                client.from("account")
                    .select {
                        filter {
                            ilike("email", trimmedEmail)
                        }
                    }
                    .decodeList<AccountSignUpResult>()
            } catch (_: Exception) {
                emptyList()
            }

            // 2. Attempt Supabase Auth Sign In (GoTrue)
            var authErrorMsg: String? = null
            var authSuccess = false
            try {
                client.auth.signInWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
                authSuccess = true
            } catch (e: Exception) {
                Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
                authErrorMsg = e.message
            }

            // If GoTrue Auth sign in failed AND no account row exists in public.account table
            if (!authSuccess && accounts.isEmpty()) {
                val cleanMessage = sanitizeAuthErrorMessage(authErrorMsg, isSignUp = false)
                return Result.failure(Exception(cleanMessage))
            }

            // 3. Resolve logged-in user profile details
            val authUser = client.auth.currentUserOrNull()
            val dbAccount = accounts.firstOrNull()

            val metaName = authUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
            val fallbackName = dbAccount?.fullName?.ifBlank { null }
                ?: metaName
                ?: trimmedEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            val currentAccountId = dbAccount?.accountId ?: authUser?.id ?: UUID.randomUUID().toString()

            val supervisorFarmId = if (currentAccountId.isNotBlank()) {
                try {
                    val rows = client.from("supervisor_profile").select { filter { eq("account_id", currentAccountId) } }.decodeList<JsonObject>()
                    rows.firstOrNull()?.get("farm_id")?.jsonPrimitive?.contentOrNull
                } catch (_: Exception) { null }
            } else null

            val workerFarmId = if (currentAccountId.isNotBlank() && supervisorFarmId == null) {
                try {
                    val rows = client.from("worker_profile").select { filter { eq("account_id", currentAccountId) } }.decodeList<JsonObject>()
                    rows.firstOrNull()?.get("farm_id")?.jsonPrimitive?.contentOrNull
                } catch (_: Exception) { null }
            } else null

            val isSupervisorProfile = supervisorFarmId != null
            val isWorkerProfile = workerFarmId != null

            val metaRole = authUser?.userMetadata?.get("role")?.jsonPrimitive?.contentOrNull
            val detectedRole = when {
                isSupervisorProfile -> "Supervisor"
                isWorkerProfile -> "Worker"
                !dbAccount?.role.isNullOrBlank() -> dbAccount.role
                !metaRole.isNullOrBlank() -> metaRole
                else -> "Manager"
            }

            val userRole = expectedRole ?: detectedRole

            val account = dbAccount ?: AccountSignUpResult(
                accountId = currentAccountId,
                fullName = fallbackName,
                email = authUser?.email ?: trimmedEmail,
                phoneNumber = null,
                role = userRole
            )

            // Force update role in account table if signing in from Supervisor/Worker portal
            try {
                if (expectedRole.equals("Supervisor", ignoreCase = true) || expectedRole.equals("Worker", ignoreCase = true)) {
                    client.from("account")
                        .update(buildJsonObject { put("role", expectedRole) }) {
                            filter {
                                eq("email", trimmedEmail)
                            }
                        }
                }
            } catch (_: Exception) {}

            if (userRole.equals("Manager", ignoreCase = true)) {
                ensureManagerAccount(account.accountId, account.fullName.ifBlank { fallbackName }, account.email, account.phoneNumber)
            }

            val farm = FarmRepository(client).getFarmForUser(account.accountId)
            val resolvedFarmId = supervisorFarmId ?: workerFarmId ?: farm?.farmId ?: ""

            val myDuties = if (userRole.equals("Supervisor", ignoreCase = true)) {
                SupervisorDutyRepository(client).getMyDuties(account.accountId).getOrDefault(setOf(SupervisorDuty.ATTENDANCE)).ifEmpty { setOf(SupervisorDuty.ATTENDANCE) }
            } else emptySet()

            CurrentSession.updateSession(
                userId = account.accountId,
                fullName = account.fullName.ifBlank { fallbackName },
                email = account.email,
                phoneNumber = account.phoneNumber,
                userRole = userRole,
                farmId = resolvedFarmId,
                supervisorDuties = myDuties
            )

            Result.success(account.copy(role = userRole))
        } catch (e: Exception) {
            Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
            Result.failure(Exception(sanitizeAuthErrorMessage(e.message, isSignUp = false)))
        }
    }

    suspend fun signUpManager(
        fullName: String,
        email: String,
        phoneNumber: String?,
        password: String
    ): Result<AccountSignUpResult> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) return Result.failure(Exception("Please enter your email address."))
        if (password.isBlank()) return Result.failure(Exception("Please enter a password."))

        return try {
            withTimeoutOrNull(1500.milliseconds) {
                runCatching { client.auth.awaitInitialization() }
            }

            // 1. Check if account already exists in public.account table
            val existingAccount = try {
                client.from("account")
                    .select { filter { ilike("email", trimmedEmail) } }
                    .decodeSingleOrNull<AccountSignUpResult>()
            } catch (_: Exception) { null }

            if (existingAccount != null) {
                return Result.failure(Exception("An account with this email already exists. Please sign in instead."))
            }

            // 2. Register with Supabase Auth (GoTrue)
            var authUserId: String? = null
            var signUpError: String? = null

            try {
                client.auth.signUpWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName)
                        if (phoneNumber != null) put("phone_number", phoneNumber)
                        put("role", "Manager")
                    }
                }
                authUserId = client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
                Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
                signUpError = e.message
            }

            if (signUpError != null && (signUpError.contains("already", ignoreCase = true) || signUpError.contains("exists", ignoreCase = true))) {
                return Result.failure(Exception("An account with this email already exists. Please sign in instead."))
            }

            // Attempt signInWith if session wasn't automatically established
            if (authUserId == null) {
                try {
                    client.auth.signInWith(Email) {
                        this.email = trimmedEmail
                        this.password = password
                    }
                    authUserId = client.auth.currentUserOrNull()?.id
                } catch (e: Exception) {
                    Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
                }
            }

            val currentAuthId = authUserId ?: client.auth.currentUserOrNull()?.id ?: UUID.randomUUID().toString()

            // 3. Ensure account and manager_profile rows exist in database
            ensureManagerAccount(currentAuthId, fullName, trimmedEmail, phoneNumber)

            val result = AccountSignUpResult(
                accountId = currentAuthId,
                fullName = fullName,
                email = trimmedEmail,
                phoneNumber = phoneNumber,
                role = "Manager"
            )

            CurrentSession.updateSession(
                userId = result.accountId,
                fullName = result.fullName,
                email = result.email,
                phoneNumber = result.phoneNumber,
                userRole = "Manager",
                farmId = ""
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
            val cleanMessage = sanitizeAuthErrorMessage(e.message, isSignUp = true)
            Result.failure(Exception(cleanMessage))
        }
    }

    suspend fun restoreSession(): Boolean {
        return try {
            withTimeoutOrNull(1500.milliseconds) {
                runCatching { client.auth.awaitInitialization() }
            }

            val user = client.auth.currentUserOrNull() ?: return false
            val userEmail = user.email ?: return false

            coroutineScope {
                val accountDeferred = async {
                    getMyAccount() ?: try {
                        client.from("account")
                            .select { filter { eq("email", userEmail) } }
                            .decodeSingleOrNull<AccountSignUpResult>()
                    } catch (_: Exception) { null }
                }

                val account = accountDeferred.await()
                if (account == null) {
                    try { logout() } catch (_: Exception) { CurrentSession.clear() }
                    return@coroutineScope false
                }

                val currentAccId = account.accountId.ifBlank { user.id }

                val supervisorFarmDeferred = async {
                    try {
                        val rows = client.from("supervisor_profile").select { filter { eq("account_id", currentAccId) } }.decodeList<JsonObject>()
                        rows.firstOrNull()?.get("farm_id")?.jsonPrimitive?.contentOrNull
                    } catch (_: Exception) { null }
                }

                val workerFarmDeferred = async {
                    try {
                        val rows = client.from("worker_profile").select { filter { eq("account_id", currentAccId) } }.decodeList<JsonObject>()
                        rows.firstOrNull()?.get("farm_id")?.jsonPrimitive?.contentOrNull
                    } catch (_: Exception) { null }
                }

                val farmDeferred = async {
                    FarmRepository(client).getFarmForUser(currentAccId)
                }

                val dutiesDeferred = async {
                    SupervisorDutyRepository(client).getMyDuties().getOrDefault(emptySet())
                }

                val supervisorFarmId = supervisorFarmDeferred.await()
                val workerFarmId = workerFarmDeferred.await()
                val farm = farmDeferred.await()
                val myDuties = dutiesDeferred.await()

                val metadata = user.userMetadata
                val fullNameFromMeta = metadata?.get("full_name")?.jsonPrimitive?.content
                val phoneFromMeta = metadata?.get("phone_number")?.jsonPrimitive?.content
                val roleFromMeta = metadata?.get("role")?.jsonPrimitive?.content

                val defaultName = userEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val resolvedName = account.fullName.ifBlank { fullNameFromMeta ?: defaultName }
                val resolvedPhone = account.phoneNumber ?: phoneFromMeta

                val isSupervisorProfile = supervisorFarmId != null || account.role.equals("Supervisor", ignoreCase = true) || roleFromMeta.equals("Supervisor", ignoreCase = true)
                val isWorkerProfile = workerFarmId != null || account.role.equals("Worker", ignoreCase = true) || roleFromMeta.equals("Worker", ignoreCase = true)

                val detectedRole = when {
                    isSupervisorProfile -> "Supervisor"
                    isWorkerProfile -> "Worker"
                    !account.role.isNullOrBlank() -> account.role
                    !roleFromMeta.isNullOrBlank() -> roleFromMeta
                    else -> "Manager"
                }

                if (detectedRole.equals("Manager", ignoreCase = true)) {
                    ensureManagerAccount(user.id, resolvedName, userEmail, resolvedPhone)
                }

                val resolvedFarmId = supervisorFarmId ?: workerFarmId ?: farm?.farmId ?: ""

                CurrentSession.updateSession(
                    userId = currentAccId,
                    fullName = resolvedName,
                    email = userEmail,
                    phoneNumber = resolvedPhone,
                    userRole = detectedRole,
                    farmId = resolvedFarmId,
                    supervisorDuties = myDuties
                )

                true
            }
        } catch (e: Exception) {
            Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
            try {
                logout()
            } catch (_: Exception) {
                CurrentSession.clear()
            }
            false
        }
    }

    suspend fun logout() {
        CurrentSession.clearSession(client)
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val trimmed = newPassword.trim()
            if (trimmed.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters long."))
            }
            client.auth.updateUser {
                password = trimmed
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
            Result.failure(Exception(sanitizeAuthErrorMessage(e.message, isSignUp = false)))
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String,
        phoneNumber: String?
    ): Result<Unit> {
        return try {
            if (userId.isNotBlank()) {
                client.from("account").update(
                    buildJsonObject {
                        put("full_name", fullName)
                        if (phoneNumber != null) put("phone_number", phoneNumber)
                    }
                ) {
                    filter {
                        eq("account_id", userId)
                    }
                }
            }

            try {
                client.auth.updateUser {
                    data = buildJsonObject {
                        put("full_name", fullName)
                        if (phoneNumber != null) put("phone_number", phoneNumber)
                    }
                }
            } catch (_: Exception) {}

            CurrentSession.fullName = fullName
            CurrentSession.phoneNumber = phoneNumber

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "FAILED: ${e::class.qualifiedName} - ${e.message}", e)
            Result.failure(Exception(e.message ?: "Failed to update profile"))
        }
    }

    private suspend fun createAccount(
        fullName: String,
        email: String,
        phoneNumber: String?,
        role: String = "Manager"
    ) {
        val user = client.auth.currentUserOrNull() ?: return
        val currentUserId = user.id

        val roleId = try {
            client.from("role")
                .select {
                    filter {
                        ilike("role_name", role)
                    }
                }
                .decodeList<RoleLookup>()
                .firstOrNull()?.roleId
        } catch (_: Exception) {
            null
        }

        try {
            client.from("account").upsert(
                buildJsonObject {
                    put("account_id", currentUserId)
                    if (roleId != null) put("role_id", roleId)
                    put("full_name", fullName)
                    put("email", email)
                    if (phoneNumber != null) put("phone_number", phoneNumber)
                }
            )
        } catch (_: Exception) {}
    }

    suspend fun getMyAccount(): AccountSignUpResult? {
        val user = client.auth.currentUserOrNull() ?: return null
        return try {
            client.from("account")
                .select {
                    filter {
                        eq("account_id", user.id)
                    }
                }
                .decodeSingleOrNull<AccountSignUpResult>()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
