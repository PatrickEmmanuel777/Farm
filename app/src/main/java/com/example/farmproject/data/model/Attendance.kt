package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRecord(
    @SerialName("attendance_id") val attendanceId: String? = null,
    @SerialName("farm_id") val farmId: String,
    @SerialName("worker_profile_id") val workerProfileId: String? = null,
    @SerialName("supervisor_profile_id") val supervisorProfileId: String? = null,
    @SerialName("work_date") val workDate: String,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    @SerialName("check_in") val rawCheckIn: String? = null,
    @SerialName("check_out") val rawCheckOut: String? = null,
    val status: String = "present",
    val notes: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("supervisor_id") val supervisorId: String? = null,
) {
    val checkIn: String? get() = clockIn ?: rawCheckIn
    val checkOut: String? get() = clockOut ?: rawCheckOut

    val displayStatus: String
        get() = when (status.lowercase()) {
            "late" -> "Late"
            "absent" -> "Absent"
            "on_leave", "on leave" -> "On Leave"
            else -> "Present"
        }
}

@Serializable
data class AttendanceDisplay(
    val record: AttendanceRecord?,
    val workerProfileId: String,
    val workerName: String,
    val employeeNumber: String? = null,
    val phoneNumber: String? = null,
    val role: String = "Worker",
) {
    val currentStatus: String
        get() = record?.displayStatus ?: "Not Marked"

    @Suppress("unused")
    val isMarked: Boolean
        get() = record != null
}
