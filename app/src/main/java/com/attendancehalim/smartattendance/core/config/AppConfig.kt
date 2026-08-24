package com.attendancehalim.smartattendance.core.config

/**
 * Centralized Application Configuration.
 * Single source of truth for the Google Apps Script Web App Backend URL.
 */
object AppConfig {
    /**
     * Deployed Google Apps Script Web App Base URL.
     * Dedicated Account: attendancehalim@gmail.com
     * Timezone: Asia/Kolkata
     */
    const val BACKEND_BASE_URL = "https://script.google.com/macros/s/AKfycbziuxqqrju6XqJzjDXSWEwEI2lWVu3FcY9wBZK7Hesi9sl3z7Kscew9GG3kaoGtE-NdBQ/"
    
    const val BACKEND_EXEC_PATH = "exec"
}
