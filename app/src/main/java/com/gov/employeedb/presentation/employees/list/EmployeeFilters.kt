package com.gov.employeedb.presentation.employees.list

import androidx.compose.runtime.Immutable
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.ServiceBucket

/**
 * The filters currently applied to the list (PLAN.md Phase-6 checklist).
 *
 * [query] lives here as well as on the UI state so one object fully describes "what the list is
 * showing". Each chip group maps one-to-one onto a single-value DAO parameter (§5.6), which is why
 * the sheet keeps at most one selection per group; [departments] therefore holds 0 or 1 entry.
 */
@Immutable
data class EmployeeFilters(
    val query: String = "",
    val departments: Set<String> = emptySet(),
    val ageBuckets: Set<AgeBucket> = emptySet(),
    val serviceBuckets: Set<ServiceBucket> = emptySet(),
) {

    /** True when at least one chip group narrows the list (the query is tracked separately). */
    val isActive: Boolean
        get() = departments.isNotEmpty() || ageBuckets.isNotEmpty() || serviceBuckets.isNotEmpty()

    /** How many chip selections are active, shown on the filter chip. */
    val activeCount: Int
        get() = departments.size + ageBuckets.size + serviceBuckets.size

    /** The single selected department, or `""` for "all" — the DAO's equality parameter. */
    val department: String
        get() = departments.firstOrNull().orEmpty()

    /** The single selected age band, or `null` for "all". */
    val ageBucket: AgeBucket?
        get() = ageBuckets.firstOrNull()

    /** The single selected service band, or `null` for "all". */
    val serviceBucket: ServiceBucket?
        get() = serviceBuckets.firstOrNull()
}
