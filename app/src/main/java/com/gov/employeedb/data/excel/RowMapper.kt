package com.gov.employeedb.data.excel

import com.gov.employeedb.core.LocaleDigits
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import java.text.Normalizer
import java.time.LocalDate

/**
 * Turns one raw spreadsheet row into a domain [Employee] (PLAN.md Phase-5 checklist). Every text
 * value is trimmed and NFC-normalized, Arabic-Indic digits are converted to ASCII, and the three
 * core fields are validated; a row missing a core field (or with an unparseable core date) is not
 * imported at all, while a non-fatal `INVALID_NUMBER` salary still imports with salary `null`
 * (PLAN.md §6.4). Errors carry the raw header name so the report is readable.
 */
object RowMapper {

    /** The outcome for one row: [employee] is `null` when a core error made the row unimportable. */
    data class RowMapResult(
        val employee: Employee?,
        val errors: List<ImportRowError>,
    )

    fun mapRow(
        cells: List<String?>,
        headers: List<String>,
        mapping: Map<Int, EmployeeField>,
        today: LocalDate,
        sourceRow: Int,
        batchId: String,
    ): RowMapResult {
        if (mapping.isEmpty()) {
            return RowMapResult(null, listOf(wholeRowError(sourceRow, ImportErrorReason.UNPARSEABLE)))
        }
        val fieldToColumn = mapping.entries.associate { (column, field) -> field to column }
        val errors = mutableListOf<ImportRowError>()
        val values = mutableMapOf<EmployeeField, String?>()

        for (field in EmployeeField.entries) {
            val column = fieldToColumn[field] ?: continue
            values[field] = clean(cells.getOrNull(column))
        }

        val fullName = values[EmployeeField.FULL_NAME]?.takeIf { it.isNotEmpty() }
        if (fullName == null) {
            errors += cellError(sourceRow, headers, fieldToColumn[EmployeeField.FULL_NAME], ImportErrorReason.MISSING_REQUIRED)
            return RowMapResult(null, errors.toList())
        }

        var fatal = false
        val dateOfBirth = values.checkDate(EmployeeField.DATE_OF_BIRTH, today, headers, fieldToColumn, sourceRow, errors) {
            fatal = true
        }
        val hireDate = values.checkDate(EmployeeField.HIRE_DATE, today, headers, fieldToColumn, sourceRow, errors) {
            fatal = true
        }

        val salaryRaw = values[EmployeeField.SALARY]
        val salary = parseSalary(salaryRaw)
        if (salaryRaw != null && salary == null) {
            errors += cellError(sourceRow, headers, fieldToColumn[EmployeeField.SALARY], salaryRaw, ImportErrorReason.INVALID_NUMBER)
        }

        if (fatal) return RowMapResult(null, errors.toList())

        val employee = Employee(
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            hireDate = hireDate,
            department = values[EmployeeField.DEPARTMENT],
            position = values[EmployeeField.POSITION],
            grade = values[EmployeeField.GRADE],
            salary = salary,
            email = values[EmployeeField.EMAIL],
            phone = values[EmployeeField.PHONE],
            address = values[EmployeeField.ADDRESS],
            notes = values[EmployeeField.NOTES],
            importBatchId = batchId,
            sourceRow = sourceRow,
        )
        return RowMapResult(employee, errors.toList())
    }

    }

    private fun Map<EmployeeField, String?>.checkDate(
        field: EmployeeField,
        today: LocalDate,
        headers: List<String>,
        fieldToColumn: Map<EmployeeField, Int>,
        sourceRow: Int,
        errors: MutableList<ImportRowError>,
        onFatal: () -> Unit,
    ): LocalDate? {
        val raw = this[field]
        if (raw.isNullOrEmpty()) {
            errors += cellError(sourceRow, headers, fieldToColumn[field], ImportErrorReason.MISSING_REQUIRED)
            onFatal()
            return null
        }
        val parsed = DateParser.parse(raw, today = today)
        if (parsed == null) {
            errors += cellError(sourceRow, headers, fieldToColumn[field], raw, ImportErrorReason.INVALID_DATE)
            onFatal()
        }
        return parsed
    }

    private fun cellError(
        sourceRow: Int,
        headers: List<String>,
        column: Int?,
        reason: ImportErrorReason,
    ): ImportRowError = ImportRowError(
        sourceRow = sourceRow,
        column = column?.let { headers.getOrNull(it) },
        rawValue = "",
        reason = reason,
    )

    private fun cellError(
        sourceRow: Int,
        headers: List<String>,
        column: Int?,
        rawValue: String,
        reason: ImportErrorReason,
    ): ImportRowError = ImportRowError(
        sourceRow = sourceRow,
        column = column?.let { headers.getOrNull(it) },
        rawValue = rawValue,
        reason = reason,
    )

    private fun wholeRowError(sourceRow: Int, reason: ImportErrorReason): ImportRowError =
        ImportRowError(sourceRow, column = null, rawValue = "", reason = reason)

    /** Trim → NFC → ASCII digits → blank collapses to `null`. */
    private fun clean(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
        val ascii = LocaleDigits.toAsciiDigits(normalized)
        return ascii.takeIf { it.isNotEmpty() }
    }

    /**
     * Lenient-but-strict currency parse (PLAN.md §6.4): whitespace, group separators and a single
     * trailing `.`/`,` decimal separator are accepted (`1 234,56` → `1234.56`), but any remaining
     * non-numeric character (a `€`/`$`/… symbol) yields `null` so the caller flags `INVALID_NUMBER`
     * and imports the row with salary `null`. Blank → `null` (no error, just no salary).
     */
    private fun parseSalary(raw: String?): Double? {
        val cleaned = raw?.let { LocaleDigits.toAsciiDigits(it.trim()) }?.takeIf { it.isNotEmpty() }
            ?: return null
        if (cleaned.any { !it.isDigit() && it != '.' && it != ',' && !it.isWhitespace() && it != NBSP }) {
            return null
        }
        // Drop all whitespace, then treat the LAST '.'/',' as the decimal separator.
        val digits = cleaned.filter { it.isDigit() || it == '.' || it == ',' }
        if (digits.isEmpty()) return null
        val lastDot = digits.lastIndexOf('.')
        val lastComma = digits.lastIndexOf(',')
        val decimalSeparator = if (lastDot > lastComma) '.' else ','
        val normalized = buildString {
            for (ch in digits) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == decimalSeparator -> append('.')
                }
            }
        }
        return normalized.toDoubleOrNull()
    }

    private const val NBSP = '\u00A0'
}