package com.attendancehalim.smartattendance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySanitizationTest {

    private val sensitivePatterns = listOf(
        Regex("""(?i)"password"\s*:\s*"[^"]*""""),
        Regex("""(?i)"token"\s*:\s*"[^"]*""""),
        Regex("""(?i)"authToken"\s*:\s*"[^"]*""""),
        Regex("""(?i)"passwordHash"\s*:\s*"[^"]*""""),
        Regex("""(?i)Bearer\s+[A-Za-z0-9\-_=.]+"""),
        Regex("""(?i)token=[A-Za-z0-9\-_=.]+""")
    )

    private fun sanitize(input: String): String {
        var result = input
        for (pattern in sensitivePatterns) {
            result = pattern.replace(result) { matchResult ->
                val matched = matchResult.value
                if (matched.contains(":")) {
                    val prefix = matched.substringBefore(":")
                    """$prefix: "[REDACTED]""""
                } else if (matched.startsWith("Bearer", ignoreCase = true)) {
                    "Bearer [REDACTED]"
                } else if (matched.startsWith("token=", ignoreCase = true)) {
                    "token=[REDACTED]"
                } else {
                    "[REDACTED]"
                }
            }
        }
        return result
    }

    @Test
    fun testSanitize_redactsPassword() {
        val input = """{"action":"workerLogin","identifier":"EMP-0001","password":"SecretPassword123"}"""
        val sanitized = sanitize(input)
        assertFalse(sanitized.contains("SecretPassword123"))
        assertTrue(sanitized.contains(""""password": "[REDACTED]""""))
    }

    @Test
    fun testSanitize_redactsToken() {
        val input = """{"action":"listWorkers","token":"eyJhbGciOiJIUzI1NiJ9.superSecretToken"}"""
        val sanitized = sanitize(input)
        assertFalse(sanitized.contains("superSecretToken"))
        assertTrue(sanitized.contains(""""token": "[REDACTED]""""))
    }

    @Test
    fun testSanitize_redactsBearerTokenHeader() {
        val input = "Authorization: Bearer header.payload.signatureValue123"
        val sanitized = sanitize(input)
        assertFalse(sanitized.contains("signatureValue123"))
        assertTrue(sanitized.contains("Bearer [REDACTED]"))
    }

    @Test
    fun testSanitize_redactsTokenQueryParameter() {
        val input = "https://script.google.com/macros/s/exec?action=sync&token=secretSessionToken99"
        val sanitized = sanitize(input)
        assertFalse(sanitized.contains("secretSessionToken99"))
        assertTrue(sanitized.contains("token=[REDACTED]"))
    }

    @Test
    fun testSanitize_redactsMultipleSensitiveFields() {
        val input = """{"action":"sync","password":"myPass","token":"myToken","passwordHash":"hashVal"}"""
        val sanitized = sanitize(input)
        assertFalse(sanitized.contains("myPass"))
        assertFalse(sanitized.contains("myToken"))
        assertFalse(sanitized.contains("hashVal"))
        assertTrue(sanitized.contains(""""password": "[REDACTED]""""))
        assertTrue(sanitized.contains(""""token": "[REDACTED]""""))
        assertTrue(sanitized.contains(""""passwordHash": "[REDACTED]""""))
    }
}
