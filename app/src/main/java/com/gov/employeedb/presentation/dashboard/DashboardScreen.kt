package com.gov.employeedb.presentation.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
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
 * Dashboard: the totals, averages and retirement-soon stats, plus the two summary charts.
 *
 * Phase 3 routes the screen and renders the honest empty state — there is no data layer yet, so
 * every statistic would be zero. Phase 9 fills it in with live summaries over the repository.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(titleRes = R.string.dashboard, modifier = modifier) { padding ->
        EmptyState(
            icon = Icons.Outlined.Dashboard,
            headlineRes = R.string.no_data,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Preview(name = "Dashboard")
@Composable
private fun DashboardScreenPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        DashboardScreen()
    }
}