package com.attendancehalim.smartattendance.data.repository

import com.attendancehalim.smartattendance.data.local.session.SessionManager
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class SessionRepositoryImpl(
    private val sessionManager: SessionManager
) : SessionRepository {

    override val sessionFlow: Flow<UserSession> = sessionManager.sessionFlow

    override suspend fun saveSession(session: UserSession) {
        sessionManager.saveSession(session)
    }

    override suspend fun updateRole(role: UserRole) {
        sessionManager.updateRole(role)
    }

    override suspend fun clearSession() {
        sessionManager.clearSession()
    }

    override suspend fun getActiveSession(): UserSession {
        return sessionManager.getActiveSession()
    }
}
