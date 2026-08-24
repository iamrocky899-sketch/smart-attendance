package com.attendancehalim.smartattendance.presentation.worker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.report.ExcelReportGenerator
import com.attendancehalim.smartattendance.core.report.PdfReportGenerator
import com.attendancehalim.smartattendance.core.report.ReportFileHelper
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyAttendanceRegister
import com.attendancehalim.smartattendance.domain.model.DailyReportItem
import com.attendancehalim.smartattendance.domain.model.GeneratedReport
import com.attendancehalim.smartattendance.domain.model.ReportType
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class WorkerMyAttendanceUiState(
    val session: UserSession = UserSession(),
    val groupedRecords: Map<String, List<DailyAttendanceRegister>> = emptyMap(),
    val totalDaysRecorded: Int = 0,
    val pendingSyncCount: Int = 0,
    val selectedItem: DailyAttendanceRegister? = null,
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val isExportingExcel: Boolean = false,
    val exportedFile: File? = null,
    val exportedMimeType: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerMyAttendanceViewModel(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<DailyAttendanceRegister?>(null)
    private val _isExportingPdf = MutableStateFlow(false)
    private val _isExportingExcel = MutableStateFlow(false)
    private val _exportedFile = MutableStateFlow<File?>(null)
    private val _exportedMimeType = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorkerMyAttendanceUiState> = sessionRepository.sessionFlow
        .flatMapLatest { session ->
            if (session.employeeId.isBlank()) {
                flowOf(WorkerMyAttendanceUiState(session = session))
            } else {
                val recordsFlow = attendanceRepository.getAttendanceForWorker(session.employeeId)
                val pendingCountFlow = attendanceRepository.getPendingCount()

                combine(
                    recordsFlow,
                    pendingCountFlow,
                    _selectedItem,
                    _isExportingPdf,
                    _isExportingExcel,
                    _exportedFile,
                    _exportedMimeType,
                    _errorMessage,
                    _successMessage
                ) { params: Array<Any?> ->
                    @Suppress("UNCHECKED_CAST")
                    val rawRecords = params[0] as List<AttendanceRecord>
                    val pendingCount = params[1] as Int
                    val selected = params[2] as DailyAttendanceRegister?
                    val exportingPdf = params[3] as Boolean
                    val exportingExcel = params[4] as Boolean
                    val file = params[5] as File?
                    val mime = params[6] as String?
                    val error = params[7] as String?
                    val success = params[8] as String?

                    val dailyList = groupRecordsIntoDailyRegister(rawRecords)
                    val groupedByMonth = dailyList.groupBy { it.monthHeader }

                    WorkerMyAttendanceUiState(
                        session = session,
                        groupedRecords = groupedByMonth,
                        totalDaysRecorded = dailyList.size,
                        pendingSyncCount = pendingCount,
                        selectedItem = selected,
                        isLoading = false,
                        isExportingPdf = exportingPdf,
                        isExportingExcel = exportingExcel,
                        exportedFile = file,
                        exportedMimeType = mime,
                        errorMessage = error,
                        successMessage = success
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkerMyAttendanceUiState(isLoading = true)
        )

    fun selectItem(item: DailyAttendanceRegister) {
        _selectedItem.value = item
    }

    fun clearSelectedItem() {
        _selectedItem.value = null
    }

    fun exportMyPdf(context: Context) {
        val session = uiState.value.session
        if (session.employeeId.isBlank()) return

        viewModelScope.launch {
            _isExportingPdf.value = true
            _errorMessage.value = null

            try {
                val records = attendanceRepository.getAttendanceForWorker(session.employeeId).first()
                val report = buildMyWorkerReport(session, records)
                val file = PdfReportGenerator.generatePdf(context, report)

                _isExportingPdf.value = false
                _exportedFile.value = file
                _exportedMimeType.value = "application/pdf"
                _successMessage.value = "My Attendance PDF ready: ${file.name}"
            } catch (e: Exception) {
                _isExportingPdf.value = false
                _errorMessage.value = "Failed to export PDF: ${e.localizedMessage}"
            }
        }
    }

    fun exportMyExcel(context: Context) {
        val session = uiState.value.session
        if (session.employeeId.isBlank()) return

        viewModelScope.launch {
            _isExportingExcel.value = true
            _errorMessage.value = null

            try {
                val records = attendanceRepository.getAttendanceForWorker(session.employeeId).first()
                val report = buildMyWorkerReport(session, records)
                val file = ExcelReportGenerator.generateExcel(context, report)

                _isExportingExcel.value = false
                _exportedFile.value = file
                _exportedMimeType.value = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                _successMessage.value = "My Attendance Excel ready: ${file.name}"
            } catch (e: Exception) {
                _isExportingExcel.value = false
                _errorMessage.value = "Failed to export Excel: ${e.localizedMessage}"
            }
        }
    }

    fun openExportedFile(context: Context) {
        val file = _exportedFile.value ?: return
        val mime = _exportedMimeType.value ?: "application/pdf"
        ReportFileHelper.openFile(context, file, mime)
    }

    fun shareExportedFile(context: Context) {
        val file = _exportedFile.value ?: return
        val mime = _exportedMimeType.value ?: "application/pdf"
        ReportFileHelper.shareFile(context, file, mime, "My Attendance Register")
    }

    fun dismissExportDialog() {
        _exportedFile.value = null
        _exportedMimeType.value = null
        _successMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private fun buildMyWorkerReport(
        session: UserSession,
        records: List<AttendanceRecord>
    ): GeneratedReport {
        val byDate = records.groupBy { it.date }.toSortedMap(compareByDescending { it })
        val items = mutableListOf<DailyReportItem>()

        for ((date, dayRecs) in byDate) {
            val punchIn = dayRecs.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
            val punchOut = dayRecs.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

            val duration = if (punchIn != null && punchOut != null) {
                DateTimeUtils.calculateDuration(punchIn.createdAt, punchOut.createdAt, punchIn.time, punchOut.time)
            } else if (punchIn != null) {
                "Incomplete"
            } else {
                "--"
            }

            val status = if (punchIn?.type == AttendanceType.MANUAL) "MANUAL" else if (punchIn != null && punchOut != null) "PRESENT" else "INCOMPLETE"
            val inArea = punchIn?.localArea?.ifBlank { "Location unavailable" } ?: "Location unavailable"
            val outArea = punchOut?.localArea?.ifBlank { "Location unavailable" } ?: "--"

            items.add(
                DailyReportItem(
                    employeeId = session.employeeId,
                    employeeName = session.userName,
                    date = date,
                    inTime = punchIn?.time ?: "--:--",
                    outTime = punchOut?.time,
                    duration = duration,
                    status = status,
                    inArea = inArea,
                    outArea = outArea,
                    inLat = punchIn?.latitude ?: 0.0,
                    inLng = punchIn?.longitude ?: 0.0,
                    inAccuracy = punchIn?.accuracy ?: 0f,
                    outLat = punchOut?.latitude ?: 0.0,
                    outLng = punchOut?.longitude ?: 0.0,
                    outAccuracy = punchOut?.accuracy ?: 0f,
                    attendanceType = punchIn?.type?.name ?: "PUNCH_IN",
                    syncStatus = punchIn?.syncStatus?.name ?: "SYNCED"
                )
            )
        }

        return GeneratedReport(
            type = ReportType.WORKER,
            title = "MY ATTENDANCE REGISTER",
            subtitle = "Employee: ${session.userName} (${session.employeeId}) • ${session.workplaceName}",
            filterDate = DateTimeUtils.getCurrentDate(),
            filterMonth = DateTimeUtils.getCurrentDate().take(7),
            generatedAt = DateTimeUtils.getCurrentTime() + " " + DateTimeUtils.formatShortDate(DateTimeUtils.getCurrentDate()),
            isLive = true,
            totalWorkers = 1,
            presentCount = items.size,
            notMarkedCount = 0,
            totalHoursFormatted = "${items.size * 8}h",
            items = items
        )
    }

    private fun groupRecordsIntoDailyRegister(records: List<AttendanceRecord>): List<DailyAttendanceRegister> {
        val todayDate = DateTimeUtils.getCurrentDate()
        val byDate = records.groupBy { it.date }
        val sortedDates = byDate.keys.sortedDescending()

        return sortedDates.map { date ->
            val dayRecords = byDate[date] ?: emptyList()
            val punchIn = dayRecords.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
            val punchOut = dayRecords.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

            val duration: String? = when {
                punchIn != null && punchOut != null -> {
                    DateTimeUtils.calculateDuration(punchIn.createdAt, punchOut.createdAt, punchIn.time, punchOut.time)
                }
                punchIn != null -> {
                    if (date == todayDate) "In Progress" else "Incomplete"
                }
                punchOut != null -> "Punch Out Only"
                else -> null
            }

            val locationSummary = when {
                punchIn != null && punchIn.localArea.isNotBlank() && punchIn.localArea != "Location name unavailable" -> punchIn.localArea
                punchOut != null && punchOut.localArea.isNotBlank() && punchOut.localArea != "Location name unavailable" -> punchOut.localArea
                punchIn != null -> punchIn.localArea
                punchOut != null -> punchOut.localArea
                else -> "Location unavailable"
            }

            val overallSync = when {
                dayRecords.all { it.syncStatus == SyncStatus.SYNCED } -> SyncStatus.SYNCED
                dayRecords.any { it.syncStatus == SyncStatus.FAILED } -> SyncStatus.FAILED
                else -> SyncStatus.PENDING
            }

            DailyAttendanceRegister(
                date = date,
                displayDay = DateTimeUtils.formatShortDate(date),
                fullDateDisplay = DateTimeUtils.formatFullDate(date),
                monthHeader = DateTimeUtils.formatMonthYear(date),
                punchInRecord = punchIn,
                punchOutRecord = punchOut,
                workingDuration = duration,
                locationSummary = locationSummary,
                overallSyncStatus = overallSync
            )
        }
    }
}
