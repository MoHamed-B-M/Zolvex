package com.gov.employeedb.data.excel

/**
 * Export format selection (Phase 11 Settings UI). Each format has its own MIME type and extension.
 */
enum class ExportFormat {
    XLSX,
    CSV,
}

/**
 * MIME types and filename predicates for the Excel/CSV import/export surface (PLAN.md §6.3).
 *
 * The file picker restricts itself to [`OPEN_TYPES`]; only `.xlsx` and `.csv` are parsed or written.
 */
object ExcelMime {

    const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val CSV_MIME = "text/csv"

    /** Text/fallback types so government files stored under a generic MIME still get picked. */
    const val OCTET_STREAM = "application/octet-stream"
    const val WILDCARD = "*/*"

    val OPEN_TYPES: Array<String> = arrayOf(XLSX_MIME, CSV_MIME, OCTET_STREAM, WILDCARD)

    /** True when [name] looks like an `.xlsx` document (extension check, case-insensitive). */
    fun isXlsxName(name: String): Boolean = name.lowercase().endsWith(XLSX_EXTENSION)

    /** True when [name] looks like a `.csv` document (extension check, case-insensitive). */
    fun isCsvName(name: String): Boolean = name.lowercase().endsWith(CSV_EXTENSION)

    /** True for any spreadsheet extension we can recognize, whether supported or not. */
    fun isSpreadsheetName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(XLSX_EXTENSION) || lower.endsWith(CSV_EXTENSION) || lower.endsWith(XLS_EXTENSION) || lower.endsWith(XLSM_EXTENSION)
    }

    fun mimeForFormat(format: ExportFormat): String = when (format) {
        ExportFormat.XLSX -> XLSX_MIME
        ExportFormat.CSV -> CSV_MIME
    }

    fun extensionForFormat(format: ExportFormat): String = when (format) {
        ExportFormat.XLSX -> XLSX_EXTENSION
        ExportFormat.CSV -> CSV_EXTENSION
    }

    private const val XLSX_EXTENSION = ".xlsx"
    private const val CSV_EXTENSION = ".csv"
    private const val XLS_EXTENSION = ".xls"
    private const val XLSM_EXTENSION = ".xlsm"
}