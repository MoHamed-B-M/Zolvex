package com.gov.employeedb.data.repository

import com.gov.employeedb.data.local.dao.EmployeeDao
import com.gov.employeedb.data.local.projection.EmployeeListItemEntity
import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.data.mapper.EmployeeMappers
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.repository.EmployeeRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [EmployeeRepository] backed by Room; entity ↔ domain mapping happens here, never in the use cases. */
@Singleton
class EmployeeRepositoryImpl @Inject constructor(
    private val dao: EmployeeDao,
) : EmployeeRepository {

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
    ): Flow<List<EmployeeSummary>> = dao.observePage(
        q = query,
        department = department,
        birthFrom = birthFrom,
        birthTo = birthTo,
        hireFrom = hireFrom,
        hireTo = hireTo,
        sortKey = sortKey,
        ascending = ascending,
        limit = limit,
        offset = offset,
    ).mapSummary()

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override fun observeStatRows(): Flow<List<EmployeeStatRow>> = dao.observeStatRows()

    override fun observeRecent(limit: Int): Flow<List<EmployeeSummary>> =
        dao.observeRecent(limit).mapSummary()

    override fun observeById(id: Long): Flow<Employee?> =
        dao.observeById(id).map { entity -> entity?.let(EmployeeMappers::fromEntity) }

    override suspend fun create(employee: Employee): Long =
        dao.insert(EmployeeMappers.toEntity(employee.stamped()))

    override suspend fun existingImportKeys(): List<ImportKeyRow> = dao.getImportKeys()

    override suspend fun insertImportBatch(employees: List<Employee>): List<Long> =
        dao.insertBatch(employees.map { EmployeeMappers.toEntity(it.stamped()) })

    override suspend fun getAllForExport(): List<Employee> =
        dao.getAll().map(EmployeeMappers::fromEntity)

    override suspend fun update(employee: Employee) =
        dao.update(EmployeeMappers.toEntity(employee.copy(updatedAt = System.currentTimeMillis())))

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()

    /** Fills [Employee.createdAt]/[Employee.updatedAt] only when the caller left them unset. */
    private fun Employee.stamped(): Employee {
        val now = System.currentTimeMillis()
        return copy(
            createdAt = createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
        )
    }

    /** Maps every page the same way, always with a single `today` per emission. */
    private fun Flow<List<EmployeeListItemEntity>>.mapSummary(): Flow<List<EmployeeSummary>> =
        map { items ->
            val today = LocalDate.now()
            items.map { EmployeeMappers.toSummary(it, today) }
        }
}