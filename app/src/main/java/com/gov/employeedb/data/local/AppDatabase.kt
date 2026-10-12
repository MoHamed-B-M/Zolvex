package com.gov.employeedb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gov.employeedb.data.local.dao.EmployeeDao
import com.gov.employeedb.data.local.entity.EmployeeEntity

/**
 * The single Room database. `exportSchema = true` writes the JSON consumed by the migration tests,
 * and [com.gov.employeedb.di.DatabaseModule] wires it with `.addMigrations(*Migrations.ALL)`.
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