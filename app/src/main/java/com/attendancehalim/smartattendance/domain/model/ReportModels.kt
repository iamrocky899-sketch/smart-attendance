package com.attendancehalim.smartattendance.domain.model

enum class ReportType(val title: String, val badge: String, val description: String) {
    DAILY("Daily Attendance Report", "DAILY", "Detailed records of daily check-ins, check-outs, GPS locations, and working duration."),
    MONTHLY("Monthly Attendance Register", "MONTHLY", "Worker-wise monthly summary of present days, not marked days, and total working hours."),
    WORKER("Worker Attendance Report", "WORKER", "Complete individual monthly attendance logs, GPS accuracy, and check-in timeline."),
    SUMMARY("Attendance Summary", "SUMMARY", "Centralized real-time overview of workforce attendance metrics and sync queues.");

    companion object {
        fun fromString(value: String): ReportType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

data class DailyReportItem(
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val inTime: String,
    val outTime: String?,
    val duration: String,
    val status: String,
    val inArea: String,
    val outArea: String,
    val inLat: Double = 0.0,
    val inLng: Double = 0.0,
    val inAccuracy: Float = 0f,
    val outLat: Double = 0.0,
    val outLng: Double = 0.0,
    val outAccuracy: Float = 0f,
    val attendanceType: String = "PUNCH_IN",
    val syncStatus: String = "SYNCED"
)

data class MonthlyWorkerSummary(
    val employeeId: String,
    val employeeName: String,
    val workplaceName: String = "",
    val presentDays: Int,
    val notMarkedDays: Int,
    val totalMinutes: Long,
    val totalHoursFormatted: String,
    val averageHoursFormatted: String
)

data class GeneratedReport(
    val type: ReportType,
    val title: String,
    val subtitle: String,
    val filterDate: String = "",
    val filterMonth: String = "",
    val generatedAt: String,
    val isLive: Boolean = true,
    val totalWorkers: Int = 0,
    val presentCount: Int = 0,
    val notMarkedCount: Int = 0,
    val totalHoursFormatted: String? = null,
    val averageHoursFormatted: String? = null,
    val items: List<DailyReportItem> = emptyList(),
    val workerSummaries: List<MonthlyWorkerSummary> = emptyList(),
    val workerProfile: WorkerProfile? = null
)
