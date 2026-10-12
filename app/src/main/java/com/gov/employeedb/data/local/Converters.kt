package com.gov.employeedb.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Serializes [LocalDate] as its epochDay (INTEGER) so date columns sort, filter and migrate exactly
 * as numbers. The converters take non-null values on purpose: Room 2.7+ "TypeConverter analyzer"
 * only matches non-null receiver/return types and wraps null handling itself, so an entity field of
 * type `LocalDate?` still round-trips through these functions (PLAN.md Appendix A, A16).
 *
 * Converters are class members because Room only discovers `@TypeConverter`s declared on a
 * class/object referenced from `@Database(typeConverters = ...)` — never on top-level functions.
 */
class Converters {

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}