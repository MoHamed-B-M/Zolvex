package com.gov.employeedb.data.local.projection

import androidx.room.TypeConverters
import com.gov.employeedb.data.local.Converters
import java.time.LocalDate

/**
 * The statistic projection feeding [com.gov.employeedb.domain.analytics.WorkforceAnalytics]. The
 * identity columns ([id], [fullName], [position]) are selected too, because
 * `AnalyticsSnapshot.longestServing` needs a display name and the dashboard recents build rows from
 * summaries (PLAN.md Appendix A, A16); the DAO aliases the date columns to match these field names.
 */
@TypeConverters(Converters::class)
data class EmployeeStatRow(
    val id: Long,
    val fullName: String,
    val position: String?,
    val birthDate: LocalDate?,
    val hireDate: LocalDate?,
    val department: String?,
    val grade: String?,
)