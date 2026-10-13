package com.gov.employeedb.data.local.projection

/**
 * The `(fullNameKey, birth_date)` pair the import pipeline preloads once into a `HashSet` of
 * fingerprints (PLAN.md §6.3) so duplicate detection is a set lookup instead of N queries.
 * [birthEpochDay] is nullable because the stored `birth_date` column is nullable.
 */
data class ImportKeyRow(
    val fullNameKey: String,
    val birthEpochDay: Long?,
)