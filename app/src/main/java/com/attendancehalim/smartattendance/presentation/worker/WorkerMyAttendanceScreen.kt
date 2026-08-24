package com.attendancehalim.smartattendance.presentation.worker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceRecord
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyAttendanceRegister
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.presentation.components.AppOutlinedButton
import com.attendancehalim.smartattendance.presentation.components.SyncStatusBadge
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyDark
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PendingYellow
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.SyncedBlue
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun WorkerMyAttendanceScreen(
    uiState: WorkerMyAttendanceUiState,
    onSelectItem: (DailyAttendanceRegister) -> Unit,
    onClearSelectedItem: () -> Unit,
    onExportPdf: (android.content.Context) -> Unit = {},
    onExportExcel: (android.content.Context) -> Unit = {},
    onOpenFile: (android.content.Context) -> Unit = {},
    onShareFile: (android.content.Context) -> Unit = {},
    onDismissExportDialog: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    text = "Attendance Export Ready",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your attendance record was saved locally:",
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
                    onClick = { onOpenFile(context) }
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(
                        text = "Share",
                        onClick = { onShareFile(context) }
                    )
                    TextButton(onClick = onDismissExportDialog) {
                        Text("Done", color = TextSecondary)
                    }
                }
            }
        )
    }

    // Attendance Record Details Dialog
    if (uiState.selectedItem != null) {
        AttendanceDetailsDialog(
            item = uiState.selectedItem,
            employeeName = uiState.session.userName,
            employeeId = uiState.session.employeeId,
            onDismiss = onClearSelectedItem
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                    Column {
                        Text(
                            text = "MY ATTENDANCE",
                            color = SurfaceWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${uiState.session.userName.ifBlank { "Worker" }} (${uiState.session.employeeId})",
                            color = SurfaceWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Export Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onExportPdf(context) },
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceWhite.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isExportingPdf) {
                                CircularProgressIndicator(
                                    color = SurfaceWhite,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onExportExcel(context) },
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceWhite.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isExportingExcel) {
                                CircularProgressIndicator(
                                    color = SurfaceWhite,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Export Excel",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // CONTENT BODY
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NavyPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (uiState.groupedRecords.isEmpty()) {
            // EMPTY STATE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = BlueAccent.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Attendance Records",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You have not marked any attendance yet. Tap Mark Attendance on your dashboard to punch in.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            // ATTENDANCE REGISTER LIST GROUPED BY MONTH
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Summary Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Days: ${uiState.totalDaysRecorded}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        if (uiState.pendingSyncCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PendingYellow.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = PendingYellow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.pendingSyncCount} Pending Sync",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.groupedRecords.forEach { (monthHeader, days) ->
                    item(key = "header_$monthHeader") {
                        MonthHeader(title = monthHeader)
                    }

                    items(
                        items = days,
                        key = { it.date }
                    ) { dayItem ->
                        AttendanceDayCard(
                            item = dayItem,
                            onClick = { onSelectItem(dayItem) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(title: String) {
    Text(
        text = title,
        color = NavyPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun AttendanceDayCard(
    item: DailyAttendanceRegister,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge on Left
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(12.dp),
                color = NavyPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val parts = item.displayDay.trim().split(" ")
                    val dayNum = parts.firstOrNull() ?: ""
                    val monthText = parts.getOrNull(1) ?: ""
                    Text(
                        text = dayNum,
                        color = NavyPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = monthText.uppercase(Locale.getDefault()),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row: Status chip & Day display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.fullDateDisplay.ifBlank { item.displayDay },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    SyncStatusBadge(status = item.overallSyncStatus)
                }

                // Punch Times Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Punch In
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "In: ",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (item.punchInRecord != null) DateTimeUtils.formatTimeDisplay(item.punchInRecord.time) else "--",
                            color = if (item.punchInRecord != null) PunchInGreen else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Punch Out
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Out: ",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (item.punchOutRecord != null) DateTimeUtils.formatTimeDisplay(item.punchOutRecord.time) else if (item.workingDuration == "In Progress") "In Progress" else "--",
                            color = if (item.punchOutRecord != null) PunchOutRed else if (item.workingDuration == "In Progress") BlueAccent else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Duration & Location Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location summary
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.locationSummary.ifBlank { "Smart Facility" },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Working duration if available
                    if (item.workingDuration != null && item.workingDuration != "In Progress") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.workingDuration,
                                color = NavyPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AttendanceDetailsDialog(
    item: DailyAttendanceRegister,
    employeeName: String,
    employeeId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                SyncStatusBadge(status = item.overallSyncStatus)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Worker & Date Header Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = employeeName.ifBlank { "Worker" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Employee ID: $employeeId",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Date: ${item.fullDateDisplay}",
                            fontSize = 12.sp,
                            color = NavyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.workingDuration != null) {
                            Text(
                                text = "Working Duration: ${item.workingDuration}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // PUNCH IN DETAILS
                if (item.punchInRecord != null) {
                    PunchRecordDetailCard(
                        title = "Punch In",
                        record = item.punchInRecord,
                        headerColor = PunchInGreen
                    )
                }

                // PUNCH OUT DETAILS
                if (item.punchOutRecord != null) {
                    PunchRecordDetailCard(
                        title = "Punch Out",
                        record = item.punchOutRecord,
                        headerColor = PunchOutRed
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = "No Punch Out recorded for this day",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Close",
                onClick = onDismiss,
                containerColor = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PunchRecordDetailCard(
    title: String,
    record: AttendanceRecord,
    headerColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, headerColor.copy(alpha = 0.3f)),
        color = SurfaceWhite
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = headerColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateTimeUtils.formatTimeDisplay(record.time),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Location", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = record.localArea.ifBlank { "Location name unavailable" },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Coordinates", color = TextSecondary, fontSize = 11.sp)
                Text(
                    text = String.format(Locale.US, "%.5f, %.5f (±%dm)", record.latitude, record.longitude, record.accuracy.toInt()),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sync Status", color = TextSecondary, fontSize = 11.sp)
                SyncStatusBadge(status = record.syncStatus)
            }
        }
    }
}
