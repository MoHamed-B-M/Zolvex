package com.gov.employeedb.presentation.importx

import androidx.annotation.StringRes
import com.gov.employeedb.R
import com.gov.employeedb.core.AppError

/**
 * [AppError] → localized line for the wizard's inline failure (PLAN.md Phase-7 checklist: "retry on
 * error"). The mapping lives here rather than in `AppError` so the data layer keeps no dependency on
 * Android resources, and no branch can fall through to an untranslated literal.
 */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    is AppError.FileRead -> R.string.file_read_error
    is AppError.UnsupportedFile -> R.string.unsupported_file_type
}
