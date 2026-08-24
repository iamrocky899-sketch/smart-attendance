package com.attendancehalim.smartattendance

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import com.attendancehalim.smartattendance.presentation.main.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogoutSessionTest {

    private class FakeSessionRepository : SessionRepository {
        private val _sessionFlow = MutableStateFlow(UserSession())
        override val sessionFlow: Flow<UserSession> = _sessionFlow

        override suspend fun saveSession(session: UserSession) {
            _sessionFlow.value = session
        }

        override suspend fun updateRole(role: UserRole) {
            _sessionFlow.value = _sessionFlow.value.copy(role = role)
        }

        override suspend fun clearSession() {
            // Emulates DataStore preferences.clear()
            _sessionFlow.value = UserSession(
                isLoggedIn = false,
                employeeId = "",
                userName = "",
                mobileNumber = "",
                role = UserRole.WORKER,
                workplaceName = "",
                photoUrl = "",
                status = "ACTIVE",
                authToken = "",
                tokenExpiry = 0L
            )
        }

        override suspend fun getActiveSession(): UserSession {
            return _sessionFlow.value
        }
    }

    private class FakeAuthRepository(private val sessionRepository: SessionRepository) : AuthRepository {
        var networkCalled = false

        override suspend fun loginWorker(employeeId: String, password: String): Resource<UserSession> {
            networkCalled = true
            val session = UserSession(
                isLoggedIn = true,
                employeeId = employeeId,
                userName = "Worker $employeeId",
                role = UserRole.WORKER,
                authToken = "header.payload.signature",
                tokenExpiry = System.currentTimeMillis() + 86400000L
            )
            sessionRepository.saveSession(session)
            return Resource.Success(session)
        }

        override suspend fun loginAdmin(username: String, password: String): Resource<UserSession> {
            networkCalled = true
            val session = UserSession(
                isLoggedIn = true,
                employeeId = username,
                userName = "Admin User",
                role = UserRole.ADMIN,
                authToken = "admin_header.payload.signature",
                tokenExpiry = System.currentTimeMillis() + 86400000L
            )
            sessionRepository.saveSession(session)
            return Resource.Success(session)
        }

        override suspend fun logout(): Resource<Unit> {
            // Offline local logout - NO network request required
            networkCalled = false
            sessionRepository.clearSession()
            return Resource.Success(Unit)
        }

        override suspend fun switchRole(targetRole: UserRole): Resource<Unit> {
            sessionRepository.updateRole(targetRole)
            return Resource.Success(Unit)
        }
    }

    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    fun setup() {
        fakeSessionRepository = FakeSessionRepository()
        fakeAuthRepository = FakeAuthRepository(fakeSessionRepository)
    }

    @Test
    fun testWorkerLogout_clearsSessionCompletely() = runBlocking {
        // 1. Log in Worker
        fakeAuthRepository.loginWorker("EMP001", "password123")
        var currentSession = fakeSessionRepository.getActiveSession()
        assertTrue(currentSession.isLoggedIn)
        assertEquals("EMP001", currentSession.employeeId)
        assertEquals(UserRole.WORKER, currentSession.role)
        assertEquals("header.payload.signature", currentSession.authToken)

        // 2. Perform Logout
        val logoutResult = fakeAuthRepository.logout()
        assertTrue(logoutResult is Resource.Success)

        // 3. Verify session is completely cleared
        currentSession = fakeSessionRepository.getActiveSession()
        assertFalse(currentSession.isLoggedIn)
        assertEquals("", currentSession.employeeId)
        assertEquals("", currentSession.userName)
        assertEquals("", currentSession.authToken)
        assertEquals(0L, currentSession.tokenExpiry)
    }

    @Test
    fun testAdminLogout_clearsSessionCompletely() = runBlocking {
        // 1. Log in Admin
        fakeAuthRepository.loginAdmin("ADMIN01", "admin123")
        var currentSession = fakeSessionRepository.getActiveSession()
        assertTrue(currentSession.isLoggedIn)
        assertEquals("ADMIN01", currentSession.employeeId)
        assertEquals(UserRole.ADMIN, currentSession.role)
        assertEquals("admin_header.payload.signature", currentSession.authToken)

        // 2. Perform Logout
        val logoutResult = fakeAuthRepository.logout()
        assertTrue(logoutResult is Resource.Success)

        // 3. Verify session is completely cleared
        currentSession = fakeSessionRepository.getActiveSession()
        assertFalse(currentSession.isLoggedIn)
        assertEquals("", currentSession.employeeId)
        assertEquals("", currentSession.authToken)
        assertEquals(0L, currentSession.tokenExpiry)
    }

    @Test
    fun testLogout_preservesRoomAttendanceRecordsAndPendingSync() = runBlocking {
        // Mock local Room attendance records list
        val localRoomRecords = mutableListOf(
            AttendanceRecord(
                id = "att_1",
                employeeId = "EMP001",
                employeeName = "Worker EMP001",
                type = AttendanceType.PUNCH_IN,
                date = "2026-08-24",
                time = "09:00 AM",
                latitude = 22.5726,
                longitude = 88.3639,
                accuracy = 12.5f,
                localArea = "Sector V, Kolkata",
                syncStatus = SyncStatus.SYNCED
            ),
            AttendanceRecord(
                id = "att_2",
                employeeId = "EMP001",
                employeeName = "Worker EMP001",
                type = AttendanceType.PUNCH_OUT,
                date = "2026-08-24",
                time = "05:00 PM",
                latitude = 22.5726,
                longitude = 88.3639,
                accuracy = 15.0f,
                localArea = "Sector V, Kolkata",
                syncStatus = SyncStatus.PENDING
            )
        )

        // User is logged in
        fakeAuthRepository.loginWorker("EMP001", "password123")
        assertEquals(2, localRoomRecords.size)

        // User logs out
        fakeAuthRepository.logout()

        // Verify Room records are completely preserved
        assertEquals(2, localRoomRecords.size)
        val pendingRecord = localRoomRecords.firstOrNull { it.syncStatus == SyncStatus.PENDING }
        assertTrue(pendingRecord != null)
        assertEquals("att_2", pendingRecord?.id)
        assertEquals(SyncStatus.PENDING, pendingRecord?.syncStatus)
    }

    @Test
    fun testLogout_doesNotRequireNetwork() = runBlocking {
        fakeAuthRepository.loginWorker("EMP001", "password123")
        fakeAuthRepository.networkCalled = false

        // Offline logout
        fakeAuthRepository.logout()

        // Ensure network was never invoked
        assertFalse(fakeAuthRepository.networkCalled)
        val session = fakeSessionRepository.getActiveSession()
        assertFalse(session.isLoggedIn)
    }

    @Test
    fun testMainViewModel_resolvesToUnauthenticatedAfterLogout() = runBlocking {
        MainViewModel(fakeSessionRepository)

        // Initially unauthenticated
        val initialSession = fakeSessionRepository.sessionFlow.first()
        assertFalse(initialSession.isLoggedIn)

        // Worker logs in
        fakeAuthRepository.loginWorker("EMP001", "password123")
        val loggedInSession = fakeSessionRepository.sessionFlow.first()
        assertTrue(loggedInSession.isLoggedIn)

        // Worker logs out
        fakeAuthRepository.logout()
        val postLogoutSession = fakeSessionRepository.sessionFlow.first()
        assertFalse(postLogoutSession.isLoggedIn)
        assertEquals("", postLogoutSession.employeeId)
    }
}