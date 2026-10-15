package com.gov.employeedb.domain.usecase

import com.gov.employeedb.data.excel.ExcelReader
import com.gov.employeedb.data.excel.ImportProgress
import com.gov.employeedb.data.excel.ImportResult
import com.gov.employeedb.data.excel.ImportRowError
import com.gov.employeedb.data.excel.RowMapper
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.data.mapper.normalizedKey
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.di.IoDispatcher
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * The full import pipeline (PLAN.md §6.1 step 4 / §6.3): streams the sheet through
 * [ExcelReader.streamRows], maps each row with [RowMapper], dedupes by
 * `fullNameKey:birthEpochDay` (preloaded existing keys + in-file repeats, only when
 * [skipDuplicates] is on), and commits 500-row `@Transaction` chunks. A mid-import cancellation
 * leaves the already-committed chunks durable, per the plan's partial-import contract.
 */
class ImportExcelUseCase @Inject constructor(
    private val repository: EmployeeRepository,
    private val reader: ExcelReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    operator fun invoke(
        input: InputStream,
        headers: List<String>,
        mapping: Map<Int, EmployeeField>,
        skipDuplicates: Boolean,
        headerRowIndex: Int,
        totalRows: Int,
        today: LocalDate,
    ): Flow<ImportProgress> = flow {
        val batchId = UUID.randomUUID().toString()
        val existing = if (skipDuplicates) {
            repository.existingImportKeys().mapTo(mutableSetOf()) { it.toFingerprint() }
        } else {
            emptySet()
        }
        val seen = HashSet<String>()
        val errors = ArrayList<ImportRowError>(ERROR_CAP + 1)
        val buffer = ArrayList<Employee>(CHUNK)
        var imported = 0
        var skipped = 0
        var failed = 0

        emit(ImportProgress.Started(totalRows))

        var nextProgressAt = PROGRESS_EVERY
        reader.streamRows(input).collect { row ->
            // Header row plus any preamble rows (merged-title fixtures) are not data.
            if (row.sourceRow <= headerRowIndex + 1) return@collect

            val mapped = RowMapper.mapRow(row.cells, headers, mapping, today, row.sourceRow, batchId)
            if (mapped.errors.isNotEmpty()) {
                failed++
                if (errors.size < ERROR_CAP) errors += mapped.errors
                emit(ImportProgress.RowFailed(mapped.errors.first()))
            }
            val employee = mapped.employee
            if (employee != null) {
                val duplicated = skipDuplicates && (employee.toFingerprint() in existing || !seen.add(employee.toFingerprint()))
                if (duplicated) {
                    skipped++
                } else {
                    buffer += employee
                    if (buffer.size >= CHUNK) {
                        repository.insertImportBatch(buffer)
                        imported += buffer.size
                        buffer.clear()
                        emit(ImportProgress.Progress(imported + skipped + failed, totalRows))
                    }
                }
            }
            // Chunk commits alone leave files under 500 rows at 0% until Done; report
            // every settled batch of rows so the ring moves on any file size.
            if (imported + skipped + failed >= nextProgressAt) {
                nextProgressAt += PROGRESS_EVERY
                emit(ImportProgress.Progress(imported + skipped + failed, totalRows))
            }
        }
        if (buffer.isNotEmpty()) {
            repository.insertImportBatch(buffer)
            imported += buffer.size
            buffer.clear()
        }

        val result = ImportResult(
            imported = imported,
            skippedDuplicates = skipped,
            failed = failed,
            errors = errors.toList(),
            batchId = batchId,
            errorsTruncated = failed > ERROR_CAP,
        )
        emit(ImportProgress.Progress(imported + skipped + failed, totalRows))
        emit(ImportProgress.Done(result.imported, result.skippedDuplicates, result.failed, result.errors, result.batchId))
    }.flowOn(dispatcher)

    private fun Employee.toFingerprint(): String {
        val key = fullName.normalizedKey()
        val birth = dateOfBirth?.toEpochDay()
        return "$key:${birth ?: NULL_BIRTH}"
    }

    private fun ImportKeyRow.toFingerprint(): String = "$fullNameKey:${birthEpochDay ?: NULL_BIRTH}"

    private companion object {
        const val CHUNK = 500
        const val PROGRESS_EVERY = 50
        const val ERROR_CAP = 5000
        const val NULL_BIRTH = "null"
    }
}