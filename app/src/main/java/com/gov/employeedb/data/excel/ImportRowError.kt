package com.gov.employeedb.data.excel

/**
 * One rejected cell or whole row during import (PLAN.md §6.3). [sourceRow] is the 1-based spreadsheet
 * row number; [column] is the raw header of the offending cell, and `null` for whole-row problems.
 * The raw value is preserved verbatim so the error report can show exactly what was read.
 */
data class ImportRowError(
    val sourceRow: Int,
    val column: String?,
    val rawValue: String,
    val reason: ImportErrorReason,
)

/** Why a row or cell was flagged, surfaced verbatim in the exported error report (PLAN.md §6.3). */
enum class ImportErrorReason {
    MISSING_REQUIRED,
    INVALID_DATE,
    INVALID_NUMBER,
    DUPLICATE,
    TOO_LONG,
    UNPARSEABLE,
}