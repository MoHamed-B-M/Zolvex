package com.gov.employeedb.presentation.importx

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gov.employeedb.R
import com.gov.employeedb.core.AppError
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.data.excel.ImportProgress
import com.gov.employeedb.data.excel.ImportResult
import com.gov.employeedb.domain.model.EmployeeField

/**
 * The wizard's five positions (PLAN.md Phase-7 checklist). The horizontal pager has four pages,
 * because [IMPORTING] is the progress face of the preview page rather than a page of its own —
 * [pageIndex] is what the pager and the step dots scroll to.
 */
enum class ImportStep(@StringRes val labelRes: Int, val pageIndex: Int) {
    SELECT(R.string.import_step_select, 0),
    MAPPING(R.string.import_step_map, 1),
    PREVIEW(R.string.import_step_preview, 2),
    IMPORTING(R.string.import_step_preview, 2),
    RESULT(R.string.import_step_result, 3),
    ;

    companion object {
        /** The four dots drawn under the top bar, in order. */
        val DOTS: List<ImportStep> = listOf(SELECT, MAPPING, PREVIEW, RESULT)
    }
}

/**
 * Everything the import wizard renders from (PLAN.md Phase-7 checklist).
 *
 * The state lives in the ViewModel, so it survives the pager recreation when its host leaves the
 * composition. [mapping] only carries mapped columns — an ignored column is simply absent — which
 * makes duplicate detection and the pipeline payload the same object.
 */
@Immutable
data class ImportUiState(
    val step: ImportStep = ImportStep.SELECT,
    val uri: Uri? = null,
    val fileName: String? = null,
    val preview: ExcelPreview? = null,
    val mapping: Map<Int, EmployeeField> = emptyMap(),
    val skipDuplicates: Boolean = true,
    val isReadingFile: Boolean = false,
    val isDownloadingReport: Boolean = false,
    val progress: ImportProgress? = null,
    val result: ImportResult? = null,
    val error: AppError? = null,
) {

    /** The columns the detector (or a stored profile) already mapped; highlighted in the list. */
    val autoDetectedColumns: Set<Int>
        get() = preview?.detectedMapping?.keys ?: emptySet()

    /** The first field claimed by more than one column, or `null` when every field is unique. */
    val duplicateField: EmployeeField?
        get() = mapping.values
            .groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key

    /** Every field an import cannot proceed without has a column. */
    val coreFieldsMapped: Boolean
        get() = EmployeeField.REQUIRED_CORE.all { it in mapping.values }

    /** The mapping can be confirmed (PLAN.md §6.2 rule 5). */
    val mappingValid: Boolean
        get() = coreFieldsMapped && duplicateField == null

    /** Rows already handled, from the live progress event. */
    val processed: Int
        get() = when (val event = progress) {
            is ImportProgress.Progress -> event.processed
            is ImportProgress.Done -> event.imported + event.skippedDuplicates + event.failed
            else -> 0
        }

    /** The sheet's row estimate, from the latest progress event or the preview. */
    val totalRows: Int
        get() = when (val event = progress) {
            is ImportProgress.Started -> event.totalRows
            is ImportProgress.Progress -> event.totalRows
            else -> preview?.totalRowEstimate ?: 0
        }

    /** `0f..1f` for the progress ring, safe while the total is still unknown. */
    val progressFraction: Float
        get() = if (totalRows <= 0) 0f else (processed.toFloat() / totalRows).coerceIn(0f, 1f)
}
