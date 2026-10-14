package com.gov.employeedb.presentation.employees.detail

import androidx.compose.runtime.Immutable
import com.gov.employeedb.domain.model.Employee

/**
 * Everything the read-only employee screen renders from (PLAN.md Phase-8 checklist).
 *
 * [isLoading] starts true because the row is observed by id from the moment the screen composes. A
 * finished load with a null [employee] is the honest "no such row" answer (deleted elsewhere, or a
 * stale deep link): it is a final state, unlike [error], so the screen offers no retry for it.
 */
@Immutable
data class EmployeeDetailUiState(
    val employee: Employee? = null,
    val isLoading: Boolean = true,
    val error: Boolean = false,
)
