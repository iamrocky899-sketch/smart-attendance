package com.attendancehalim.smartattendance.core.report

import android.content.Context
import com.attendancehalim.smartattendance.domain.model.GeneratedReport
import com.attendancehalim.smartattendance.domain.model.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelReportGenerator {

    suspend fun generateExcel(
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
        val file = File(reportsDir, "SMART_ATTENDANCE_${safeName}_$timestamp.xlsx")

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // 1. [Content_Types].xml
            addZipEntry(zip, "[Content_Types].xml", buildContentTypesXml())

            // 2. _rels/.rels
            addZipEntry(zip, "_rels/.rels", buildRootRelsXml())

            // 3. xl/_rels/workbook.xml.rels
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())

            // 4. xl/workbook.xml
            addZipEntry(zip, "xl/workbook.xml", buildWorkbookXml())

            // 5. xl/styles.xml
            addZipEntry(zip, "xl/styles.xml", buildStylesXml())

            // 6. xl/worksheets/sheet1.xml
            val sheetXml = if (report.type == ReportType.MONTHLY && report.workerSummaries.isNotEmpty()) {
                buildMonthlySheetXml(report)
            } else {
                buildDailySheetXml(report)
            }
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }

        file
    }

    private fun addZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes, 0, bytes.size)
        zip.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Attendance Report" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="10"/><name val="Segoe UI"/></font>
    <font><b/><sz val="11"/><name val="Segoe UI"/><color rgb="FFFFFFFF"/></font>
    <font><b/><sz val="13"/><name val="Segoe UI"/><color rgb="FF1B365D"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1B365D"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""
    }

    private fun buildDailySheetXml(report: GeneratedReport): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="16" customWidth="1"/>
    <col min="2" max="2" width="24" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="12" customWidth="1"/>
    <col min="5" max="5" width="12" customWidth="1"/>
    <col min="6" max="6" width="15" customWidth="1"/>
    <col min="7" max="7" width="14" customWidth="1"/>
    <col min="8" max="8" width="30" customWidth="1"/>
    <col min="9" max="9" width="30" customWidth="1"/>
    <col min="10" max="10" width="15" customWidth="1"/>
    <col min="11" max="11" width="15" customWidth="1"/>
    <col min="12" max="12" width="15" customWidth="1"/>
    <col min="13" max="13" width="15" customWidth="1"/>
    <col min="14" max="14" width="15" customWidth="1"/>
    <col min="15" max="15" width="15" customWidth="1"/>
    <col min="16" max="16" width="16" customWidth="1"/>
    <col min="17" max="17" width="14" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Row 1: Title
        sb.append("""    <row r="1">
      <c r="A1" s="2" t="inlineStr"><is><t>SMART ATTENDANCE — ${escapeXml(report.title)}</t></is></c>
    </row>
""")

        // Row 2: Subtitle & Timestamp
        sb.append("""    <row r="2">
      <c r="A2" s="3" t="inlineStr"><is><t>${escapeXml(report.subtitle)} | Generated: ${escapeXml(report.generatedAt)} | Source: ${if (report.isLive) "LIVE SERVER" else "CACHED LOCAL"}</t></is></c>
    </row>
""")

        // Row 3: Blank
        sb.append("""    <row r="3"></row>
""")

        // Row 4: Column Headers (style 1 = Navy Header)
        val headers = listOf(
            "Employee ID", "Employee Name", "Date", "Punch In", "Punch Out",
            "Working Duration", "Status", "Punch In Area", "Punch Out Area",
            "Punch In Latitude", "Punch In Longitude", "Punch In Accuracy (m)",
            "Punch Out Latitude", "Punch Out Longitude", "Punch Out Accuracy (m)",
            "Attendance Type", "Sync Status"
        )

        sb.append("""    <row r="4">
""")
        for (c in headers.indices) {
            val colLetter = getColumnLetter(c + 1)
            sb.append("""      <c r="${colLetter}4" s="1" t="inlineStr"><is><t>${escapeXml(headers[c])}</t></is></c>
""")
        }
        sb.append("""    </row>
""")

        // Data Rows starting at r=5
        for (i in report.items.indices) {
            val r = i + 5
            val item = report.items[i]
            sb.append("""    <row r="$r">
""")
            val cells = listOf(
                item.employeeId,
                item.employeeName,
                item.date,
                item.inTime,
                item.outTime ?: "--:--",
                item.duration,
                item.status,
                item.inArea,
                item.outArea,
                item.inLat.toString(),
                item.inLng.toString(),
                item.inAccuracy.toString(),
                item.outLat.toString(),
                item.outLng.toString(),
                item.outAccuracy.toString(),
                item.attendanceType,
                item.syncStatus
            )

            for (c in cells.indices) {
                val colLetter = getColumnLetter(c + 1)
                sb.append("""      <c r="$colLetter$r" s="0" t="inlineStr"><is><t>${escapeXml(cells[c])}</t></is></c>
""")
            }
            sb.append("""    </row>
""")
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildMonthlySheetXml(report: GeneratedReport): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="16" customWidth="1"/>
    <col min="2" max="2" width="26" customWidth="1"/>
    <col min="3" max="3" width="22" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
    <col min="5" max="5" width="16" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="18" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Row 1: Title
        sb.append("""    <row r="1">
      <c r="A1" s="2" t="inlineStr"><is><t>SMART ATTENDANCE — ${escapeXml(report.title)}</t></is></c>
    </row>
""")

        // Row 2: Subtitle
        sb.append("""    <row r="2">
      <c r="A2" s="3" t="inlineStr"><is><t>${escapeXml(report.subtitle)} | Generated: ${escapeXml(report.generatedAt)}</t></is></c>
    </row>
""")

        sb.append("""    <row r="3"></row>
""")

        val headers = listOf(
            "Employee ID", "Employee Name", "Workplace", "Present Days",
            "Not Marked Days", "Total Working Hours", "Average Daily Hours"
        )

        sb.append("""    <row r="4">
""")
        for (c in headers.indices) {
            val colLetter = getColumnLetter(c + 1)
            sb.append("""      <c r="${colLetter}4" s="1" t="inlineStr"><is><t>${escapeXml(headers[c])}</t></is></c>
""")
        }
        sb.append("""    </row>
""")

        for (i in report.workerSummaries.indices) {
            val r = i + 5
            val sum = report.workerSummaries[i]
            sb.append("""    <row r="$r">
""")
            val cells = listOf(
                sum.employeeId,
                sum.employeeName,
                sum.workplaceName.ifBlank { "Main Facility" },
                "${sum.presentDays} Days",
                "${sum.notMarkedDays} Days",
                sum.totalHoursFormatted,
                sum.averageHoursFormatted
            )

            for (c in cells.indices) {
                val colLetter = getColumnLetter(c + 1)
                sb.append("""      <c r="$colLetter$r" s="0" t="inlineStr"><is><t>${escapeXml(cells[c])}</t></is></c>
""")
            }
            sb.append("""    </row>
""")
        }

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun getColumnLetter(colIndex: Int): String {
        var num = colIndex
        var result = ""
        while (num > 0) {
            val rem = (num - 1) % 26
            result = ('A' + rem) + result
            num = (num - 1) / 26
        }
        return result
    }
}
