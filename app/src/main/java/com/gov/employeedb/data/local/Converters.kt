package com.gov.employeedb.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Serializes [LocalDate] as its epochDay (INTEGER) so date columns sort, filter and migrate exactly
 * as numbers. Converters are class members because Room only discovers `@TypeConverter`s declared on
 * a class/object referenced from `@Database(typeConverters = ...)` — never on top-level functions
 * (PLAN.md Appendix A, A16).
 */
class Converters {

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}