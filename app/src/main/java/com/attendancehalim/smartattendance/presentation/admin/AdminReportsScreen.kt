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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendancehalim.smartattendance.domain.model.ReportType
import com.attendancehalim.smartattendance.ui.theme.BlueAccent
import com.attendancehalim.smartattendance.ui.theme.NavyPrimary
import com.attendancehalim.smartattendance.ui.theme.PendingYellow
import com.attendancehalim.smartattendance.ui.theme.PunchInGreen
import com.attendancehalim.smartattendance.ui.theme.SurfaceBorder
import com.attendancehalim.smartattendance.ui.theme.SurfaceCard
import com.attendancehalim.smartattendance.ui.theme.SurfaceWhite
import com.attendancehalim.smartattendance.ui.theme.TextMuted
import com.attendancehalim.smartattendance.ui.theme.TextPrimary
import com.attendancehalim.smartattendance.ui.theme.TextSecondary

@Composable
fun AdminReportsScreen(
    totalWorkers: Int,
    totalAttendanceCount: Int,
    pendingSyncCount: Int,
    onSelectReportType: (ReportType) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        text = "Reports & Export Center",
                        color = SurfaceWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "PDF & Excel Attendance Records & Summaries",
                        color = SurfaceWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // BODY with navigation bar insets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // METRICS SUMMARY ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "REGISTERED WORKERS",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalWorkers Staff",
                            color = NavyPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOTAL ATTENDANCE",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalAttendanceCount Logs",
                            color = NavyPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // REPORT SELECTION SECTION
            Text(
                text = "AVAILABLE ATTENDANCE REPORTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            ReportTypeCard(
                title = ReportType.DAILY.title,
                description = ReportType.DAILY.description,
                icon = Icons.Default.DateRange,
                badge = ReportType.DAILY.badge,
                onClick = { onSelectReportType(ReportType.DAILY) }
            )

            ReportTypeCard(
                title = ReportType.MONTHLY.title,
                description = ReportType.MONTHLY.description,
                icon = Icons.Default.CalendarMonth,
                badge = ReportType.MONTHLY.badge,
                onClick = { onSelectReportType(ReportType.MONTHLY) }
            )

            ReportTypeCard(
                title = ReportType.WORKER.title,
                description = ReportType.WORKER.description,
                icon = Icons.Default.Person,
                badge = ReportType.WORKER.badge,
                onClick = { onSelectReportType(ReportType.WORKER) }
            )

            ReportTypeCard(
                title = ReportType.SUMMARY.title,
                description = ReportType.SUMMARY.description,
                icon = Icons.Default.Assessment,
                badge = ReportType.SUMMARY.badge,
                onClick = { onSelectReportType(ReportType.SUMMARY) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportTypeCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = NavyPrimary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BlueAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            color = BlueAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
