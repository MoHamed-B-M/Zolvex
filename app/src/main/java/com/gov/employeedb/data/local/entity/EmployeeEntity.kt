package com.gov.employeedb.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.gov.employeedb.data.local.Converters
import java.time.LocalDate

/**
 * The `employees` table (PLAN.md §5.5). Dates are stored via [com.gov.employeedb.data.local.Converters]
 * as epochDay INTEGERs so SQL range filters and ordering stay cheap.
 *
 * [fullNameKey] is a stored, normalized duplicate of [fullName] (`trim().lowercase(ROOT)`) that makes
 * the import dedupe lookup a single indexed query (composite with [dateOfBirth]); it never surfaces in
 * the domain model (PLAN.md §5.6).
 */
@TypeConverters(Converters::class)
@Entity(
    tableName = "employees",
    indices = [
        Index("fullName"),
        Index("department"),
        Index("hire_date"),
        Index("birth_date"),
        Index("importBatchId"),
        Index(value = ["fullNameKey", "birth_date"]),
    ],
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "fullName") val fullName: String,
    @ColumnInfo(name = "fullNameKey") val fullNameKey: String,
    @ColumnInfo(name = "birth_date") val dateOfBirth: LocalDate?,
    @ColumnInfo(name = "hire_date") val hireDate: LocalDate?,
    val department: String?,
    val position: String?,
    val grade: String?,
    val salary: Double?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val notes: String?,
    val importBatchId: String?,
    val sourceRow: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)