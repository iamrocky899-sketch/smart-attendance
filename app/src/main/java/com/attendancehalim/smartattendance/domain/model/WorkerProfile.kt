package com.attendancehalim.smartattendance.domain.model

data class WorkerProfile(
    val employeeId: String,
    val fullName: String,
    val mobileNumber: String = "",
    val workplaceName: String = "",
    val designation: String = "",
    val photoUrl: String = "",
    val joiningDate: String = "",
    val isActive: Boolean = true,
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
