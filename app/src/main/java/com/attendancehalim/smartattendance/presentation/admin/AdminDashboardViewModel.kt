package com.attendancehalim.smartattendance.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorkerTodayStatus(val title: String) {
    PRESENT("Present"),
    PUNCHED_IN("Punched In"),
    PUNCHED_OUT("Punched Out"),
    NOT_MARKED("Not Marked")
}

data class AdminTodayAttendanceRow(
    val employeeId: String,
    val employeeName: String,
    val workplaceName: String,
    val punchInTime: String?,
    val punchOutTime: String?,
    val status: WorkerTodayStatus,
    val locationSummary: String
)

data class AdminDashboardUiState(
    val session: UserSession = UserSession(),
    val todayDateFormatted: String = "",
    val totalWorkersCount: Int = 0,
    val presentTodayCount: Int = 0,
    val punchedInCount: Int = 0,
    val punchedOutCount: Int = 0,
    val notMarkedCount: Int = 0,
    val todayAttendanceList: List<AdminTodayAttendanceRow> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModel(
    private val sessionRepository: SessionRepository,
    private val workerRepository: WorkerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    private val _isLoggedOut = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            workerRepository.seedInitialWorkersIfEmpty()
            refresh()
        }

        // 10-second live refresh polling loop for Admin Dashboard
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(10_000L)
                refresh()
            }
        }
    }

    val uiState: StateFlow<AdminDashboardUiState> = sessionRepository.sessionFlow
        .flatMapLatest { session ->
            if (session.employeeId.isBlank()) {
                combine(_isLoggingOut, _isLoggedOut, _isRefreshing) { loggingOut, loggedOut, refreshing ->
                    AdminDashboardUiState(
                        session = session,
                        isRefreshing = refreshing,
                        isLoggingOut = loggingOut,
                        isLoggedOut = loggedOut
                    )
                }
            } else {
                val currentDate = DateTimeUtils.getCurrentDate()
                val workersFlow = workerRepository.getAllWorkers()
                val todayAttendanceFlow = attendanceRepository.getAttendanceForDate(currentDate)

                combine(
                    workersFlow,
                    todayAttendanceFlow,
                    _isLoggingOut,
                    _isLoggedOut,
                    _isRefreshing
                ) { workers, todayRecords, isLoggingOut, isLoggedOut, isRefreshing ->
                    calculateDashboardState(
                        session = session,
                        currentDate = currentDate,
                        workers = workers,
                        todayRecords = todayRecords,
                        loggingOut = isLoggingOut,
                        loggedOut = isLoggedOut,
                        refreshing = isRefreshing
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminDashboardUiState()
        )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                workerRepository.refreshWorkersFromRemote()
                attendanceRepository.refreshTodayAttendance()
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun calculateDashboardState(
        session: UserSession,
        currentDate: String,
        workers: List<WorkerProfile>,
        todayRecords: List<AttendanceRecord>,
        loggingOut: Boolean,
        loggedOut: Boolean,
        refreshing: Boolean = false
    ): AdminDashboardUiState {
        val totalWorkers = workers.size
        var presentCount = 0
        var punchedInCount = 0
        var punchedOutCount = 0
        var notMarkedCount = 0

        val rows = workers.map { worker ->
            val workerDayRecords = todayRecords.filter { it.employeeId == worker.employeeId }
            val punchIn = workerDayRecords.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
            val punchOut = workerDayRecords.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

            val status: WorkerTodayStatus = when {
                punchIn != null && punchOut != null -> {
                    presentCount++
                    punchedOutCount++
                    WorkerTodayStatus.PRESENT
                }
                punchIn != null && punchOut == null -> {
                    presentCount++
                    punchedInCount++
                    WorkerTodayStatus.PUNCHED_IN
                }
                else -> {
                    notMarkedCount++
                    WorkerTodayStatus.NOT_MARKED
                }
            }

            val location = when {
                punchIn != null && punchIn.localArea.isNotBlank() && punchIn.localArea != "Location name unavailable" -> punchIn.localArea
                punchOut != null && punchOut.localArea.isNotBlank() && punchOut.localArea != "Location name unavailable" -> punchOut.localArea
                punchIn != null -> punchIn.localArea
                else -> "No location"
            }

            AdminTodayAttendanceRow(
                employeeId = worker.employeeId,
                employeeName = worker.fullName,
                workplaceName = worker.workplaceName,
                punchInTime = punchIn?.time?.let { DateTimeUtils.formatTimeDisplay(it) },
                punchOutTime = punchOut?.time?.let { DateTimeUtils.formatTimeDisplay(it) },
                status = status,
                locationSummary = location
            )
        }

        return AdminDashboardUiState(
            session = session,
            todayDateFormatted = DateTimeUtils.formatFullDate(currentDate),
            totalWorkersCount = totalWorkers,
            presentTodayCount = presentCount,
            punchedInCount = punchedInCount,
            punchedOutCount = punchedOutCount,
            notMarkedCount = notMarkedCount,
            todayAttendanceList = rows,
            isRefreshing = refreshing,
            isLoggingOut = loggingOut,
            isLoggedOut = loggedOut
        )
    }

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
}
