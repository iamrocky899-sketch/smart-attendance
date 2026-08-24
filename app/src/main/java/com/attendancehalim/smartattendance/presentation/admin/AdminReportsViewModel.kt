package com.attendancehalim.smartattendance.presentation.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendancehalim.smartattendance.core.network.NetworkMonitor
import com.attendancehalim.smartattendance.core.report.ExcelReportGenerator
import com.attendancehalim.smartattendance.core.report.PdfReportGenerator
import com.attendancehalim.smartattendance.core.report.ReportFileHelper
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyReportItem
import com.attendancehalim.smartattendance.domain.model.GeneratedReport
import com.attendancehalim.smartattendance.domain.model.MonthlyWorkerSummary
import com.attendancehalim.smartattendance.domain.model.ReportType
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class AdminReportsUiState(
    val selectedReportType: ReportType = ReportType.DAILY,
    val filterDate: String = DateTimeUtils.getCurrentDate(),
    val filterMonth: String = DateTimeUtils.getCurrentDate().take(7),
    val selectedWorkerId: String? = null,
    val searchQuery: String = "",
    val workers: List<WorkerProfile> = emptyList(),
    val filteredWorkers: List<WorkerProfile> = emptyList(),
    val generatedReport: GeneratedReport? = null,
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val isExportingExcel: Boolean = false,
    val exportedFile: File? = null,
    val exportedMimeType: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val totalRegisteredWorkers: Int = 0,
    val totalAttendanceRecords: Int = 0,
    val pendingSyncCount: Int = 0
)

class AdminReportsViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val workerRepository: WorkerRepository,
    private val sessionRepository: SessionRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Sync latest data from backend if online
            if (networkMonitor.isCurrentlyOnline()) {
                workerRepository.refreshWorkersFromRemote()
            }

            val workersList = workerRepository.getAllWorkers().first()
            val allAttendance = attendanceRepository.getAllAttendance().first()
            val pendingCount = attendanceRepository.getPendingCount().first()

            val initialWorkerId = workersList.firstOrNull()?.employeeId

            _uiState.update {
                it.copy(
                    workers = workersList,
                    filteredWorkers = workersList,
                    selectedWorkerId = initialWorkerId,
                    totalRegisteredWorkers = workersList.size,
                    totalAttendanceRecords = allAttendance.size,
                    pendingSyncCount = pendingCount,
                    isLoading = false
                )
            }

            generateReport()
        }
    }

    fun setReportType(type: ReportType) {
        _uiState.update { it.copy(selectedReportType = type, errorMessage = null) }
        generateReport()
    }

    fun setDateFilter(date: String) {
        _uiState.update { it.copy(filterDate = date, errorMessage = null) }
        generateReport()
    }

    fun setMonthFilter(month: String) {
        _uiState.update { it.copy(filterMonth = month, errorMessage = null) }
        generateReport()
    }

    fun setSelectedWorker(workerId: String?) {
        _uiState.update { it.copy(selectedWorkerId = workerId, errorMessage = null) }
        generateReport()
    }

    fun onSearchQueryChanged(query: String) {
        val workers = _uiState.value.workers
        val filtered = if (query.isBlank()) {
            workers
        } else {
            workers.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.employeeId.contains(query, ignoreCase = true) ||
                it.workplaceName.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(searchQuery = query, filteredWorkers = filtered) }
    }

    fun generateReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val state = _uiState.value
                val workers = workerRepository.getAllWorkers().first()
                val allAttendance = attendanceRepository.getAllAttendance().first()
                val isLive = networkMonitor.isCurrentlyOnline()

                val report = when (state.selectedReportType) {
                    ReportType.DAILY -> buildDailyReport(state.filterDate, workers, allAttendance, isLive)
                    ReportType.MONTHLY -> buildMonthlyReport(state.filterMonth, workers, allAttendance, isLive)
                    ReportType.WORKER -> buildWorkerReport(state.selectedWorkerId, state.filterMonth, workers, allAttendance, isLive)
                    ReportType.SUMMARY -> buildSummaryReport(state.filterDate, workers, allAttendance, isLive)
                }

                _uiState.update {
                    it.copy(
                        generatedReport = report,
                        isLoading = false,
                        totalRegisteredWorkers = workers.size,
                        totalAttendanceRecords = allAttendance.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate report: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun buildDailyReport(
        date: String,
        workers: List<WorkerProfile>,
        allAttendance: List<AttendanceRecord>,
        isLive: Boolean
    ): GeneratedReport {
        val dayRecords = allAttendance.filter { it.date == date }
        val groupedByWorker = dayRecords.groupBy { it.employeeId }

        val items = mutableListOf<DailyReportItem>()
        var presentCount = 0

        // Iterate over registered workers to show both present and absent/not marked
        for (w in workers) {
            val recs = groupedByWorker[w.employeeId]
            if (recs != null && recs.isNotEmpty()) {
                presentCount++
                val punchIn = recs.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
                val punchOut = recs.firstOrNull { it.type == AttendanceType.PUNCH_OUT }

                val duration = if (punchIn != null && punchOut != null) {
                    DateTimeUtils.calculateDuration(punchIn.createdAt, punchOut.createdAt, punchIn.time, punchOut.time)
                } else if (punchIn != null) {
                    if (date == DateTimeUtils.getCurrentDate()) "In Progress" else "Incomplete"
                } else {
                    "--"
                }

                val status = if (punchIn?.type == AttendanceType.MANUAL) {
                    "MANUAL"
                } else if (punchIn != null && punchOut != null) {
                    "PRESENT"
                } else if (punchIn != null) {
                    "INCOMPLETE"
                } else {
                    "PUNCH_OUT"
                }

                val inArea = punchIn?.localArea?.ifBlank { "Location unavailable" } ?: "Location unavailable"
                val outArea = punchOut?.localArea?.ifBlank { "Location unavailable" } ?: "--"

                items.add(
                    DailyReportItem(
                        employeeId = w.employeeId,
                        employeeName = w.fullName,
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
            } else {
                // Not Marked
                items.add(
                    DailyReportItem(
                        employeeId = w.employeeId,
                        employeeName = w.fullName,
                        date = date,
                        inTime = "--:--",
                        outTime = null,
                        duration = "0h 00m",
                        status = "NOT MARKED",
                        inArea = "--",
                        outArea = "--",
                        attendanceType = "ABSENT",
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        val notMarked = workers.size - presentCount

        return GeneratedReport(
            type = ReportType.DAILY,
            title = "DAILY ATTENDANCE REPORT",
            subtitle = "Date: ${DateTimeUtils.formatFullDate(date)}",
            filterDate = date,
            filterMonth = date.take(7),
            generatedAt = DateTimeUtils.getCurrentTime() + " " + DateTimeUtils.formatShortDate(date),
            isLive = isLive,
            totalWorkers = workers.size,
            presentCount = presentCount,
            notMarkedCount = notMarked.coerceAtLeast(0),
            items = items
        )
    }

    private fun buildMonthlyReport(
        month: String,
        workers: List<WorkerProfile>,
        allAttendance: List<AttendanceRecord>,
        isLive: Boolean
    ): GeneratedReport {
        val monthRecords = allAttendance.filter { it.date.startsWith(month) }
        val byWorker = monthRecords.groupBy { it.employeeId }

        val workerSummaries = mutableListOf<MonthlyWorkerSummary>()
        var totalPresentDays = 0

        for (w in workers) {
            val recs = byWorker[w.employeeId] ?: emptyList()
            val daysMap = recs.groupBy { it.date }
            val presentDaysCount = daysMap.size
            totalPresentDays += presentDaysCount

            var totalMinutes = 0L
            for ((_, dayRecs) in daysMap) {
                val punchIn = dayRecs.firstOrNull { it.type == AttendanceType.PUNCH_IN || it.type == AttendanceType.MANUAL }
                val punchOut = dayRecs.firstOrNull { it.type == AttendanceType.PUNCH_OUT }
                if (punchIn != null && punchOut != null) {
                    val diff = (punchOut.createdAt - punchIn.createdAt).coerceAtLeast(0)
                    totalMinutes += diff / (1000 * 60)
                } else if (punchIn != null) {
                    totalMinutes += 480 // default 8 hours credit for recorded shifts
                }
            }

            val totalHours = totalMinutes / 60
            val remainingMins = totalMinutes % 60
            val totalHoursStr = "${totalHours}h ${remainingMins}m"

            val avgHoursPerDay = if (presentDaysCount > 0) {
                val avgMins = totalMinutes / presentDaysCount
                "${avgMins / 60}h ${avgMins % 60}m"
            } else {
                "0h 00m"
            }

            workerSummaries.add(
                MonthlyWorkerSummary(
                    employeeId = w.employeeId,
                    employeeName = w.fullName,
                    workplaceName = w.workplaceName,
                    presentDays = presentDaysCount,
                    notMarkedDays = (30 - presentDaysCount).coerceAtLeast(0),
                    totalMinutes = totalMinutes,
                    totalHoursFormatted = totalHoursStr,
                    averageHoursFormatted = avgHoursPerDay
                )
            )
        }

        return GeneratedReport(
            type = ReportType.MONTHLY,
            title = "MONTHLY ATTENDANCE REGISTER",
            subtitle = "Period: ${DateTimeUtils.formatMonthYear(month + "-01")}",
            filterDate = month + "-01",
            filterMonth = month,
            generatedAt = DateTimeUtils.getCurrentTime() + " " + DateTimeUtils.formatShortDate(DateTimeUtils.getCurrentDate()),
            isLive = isLive,
            totalWorkers = workers.size,
            presentCount = totalPresentDays,
            notMarkedCount = (workers.size * 26 - totalPresentDays).coerceAtLeast(0),
            totalHoursFormatted = "${(totalPresentDays * 8)}h",
            workerSummaries = workerSummaries
        )
    }

    private fun buildWorkerReport(
        targetWorkerId: String?,
        month: String,
        workers: List<WorkerProfile>,
        allAttendance: List<AttendanceRecord>,
        isLive: Boolean
    ): GeneratedReport {
        val worker = workers.find { it.employeeId.equals(targetWorkerId, ignoreCase = true) } ?: workers.firstOrNull()
        val workerId = worker?.employeeId ?: ""

        val workerMonthRecords = allAttendance.filter {
            it.employeeId.equals(workerId, ignoreCase = true) && it.date.startsWith(month)
        }
        val byDate = workerMonthRecords.groupBy { it.date }.toSortedMap(compareByDescending { it })

        val items = mutableListOf<DailyReportItem>()
        var presentDays = 0

        for ((date, dayRecs) in byDate) {
            presentDays++
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
                    employeeId = workerId,
                    employeeName = worker?.fullName ?: workerId,
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
            title = "INDIVIDUAL WORKER REPORT",
            subtitle = "Worker: ${worker?.fullName ?: "N/A"} (${worker?.employeeId ?: ""}) • ${DateTimeUtils.formatMonthYear(month + "-01")}",
            filterDate = month + "-01",
            filterMonth = month,
            generatedAt = DateTimeUtils.getCurrentTime() + " " + DateTimeUtils.formatShortDate(DateTimeUtils.getCurrentDate()),
            isLive = isLive,
            totalWorkers = 1,
            presentCount = presentDays,
            notMarkedCount = (26 - presentDays).coerceAtLeast(0),
            totalHoursFormatted = "${presentDays * 8}h",
            items = items,
            workerProfile = worker
        )
    }

    private fun buildSummaryReport(
        date: String,
        workers: List<WorkerProfile>,
        allAttendance: List<AttendanceRecord>,
        isLive: Boolean
    ): GeneratedReport {
        return buildDailyReport(date, workers, allAttendance, isLive).copy(
            type = ReportType.SUMMARY,
            title = "ATTENDANCE METRICS SUMMARY"
        )
    }

    fun exportPdf(context: Context) {
        val report = _uiState.value.generatedReport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true, errorMessage = null) }
            try {
                val file = PdfReportGenerator.generatePdf(context, report)
                _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        exportedFile = file,
                        exportedMimeType = "application/pdf",
                        successMessage = "PDF report generated successfully: ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        errorMessage = "PDF export failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun exportExcel(context: Context) {
        val report = _uiState.value.generatedReport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingExcel = true, errorMessage = null) }
            try {
                val file = ExcelReportGenerator.generateExcel(context, report)
                _uiState.update {
                    it.copy(
                        isExportingExcel = false,
                        exportedFile = file,
                        exportedMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        successMessage = "Excel report generated successfully: ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingExcel = false,
                        errorMessage = "Excel export failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun openExportedFile(context: Context) {
        val file = _uiState.value.exportedFile ?: return
        val mime = _uiState.value.exportedMimeType ?: "application/pdf"
        ReportFileHelper.openFile(context, file, mime)
    }

    fun shareExportedFile(context: Context) {
        val file = _uiState.value.exportedFile ?: return
        val mime = _uiState.value.exportedMimeType ?: "application/pdf"
        val title = _uiState.value.generatedReport?.title ?: "Attendance Report"
        ReportFileHelper.shareFile(context, file, mime, title)
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportedFile = null, exportedMimeType = null, successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
