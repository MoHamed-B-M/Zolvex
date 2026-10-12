package com.gov.employeedb.data.local.projection

import androidx.room.TypeConverters
import com.gov.employeedb.data.local.Converters
import java.time.LocalDate

/**
 * The row shape consumed by the employee list and "recent" queries. Column aliases in the DAO SQL
 * (`birth_date AS dateOfBirth`, `hire_date AS hireDate`) bind these plain constructor parameters
 * directly, so no `@ColumnInfo` is needed on a projection.
 */
@TypeConverters(Converters::class)
data class EmployeeListItemEntity(
    val id: Long,
    val fullName: String,
    val department: String?,
    val position: String?,
    val dateOfBirth: LocalDate?,
    val hireDate: LocalDate?,
)