package com.gov.employeedb.core

/**
 * Typed failure sentinel produced by the data layer (PLAN.md §6.3, Phase-7 wizard).
 *
 * A failure is intentionally opaque to the presentation layer below the resource level: the wizard
 * maps each variant to a localized string and an inline retry action, so no `message` here is ever
 * shown verbatim (the app never hardcodes user-facing strings).
 */
sealed interface AppError {
    /** Opening or parsing a file failed (or its stream could not be created). */
    data class FileRead(
        val fileName: String?,
        val detail: String?,
    ) : AppError

    /** The picked document is not a `.xlsx` workbook (`.xls`/`.xlsm`/other — PLAN.md §6.3). */
    data class UnsupportedFile(val fileName: String) : AppError
}