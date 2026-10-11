package com.gov.employeedb.presentation.employees.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The searchable, sortable employee list.
 *
 * Phase 3 routes the screen; the list itself arrives in Phase 6 with the Room-backed repository.
 * Until then the truthful state is "no employees yet", which is exactly the state a fresh install
 * will be in once the data layer lands.
 */
@Composable
fun EmployeeListScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(titleRes = R.string.employees, modifier = modifier) { padding ->
        EmptyState(
            icon = Icons.Outlined.People,
            headlineRes = R.string.empty_state_employees,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Preview(name = "Employee list")
@Composable
private fun EmployeeListScreenPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeListScreen()
    }
}