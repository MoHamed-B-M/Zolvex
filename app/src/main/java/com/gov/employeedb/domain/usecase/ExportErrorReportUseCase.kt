package com.gov.employeedb.domain.usecase

import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelWriter
import com.gov.employeedb.data.excel.ImportRowError
import com.gov.employeedb.data.excel.ExcelWriter.ErrorReportLabels
import com.gov.employeedb.data.excel.SafDocumentHelper
import com.gov.employeedb.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * The Result step's error report (PLAN.md §6.1/§6.4): writes the rejected rows as a real `.xlsx`
 * (`Errors` sheet) through SAF `CreateDocument`, so Excel/LibreOffice open it without a CSV
 * encoding round-trip. [labels] are supplied by the caller, including an optional truncation note.
 */
class ExportErrorReportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val writer: ExcelWriter,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        uri: Uri,
        errors: List<ImportRowError>,
        labels: ErrorReportLabels,
    ): AppResult<Unit> = withContext(dispatcher) {
        val name = SafDocumentHelper.displayName(context, uri)
        val out = SafDocumentHelper.openOutput(context, uri)
        if (out == null) {
            return@withContext AppResult.Failure(AppError.FileRead(name, "cannot open output"))
        }
        try {
            out.use { stream -> writer.writeErrorReport(stream, errors, labels) }
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            AppResult.Failure(AppError.FileRead(name, t.message ?: t.javaClass.simpleName))
        }
    }
}