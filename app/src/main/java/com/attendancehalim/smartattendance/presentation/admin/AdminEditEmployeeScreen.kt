package com.attendancehalim.smartattendance.presentation.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
import com.attendancehalim.smartattendance.presentation.components.AppButton
import com.attendancehalim.smartattendance.presentation.components.AppCard
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.PunchOutRed
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@Composable
fun AdminEditEmployeeScreen(
    worker: WorkerProfile?,
    uiState: AdminEmployeesUiState,
    onSaveUpdate: (WorkerProfile) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (worker == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Worker not found.", color = TextSecondary)
        }
        return
    }

    var fullName by remember { mutableStateOf(worker.fullName) }
    var mobileNumber by remember { mutableStateOf(worker.mobileNumber) }
    var workplaceName by remember { mutableStateOf(worker.workplaceName) }
    var designation by remember { mutableStateOf(worker.designation) }
    var joiningDate by remember { mutableStateOf(worker.joiningDate) }
    var isActive by remember { mutableStateOf(worker.isActive) }

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
                        text = "Edit Employee",
                        color = SurfaceWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${worker.employeeId} • Permanent Identifier",
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
            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PunchOutRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = PunchOutRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage,
                            color = PunchOutRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // READ-ONLY EMPLOYEE ID CARD
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Permanent Employee ID",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = worker.employeeId,
                            color = NavyPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Identifier",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // FORM FIELDS
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Full Name
                    Column {
                        Text(
                            text = "Full Name *",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Mobile Number
                    Column {
                        Text(
                            text = "Mobile Number",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Workplace
                    Column {
                        Text(
                            text = "Workplace / Facility",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = workplaceName,
                            onValueChange = { workplaceName = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Designation
                    Column {
                        Text(
                            text = "Designation / Role",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Work, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Joining Date
                    Column {
                        Text(
                            text = "Joining Date (YYYY-MM-DD)",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = joiningDate,
                            onValueChange = { joiningDate = it },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = NavyPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Status Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account Status",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isActive) "Active (Authorized)" else "Inactive (Access Blocked)",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurfaceWhite,
                                checkedTrackColor = PunchInGreen,
                                uncheckedThumbColor = SurfaceWhite,
                                uncheckedTrackColor = PunchOutRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Button
            AppButton(
                text = "SAVE CHANGES",
                onClick = {
                    val updated = worker.copy(
                        fullName = fullName,
                        mobileNumber = mobileNumber,
                        workplaceName = workplaceName,
                        designation = designation,
                        joiningDate = joiningDate,
                        isActive = isActive
                    )
                    onSaveUpdate(updated)
                    onNavigateBack()
                },
                containerColor = NavyPrimary,
                isLoading = uiState.isActionLoading
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
