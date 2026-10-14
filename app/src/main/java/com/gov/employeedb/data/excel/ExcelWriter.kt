package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.util.Locale
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.ss.util.CellRangeAddress

/**
 * Streaming `.xlsx` and CSV writers (PLAN.md §6.3 / Phase-5,11 checklist) built on
 * `SXSSFWorkbook(null, 100, false, false)` — a 100-row window and no temp files, so exports
 * stay flat on memory even at 10k rows. Every method takes its display headers/labels from the
 * caller: the data layer never embeds a user-facing string. `SXSSFWorkbook.close()` disposes the
 * row window and the underlying workbook, so it is used via `use { }`.
 */
object ExcelWriter {

    // ============ XLSX writers ============

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
            workbook.write(out)
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
            workbook.write(out)
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
            workbook.write(out)
        }
    }

    // ============ CSV writers (RFC 4180, ; delimiter, UTF-8 BOM) ============

    /**
     * Writes the employee list as CSV (RFC 4180). Headers come from [headersFor] so the data layer
     * never embeds user-facing strings. Uses `;` as delimiter for better locale compatibility
     * (European Excel expects `;` when the decimal separator is `,`), and quotes all fields.
     */
    fun writeEmployeesCsv(
        out: OutputStream,
        employees: List<Employee>,
        headersFor: (EmployeeField) -> String,
    ) {
        val writer = OutputStreamWriter(out, CSV_CHARSET).use { w ->
            // BOM for UTF-8 so Excel opens it without mojibake
            w.write("\uFEFF")
            // Header row
            val headers = EmployeeField.entries.map { headersFor(it) }
            w.write(escapeCsvRow(headers))
            w.write("\n")
            // Data rows
            for (employee in employees) {
                val row = EmployeeField.entries.map { field -> csvValue(employee, field) }
                w.write(escapeCsvRow(row))
                w.write("\n")
            }
        }
    }

    /**
     * Writes the analytics snapshot as a single CSV sheet with the same logical blocks as the XLSX
     * version. [labels] follows the `ANALYTICS_*` positional contract.
     */
    fun writeAnalyticsCsv(
        out: OutputStream,
        snapshot: AnalyticsSnapshot,
        labels: List<String>,
    ) {
        require(labels.size >= ANALYTICS_INDEX) { "analytics labels must cover all ANALYTICS_INDEX entries" }
        val writer = OutputStreamWriter(out, CSV_CHARSET).use { w ->
            w.write("\uFEFF")

            // Overview title
            w.write(escapeCsvCell(labels[ANALYTICS_OVERVIEW_TITLE]))
            w.write("\n")
            // Stats block
            val stats = listOf(
                Pair(labels[ANALYTICS_TOTAL_ROW], snapshot.total.toString()),
                Pair(labels[ANALYTICS_AVERAGE_AGE], snapshot.averageAge?.formatNumber() ?: ""),
                Pair(labels[ANALYTICS_MEDIAN_AGE], snapshot.medianAge?.formatNumber() ?: ""),
                Pair(labels[ANALYTICS_AVERAGE_SERVICE], snapshot.averageService?.formatNumber() ?: ""),
                Pair(labels[ANALYTICS_MEDIAN_SERVICE], snapshot.medianService?.formatNumber() ?: ""),
                Pair(labels[ANALYTICS_RETIREMENT_SOON], snapshot.retirementSoon.toString()),
            )
            for ((label, value) in stats) {
                w.write(escapeCsvRow(listOf(label, value)))
                w.write("\n")
            }

            // Age buckets
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_AGE_TITLE])))
            w.write("\n")
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_COL_LABEL], labels[ANALYTICS_COL_COUNT])))
            w.write("\n")
            for (bucket in snapshot.ageBuckets) {
                w.write(escapeCsvRow(listOf(bucket.label, bucket.count.toString())))
                w.write("\n")
            }

            // Service buckets
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_SERVICE_TITLE])))
            w.write("\n")
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_COL_LABEL], labels[ANALYTICS_COL_COUNT])))
            w.write("\n")
            for (bucket in snapshot.serviceBuckets) {
                w.write(escapeCsvRow(listOf(bucket.label, bucket.count.toString())))
                w.write("\n")
            }

            // Department buckets
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_DEPARTMENT_TITLE])))
            w.write("\n")
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_COL_LABEL], labels[ANALYTICS_COL_COUNT])))
            w.write("\n")
            for (bucket in snapshot.departmentCounts) {
                w.write(escapeCsvRow(listOf(bucket.label, bucket.count.toString())))
                w.write("\n")
            }

            // Grade buckets
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_GRADE_TITLE])))
            w.write("\n")
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_COL_LABEL], labels[ANALYTICS_COL_COUNT])))
            w.write("\n")
            for (bucket in snapshot.gradeCounts) {
                w.write(escapeCsvRow(listOf(bucket.label, bucket.count.toString())))
                w.write("\n")
            }

            // Longest serving
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_LONGEST_TITLE])))
            w.write("\n")
            w.write(escapeCsvRow(listOf(labels[ANALYTICS_COL_LABEL], labels[ANALYTICS_COL_COUNT])))
            w.write("\n")
            for (summary in snapshot.longestServing) {
                w.write(escapeCsvRow(listOf(summary.fullName, summary.serviceYears?.toString() ?: "")))
                w.write("\n")
            }
        }
    }

    /** RFC 4180 escaping: quotes fields containing delimiter, quotes, or newlines; doubles inner quotes. */
    private fun escapeCsvCell(value: String): String {
        val needsQuoting = value.contains(CSV_DELIMITER) || value.contains('"') || value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }

    private fun escapeCsvRow(cells: List<String>): String =
        cells.map { escapeCsvCell(it) }.joinToString(CSV_DELIMITER.toString())

    private fun csvValue(employee: Employee, field: EmployeeField): String = when (field) {
        EmployeeField.FULL_NAME -> employee.fullName
        EmployeeField.DATE_OF_BIRTH -> employee.dateOfBirth?.toString() ?: ""
        EmployeeField.HIRE_DATE -> employee.hireDate?.toString() ?: ""
        EmployeeField.DEPARTMENT -> employee.department ?: ""
        EmployeeField.POSITION -> employee.position ?: ""
        EmployeeField.GRADE -> employee.grade ?: ""
        EmployeeField.SALARY -> employee.salary?.toString() ?: ""
        EmployeeField.EMAIL -> employee.email ?: ""
        EmployeeField.PHONE -> employee.phone ?: ""
        EmployeeField.ADDRESS -> employee.address ?: ""
        EmployeeField.NOTES -> employee.notes ?: ""
    }

    // ---- XLSX cell writing helpers ----

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
            val e = entries[i] as Pair<*, *>
            val r = sheet.createRow(row + i)
            r.createCell(0).apply {
                setCellValue(e.first.toString())
                cellStyle = headerStyle
            }
            r.createCell(1).setCellValue(e.second.toString())
        }
        return row + entries.size + 1
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
        setFont(font)
        fillForegroundColor = IndexedColors.DARK_BLUE.index
        setFillPattern(FillPatternType.SOLID_FOREGROUND)
        setAlignment(HorizontalAlignment.CENTER)
    }

    private fun org.apache.poi.ss.usermodel.Workbook.titleStyle(): CellStyle = createCellStyle().apply {
        val font = createFont().apply { bold = true }
        setFont(font)
        setAlignment(HorizontalAlignment.LEFT)
    }

    private fun org.apache.poi.ss.usermodel.Workbook.dateStyle(): CellStyle = createCellStyle().apply {
        dataFormat = createDataFormat().getFormat(DATE_FORMAT)
        // skip border for minimal compatibility
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

    /** UTF-8 with BOM so Excel recognises the encoding. */
    private val CSV_CHARSET = java.nio.charset.StandardCharsets.UTF_8
    /** Semicolon delimiter for locale-safe CSV (European Excel prefers `;`). */
    private const val CSV_DELIMITER = ';'
}