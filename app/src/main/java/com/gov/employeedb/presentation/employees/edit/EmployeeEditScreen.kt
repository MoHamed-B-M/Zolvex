package com.gov.employeedb.presentation.employees.edit

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
 * The employee form, shared by create (`id == null`) and edit (`id` set).
 *
 * Phase 3 routes the destination so the graph is complete and back navigation from it works; the
 * form itself arrives in Phase 8 with the repository it validates against.
 */
@Composable
fun EmployeeEditScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.edit_employee,
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

@Preview(name = "Employee edit")
@Composable
private fun EmployeeEditScreenPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeEditScreen(onNavigateUp = {})
    }
}