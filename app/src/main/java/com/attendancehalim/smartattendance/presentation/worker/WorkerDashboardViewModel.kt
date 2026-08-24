package com.attendancehalim.smartattendance.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TodayAttendanceStatus(val displayTitle: String, val description: String) {
    NOT_MARKED("Not Marked", "You haven't marked attendance today"),
    PUNCHED_IN("Punched In", "You are currently on duty"),
    PUNCHED_OUT("Punched Out", "You have completed your shift")
}

data class WorkerDashboardUiState(
    val session: UserSession = UserSession(),
    val todayDateFormatted: String = "",
    val todayStatus: TodayAttendanceStatus = TodayAttendanceStatus.NOT_MARKED,
    val lastPunchTime: String? = null,
    val lastPunchType: AttendanceType? = null,
    val pendingSyncCount: Int = 0,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerDashboardViewModel(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    private val _isLoggedOut = MutableStateFlow(false)

    val uiState: StateFlow<WorkerDashboardUiState> = sessionRepository.sessionFlow
        .flatMapLatest { session ->
            if (session.employeeId.isBlank()) {
                combine(_isLoggingOut, _isLoggedOut) { loggingOut, loggedOut ->
                    WorkerDashboardUiState(
                        session = session,
                        isLoggingOut = loggingOut,
                        isLoggedOut = loggedOut
                    )
                }
            } else {
                val currentDate = DateTimeUtils.getCurrentDate()
                val todayRecordsFlow = attendanceRepository.getTodayAttendanceForWorker(
                    employeeId = session.employeeId,
                    date = currentDate
                )
                val pendingCountFlow = attendanceRepository.getPendingCount()

                combine(
                    todayRecordsFlow,
                    pendingCountFlow,
                    _isLoggingOut,
                    _isLoggedOut
                ) { todayRecords, pendingCount, loggingOut, loggedOut ->
                    val status = deriveTodayStatus(todayRecords)
                    val lastRecord = todayRecords.maxByOrNull { it.createdAt }

                    WorkerDashboardUiState(
                        session = session,
                        todayDateFormatted = DateTimeUtils.getDisplayDate(currentDate),
                        todayStatus = status,
                        lastPunchTime = lastRecord?.time,
                        lastPunchType = lastRecord?.type,
                        pendingSyncCount = pendingCount,
                        isLoggingOut = loggingOut,
                        isLoggedOut = loggedOut
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkerDashboardUiState()
        )

    fun logout(onSuccess: (() -> Unit)? = null) {
        if (_isLoggingOut.value) return
        _isLoggingOut.value = true

        viewModelScope.launch {
            try {
                authRepository.logout()
            } finally {
                _isLoggingOut.value = false
                _isLoggedOut.value = true
                onSuccess?.invoke()
            }
        }
    }

    private fun deriveTodayStatus(records: List<AttendanceRecord>): TodayAttendanceStatus {
        if (records.isEmpty()) return TodayAttendanceStatus.NOT_MARKED
        val latest = records.maxByOrNull { it.createdAt } ?: return TodayAttendanceStatus.NOT_MARKED
        return when (latest.type) {
            AttendanceType.PUNCH_IN, AttendanceType.MANUAL -> TodayAttendanceStatus.PUNCHED_IN
            AttendanceType.PUNCH_OUT -> TodayAttendanceStatus.PUNCHED_OUT
        }
    }
}
