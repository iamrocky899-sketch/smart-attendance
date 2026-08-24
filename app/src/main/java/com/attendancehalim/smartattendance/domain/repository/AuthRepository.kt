package com.attendancehalim.smartattendance.domain.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession

interface AuthRepository {
    suspend fun loginWorker(employeeId: String, password: String): Resource<UserSession>

    suspend fun loginAdmin(username: String, password: String): Resource<UserSession>

    suspend fun logout(): Resource<Unit>

    suspend fun switchRole(targetRole: UserRole): Resource<Unit>
}
