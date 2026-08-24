package com.attendancehalim.smartattendance.presentation.admin

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.AttendanceType
import com.attendancehalim.smartattendance.domain.model.DailyAttendanceRegister
import com.attendancehalim.smartattendance.domain.model.SyncStatus
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
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

@Composable
fun AdminWorkerAttendanceScreen(
    worker: WorkerProfile?,
    workerRegisters: List<DailyAttendanceRegister>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRegister by remember { mutableStateOf<DailyAttendanceRegister?>(null) }

    // Group registers by month header
    val groupedByMonth = remember(workerRegisters) {
        workerRegisters.groupBy { it.monthHeader }
    }

    // Detail Dialog
    if (selectedRegister != null) {
        val register = selectedRegister!!
        val inRecord = register.punchInRecord
        val outRecord = register.punchOutRecord
        val empName = worker?.fullName ?: inRecord?.employeeName ?: "Worker"
        val empId = worker?.employeeId ?: inRecord?.employeeId ?: "EMP-0000"

        AlertDialog(
            onDismissRequest = { selectedRegister = null },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attendance Record",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = register.fullDateDisplay,
                                color = NavyPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { selectedRegister = null }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp)

                    if (inRecord != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = PunchInGreen.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, PunchInGreen.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (inRecord.type == AttendanceType.MANUAL) "PUNCH IN (MANUAL)" else "PUNCH IN",
                                        color = PunchInGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = DateTimeUtils.formatTimeDisplay(inRecord.time),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = inRecord.localArea, color = TextSecondary, fontSize = 12.sp)
                                if (inRecord.latitude != 0.0 || inRecord.longitude != 0.0) {
                                    Text(
                                        text = "GPS: ${inRecord.latitude}, ${inRecord.longitude} (±${inRecord.accuracy.toInt()}m)",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (inRecord.type == AttendanceType.MANUAL) {
                                    Text(
                                        text = "Recorded by Admin: ${inRecord.createdByAdminName ?: inRecord.createdByAdminId ?: "Admin"}",
                                        color = NavyPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (!inRecord.notes.isNullOrBlank()) {
                                    Text(text = "Note: ${inRecord.notes}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (outRecord != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = PunchOutRed.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, PunchOutRed.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "PUNCH OUT", color = PunchOutRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = DateTimeUtils.formatTimeDisplay(outRecord.time),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = outRecord.localArea, color = TextSecondary, fontSize = 12.sp)
                                if (outRecord.latitude != 0.0 || outRecord.longitude != 0.0) {
                                    Text(
                                        text = "GPS: ${outRecord.latitude}, ${outRecord.longitude} (±${outRecord.accuracy.toInt()}m)",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (outRecord.type == AttendanceType.MANUAL || outRecord.createdByAdminId != null) {
                                    Text(
                                        text = "Recorded by Admin: ${outRecord.createdByAdminName ?: outRecord.createdByAdminId ?: "Admin"}",
                                        color = NavyPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (!outRecord.notes.isNullOrBlank()) {
                                    Text(text = "Note: ${outRecord.notes}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Working Duration: ${register.workingDuration ?: "Incomplete"}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (register.overallSyncStatus == SyncStatus.SYNCED) SyncedBlue.copy(alpha = 0.12f) else PendingYellow.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = register.overallSyncStatus.name,
                                color = if (register.overallSyncStatus == SyncStatus.SYNCED) SyncedBlue else PendingYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AppButton(
                    text = "Close",
                    onClick = { selectedRegister = null },
                    containerColor = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(20.dp)
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
                verticalAlignment = Alignment.CenterVertically
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
                        text = worker?.fullName ?: "Worker Attendance",
                        color = SurfaceWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${worker?.employeeId ?: ""} • Attendance Register (${workerRegisters.size} days)",
                        color = SurfaceWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ATTENDANCE LIST GROUPED BY MONTH
        if (workerRegisters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = BlueAccent.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Attendance Records",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No attendance logs found for this worker.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByMonth.forEach { (monthHeader, registersInMonth) ->
                    item {
                        Text(
                            text = monthHeader.uppercase(),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(
                        items = registersInMonth,
                        key = { it.date }
                    ) { reg ->
                        WorkerRegisterCard(
                            register = reg,
                            onClick = { selectedRegister = reg }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkerRegisterCard(
    register: DailyAttendanceRegister,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inRecord = register.punchInRecord
    val outRecord = register.punchOutRecord
    val isManual = inRecord?.type == AttendanceType.MANUAL || outRecord?.type == AttendanceType.MANUAL

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = register.displayDay,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isManual) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SyncedBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "MANUAL",
                            color = SyncedBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = PunchInGreen) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = inRecord?.time?.let { DateTimeUtils.formatTimeDisplay(it) } ?: "--:--",
                            color = if (inRecord != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = PunchOutRed) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = outRecord?.time?.let { DateTimeUtils.formatTimeDisplay(it) } ?: "--:--",
                            color = if (outRecord != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (register.workingDuration != null) {
                        Text(
                            text = register.workingDuration,
                            color = NavyPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = register.locationSummary,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
