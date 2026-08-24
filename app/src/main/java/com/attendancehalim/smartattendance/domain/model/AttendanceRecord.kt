package com.attendancehalim.smartattendance.domain.model

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val time: String,
    val type: AttendanceType,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val localArea: String,
    val syncStatus: SyncStatus,
    val createdByAdminId: String? = null,
    val createdByAdminName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
