package com.example.farmproject.data.session

import com.example.farmproject.data.model.SupervisorDuty
import com.example.farmproject.data.model.hasDutyAccess
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

object CurrentSession {

    // Persisted so it survives a cold start. Cleared together with the rest on logout.
    var farmId: String = ""
    var userId: String? = null
    var userRole: String? = null
    var fullName: String? = null
    var email: String? = null
    var phoneNumber: String? = null
    var supervisorDuties: Set<SupervisorDuty> = emptySet()

    fun updateSession(
        userId: String?,
        fullName: String?,
        email: String?,
        phoneNumber: String?,
        userRole: String?,
        farmId: String = "",
        supervisorDuties: Set<SupervisorDuty> = emptySet()
    ) {
        this.userId = userId
        this.fullName = fullName
        this.email = email
        this.phoneNumber = phoneNumber
        this.userRole = userRole
        if (farmId.isNotBlank()) this.farmId = farmId
        if (supervisorDuties.isNotEmpty()) this.supervisorDuties = supervisorDuties
    }

    fun isManager(): Boolean = userRole?.equals("Manager", ignoreCase = true) == true
    fun isSupervisor(): Boolean = userRole?.equals("Supervisor", ignoreCase = true) == true
    fun isWorker(): Boolean = userRole?.equals("Worker", ignoreCase = true) == true

    fun hasDuty(duty: SupervisorDuty): Boolean = hasDutyAccess(isManager(), supervisorDuties, duty)

    suspend fun ensureFarmId(client: SupabaseClient): String {
        if (farmId.isNotBlank()) return farmId

        val currentUserId = userId ?: client.auth.currentUserOrNull()?.id
        if (!currentUserId.isNullOrBlank()) {
            try {
                val existingFarms = client.from("farm")
                    .select {
                        filter {
                            eq("account_id", currentUserId)
                        }
                    }
                    .decodeList<JsonObject>()

                val foundFarmId = existingFarms.firstOrNull()?.get("farm_id")?.jsonPrimitive?.content
                if (!foundFarmId.isNullOrBlank()) {
                    farmId = foundFarmId
                    return foundFarmId
                }

                // Create a default farm row if none exists for this manager
                val newFarmId = UUID.randomUUID().toString()
                client.from("farm").insert(
                    buildJsonObject {
                        put("farm_id", newFarmId)
                        put("name", "My Farm")
                        put("total_size", 10.0)
                        put("unit", "Acres")
                        put("county", "Nairobi")
                        put("account_id", currentUserId)
                    }
                )
                farmId = newFarmId
                return newFarmId
            } catch (_: Exception) {
            }
        }

        val fallbackId = UUID.randomUUID().toString()
        farmId = fallbackId
        return fallbackId
    }

    /** Wipes the in-memory state only. */
    fun clear() {
        userId = null
        fullName = null
        email = null
        phoneNumber = null
        userRole = null
        farmId = ""
        supervisorDuties = emptySet()
    }

    suspend fun clearSession(client: SupabaseClient) {
        try {
            client.auth.signOut(SignOutScope.LOCAL)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: the server call failed, but the persisted session MUST still be
            // removed, otherwise the next launch silently logs the user back in.
            try {
                client.auth.clearSession()
            } catch (_: Exception) {
            }
        }
        clear()
    }
}
