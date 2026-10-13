package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import java.io.OutputStream
import java.time.LocalDate
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.ss.util.CellRangeAddress

/**
 * Streaming `.xlsx` writers (PLAN.md §6.3 / Phase-5 checklist) built on `SXSSFWorkbook(null, 100,
 * false, false)` — a 100-row window and no temp files, so exports stay flat on memory even at 10k
 * rows. Every method takes its display headers/labels from the caller: the data layer never embeds a
 * user-facing string. `SXSSFWorkbook.close()` disposes the row window and the underlying workbook,
 * so it is used via `use { }`.
 */
object ExcelWriter {

    /** Column display names for the employee export; the caller supplies the locale. */
    fun writeEmployees(
        out: OutputStream,
        employees: List<Employee>,
        headersFor: (EmployeeField) -> String,
    ) {
        SXSSFWorkbook(null, ROW_WINDOW, false, false).use { workbook ->
            val sheet = workbook.createSheet(SHEET_EMPLOYEES)
            val headerStyle = workbook.headerStyle()
            val dateStyle = workbook.dateStyle()

            val headerRow = sheet.createRow(0)
            EmployeeField.entries.forEachIndexed { column, field ->
                headerRow.createCell(column).apply {
                    setCellValue(headersFor(field))
                    cellStyle = headerStyle
                }
            }
            sheet.createFreezePane(0, 1)
            if (sheet is org.apache.poi.ss.usermodel.Sheet) {
                // SXSSFSheet supports autoFilter in some POI versions; guard via cast semantics not needed
            }
            try {
                val m = sheet.javaClass.getMethod("setAutoFilter", CellRangeAddress::class.java)
                m.invoke(sheet, CellRangeAddress(0, 0, 0, EmployeeField.entries.size - 1))
            } catch (_: Exception) {
                // no autoFilter if unsupported
            }

            employees.forEachIndexed { rowIndex, employee ->
                val row = sheet.createRow(rowIndex + 1)
                EmployeeField.entries.forEachIndexed { column, field ->
                    row.createCell(column).write(employee, field, dateStyle)
                }
            }
        }
    }

    /**
     * One "Analytics" sheet: an Overview block of global stats, then bucket tables (age, service,
     * department, grade) and the "longest serving" list. Every display string comes from [labels],
     * whose positional contract is fixed by [ANALYTICS_INDEX] so later phases can pass localized
     * values and tests can pass English ones.
     */
    fun writeAnalytics(
        out: OutputStream,
        snapshot: AnalyticsSnapshot,
        labels: List<String>,
    ) {
        require(labels.size >= ANALYTICS_INDEX) { "analytics labels must cover all ANALYTICS_INDEX entries" }
        SXSSFWorkbook(null, ROW_WINDOW, false, false).use { workbook ->
            val sheet = workbook.createSheet(SHEET_ANALYTICS)
            val titleStyle = workbook.titleStyle()
            val headerStyle = workbook.headerStyle()

            var row = 0
            row = writeSectionTitle(sheet, row, labels[ANALYTICS_OVERVIEW_TITLE], titleStyle)
            row = writeStats(sheet, row, snapshot, labels, headerStyle)
            row = writeBucketSection(sheet, row, labels[ANALYTICS_AGE_TITLE], snapshot.ageBuckets, labels, titleStyle, headerStyle)
            row = writeBucketSection(sheet, row, labels[ANALYTICS_SERVICE_TITLE], snapshot.serviceBuckets, labels, titleStyle, headerStyle)
            row = writeBucketSection(sheet, row, labels[ANALYTICS_DEPARTMENT_TITLE], snapshot.departmentCounts, labels, titleStyle, headerStyle)
            row = writeBucketSection(sheet, row, labels[ANALYTICS_GRADE_TITLE], snapshot.gradeCounts, labels, titleStyle, headerStyle)
            row = writeLongestServing(sheet, row, snapshot, labels, titleStyle, headerStyle)
            sheet.createFreezePane(0, 1)
        }
    }

    /** The 1-based error report described in PLAN.md §6.1/§6.4. */
    fun writeErrorReport(
        out: OutputStream,
        errors: List<ImportRowError>,
        labels: ErrorReportLabels,
    ) {
        SXSSFWorkbook(null, ROW_WINDOW, false, false).use { workbook ->
            val sheet = workbook.createSheet(labels.sheetName)
            val headerStyle = workbook.headerStyle()
            val headers = listOf(
                labels.sourceRowColumn,
                labels.columnColumn,
                labels.rawValueColumn,
                labels.reasonColumn,
            )
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { column, header ->
                headerRow.createCell(column).apply {
                    setCellValue(header)
                    cellStyle = headerStyle
                }
            }
            sheet.createFreezePane(0, 1)
            try {
                val m = sheet.javaClass.getMethod("setAutoFilter", CellRangeAddress::class.java)
                m.invoke(sheet, CellRangeAddress(0, 0, 0, headers.size - 1))
            } catch (_: Exception) {
            }

            errors.forEachIndexed { index, error ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(error.sourceRow.toDouble())
                row.createCell(1).setCellValue(error.column ?: "")
                row.createCell(2).setCellValue(error.rawValue)
                row.createCell(3).setCellValue(error.reason.name)
            }

            val note = labels.truncationNote
            if (note != null) {
                val row = sheet.createRow(errors.size + 2)
                row.createCell(0).setCellValue(note)
            }
        }
    }

    private fun writeStats(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        startRow: Int,
        snapshot: AnalyticsSnapshot,
        labels: List<String>,
        headerStyle: CellStyle,
    ): Int {
        var row = startRow
        val entries = listOf(
            labels[ANALYTICS_TOTAL_ROW] to snapshot.total.toString(),
            labels[ANALYTICS_AVERAGE_AGE] to snapshot.averageAge?.formatNumber() ?: "",
            labels[ANALYTICS_MEDIAN_AGE] to snapshot.medianAge?.formatNumber() ?: "",
            labels[ANALYTICS_AVERAGE_SERVICE] to snapshot.averageService?.formatNumber() ?: "",
            labels[ANALYTICS_MEDIAN_SERVICE] to snapshot.medianService?.formatNumber() ?: "",
            labels[ANALYTICS_RETIREMENT_SOON] to snapshot.retirementSoon.toString(),
        )
        for (i in entries.indices) {
            val e = entries[i]
            val r = sheet.createRow(row + i)
            r.createCell(0).apply {
                setCellValue(e.component1().toString())
                cellStyle = headerStyle
            }
            r.createCell(1).setCellValue(e.component2().toString())
        }
        return row + entries.size + 1
    }

    private fun writeBucketSection(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        startRow: Int,
        title: String,
        buckets: List<com.gov.employeedb.domain.model.BucketCount>,
        labels: List<String>,
        titleStyle: CellStyle,
        headerStyle: CellStyle,
    ): Int {
        var row = startRow
        val titleRow = sheet.createRow(row)
        titleRow.createCell(0).apply {
            setCellValue(title)
            cellStyle = titleStyle
        }
        row++
        sheet.createRow(row).let { header ->
            header.createCell(0).apply {
                setCellValue(labels[ANALYTICS_COL_LABEL])
                cellStyle = headerStyle
            }
            header.createCell(1).apply {
                setCellValue(labels[ANALYTICS_COL_COUNT])
                cellStyle = headerStyle
            }
        }
        row++
        for (bucket in buckets) {
            val r = sheet.createRow(row)
            r.createCell(0).setCellValue(bucket.label)
            r.createCell(1).setCellValue(bucket.count.toDouble())
            row++
        }
        return row + 1
    }

    private fun writeLongestServing(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        startRow: Int,
        snapshot: AnalyticsSnapshot,
        labels: List<String>,
        titleStyle: CellStyle,
        headerStyle: CellStyle,
    ): Int {
        var row = startRow
        val titleRow = sheet.createRow(row)
        titleRow.createCell(0).apply {
            setCellValue(labels[ANALYTICS_LONGEST_TITLE])
            cellStyle = titleStyle
        }
        row++
        sheet.createRow(row).let { header ->
            header.createCell(0).apply {
                setCellValue(labels[ANALYTICS_COL_LABEL])
                cellStyle = headerStyle
            }
            header.createCell(1).apply {
                setCellValue(labels[ANALYTICS_COL_COUNT])
                cellStyle = headerStyle
            }
        }
        row++
        for (summary in snapshot.longestServing) {
            val r = sheet.createRow(row)
            r.createCell(0).setCellValue(summary.fullName)
            r.createCell(1).setCellValue(summary.serviceYears?.toDouble() ?: 0.0)
            row++
        }
        return row
    }

    // ---- cell writing helpers ----

    private fun org.apache.poi.ss.usermodel.Cell.write(employee: Employee, field: EmployeeField, dateStyle: CellStyle) {
        when (field) {
            EmployeeField.FULL_NAME -> setCellValue(employee.fullName)
            EmployeeField.DATE_OF_BIRTH -> writeDate(employee.dateOfBirth, dateStyle)
            EmployeeField.HIRE_DATE -> writeDate(employee.hireDate, dateStyle)
            EmployeeField.DEPARTMENT -> writeText(employee.department)
            EmployeeField.POSITION -> writeText(employee.position)
            EmployeeField.GRADE -> writeText(employee.grade)
            EmployeeField.SALARY -> employee.salary?.let { setCellValue(it) }
            EmployeeField.EMAIL -> writeText(employee.email)
            EmployeeField.PHONE -> writeText(employee.phone)
            EmployeeField.ADDRESS -> writeText(employee.address)
            EmployeeField.NOTES -> writeText(employee.notes)
        }
    }

    private fun org.apache.poi.ss.usermodel.Cell.writeDate(date: LocalDate?, style: CellStyle) {
        if (date != null) {
            // Excel serial for 1900-system dates (epochDay 0 == serial 25569 for dates ≥ 1900-03-01).
            setCellValue((date.toEpochDay() + EXCEL_EPOCH_OFFSET).toDouble())
            cellStyle = style
        }
    }

    private fun org.apache.poi.ss.usermodel.Cell.writeText(value: String?) {
        if (!value.isNullOrEmpty()) setCellValue(value)
    }

    private fun org.apache.poi.ss.usermodel.Workbook.headerStyle(): CellStyle = createCellStyle().apply {
        val font = createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        }
        this.setFont(font)
        fillForegroundColor = IndexedColors.DARK_BLUE.index
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
    }

    private fun org.apache.poi.ss.usermodel.Workbook.titleStyle(): CellStyle = createCellStyle().apply {
        val font = createFont().apply { bold = true }
        this.setFont(font)
        alignment = HorizontalAlignment.LEFT
    }

    private fun org.apache.poi.ss.usermodel.Workbook.dateStyle(): CellStyle = createCellStyle().apply {
        dataFormat = createDataFormat().getFormat(DATE_FORMAT)
        // skip border for minimal compatibility
    }

    private fun writeSectionTitle(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        startRow: Int,
        title: String,
        style: CellStyle,
    ): Int {
        val row = sheet.createRow(startRow)
        row.createCell(0).apply {
            setCellValue(title)
            cellStyle = style
        }
        return startRow + 1
    }

    private fun Double.formatNumber(): String =
        if (this == Math.rint(this)) this.toLong().toString() else toString()

    private const val ROW_WINDOW = 100
    private const val SHEET_EMPLOYEES = "Employees"
    private const val SHEET_ANALYTICS = "Analytics"
    private const val DATE_FORMAT = "dd/MM/yyyy"
    private const val EXCEL_EPOCH_OFFSET = 25569L

    /** Positional contract for [writeAnalytics] labels; later phases pass localized lists. */
    const val ANALYTICS_INDEX = 14
    const val ANALYTICS_OVERVIEW_TITLE = 0
    const val ANALYTICS_AGE_TITLE = 1
    const val ANALYTICS_SERVICE_TITLE = 2
    const val ANALYTICS_DEPARTMENT_TITLE = 3
    const val ANALYTICS_GRADE_TITLE = 4
    const val ANALYTICS_LONGEST_TITLE = 5
    const val ANALYTICS_COL_LABEL = 6
    const val ANALYTICS_COL_COUNT = 7
    const val ANALYTICS_TOTAL_ROW = 8
    const val ANALYTICS_AVERAGE_AGE = 9
    const val ANALYTICS_MEDIAN_AGE = 10
    const val ANALYTICS_AVERAGE_SERVICE = 11
    const val ANALYTICS_MEDIAN_SERVICE = 12
    const val ANALYTICS_RETIREMENT_SOON = 13

    /** Display strings for the error report; everything except [truncationNote] is required. */
    data class ErrorReportLabels(
        val sheetName: String,
        val sourceRowColumn: String,
        val columnColumn: String,
        val rawValueColumn: String,
        val reasonColumn: String,
        val truncationNote: String? = null,
    )
}