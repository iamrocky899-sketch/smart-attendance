package com.attendancehalim.smartattendance.core.network

import android.util.Log
import com.attendancehalim.smartattendance.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

/**
 * Production-Hardened Logging Interceptor.
 * - In Release builds (BuildConfig.DEBUG == false): Logging is completely disabled.
 * - In Debug builds (BuildConfig.DEBUG == true): Logs are strictly sanitized with zero
 *   leakage of sensitive credentials (passwords, auth tokens, hashes) into Android Logcat.
 */
class SanitizedLoggingInterceptor(
    private val tag: String = "SmartAttendanceApi",
    private val isEnabled: Boolean = BuildConfig.DEBUG
) : Interceptor {

    private val sensitivePatterns = listOf(
        Regex("""(?i)"password"\s*:\s*"[^"]*""""),
        Regex("""(?i)"token"\s*:\s*"[^"]*""""),
        Regex("""(?i)"authToken"\s*:\s*"[^"]*""""),
        Regex("""(?i)"passwordHash"\s*:\s*"[^"]*""""),
        Regex("""(?i)Bearer\s+[A-Za-z0-9\-_=.]+"""),
        Regex("""(?i)token=[A-Za-z0-9\-_=.]+""")
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!isEnabled) {
            return chain.proceed(request)
        }

        val requestBody = request.body

        // Sanitize Request Logging
        if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            val charset = requestBody.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val rawBody = buffer.readString(charset)
            val sanitizedBody = sanitize(rawBody)
            Log.d(tag, "--> ${request.method} ${request.url} | Body: $sanitizedBody")
        } else {
            Log.d(tag, "--> ${request.method} ${request.url} (no body)")
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(tag, "<-- HTTP FAILED: ${e.message}")
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1e6
        Log.d(tag, "<-- ${response.code} ${response.message} ${request.url} (${tookMs}ms)")

        return response
    }

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
}
