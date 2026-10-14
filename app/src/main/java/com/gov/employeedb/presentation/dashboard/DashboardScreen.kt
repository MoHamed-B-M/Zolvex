package com.gov.employeedb.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The dashboard destination (PLAN.md Phase-9 checklist): the Hilt glue around [DashboardContent].
 *
 * All state lives in [DashboardViewModel] and the whole layout is [DashboardContent], so this
 * function only wires the two together. [onEmployeeClick] travels up to the navigation graph, which
 * is what opens the detail screen from the newest-employees list.
 */
@Composable
fun DashboardScreen(
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onRefresh = { viewModel.onEvent(DashboardEvent.Refresh) },
        onSeed = { viewModel.onEvent(DashboardEvent.SeedDebugData) },
        onEmployeeClick = onEmployeeClick,
        modifier = modifier,
    )
}
