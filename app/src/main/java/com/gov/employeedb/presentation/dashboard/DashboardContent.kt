package com.gov.employeedb.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.presentation.common.components.BarChartCard
import com.gov.employeedb.presentation.common.components.BucketLegend
import com.gov.employeedb.presentation.common.components.DonutChart
import com.gov.employeedb.presentation.common.components.EmployeeCard
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ErrorState
import com.gov.employeedb.presentation.common.components.LoadingShimmer
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.components.SeedTestDataAction
import com.gov.employeedb.presentation.common.components.StatCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * The dashboard body (PLAN.md Phase-9 checklist): the stat grid, the age and service distributions
 * and the newest employees, over a pull-to-refresh gesture.
 *
 * [state] carries the four outcomes apart: [DashboardUiState.error] replaces the whole body with a
 * retry affordance; otherwise the list is wrapped in [PullToRefreshBox] so a pull can read again
 * without the screen ever going blank. The loading and empty cases are items of the same list rather
 * than a branch around it, which is what keeps the pull gesture alive while they are on screen.
 */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onSeed: () -> Unit,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.dashboard,
        modifier = modifier,
        actions = { SeedTestDataAction(isSeeding = state.isSeeding, onSeed = onSeed) },
    ) { padding ->
        if (state.error) {
            ErrorState(
                onRetry = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                DashboardList(state = state, onEmployeeClick = onEmployeeClick)
            }
        }
    }
}

@Composable
private fun DashboardList(
    state: DashboardUiState,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            state.isLoading -> item(key = "loading") {
                Box(modifier = Modifier.padding(vertical = 16.dp)) {
                    LoadingShimmer()
                }
            }

            state.isEmpty -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.People,
                    headlineRes = R.string.empty_state_employees,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
            }

            else -> {
                item(key = "stats") {
                    StaggeredSection(index = 0) { StatGrid(state = state) }
                }
                item(key = "age") {
                    StaggeredSection(index = 1) {
                        SectionCard(titleRes = R.string.analytics_age) {
                            DonutChart(
                                buckets = state.ageBuckets,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(200.dp),
                            )
                            BucketLegend(buckets = state.ageBuckets)
                        }
                    }
                }
                item(key = "service") {
                    StaggeredSection(index = 2) { BarChartCard(buckets = state.serviceBuckets) }
                }
                item(key = "recent_header") {
                    StaggeredSection(index = 3) {
                        Text(
                            text = stringResource(R.string.recent_employees),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                itemsIndexed(
                    items = state.recent,
                    key = { _, employee -> employee.id },
                ) { index, employee ->
                    StaggeredSection(index = index + RECENT_SECTION_OFFSET) {
                        EmployeeCard(
                            employee = employee,
                            onClick = { onEmployeeClick(employee.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGrid(state: DashboardUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(
                icon = Icons.Outlined.People,
                labelRes = R.string.total_employees,
                value = state.total,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.CalendarMonth,
                labelRes = R.string.average_age,
                value = state.averageAge,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(
                icon = Icons.Outlined.Analytics,
                labelRes = R.string.average_service,
                value = state.averageService,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.WarningAmber,
                labelRes = R.string.retirement_soon,
                value = state.retirementSoon,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One dashboard section, fading and sliding in with a delay derived from [index].
 *
 * The transition state starts settled under inspection so a preview shows the finished layout, and
 * the [LaunchedEffect] starts it on a real device regardless of what it started at. Nothing is
 * expanded or collapsed, so the entrance never re-measures the list item behind it.
 */
@Composable
private fun StaggeredSection(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val visibleState = remember { MutableTransitionState(inspectionMode) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = AppMotion.sectionEntrySpec(index)) +
            slideInVertically(animationSpec = AppMotion.sectionEntrySpec(index)) { it / 8 },
        modifier = modifier,
    ) {
        content()
    }
}

/** The first [EmployeeSummary] section index, after the stat grid and the two charts. */
private const val RECENT_SECTION_OFFSET = 4

@Preview(name = "DashboardContent")
@Composable
private fun DashboardContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        DashboardContent(
            state = previewState,
            onRefresh = {},
            onSeed = {},
            onEmployeeClick = {},
        )
    }
}

private val previewState = DashboardUiState(
    isLoading = false,
    total = 100,
    averageAge = 34.7,
    averageService = 6.0,
    retirementSoon = 12,
    ageBuckets = listOf(
        BucketCount(AgeBucket.UNDER_25.name, 12, 0.12f),
        BucketCount(AgeBucket.B25_34.name, 34, 0.34f),
        BucketCount(AgeBucket.B35_44.name, 28, 0.28f),
        BucketCount(AgeBucket.B45_54.name, 18, 0.18f),
        BucketCount(AgeBucket.B55_PLUS.name, 8, 0.08f),
    ),
    serviceBuckets = listOf(
        BucketCount(ServiceBucket.LESS_5.name, 22, 0.22f),
        BucketCount(ServiceBucket.B5_10.name, 31, 0.31f),
        BucketCount(ServiceBucket.B10_15.name, 26, 0.26f),
        BucketCount(ServiceBucket.B15_20.name, 15, 0.15f),
        BucketCount(ServiceBucket.B20_PLUS.name, 6, 0.06f),
    ),
    recent = listOf(
        EmployeeSummary(1L, "Amine Benali", "Finance", "Analyst", 34, 8),
        EmployeeSummary(2L, "Sara Haddad", "Human Resources", "Officer", 41, 12),
        EmployeeSummary(3L, "Youssef Karim", "Operations", "Manager", 52, 21),
    ),
)
