package com.gov.employeedb.presentation.employees.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
 * The read-only employee detail screen, reachable now through the `app://employee/detail/{id}`
 * deep link (or, once Phase 6 lands, from the list).
 *
 * Phase 3 routes the destination; Phase 8 renders the employee from the repository. The
 * `app://employee/detail/{id}` deep link is already active, so routing and argument parsing are
 * verified end to end before the screen has any content to show.
 */
@Composable
fun EmployeeDetailScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.employee_detail,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    ) { padding ->
        EmptyState(
            icon = Icons.Outlined.Info,
            headlineRes = R.string.no_data,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Preview(name = "Employee detail")
@Composable
private fun EmployeeDetailScreenPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeDetailScreen(onNavigateUp = {})
    }
}