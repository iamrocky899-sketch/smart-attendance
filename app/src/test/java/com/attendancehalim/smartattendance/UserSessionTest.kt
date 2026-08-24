package com.attendancehalim.smartattendance

import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSessionTest {

    @Test
    fun testUserSession_defaults() {
        val session = UserSession()
        assertFalse(session.isLoggedIn)
        assertEquals("", session.employeeId)
        assertEquals(UserRole.WORKER, session.role)
        assertFalse(session.isTokenExpired)
    }

    @Test
    fun testUserSession_tokenExpiration_notExpired() {
        val futureExpiry = System.currentTimeMillis() + 3600000L // 1 hour in future
        val session = UserSession(
            isLoggedIn = true,
            employeeId = "EMP-0001",
            userName = "Rahul Das",
            tokenExpiry = futureExpiry
        )
        assertFalse(session.isTokenExpired)
    }

    @Test
    fun testUserSession_tokenExpiration_expired() {
        val pastExpiry = System.currentTimeMillis() - 1000L // 1 second in past
        val session = UserSession(
            isLoggedIn = true,
            employeeId = "EMP-0001",
            userName = "Rahul Das",
            tokenExpiry = pastExpiry
        )
        assertTrue(session.isTokenExpired)
    }

    @Test
    fun testUserRole_fromString() {
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
        assertEquals(UserRole.WORKER, UserRole.fromString("WORKER"))
        assertEquals(UserRole.WORKER, UserRole.fromString("unknown_role"))
    }
}
