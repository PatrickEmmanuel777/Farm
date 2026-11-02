package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =========================================================
// Clock-in requests (worker asks, manager / attendance supervisor decides)
// Timestamps are ISO-8601 strings; dates are "yyyy-MM-dd".
// =========================================================

@Serializable
enum class ClockRequestStatus {
    @SerialName("pending") PENDING,
    @SerialName("approved") APPROVED,
    @SerialName("rejected") REJECTED;

    val label: String
        get() = when (this) {
            PENDING -> "Pending"
            APPROVED -> "Approved"
            REJECTED -> "Rejected"
        }
}

/**
 * A row of attendance_clock_request. Returned by request_clock_in(),
 * get_my_clock_requests() and review_clock_request().
 */
@Serializable
data class ClockRequest(
    @SerialName("request_id") val requestId: String,
    @SerialName("farm_id") val farmId: String,
    @SerialName("requester_account_id") val requesterAccountId: String,
    @SerialName("requested_at") val requestedAt: String,
    val status: ClockRequestStatus,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * One row of list_pending_clock_requests(). It returns only the employee
 * number, not a name. For a screen that needs names, use the attendance
 * board (FarmAttendanceRow): it has fullName and pendingRequestId, so you
 * can match on requestId == pendingRequestId.
 */
@Serializable
data class PendingClockRequest(
    @SerialName("request_id") val requestId: String,
    @SerialName("requester_account_id") val requesterAccountId: String,
    @SerialName("employee_number") val employeeNumber: String? = null,
    @SerialName("requested_at") val requestedAt: String,
)

/**
 * One row of get_farm_attendance(): everyone on the farm for a date, with
 * any open clock-in request. Use pendingRequestId to approve or reject
 * straight from this list.
 */
@Serializable
data class FarmAttendanceRow(
    @SerialName("staff_id") val staffId: String,
    @SerialName("staff_role") val staffRole: String,           // "worker" | "supervisor"
    @SerialName("full_name") val fullName: String,
    @SerialName("employee_code") val employeeCode: String? = null,
    @SerialName("attendance_id") val attendanceId: String? = null,
    val status: String? = null,                                // present | late | absent | on_leave | null
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    val duration: Double? = null,                              // hours
    val notes: String? = null,
    @SerialName("has_pending_request") val hasPendingRequest: Boolean = false,
    @SerialName("pending_request_id") val pendingRequestId: String? = null,
)
