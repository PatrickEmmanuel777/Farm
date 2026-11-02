package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FarmActivity(
    @SerialName("activity_id") val activityId: String? = null,
    val name: String? = null,
    @SerialName("activity_type_id") val activityTypeId: String? = null,
    @SerialName("farm_id") val farmId: String? = null,
    @SerialName("division_id") val divisionId: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("finish_at") val finishAt: String? = null,
    val status: String? = "pending",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Backward-compatibility getters for UI
    @SerialName("worker_profile_id") val workerProfileId: String? = null,
    @SerialName("supervisor_profile_id") val supervisorProfileId: String? = null,
    @SerialName("activity_date") val activityDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("quantity") val quantity: Double? = null,
    @SerialName("quantity_unit") val quantityUnit: String? = null,
    @SerialName("equipment_needed") val equipmentNeeded: String? = null,
    @SerialName("method") val method: String? = null,
    @SerialName("activity_name") val activityName: String? = null,
    @SerialName("duration_hours") val durationHours: Double? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null
) {
    val displayTitle: String
        get() = name?.ifBlank { null }
            ?: activityName?.ifBlank { null }
            ?: notes?.take(30)?.ifBlank { null }
            ?: "General Task"

    val safeActivityTypeId: String get() = activityTypeId.orEmpty().ifBlank { "General Activity" }
    val safeFarmId: String get() = farmId.orEmpty()
    val safeActivityDate: String
        get() = startAt?.take(10)?.ifBlank { null }
            ?: activityDate?.ifBlank { null }
            ?: dueDate?.ifBlank { null }
            ?: "2026-10-28"

    val safeStatus: String
        get() = when (status?.lowercase()?.replace(" ", "_")) {
            "in_progress", "in-progress", "ongoing" -> "In Progress"
            "completed", "done" -> "Completed"
            else -> "Pending"
        }
}
