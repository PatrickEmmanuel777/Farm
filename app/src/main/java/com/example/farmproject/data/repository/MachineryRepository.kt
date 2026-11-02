package com.example.farmproject.data.repository

import com.example.farmproject.data.model.Machinery
import com.example.farmproject.data.model.MachineryMaintenance
import com.example.farmproject.data.model.MachineryUsage
import com.example.farmproject.data.remote.SupabaseClientProvider
import com.example.farmproject.data.session.CurrentSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID

class MachineryRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) {
    suspend fun getMachineryForFarm(farmId: String): List<Machinery> {
        val targetFarmId = farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        return try {
            client.from("machinery")
                .select {
                    filter {
                        if (targetFarmId.isNotBlank()) {
                            eq("farm_id", targetFarmId)
                        }
                        eq("is_deleted", false)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Machinery>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getMachineryById(machineryId: String): Machinery? {
        return client.from("machinery")
            .select {
                filter {
                    eq("machinery_id", machineryId)
                }
            }
            .decodeSingleOrNull<Machinery>()
    }

    suspend fun insertMachinery(machinery: Machinery): Machinery {
        val resolvedFarmId = machinery.farmId.ifBlank { CurrentSession.ensureFarmId(client) }
        val cleanMachinery = machinery.copy(
            machineryId = machinery.machineryId ?: UUID.randomUUID().toString(),
            farmId = resolvedFarmId,
            purchaseDate = machinery.purchaseDate?.trim()?.ifBlank { null }
        )

        return try {
            client.from("machinery")
                .insert(cleanMachinery) {
                    select()
                }
                .decodeSingle<Machinery>()
        } catch (_: Exception) {
            try {
                client.from("machinery").insert(cleanMachinery)
                cleanMachinery
            } catch (inner: Exception) {
                throw inner
            }
        }
    }

    suspend fun updateMachinery(machinery: Machinery): Machinery {
        val id = requireNotNull(machinery.machineryId) {
            "Cannot update a Machinery with no machineryId"
        }

        return client.from("machinery")
            .update(machinery) {
                filter {
                    eq("machinery_id", id)
                }
                select()
            }
            .decodeSingle<Machinery>()
    }

    suspend fun deleteMachinery(machineryId: String) {
        client.from("machinery")
            .update(mapOf("is_deleted" to true)) {
                filter {
                    eq("machinery_id", machineryId)
                }
            }
    }

    /** Usage history for one machine, most recent first — for a details-screen tab. */
    suspend fun getUsageHistory(machineryId: String): List<MachineryUsage> {
        return client.from("machinery_usage")
            .select {
                filter {
                    eq("machinery_id", machineryId)
                    eq("is_voided", false)
                }
                order("usage_date", Order.DESCENDING)
            }
            .decodeList<MachineryUsage>()
    }

    suspend fun logMachineryUsage(usage: MachineryUsage): MachineryUsage {
        return client.from("machinery_usage")
            .insert(usage) {
                select()
            }
            .decodeSingle<MachineryUsage>()
    }

    /** Voids a usage record rather than deleting it — matches the schema's audit-trail intent. */
    @Suppress("unused")
    suspend fun voidMachineryUsage(usageId: String, voidedBy: String, reason: String) {
        client.from("machinery_usage")
            .update(
                mapOf(
                    "is_voided" to true,
                    "voided_by" to voidedBy,
                    "void_reason" to reason
                )
            ) {
                filter {
                    eq("usage_id", usageId)
                }
            }
    }

    // ---------------------------------------------------------------
    // machinery_maintenance
    // ---------------------------------------------------------------

    /** Maintenance/service history for one machine, most recent first. */
    suspend fun getMaintenanceHistory(machineryId: String): List<MachineryMaintenance> {
        return client.from("machinery_maintenance")
            .select {
                filter {
                    eq("machinery_id", machineryId)
                    eq("is_cancelled", false)
                }
                order("maintenance_date", Order.DESCENDING)
            }
            .decodeList<MachineryMaintenance>()
    }

    suspend fun logMaintenance(maintenance: MachineryMaintenance): MachineryMaintenance {
        return client.from("machinery_maintenance")
            .insert(maintenance) {
                select()
            }
            .decodeSingle<MachineryMaintenance>()
    }

    /** Cancels a maintenance record rather than deleting it. */
    @Suppress("unused")
    suspend fun cancelMaintenance(maintenanceId: String, cancelledBy: String, reason: String) {
        client.from("machinery_maintenance")
            .update(
                mapOf(
                    "is_cancelled" to true,
                    "cancelled_by" to cancelledBy,
                    "cancellation_reason" to reason
                )
            ) {
                filter {
                    eq("maintenance_id", maintenanceId)
                }
            }
    }

    suspend fun getAllMaintenanceForFarm(farmId: String): List<MachineryMaintenance> {
        val machines = getMachineryForFarm(farmId)
        val machineIds = machines.mapNotNull { it.machineryId }.filter { it.isNotBlank() }
        if (machineIds.isEmpty()) return emptyList()
        return try {
            client.from("machinery_maintenance")
                .select {
                    filter {
                        isIn("machinery_id", machineIds)
                        eq("is_cancelled", false)
                    }
                    order("maintenance_date", Order.DESCENDING)
                }
                .decodeList<MachineryMaintenance>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllUsageForFarm(farmId: String): List<MachineryUsage> {
        val machines = getMachineryForFarm(farmId)
        val machineIds = machines.mapNotNull { it.machineryId }.filter { it.isNotBlank() }
        if (machineIds.isEmpty()) return emptyList()
        return try {
            client.from("machinery_usage")
                .select {
                    filter {
                        isIn("machinery_id", machineIds)
                        eq("is_voided", false)
                    }
                    order("usage_date", Order.DESCENDING)
                }
                .decodeList<MachineryUsage>()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
