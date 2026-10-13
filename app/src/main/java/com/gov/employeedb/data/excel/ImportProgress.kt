package com.gov.employeedb.data.excel

/**
 * The event stream of an ongoing import, exactly per PLAN.md §6.3.
 *
 * [Started] carries the sheet's data-row estimate; [Progress] advances until [Done]; [RowFailed] is
 * emitted per rejected row so the wizard can react to individual failures; [Done] closes the run with
 * the final counters and a [batchId] that groups every inserted row for later traceability
 * (a `DELETE ... WHERE importBatchId = ?` affordance on the Result screen).
 */
sealed interface ImportProgress {

    data class Started(val totalRows: Int) : ImportProgress

    data class Progress(val processed: Int, val totalRows: Int) : ImportProgress

    data class RowFailed(val error: ImportRowError) : ImportProgress

    data class Done(
        val imported: Int,
        val skippedDuplicates: Int,
        val failed: Int,
        val errors: List<ImportRowError>,
        val batchId: String,
    ) : ImportProgress
}