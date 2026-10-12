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
 * `Converters` is registered here AND on the entity/DAO/projection classes: Room 2.8.5's KSP2
 * processor does not apply database-scope-only converter registration, so each consumer also
 * declares [`@TypeConverters`]. The class reference stays a simple name (`Converters::class`) — a
 * fully-qualified literal breaks KSP2's annotation-argument parsing (PLAN.md Appendix A, A16).
 */
@Database(
    entities = [EmployeeEntity::class],
    version = 1,
    exportSchema = true,
    typeConverters = [Converters::class],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
}