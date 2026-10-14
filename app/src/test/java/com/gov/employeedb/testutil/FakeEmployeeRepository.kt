package com.gov.employeedb.testutil

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The [EmployeeRepository] the single-employee tests drive.
 *
 * [rows] is a stream so a test can assert that the detail screen follows a live update, and the
 * [created] / [updated] / [deleted] lists record what the form actually persisted. [readFailure]
 * and [writeFailure] are the two failure switches: the former makes `observeById` throw, the latter
 * makes every write throw, which is how the retry and save-failure paths are reached without a
 * database. Reads and writes are separated on purpose, so a test can fail the write of a row it
 * loaded successfully.
 */
class FakeEmployeeRepository : EmployeeRepository {

    val rows = MutableStateFlow<List<Employee>>(emptyList())

    val created = mutableListOf<Employee>()
    val updated = mutableListOf<Employee>()
    val deleted = mutableListOf<Long>()

    var readFailure: Throwable? = null
    var writeFailure: Throwable? = null

    override fun observeById(id: Long): Flow<Employee?> {
        readFailure?.let { throw it }
        return rows.map { list -> list.firstOrNull { row -> row.id == id } }
    }

    override suspend fun create(employee: Employee): Long {
        failWrite()
        created += employee
        return created.size.toLong()
    }

    override suspend fun update(employee: Employee) {
        failWrite()
        updated += employee
    }

    override suspend fun delete(id: Long) {
        failWrite()
        deleted += id
    }

    override fun observeEmployees(
        query: String,
        department: String,
        birthFrom: Long?,
        birthTo: Long?,
        hireFrom: Long?,
        hireTo: Long?,
        sortKey: String,
        ascending: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<EmployeeSummary>> = flowOf(emptyList())

    override fun observeCount(): Flow<Int> = flowOf(0)

    override fun observeStatRows(): Flow<List<EmployeeStatRow>> = flowOf(emptyList())

    override fun observeRecent(limit: Int): Flow<List<EmployeeSummary>> = flowOf(emptyList())

    override suspend fun existingImportKeys(): List<ImportKeyRow> = emptyList()

    override suspend fun insertImportBatch(employees: List<Employee>): List<Long> = emptyList()

    override suspend fun getAllForExport(): List<Employee> = emptyList()

    override suspend fun deleteAll() = Unit

    private fun failWrite() {
        writeFailure?.let { throw it }
    }
}
