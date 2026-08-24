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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
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

@Composable
fun AdminAllAttendanceScreen(
    uiState: AdminAttendanceUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterDateChanged: (String?) -> Unit,
    onFilterMonthChanged: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onSelectRegister: (DailyAttendanceRegister) -> Unit,
    onClearSelectedRegister: () -> Unit,
    onNavigateToManualAttendance: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Attendance Details Dialog
    if (uiState.selectedRegister != null) {
        val register = uiState.selectedRegister
        val inRecord = register.punchInRecord
        val outRecord = register.punchOutRecord
        val empName = inRecord?.employeeName ?: outRecord?.employeeName ?: "Unknown Worker"
        val empId = inRecord?.employeeId ?: outRecord?.employeeId ?: "EMP-0000"

        AlertDialog(
            onDismissRequest = onClearSelectedRegister,
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attendance Details",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${register.fullDateDisplay}",
                                color = NavyPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onClearSelectedRegister, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    HorizontalDivider(color = SurfaceBorder, thickness = 0.8.dp)

                    // Worker Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = BlueAccent.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = empName.take(2).uppercase(),
                                        color = BlueAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = empName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Employee ID: $empId", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    // Punch In Details
                    if (inRecord != null) {
                        DetailSectionCard(
                            title = if (inRecord.type == AttendanceType.MANUAL) "PUNCH IN (MANUAL ATTENDANCE)" else "PUNCH IN",
                            time = DateTimeUtils.formatTimeDisplay(inRecord.time),
                            color = PunchInGreen,
                            location = inRecord.localArea,
                            accuracy = inRecord.accuracy,
                            latitude = inRecord.latitude,
                            longitude = inRecord.longitude,
                            adminInfo = if (inRecord.type == AttendanceType.MANUAL) "Recorded by Admin: ${inRecord.createdByAdminName ?: inRecord.createdByAdminId ?: "Admin"}" else null,
                            notes = inRecord.notes
                        )
                    }

                    // Punch Out Details
                    if (outRecord != null) {
                        DetailSectionCard(
                            title = "PUNCH OUT",
                            time = DateTimeUtils.formatTimeDisplay(outRecord.time),
                            color = PunchOutRed,
                            location = outRecord.localArea,
                            accuracy = outRecord.accuracy,
                            latitude = outRecord.latitude,
                            longitude = outRecord.longitude,
                            adminInfo = if (outRecord.type == AttendanceType.MANUAL || outRecord.createdByAdminId != null) "Recorded by Admin: ${outRecord.createdByAdminName ?: outRecord.createdByAdminId ?: "Admin"}" else null,
                            notes = outRecord.notes
                        )
                    }

                    // Duration & Sync Status
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
                    onClick = onClearSelectedRegister,
                    containerColor = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToManualAttendance,
                containerColor = NavyPrimary,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = "Manual Attendance")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Manual Attendance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                text = "All Attendance",
                                color = SurfaceWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${uiState.filteredRegisters.size} Total Daily Records",
                                color = SurfaceWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // SEARCH & FILTERS BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search by worker name or employee ID...", fontSize = 13.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Month Filters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val todayStr = DateTimeUtils.getCurrentDate()
                    val thisMonthStr = todayStr.take(7)

                    val isTodaySelected = uiState.filterDate == todayStr
                    FilterChip(
                        selected = isTodaySelected,
                        onClick = {
                            if (isTodaySelected) onFilterDateChanged(null) else onFilterDateChanged(todayStr)
                        },
                        label = { Text("Today", fontSize = 12.sp, fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = SurfaceWhite,
                            containerColor = SurfaceWhite,
                            labelColor = TextSecondary
                        )
                    )

                    val isThisMonthSelected = uiState.filterMonth == thisMonthStr && uiState.filterDate == null
                    FilterChip(
                        selected = isThisMonthSelected,
                        onClick = {
                            if (isThisMonthSelected) onFilterMonthChanged(null) else {
                                onFilterDateChanged(null)
                                onFilterMonthChanged(thisMonthStr)
                            }
                        },
                        label = { Text("This Month", fontSize = 12.sp, fontWeight = if (isThisMonthSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = SurfaceWhite,
                            containerColor = SurfaceWhite,
                            labelColor = TextSecondary
                        )
                    )

                    if (uiState.searchQuery.isNotBlank() || uiState.filterDate != null || uiState.filterMonth != null) {
                        TextButton(onClick = onClearFilters) {
                            Text("Clear Filters", color = PunchOutRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ATTENDANCE REGISTERS LIST
            if (uiState.filteredRegisters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
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
                            text = "No Attendance Records Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query or filters.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredRegisters,
                        key = { "${it.date}_${it.punchInRecord?.employeeId ?: it.punchOutRecord?.employeeId}" }
                    ) { register ->
                        AdminAttendanceCardItem(
                            register = register,
                            onClick = { onSelectRegister(register) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAttendanceCardItem(
    register: DailyAttendanceRegister,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inRecord = register.punchInRecord
    val outRecord = register.punchOutRecord
    val empName = inRecord?.employeeName ?: outRecord?.employeeName ?: "Worker"
    val empId = inRecord?.employeeId ?: outRecord?.employeeId ?: "EMP-0000"
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
            // Header Row: Worker Name & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = empName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($empId)",
                        color = NavyPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = register.displayDay,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // IN Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = PunchInGreen
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = inRecord?.time?.let { DateTimeUtils.formatTimeDisplay(it) } ?: "--:--",
                            color = if (inRecord != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // OUT Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = PunchOutRed
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = outRecord?.time?.let { DateTimeUtils.formatTimeDisplay(it) } ?: "--:--",
                            color = if (outRecord != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Duration
                    if (register.workingDuration != null) {
                        Text(
                            text = register.workingDuration,
                            color = NavyPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Type or Sync Badge
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

            Spacer(modifier = Modifier.height(6.dp))

            // Location
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

@Composable
private fun DetailSectionCard(
    title: String,
    time: String,
    color: androidx.compose.ui.graphics.Color,
    location: String,
    accuracy: Float,
    latitude: Double,
    longitude: Double,
    adminInfo: String?,
    notes: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = time, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Text(text = location, color = TextSecondary, fontSize = 12.sp)

            if (latitude != 0.0 || longitude != 0.0) {
                Text(
                    text = "GPS: $latitude, $longitude (±${accuracy.toInt()}m)",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (adminInfo != null) {
                Text(
                    text = adminInfo,
                    color = NavyPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!notes.isNullOrBlank()) {
                Text(
                    text = "Note: $notes",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
