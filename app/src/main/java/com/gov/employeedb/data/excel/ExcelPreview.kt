package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.EmployeeField

/**
 * What the import wizard shows in Step 3 (PLAN.md §6.1): the discovered header row, the raw cells of
 * the first data rows, and the detected column mapping. [totalRowEstimate] is `"about N rows"` — the
 * sheet's `<row>` count minus the header row, so progress bars and the preview line are honest.
 */
data class ExcelPreview(
    val sheetName: String,
    /** 0-based index of the discovered header row among rows 0..9 scanned by [ColumnDetector.findHeaderRow]. */
    val headerRowIndex: Int,
    val headers: List<String>,
    val detectedMapping: Map<Int, EmployeeField>,
    val sampleRows: List<List<String?>>,
    val totalRowEstimate: Int,
)