package com.example.farmproject.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDisplay(
    val profileId: String,
    val accountId: String? = null,
    val farmId: String? = null,
    val fullName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val employeeNumber: String? = null
) {
    val displayFullName: String
        get() = fullName.trim().lowercase().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.ifBlank { fullName }

    val displayPhoneNumber: String?
        get() {
            val raw = phoneNumber?.trim() ?: return null
            if (raw.isBlank()) return null
            val digits = raw.filter { it.isDigit() }
            return when {
                digits.length == 9 && digits.startsWith("7") -> "+254 $digits"
                digits.length == 10 && digits.startsWith("07") -> "+254 ${digits.substring(1)}"
                digits.length == 12 && digits.startsWith("254") -> "+254 ${digits.substring(3)}"
                else -> raw
            }
        }
}
