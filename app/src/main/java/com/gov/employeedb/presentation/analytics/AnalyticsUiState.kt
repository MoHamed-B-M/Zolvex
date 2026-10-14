package com.gov.employeedb.presentation.analytics

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AnalyticsSnapshot

/**
 * Everything the analytics screen renders from (PLAN.md Phase-10 checklist).
 *
 * [snapshot] is the **filtered** view: once [selectedDepartment] is set it describes only that
 * department, so all four tabs and every chart tell the same story. [departments] is deliberately
 * the unfiltered list instead - the filter chips must stay on screen, and keep offering a way back to
 * "all departments", while their own tab is showing a filtered slice - which is why it is kept from
 * the last unfiltered read rather than derived from [snapshot].
 *
 * [isLoading] covers the first read only: a re-filter keeps the previous snapshot on screen so the
 * charts can animate from their old values to the new ones instead of blanking out. [isEmpty] is the
 * honest "the read succeeded and there is nothing to analyse", which the screen renders as the empty
 * state rather than drawing flat charts.
 */
@Immutable
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val snapshot: AnalyticsSnapshot? = null,
    val departments: List<String> = emptyList(),
    val selectedDepartment: String? = null,
    val isExporting: Boolean = false,
    val error: Boolean = false,
) {

    /** True when the read succeeded and there is genuinely nothing to analyse. */
    val isEmpty: Boolean get() = !isLoading && !error && (snapshot?.total ?: 0) == 0
}

/**
 * The four distributions the screen shows, in tab order (PLAN.md Phase-10 checklist).
 *
 * The label is a resource id rather than a string, so the tab row stays localizable while the enum
 * itself never touches a `Context` - the same shape `ImportStep` uses for the wizard's page titles.
 * The ordinal is the tab's position, so the slide direction can be derived from it.
 */
enum class AnalyticsTab(@StringRes val labelRes: Int) {
    AGE(R.string.age),
    SERVICE(R.string.service_years),
    DEPARTMENT(R.string.department),
    GRADE(R.string.grade),
}
