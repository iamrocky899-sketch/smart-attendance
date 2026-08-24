package com.attendancehalim.smartattendance.presentation.admin

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.core.util.DateTimeUtils
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
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
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@Composable
fun AdminManualAttendanceScreen(
    uiState: AdminAttendanceUiState,
    onRecordManual: (employeeId: String, date: String, inTime: String, outTime: String?, notes: String?) -> Unit,
    onClearMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWorkerId by remember { mutableStateOf(uiState.workers.firstOrNull()?.employeeId ?: "") }
    var workerDropdownExpanded by remember { mutableStateOf(false) }
    var attendanceDate by remember { mutableStateOf(DateTimeUtils.getCurrentDate()) }
    var punchInTime by remember { mutableStateOf("08:30:00") }
    var punchOutTime by remember { mutableStateOf("17:00:00") }
    var reasonNote by remember { mutableStateOf("") }
    var showConflictDialog by remember { mutableStateOf(false) }

    val selectedWorker = uiState.workers.firstOrNull { it.employeeId == selectedWorkerId } ?: uiState.workers.firstOrNull()

    // Success Dialog
    if (uiState.manualSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = {
                onClearMessages()
                onNavigateBack()
            },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = PunchInGreen.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PunchInGreen,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Manual Attendance Saved",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.manualSuccessMessage ?: "Attendance saved successfully.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                AppButton(
                    text = "Done",
                    onClick = {
                        onClearMessages()
                        onNavigateBack()
                    },
                    containerColor = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Conflict Dialog
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = {
                Text(
                    text = "Existing Attendance Record",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "An attendance record already exists for ${selectedWorker?.fullName ?: selectedWorkerId} on $attendanceDate. Do you want to proceed with recording this manual attendance?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConflictDialog = false
                        onRecordManual(
                            selectedWorkerId,
                            attendanceDate,
                            punchInTime,
                            punchOutTime.ifBlank { null },
                            reasonNote
                        )
                    }
                ) {
                    Text("Proceed", color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
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
                        text = "Add Manual Attendance",
                        color = SurfaceWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Administrator Authorization & Attribution",
                        color = SurfaceWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // FORM with keyboard and navigation bar insets
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
            // Error banner
            if (uiState.manualErrorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PunchOutRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = PunchOutRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = uiState.manualErrorMessage, color = PunchOutRed, fontSize = 13.sp)
                    }
                }
            }

            // ADMIN ATTRIBUTION BADGE
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Creator Attribution",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${uiState.session.userName.ifBlank { "Administrator" }} (${uiState.session.employeeId.ifBlank { "ADMIN" }})",
                            color = NavyPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // FORM FIELDS
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Worker Selector
                    Column {
                        Text(
                            text = "Select Employee *",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedWorker?.let { "${it.fullName} (${it.employeeId})" } ?: "Select Worker",
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                                },
                                trailingIcon = {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyPrimary,
                                    unfocusedBorderColor = SurfaceBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { workerDropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = workerDropdownExpanded,
                                onDismissRequest = { workerDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                uiState.workers.forEach { worker ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = worker.fullName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(text = "${worker.employeeId} • ${worker.workplaceName}", fontSize = 11.sp, color = TextSecondary)
                                            }
                                        },
                                        onClick = {
                                            selectedWorkerId = worker.employeeId
                                            workerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date
                    Column {
                        Text(
                            text = "Attendance Date (YYYY-MM-DD) *",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = attendanceDate,
                            onValueChange = { attendanceDate = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Punch In Time
                    Column {
                        Text(
                            text = "Punch In Time (HH:mm:ss or HH:mm AM/PM) *",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = punchInTime,
                            onValueChange = { punchInTime = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = PunchInGreen)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Punch Out Time
                    Column {
                        Text(
                            text = "Punch Out Time (Optional)",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = punchOutTime,
                            onValueChange = { punchOutTime = it },
                            placeholder = { Text("e.g. 17:00:00", color = TextMuted) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = PunchOutRed)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Reason / Notes
                    Column {
                        Text(
                            text = "Reason / Authorization Note *",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = reasonNote,
                            onValueChange = { reasonNote = it },
                            placeholder = { Text("e.g. Worker device discharged; verified on-site presence", color = TextMuted) },
                            leadingIcon = {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = NavyPrimary)
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SAVE BUTTON
            AppButton(
                text = "SAVE MANUAL ATTENDANCE",
                onClick = {
                    val hasExisting = uiState.allRegisters.any {
                        it.date == attendanceDate && (it.punchInRecord?.employeeId == selectedWorkerId || it.punchOutRecord?.employeeId == selectedWorkerId)
                    }
                    if (hasExisting) {
                        showConflictDialog = true
                    } else {
                        onRecordManual(
                            selectedWorkerId,
                            attendanceDate,
                            punchInTime,
                            punchOutTime.ifBlank { null },
                            reasonNote
                        )
                    }
                },
                containerColor = NavyPrimary,
                isLoading = uiState.isSavingManual
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
