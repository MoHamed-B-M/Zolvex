package com.gov.employeedb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gov.employeedb.data.local.dao.EmployeeDao
import com.gov.employeedb.data.local.entity.EmployeeEntity

/**
 * The single Room database. `exportSchema = true` writes the JSON consumed by the migration tests,
 * and [com.gov.employeedb.di.DatabaseModule] wires it with `.addMigrations(*Migrations.ALL)`.
 *
 * [Converters] is registered on each consumer — the entity, the DAO and both projections — via
 * `@TypeConverters`; the database's `typeConverters` parameter is deliberately **not** used because
 * Room 2.8.5's KSP2 processor cannot read it ("No parameter with name 'typeConverters' found" is a
 * build error under this toolchain — PLAN.md Appendix A, A16).
 */
@Database(
    entities = [EmployeeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
}