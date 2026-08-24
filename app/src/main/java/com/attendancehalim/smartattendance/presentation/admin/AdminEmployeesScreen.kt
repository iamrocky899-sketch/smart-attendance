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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.WorkerProfile
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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold

@Composable
fun AdminEmployeesScreen(
    uiState: AdminEmployeesUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterStatusChanged: (WorkerFilterStatus) -> Unit,
    onNavigateToAddEmployee: () -> Unit,
    onNavigateToEmployeeDetails: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddEmployee,
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
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Worker"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Worker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
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
                                text = "Employee Management",
                                color = SurfaceWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${uiState.workers.size} Total Registered Workers",
                                color = SurfaceWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToAddEmployee,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Employee",
                            tint = SurfaceWhite
                        )
                    }
                }
            }

            // SEARCH & FILTER BAR
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
                    placeholder = { Text("Search by name, ID or workplace...", fontSize = 13.sp, color = TextMuted) },
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

                // Filter Chips (All, Active, Inactive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkerFilterStatus.entries.forEach { filter ->
                        val isSelected = uiState.filterStatus == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterStatusChanged(filter) },
                            label = {
                                Text(
                                    text = "${filter.label} (${
                                        when (filter) {
                                            WorkerFilterStatus.ALL -> uiState.workers.size
                                            WorkerFilterStatus.ACTIVE -> uiState.workers.count { it.isActive }
                                            WorkerFilterStatus.INACTIVE -> uiState.workers.count { !it.isActive }
                                        }
                                    })",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = SurfaceWhite,
                                containerColor = SurfaceWhite,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SurfaceBorder,
                                selectedBorderColor = NavyPrimary
                            )
                        )
                    }
                }
            }

            // WORKERS LIST
            if (uiState.filteredWorkers.isEmpty()) {
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
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Workers Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No workers match your search query." else "Tap + to add a new worker.",
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredWorkers,
                        key = { it.employeeId }
                    ) { worker ->
                        WorkerCardItem(
                            worker = worker,
                            onClick = { onNavigateToEmployeeDetails(worker.employeeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerCardItem(
    worker: WorkerProfile,
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Initials
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = if (worker.isActive) BlueAccent.copy(alpha = 0.12f) else TextMuted.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = worker.fullName.take(2).uppercase().ifBlank { "WK" },
                        color = if (worker.isActive) BlueAccent else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = worker.fullName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (worker.isActive) PunchInGreen.copy(alpha = 0.12f) else PunchOutRed.copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = if (worker.isActive) "ACTIVE" else "INACTIVE",
                            color = if (worker.isActive) PunchInGreen else PunchOutRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${worker.employeeId} • ${worker.designation.ifBlank { "Staff" }}",
                    color = NavyPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${worker.workplaceName.ifBlank { "Facility" }}${if (worker.mobileNumber.isNotBlank()) " • " + worker.mobileNumber else ""}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
