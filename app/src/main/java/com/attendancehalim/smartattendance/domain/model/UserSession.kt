package com.attendancehalim.smartattendance.domain.model

data class UserSession(
    val isLoggedIn: Boolean = false,
    val employeeId: String = "",
    val userName: String = "",
    val mobileNumber: String = "",
    val role: UserRole = UserRole.WORKER,
    val workplaceName: String = "",
    val photoUrl: String = "",
    val status: String = "ACTIVE",
    val authToken: String = "",
    val tokenExpiry: Long = 0L
) {
    val isTokenExpired: Boolean
        get() = tokenExpiry > 0L && System.currentTimeMillis() > tokenExpiry
}
