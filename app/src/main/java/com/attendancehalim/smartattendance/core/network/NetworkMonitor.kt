package com.attendancehalim.smartattendance.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Utility interface for monitoring device network connectivity state.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    fun isCurrentlyOnline(): Boolean
}
