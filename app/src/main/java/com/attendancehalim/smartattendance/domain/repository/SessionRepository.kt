package com.attendancehalim.smartattendance.domain.repository

import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessionFlow: Flow<UserSession>
    suspend fun saveSession(session: UserSession)
    suspend fun updateRole(role: UserRole)
    suspend fun clearSession()
    suspend fun getActiveSession(): UserSession
}
