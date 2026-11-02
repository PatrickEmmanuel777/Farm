package com.example.farmproject.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =========================================================
// Supervisor duties (sub-roles)
// =========================================================

/** The four duties a supervisor can hold. wireName matches the Postgres enum. */
enum class SupervisorDuty(
    val wireName: String,
    val label: String,
    val description: String,
) {
    ATTENDANCE(
        "attendance",
        "Attendance supervisor",
        "Accepts or rejects workers' clock-in requests",
    ),
    ACTIVITIES_RESOURCES(
        "activities_resources",
        "Activities & Resources supervisor",
        "Manages activities, inputs and machinery on the manager's behalf",
    ),
    EXPENSE(
        "expense",
        "Expense supervisor",
        "Records expenses on the manager's behalf",
    ),
    YIELD_HARVEST(
        "yield_harvest",
        "Yield/Harvest supervisor",
        "Records harvests on the manager's behalf",
    );

    companion object {
        /** Unknown values from the server are ignored instead of crashing the app. */
        fun fromWire(value: String): SupervisorDuty? =
            entries.firstOrNull { it.wireName == value }
    }
}

/** One row of list_supervisor_duties(). Duties arrive as plain strings. */
@Serializable
data class SupervisorDutyRow(
    @SerialName("supervisor_account_id") val supervisorAccountId: String,
    val duties: List<String> = emptyList(),
)

/**
 * UI-friendly version of a row. Join the supervisor's name on the client,
 * e.g. from EmployeeRepository.getSupervisorsForFarm, using supervisorAccountId.
 */
data class SupervisorDutyAssignment(
    val supervisorAccountId: String,
    val duties: Set<SupervisorDuty>,
)

/**
 * Mirror of the server rule "manager OR supervisor holding the duty".
 * Use it to show or hide buttons and navigation; the database still enforces it.
 */
fun hasDutyAccess(isManager: Boolean, myDuties: Set<SupervisorDuty>, duty: SupervisorDuty): Boolean =
    isManager || myDuties.isEmpty() || duty in myDuties
