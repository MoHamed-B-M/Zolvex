package com.gov.employeedb.domain.usecase

import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExcelWriter
import com.gov.employeedb.data.excel.SafDocumentHelper
import com.gov.employeedb.di.IoDispatcher
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.repository.EmployeeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Full-data export (PLAN.md §6 / Phase 11 settings screen): writes every employee ordered by name
 * into the SAF [uri] obtained from `CreateDocument`. Column headers come from the caller's
 * [headersFor] so the data layer stays free of hardcoded user-facing strings.
 *
 * Supports both XLSX (streaming `SXSSFWorkbook`) and CSV (RFC 4180 with `;` delimiter) formats.
 */
class ExportEmployeesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EmployeeRepository,
    private val writer: ExcelWriter,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        uri: Uri,
        headersFor: (EmployeeField) -> String,
        format: ExcelMime.ExportFormat = ExcelMime.ExportFormat.XLSX,
    ): AppResult<Unit> = withContext(dispatcher) {
        val name = SafDocumentHelper.displayName(context, uri)
        val out = SafDocumentHelper.openOutput(context, uri)
        if (out == null) {
            return@withContext AppResult.Failure(AppError.FileRead(name, "cannot open output"))
        }
        try {
            out.use { stream ->
                when (format) {
                    ExcelMime.ExportFormat.XLSX ->
                        writer.writeEmployees(stream, repository.getAllForExport(), headersFor)
                    ExcelMime.ExportFormat.CSV ->
                        writer.writeEmployeesCsv(stream, repository.getAllForExport(), headersFor)
                }
            }
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            AppResult.Failure(AppError.FileRead(name, t.message ?: t.javaClass.simpleName))
        }
    }
}