package com.attendancehalim.smartattendance.domain.model

enum class UserRole {
    WORKER,
    ADMIN;

    companion object {
        fun fromString(role: String?): UserRole {
            return entries.firstOrNull { it.name.equals(role, ignoreCase = true) } ?: WORKER
        }
    }
}
