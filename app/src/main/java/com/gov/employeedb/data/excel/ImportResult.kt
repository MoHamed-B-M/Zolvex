package com.gov.employeedb.data.excel

/**
 * Rolling totals accumulated while an import runs and surfaced on the Result step (PLAN.md §6.3).
 * [imported] counts rows actually written (a row with a non-fatal `INVALID_NUMBER` is still
 * written — salary falls back to `null`), while [failed] counts rows that produced at least one
 * error, so the two overlap exactly as the malformed-fixture contract expects
 * (`imported == 6`, `failed == 3` on `malformed.xlsx`).
 */
data class ImportResult(
    val imported: Int,
    val skippedDuplicates: Int,
    val failed: Int,
    val errors: List<ImportRowError>,
    val batchId: String,
    val errorsTruncated: Boolean,
) {
    val processed: Int get() = imported + skippedDuplicates + failed
}