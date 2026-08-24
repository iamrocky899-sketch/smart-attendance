package com.attendancehalim.smartattendance.presentation.admin

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.DailyReportItem
import com.attendancehalim.smartattendance.domain.model.GeneratedReport
import com.attendancehalim.smartattendance.domain.model.MonthlyWorkerSummary
import com.attendancehalim.smartattendance.domain.model.ReportType
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.presentation.components.AppOutlinedButton
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyDark
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PendingYellow
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportDetailScreen(
    uiState: AdminReportsUiState,
    onDateChange: (String) -> Unit,
    onMonthChange: (String) -> Unit,
    onWorkerSelect: (String?) -> Unit,
    onRefresh: () -> Unit,
    onExportPdf: (Context) -> Unit,
    onExportExcel: (Context) -> Unit,
    onOpenFile: (Context) -> Unit,
    onShareFile: (Context) -> Unit,
    onDismissExportDialog: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var workerDropdownExpanded by remember { mutableStateOf(false) }

    // Export Complete Modal Dialog
    if (uiState.exportedFile != null) {
        AlertDialog(
            onDismissRequest = onDismissExportDialog,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PunchInGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Report Generated",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File successfully generated and saved to device:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.exportedFile.name,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                AppButton(
                    text = "Open",
                    onClick = { onOpenFile(context) },
                    icon = Icons.AutoMirrored.Filled.OpenInNew
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(
                        text = "Share",
                        onClick = { onShareFile(context) },
                        icon = Icons.Default.Share
                    )
                    TextButton(onClick = onDismissExportDialog) {
                        Text("Done", color = TextSecondary)
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP NAVY HEADER with status bar insets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SurfaceWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.selectedReportType.title,
                        color = SurfaceWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.selectedReportType.badge + " VIEW",
                        color = SurfaceWhite.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Live / Cached Indicator
                val isLive = uiState.generatedReport?.isLive ?: false
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLive) PunchInGreen.copy(alpha = 0.2f) else PendingYellow.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLive) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isLive) PunchInGreen else PendingYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLive) "LIVE" else "CACHED",
                            color = if (isLive) PunchInGreen else PendingYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BODY with navigation bar and keyboard IME insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. FILTER CONFIGURATION CARD
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Report Filters & Configuration",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    when (uiState.selectedReportType) {
                        ReportType.DAILY, ReportType.SUMMARY -> {
                            OutlinedTextField(
                                value = uiState.filterDate,
                                onValueChange = onDateChange,
                                label = { Text("Selected Date (YYYY-MM-DD)") },
                                placeholder = { Text("e.g. 2026-08-24") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = NavyPrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        ReportType.MONTHLY -> {
                            OutlinedTextField(
                                value = uiState.filterMonth,
                                onValueChange = onMonthChange,
                                label = { Text("Selected Month (YYYY-MM)") },
                                placeholder = { Text("e.g. 2026-08") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = NavyPrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        ReportType.WORKER -> {
                            OutlinedTextField(
                                value = uiState.filterMonth,
                                onValueChange = onMonthChange,
                                label = { Text("Selected Month (YYYY-MM)") },
                                placeholder = { Text("e.g. 2026-08") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = NavyPrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Worker Dropdown Selector
                            ExposedDropdownMenuBox(
                                expanded = workerDropdownExpanded,
                                onExpandedChange = { workerDropdownExpanded = !workerDropdownExpanded }
                            ) {
                                val currentWorker = uiState.workers.find { it.employeeId == uiState.selectedWorkerId }
                                OutlinedTextField(
                                    value = if (currentWorker != null) "${currentWorker.fullName} (${currentWorker.employeeId})" else "Select Worker",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Worker") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = workerDropdownExpanded,
                                    onDismissRequest = { workerDropdownExpanded = false }
                                ) {
                                    uiState.workers.forEach { w ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("${w.fullName} (${w.employeeId})", fontWeight = FontWeight.Bold)
                                                    Text("${w.workplaceName} • ${w.mobileNumber}", fontSize = 12.sp, color = TextSecondary)
                                                }
                                            },
                                            onClick = {
                                                onWorkerSelect(w.employeeId)
                                                workerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Refresh Button
                    AppOutlinedButton(
                        text = "Refresh / Apply Filter",
                        onClick = onRefresh,
                        icon = Icons.Default.Refresh,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. EXPORT ACTION BUTTONS
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Export Options (Local Generation)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppButton(
                            text = "Export PDF",
                            onClick = { onExportPdf(context) },
                            icon = Icons.Default.PictureAsPdf,
                            isLoading = uiState.isExportingPdf,
                            modifier = Modifier.weight(1f)
                        )

                        AppButton(
                            text = "Export Excel",
                            onClick = { onExportExcel(context) },
                            icon = Icons.Default.TableChart,
                            isLoading = uiState.isExportingExcel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. REPORT PREVIEW
            val report = uiState.generatedReport
            if (report != null) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Title & Subtitle Banner
                        Column {
                            Text(
                                text = report.title,
                                color = NavyPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = report.subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Generated: ${report.generatedAt}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        HorizontalDivider(color = SurfaceBorder)

                        // KPI Overview Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiPreviewBox(
                                label = "WORKERS",
                                value = "${report.totalWorkers}",
                                color = NavyPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiPreviewBox(
                                label = "PRESENT",
                                value = "${report.presentCount}",
                                color = PunchInGreen,
                                modifier = Modifier.weight(1f)
                            )
                            KpiPreviewBox(
                                label = "NOT MARKED",
                                value = "${report.notMarkedCount}",
                                color = if (report.notMarkedCount > 0) PunchOutRed else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiPreviewBox(
                                label = "TOTAL HRS",
                                value = report.totalHoursFormatted ?: "--",
                                color = BlueAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = SurfaceBorder)

                        // Data Table View
                        Text(
                            text = "ATTENDANCE TABLE PREVIEW",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        if (report.type == ReportType.MONTHLY && report.workerSummaries.isNotEmpty()) {
                            MonthlyTablePreview(report.workerSummaries)
                        } else if (report.items.isNotEmpty()) {
                            DailyTablePreview(report.items, isWorkerReport = report.type == ReportType.WORKER)
                        } else {
                            Text(
                                text = "No records found matching filters.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KpiPreviewBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DailyTablePreview(
    items: List<DailyReportItem>,
    isWorkerReport: Boolean
) {
    val hScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScrollState)
            .background(SurfaceWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SurfaceBorder), RoundedCornerShape(8.dp))
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .background(NavyPrimary)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = if (isWorkerReport) "Date" else "ID", width = 80.dp, isHeader = true)
            TableCell(text = "Name", width = 110.dp, isHeader = true)
            TableCell(text = "In", width = 60.dp, isHeader = true)
            TableCell(text = "Out", width = 60.dp, isHeader = true)
            TableCell(text = "Duration", width = 70.dp, isHeader = true)
            TableCell(text = "Status", width = 75.dp, isHeader = true)
            TableCell(text = "Location & GPS", width = 160.dp, isHeader = true)
        }

        // Table Rows
        items.forEachIndexed { index, item ->
            val bg = if (index % 2 == 0) SurfaceWhite else SurfaceCard
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = if (isWorkerReport) item.date else item.employeeId, width = 80.dp, isBold = true)
                TableCell(text = item.employeeName, width = 110.dp)
                TableCell(text = item.inTime, width = 60.dp, textColor = PunchInGreen)
                TableCell(text = item.outTime ?: "--:--", width = 60.dp, textColor = if (item.outTime != null) PunchOutRed else TextMuted)
                TableCell(text = item.duration, width = 70.dp)
                StatusBadgeCell(status = item.status, width = 75.dp)
                TableCell(text = item.inArea, width = 160.dp, fontSize = 10.sp)
            }
            if (index < items.size - 1) {
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun MonthlyTablePreview(
    summaries: List<MonthlyWorkerSummary>
) {
    val hScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScrollState)
            .background(SurfaceWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, SurfaceBorder), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .background(NavyPrimary)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "Employee ID", width = 90.dp, isHeader = true)
            TableCell(text = "Name", width = 120.dp, isHeader = true)
            TableCell(text = "Workplace", width = 100.dp, isHeader = true)
            TableCell(text = "Present", width = 70.dp, isHeader = true)
            TableCell(text = "Not Marked", width = 80.dp, isHeader = true)
            TableCell(text = "Total Hours", width = 90.dp, isHeader = true)
        }

        summaries.forEachIndexed { index, sum ->
            val bg = if (index % 2 == 0) SurfaceWhite else SurfaceCard
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = sum.employeeId, width = 90.dp, isBold = true)
                TableCell(text = sum.employeeName, width = 120.dp)
                TableCell(text = sum.workplaceName.ifBlank { "Main Facility" }, width = 100.dp)
                TableCell(text = "${sum.presentDays} Days", width = 70.dp, textColor = PunchInGreen, isBold = true)
                TableCell(text = "${sum.notMarkedDays} Days", width = 80.dp, textColor = if (sum.notMarkedDays > 0) PunchOutRed else TextSecondary)
                TableCell(text = sum.totalHoursFormatted, width = 90.dp, textColor = NavyPrimary, isBold = true)
            }
            if (index < summaries.size - 1) {
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    textColor: Color = if (isHeader) SurfaceWhite else TextPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = if (isHeader) 11.sp else 11.sp
) {
    Text(
        text = text,
        color = textColor,
        fontSize = fontSize,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun StatusBadgeCell(
    status: String,
    width: androidx.compose.ui.unit.Dp
) {
    val (bgColor, txtColor) = when (status) {
        "PRESENT" -> Pair(PunchInGreen.copy(alpha = 0.15f), PunchInGreen)
        "MANUAL" -> Pair(BlueAccent.copy(alpha = 0.15f), BlueAccent)
        "INCOMPLETE" -> Pair(PendingYellow.copy(alpha = 0.15f), PendingYellow)
        else -> Pair(PunchOutRed.copy(alpha = 0.15f), PunchOutRed)
    }

    Box(modifier = Modifier.width(width)) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bgColor
        ) {
            Text(
                text = status,
                color = txtColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
