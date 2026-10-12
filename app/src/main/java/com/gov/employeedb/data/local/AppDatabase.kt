package com.gov.employeedb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gov.employeedb.data.local.dao.EmployeeDao
import com.gov.employeedb.data.local.entity.EmployeeEntity

/**
 * The single Room database. `exportSchema = true` writes the JSON consumed by the migration tests,
 * and [com.gov.employeedb.di.DatabaseModule] wires it with `.addMigrations(*Migrations.ALL)`.
 *
 * `Converters` is registered here AND on the entity/DAO/projection classes (fully qualified) as a
 * robustness measure: Room 2.8.5's KSP2 processor did not apply database-scope converters in CI
 * ("cannot figure out how to save this property"), so every scope the processor honors is covered
 * (PLAN.md Appendix A, A16).
 */
@Database(
    entities = [EmployeeEntity::class],
    version = 1,
    exportSchema = true,
    typeConverters = [com.gov.employeedb.data.local.Converters::class],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
}