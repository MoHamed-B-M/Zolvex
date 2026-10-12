package com.gov.employeedb.domain.repository

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import kotlinx.coroutines.flow.Flow

/**
 * The single data gateway consumed by the use cases. The paging/filter parameters mirror the DAO
 * query shape (PLAN.md §5.6); the implementation owns entity ↔ domain mapping.
 */
interface EmployeeRepository {

    /** One windowed page (`LIMIT :limit OFFSET :offset`) of the filtered, sorted employee list. */
    fun observeEmployees(
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
    ): Flow<List<EmployeeSummary>>

    /** Total number of stored employees, reactive. */
    fun observeCount(): Flow<Int>

    /** The stat projection feeding [com.gov.employeedb.domain.analytics.WorkforceAnalytics]. */
    fun observeStatRows(): Flow<List<EmployeeStatRow>>

    /** The newest employees by `createdAt` desc. */
    fun observeRecent(limit: Int): Flow<List<EmployeeSummary>>

    /** A single employee that live-updates when its row changes (detail screen). */
    fun observeById(id: Long): Flow<Employee?>

    /** Inserts [employee] and returns its generated row id. */
    suspend fun create(employee: Employee): Long

    suspend fun update(employee: Employee)

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}