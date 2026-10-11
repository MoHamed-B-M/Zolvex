package com.gov.employeedb.presentation.analytics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
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
 * Analytics: the Age / Service / Department / Grade tabs with their Vico charts.
 *
 * Phase 3 routes the screen; the four tabs arrive in Phase 10 together with the repository data
 * they summarise. There is no analysis to show before the first import, hence the empty state.
 */
@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    ScreenScaffold(titleRes = R.string.analytics, modifier = modifier) { padding ->
        EmptyState(
            icon = Icons.Outlined.Analytics,
            headlineRes = R.string.empty_state_analytics,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Preview(name = "Analytics")
@Composable
private fun AnalyticsScreenPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AnalyticsScreen()
    }
}