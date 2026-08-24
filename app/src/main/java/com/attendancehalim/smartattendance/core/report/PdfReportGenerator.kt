package com.attendancehalim.smartattendance.core.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.attendancehalim.smartattendance.domain.model.GeneratedReport
import com.attendancehalim.smartattendance.domain.model.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f // 0.5 inch margin

    suspend fun generatePdf(
        context: Context,
        report: GeneratedReport
    ): File = withContext(Dispatchers.IO) {
        val reportsDir = File(context.getExternalFilesDir(null), "reports").apply {
            if (!exists()) mkdirs()
        }

        val safeName = report.title
            .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            .uppercase()
        val timestamp = System.currentTimeMillis()
        val file = File(reportsDir, "SMART_ATTENDANCE_${safeName}_$timestamp.pdf")

        val pdfDocument = PdfDocument()
        var pageNumber = 1

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var currentY = MARGIN

        // Helper to start a new page
        fun newPage(): Canvas {
            drawFooter(canvas, paint, pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = MARGIN + 20f
            return canvas
        }

        // 1. HEADER BANNER
        currentY = drawHeader(canvas, paint, report, currentY)

        // 2. METRIC KPI CARDS
        currentY = drawKpis(canvas, paint, report, currentY)

        // 3. TABLE RENDERING
        if (report.type == ReportType.MONTHLY && report.workerSummaries.isNotEmpty()) {
            currentY = drawMonthlySummaryTable(
                initialCanvas = canvas,
                paint = paint,
                report = report,
                startY = currentY,
                onNewPage = { newPage() }
            )
        } else if (report.items.isNotEmpty()) {
            currentY = drawDailyItemsTable(
                initialCanvas = canvas,
                paint = paint,
                report = report,
                startY = currentY,
                onNewPage = { newPage() }
            )
        } else {
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 12f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("No attendance records found for the selected period.", PAGE_WIDTH / 2f, currentY + 40f, paint)
            currentY += 80f
        }

        // Draw final page footer
        drawFooter(canvas, paint, pageNumber)
        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        file
    }

    private fun drawHeader(
        canvas: Canvas,
        paint: Paint,
        report: GeneratedReport,
        startY: Float
    ): Float {
        var y = startY

        // Top Brand Bar
        paint.color = Color.parseColor("#1B365D")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("SMART ATTENDANCE", MARGIN, y + 14f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Official Verification & Cloud Attendance Report", MARGIN, y + 27f, paint)

        // Right-side Status / Mode Badge
        val badgeText = if (report.isLive) "● LIVE CLOUD DATA" else "● CACHED LOCAL DATA"
        val badgeColor = if (report.isLive) Color.parseColor("#16A34A") else Color.parseColor("#D97706")
        paint.color = badgeColor
        paint.textSize = 9f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(badgeText, PAGE_WIDTH - MARGIN, y + 14f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Generated: ${report.generatedAt}", PAGE_WIDTH - MARGIN, y + 27f, paint)

        y += 36f

        // Separator line
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 16f

        // Report Title
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(report.title, MARGIN, y + 12f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText(report.subtitle, MARGIN, y + 26f, paint)

        y += 36f
        return y
    }

    private fun drawKpis(
        canvas: Canvas,
        paint: Paint,
        report: GeneratedReport,
        startY: Float
    ): Float {
        var y = startY
        val availableWidth = PAGE_WIDTH - 2 * MARGIN
        val cardWidth = (availableWidth - 24f) / 4f
        val cardHeight = 44f

        val kpis = listOf(
            Pair("TOTAL WORKERS", "${report.totalWorkers}"),
            Pair("PRESENT", "${report.presentCount}"),
            Pair("NOT MARKED", "${report.notMarkedCount}"),
            Pair("TOTAL HOURS", report.totalHoursFormatted ?: "${report.presentCount * 8}h")
        )

        for (i in kpis.indices) {
            val left = MARGIN + i * (cardWidth + 8f)
            val rect = RectF(left, y, left + cardWidth, y + cardHeight)

            // Card background
            paint.color = Color.parseColor("#F1F5F9")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            // Card border
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            // KPI Title
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 7.5f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(kpis[i].first, left + 8f, y + 14f, paint)

            // KPI Value
            val valColor = when (i) {
                1 -> Color.parseColor("#16A34A")
                2 -> if (report.notMarkedCount > 0) Color.parseColor("#DC2626") else Color.parseColor("#1E293B")
                else -> Color.parseColor("#1B365D")
            }
            paint.color = valColor
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText(kpis[i].second, left + 8f, y + 32f, paint)
        }

        y += cardHeight + 16f
        return y
    }

    private fun drawDailyTableHeader(canvas: Canvas, paint: Paint, headerY: Float, headers: Array<String>, colWidths: FloatArray) {
        val rect = RectF(MARGIN, headerY, PAGE_WIDTH - MARGIN, headerY + 20f)
        paint.color = Color.parseColor("#1B365D")
        paint.style = Paint.Style.FILL
        canvas.drawRect(rect, paint)

        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT

        var curX = MARGIN + 4f
        for (c in headers.indices) {
            canvas.drawText(headers[c], curX, headerY + 13f, paint)
            curX += colWidths[c]
        }
    }

    private fun drawDailyItemsTable(
        initialCanvas: Canvas,
        paint: Paint,
        report: GeneratedReport,
        startY: Float,
        onNewPage: () -> Canvas
    ): Float {
        var canvas = initialCanvas
        var y = startY
        val colWidths = floatArrayOf(60f, 95f, 48f, 48f, 50f, 60f, 162f)
        val headers = arrayOf("ID / Date", "Employee Name", "In", "Out", "Duration", "Status", "Location & GPS")

        drawDailyTableHeader(canvas, paint, y, headers, colWidths)
        y += 20f

        val rowHeight = 22f

        for (i in report.items.indices) {
            val item = report.items[i]

            // Check page boundary
            if (y + rowHeight > PAGE_HEIGHT - MARGIN - 30f) {
                canvas = onNewPage()
                y = MARGIN + 20f
                drawDailyTableHeader(canvas, paint, y, headers, colWidths)
                y += 20f
            }

            // Alternating row background
            val rowBg = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight)
            paint.color = rowBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(rowRect, paint)

            // Border
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(rowRect, paint)

            // Text cells
            paint.style = Paint.Style.FILL
            paint.isFakeBoldText = false
            paint.textSize = 7.5f
            paint.textAlign = Paint.Align.LEFT

            var curX = MARGIN + 4f

            // Col 1: ID / Date
            paint.color = Color.parseColor("#0F172A")
            paint.isFakeBoldText = true
            val col1Text = if (report.type == ReportType.WORKER) item.date else item.employeeId
            canvas.drawText(col1Text, curX, y + 14f, paint)
            curX += colWidths[0]

            // Col 2: Name
            paint.color = Color.parseColor("#334155")
            paint.isFakeBoldText = false
            val safeName = if (item.employeeName.length > 18) item.employeeName.substring(0, 16) + ".." else item.employeeName
            canvas.drawText(safeName, curX, y + 14f, paint)
            curX += colWidths[1]

            // Col 3: In
            paint.color = Color.parseColor("#16A34A")
            canvas.drawText(item.inTime, curX, y + 14f, paint)
            curX += colWidths[2]

            // Col 4: Out
            paint.color = if (!item.outTime.isNullOrBlank()) Color.parseColor("#DC2626") else Color.parseColor("#94A3B8")
            canvas.drawText(item.outTime ?: "--:--", curX, y + 14f, paint)
            curX += colWidths[3]

            // Col 5: Duration
            paint.color = Color.parseColor("#0F172A")
            canvas.drawText(item.duration, curX, y + 14f, paint)
            curX += colWidths[4]

            // Col 6: Status
            val statusColor = when (item.status) {
                "PRESENT" -> Color.parseColor("#16A34A")
                "MANUAL" -> Color.parseColor("#2563EB")
                else -> Color.parseColor("#D97706")
            }
            paint.color = statusColor
            paint.isFakeBoldText = true
            canvas.drawText(item.status, curX, y + 14f, paint)
            curX += colWidths[5]

            // Col 7: Location & GPS
            paint.color = Color.parseColor("#475569")
            paint.isFakeBoldText = false
            val locText = if (item.inArea.length > 34) item.inArea.substring(0, 32) + ".." else item.inArea
            canvas.drawText(locText, curX, y + 14f, paint)

            y += rowHeight
        }

        return y + 16f
    }

    private fun drawMonthlyTableHeader(canvas: Canvas, paint: Paint, headerY: Float, headers: Array<String>, colWidths: FloatArray) {
        val rect = RectF(MARGIN, headerY, PAGE_WIDTH - MARGIN, headerY + 20f)
        paint.color = Color.parseColor("#1B365D")
        paint.style = Paint.Style.FILL
        canvas.drawRect(rect, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT

        var curX = MARGIN + 6f
        for (c in headers.indices) {
            canvas.drawText(headers[c], curX, headerY + 13f, paint)
            curX += colWidths[c]
        }
    }

    private fun drawMonthlySummaryTable(
        initialCanvas: Canvas,
        paint: Paint,
        report: GeneratedReport,
        startY: Float,
        onNewPage: () -> Canvas
    ): Float {
        var canvas = initialCanvas
        var y = startY
        val colWidths = floatArrayOf(70f, 130f, 110f, 60f, 60f, 93f)
        val headers = arrayOf("Employee ID", "Employee Name", "Workplace", "Present", "Not Marked", "Total Hours")

        drawMonthlyTableHeader(canvas, paint, y, headers, colWidths)
        y += 20f

        val rowHeight = 22f

        for (i in report.workerSummaries.indices) {
            val item = report.workerSummaries[i]

            if (y + rowHeight > PAGE_HEIGHT - MARGIN - 30f) {
                canvas = onNewPage()
                y = MARGIN + 20f
                drawMonthlyTableHeader(canvas, paint, y, headers, colWidths)
                y += 20f
            }

            val rowBg = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight)
            paint.color = rowBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(rowRect, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(rowRect, paint)

            paint.style = Paint.Style.FILL
            paint.isFakeBoldText = false
            paint.textSize = 8f
            paint.textAlign = Paint.Align.LEFT

            var curX = MARGIN + 6f

            paint.color = Color.parseColor("#0F172A")
            paint.isFakeBoldText = true
            canvas.drawText(item.employeeId, curX, y + 14f, paint)
            curX += colWidths[0]

            paint.color = Color.parseColor("#334155")
            paint.isFakeBoldText = false
            canvas.drawText(item.employeeName, curX, y + 14f, paint)
            curX += colWidths[1]

            paint.color = Color.parseColor("#64748B")
            canvas.drawText(item.workplaceName.ifBlank { "Main Facility" }, curX, y + 14f, paint)
            curX += colWidths[2]

            paint.color = Color.parseColor("#16A34A")
            paint.isFakeBoldText = true
            canvas.drawText("${item.presentDays} Days", curX, y + 14f, paint)
            curX += colWidths[3]

            paint.color = if (item.notMarkedDays > 0) Color.parseColor("#DC2626") else Color.parseColor("#64748B")
            canvas.drawText("${item.notMarkedDays} Days", curX, y + 14f, paint)
            curX += colWidths[4]

            paint.color = Color.parseColor("#1B365D")
            paint.isFakeBoldText = true
            canvas.drawText(item.totalHoursFormatted, curX, y + 14f, paint)

            y += rowHeight
        }

        return y + 16f
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        pageNumber: Int
    ) {
        val y = PAGE_HEIGHT - MARGIN + 14f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y - 10f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("SMART ATTENDANCE • Confidential Report • Asia/Kolkata", MARGIN, y, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN, y, paint)
    }
}
