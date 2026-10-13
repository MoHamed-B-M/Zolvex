package com.gov.employeedb.data.excel

/**
 * MIME types and filename predicates for the Excel import/export surface (PLAN.md §6.3).
 *
 * The file picker restricts itself to [`OPEN_TYPES`]; only `.xlsx` is parsed or written — HSSF
 * (`.xls`) would add ≈1.5 MB to the APK for a format the brief does not require, and `.xlsm` is a
 * macro workbook the app must not open.
 */
object ExcelMime {

    const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Text/fallback types so government files stored under a generic MIME still get picked. */
    const val OCTET_STREAM = "application/octet-stream"
    const val WILDCARD = "*/*"

    val OPEN_TYPES: Array<String> = arrayOf(XLSX_MIME, OCTET_STREAM, WILDCARD)

    /** True when [name] looks like an `.xlsx` document (extension check, case-insensitive). */
    fun isXlsxName(name: String): Boolean = name.lowercase().endsWith(XLSX_EXTENSION)

    /** True for any spreadsheet extension we can recognize, whether supported or not. */
    fun isSpreadsheetName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(XLSX_EXTENSION) || lower.endsWith(XLS_EXTENSION) || lower.endsWith(XLSM_EXTENSION)
    }

    private const val XLSX_EXTENSION = ".xlsx"
    private const val XLS_EXTENSION = ".xls"
    private const val XLSM_EXTENSION = ".xlsm"
}