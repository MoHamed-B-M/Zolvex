package com.gov.employeedb.data.local

import androidx.room.migration.Migration

/**
 * Schema evolution, oldest first. Version 1 is created directly from [AppDatabase]'s entity, so the
 * array is empty — but real, and passed through `.addMigrations(*Migrations.ALL)` so future
 * migrations live in exactly one place and destructive fallbacks stay banned (§5.6).
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}