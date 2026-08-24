package com.attendancehalim.smartattendance.domain.model

enum class AttendanceType(val title: String) {
    PUNCH_IN("Punch In"),
    PUNCH_OUT("Punch Out"),
    MANUAL("Manual Attendance");

    companion object {
        fun fromString(type: String?): AttendanceType {
            return entries.firstOrNull {
                it.name.equals(type, ignoreCase = true) || it.title.equals(type, ignoreCase = true)
            } ?: PUNCH_IN
        }
    }
}
