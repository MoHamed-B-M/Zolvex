package com.gov.employeedb.domain.usecase

import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExcelWriter
import com.gov.employeedb.data.excel.ExportFormat
import com.gov.employeedb.data.excel.SafDocumentHelper
import com.gov.employeedb.di.IoDispatcher
import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.repository.EmployeeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Analytics export (PLAN.md §6 / Phase 10): computes the workforce snapshot over the stat rows with
 * an explicit [today] and writes the "Analytics" sheet. [labels] follow [ExcelWriter]'s positional
 * contract so the caller (later phases) decides the locale.
 *
 * Supports both XLSX (streaming `SXSSFWorkbook`) and CSV (RFC 4180 with `;` delimiter) formats.
 */
class ExportAnalyticsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EmployeeRepository,
    private val writer: ExcelWriter,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        uri: Uri,
        today: LocalDate,
        labels: List<String>,
        format: ExportFormat = ExportFormat.XLSX,
    ): AppResult<Unit> = withContext(dispatcher) {
        val name = SafDocumentHelper.displayName(context, uri)
        val out = SafDocumentHelper.openOutput(context, uri)
        if (out == null) {
            return@withContext AppResult.Failure(AppError.FileRead(name, "cannot open output"))
        }
        try {
            out.use { stream ->
                val snapshot = WorkforceAnalytics.compute(repository.observeStatRows().first(), today)
                when (format) {
                    ExportFormat.XLSX ->
                        writer.writeAnalytics(stream, snapshot, labels)
                    ExportFormat.CSV ->
                        writer.writeAnalyticsCsv(stream, snapshot, labels)
                }
            }
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            AppResult.Failure(AppError.FileRead(name, t.message ?: t.javaClass.simpleName))
        }
    }
}