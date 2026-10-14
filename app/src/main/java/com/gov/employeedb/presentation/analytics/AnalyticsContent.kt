package com.gov.employeedb.presentation.analytics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.analytics.tabs.AgeTab
import com.gov.employeedb.presentation.analytics.tabs.DepartmentTab
import com.gov.employeedb.presentation.analytics.tabs.GradeTab
import com.gov.employeedb.presentation.analytics.tabs.ServiceTab
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ErrorState
import com.gov.employeedb.presentation.common.components.LoadingShimmer
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * The analytics screen without its ViewModel (PLAN.md Phase-10 checklist): the four tabs over the
 * statistic cards and charts, the department filter chips and the export action.
 *
 * The four outcomes are branched apart rather than layered: [AnalyticsUiState.error] replaces the
 * body with a retry affordance, the first read shows the shimmer, and a finished read with no rows
 * shows the empty state, whose import button is the only way to make it stop being empty. Everything
 * else - including a re-filter, which keeps the previous snapshot - renders the tabs.
 */
@Composable
fun AnalyticsContent(
    state: AnalyticsUiState,
    snackbarHostState: SnackbarHostState,
    onSelectDepartment: (String?) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.analytics,
        modifier = modifier,
        actions = { AnalyticsExportAction(isExporting = state.isExporting, onExport = onExport) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            state.error -> ErrorState(onRetry = onRetry, modifier = contentModifier)
            state.isLoading -> LoadingShimmer(modifier = contentModifier)
            state.isEmpty -> AnalyticsEmptyState(onImport = onImport, modifier = contentModifier)
            else -> AnalyticsTabs(
                state = state,
                onSelectDepartment = onSelectDepartment,
                onEmployeeClick = onEmployeeClick,
                modifier = contentModifier,
            )
        }
    }
}

/** The tabs, their filter row and the sliding [AnimatedContent] that swaps one tab for the next. */
@Composable
private fun AnalyticsTabs(
    state: AnalyticsUiState,
    onSelectDepartment: (String?) -> Unit,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = state.snapshot ?: return
    val tabs = AnalyticsTab.entries
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val slideSpec = AppMotion.fastEffects<IntOffset>()
    val fadeSpec = AppMotion.fastEffects<Float>()
    // A forward step slides the new tab in from the end edge: the right in LTR, the left in RTL.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = stringResource(tab.labelRes)) },
                )
            }
        }
        DepartmentFilterRow(
            departments = state.departments,
            selectedDepartment = state.selectedDepartment,
            onSelectDepartment = onSelectDepartment,
        )
        AnimatedContent(
            targetState = tabs[selectedTabIndex],
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val sign = if (forward) 1 else -1
                val direction = if (rtl) -sign else sign
                (
                    slideInHorizontally(animationSpec = slideSpec) { direction * it } +
                        fadeIn(animationSpec = fadeSpec)
                    )
                    .togetherWith(
                        slideOutHorizontally(animationSpec = slideSpec) { -direction * it } +
                            fadeOut(animationSpec = fadeSpec),
                    )
                    // `null` drops the size animation: the slots are all the same fixed height, so
                    // animating it would only make the charts bob up and down.
                    .using(null)
            },
            contentKey = { it },
            label = "analyticsTab",
            modifier = Modifier.weight(1f),
        ) { tab ->
            when (tab) {
                AnalyticsTab.AGE -> AgeTab(snapshot = snapshot)
                AnalyticsTab.SERVICE -> ServiceTab(
                    snapshot = snapshot,
                    onEmployeeClick = onEmployeeClick,
                )
                AnalyticsTab.DEPARTMENT -> DepartmentTab(
                    snapshot = snapshot,
                    selectedDepartment = state.selectedDepartment,
                    onDepartmentSelected = onSelectDepartment,
                )
                AnalyticsTab.GRADE -> GradeTab(snapshot = snapshot)
            }
        }
    }
}

/**
 * The department quick-chips. [departments] is the unfiltered list, so the chip that is currently
 * filtering stays on screen; tapping it again clears the filter, which is the one-tap way back to
 * the whole workforce (the same toggle the employee list's filter bar uses).
 */
@Composable
private fun DepartmentFilterRow(
    departments: List<String>,
    selectedDepartment: String?,
    onSelectDepartment: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = departments, key = { it }) { department ->
            val selected = department == selectedDepartment
            FilterChip(
                selected = selected,
                onClick = { onSelectDepartment(if (selected) null else department) },
                label = { Text(text = department) },
            )
        }
    }
}

/** The "no data to analyse" block, with the import that would change that. */
@Composable
private fun AnalyticsEmptyState(onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.Analytics,
            headlineRes = R.string.empty_state_analytics,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onImport) { Text(text = stringResource(R.string.import_excel)) }
    }
}

/** The export top-bar action; while a workbook is being written the icon becomes a spinner. */
@Composable
private fun AnalyticsExportAction(
    isExporting: Boolean,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onExport, modifier = modifier, enabled = !isExporting) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = stringResource(R.string.cd_export),
            )
        }
    }
}

@Preview(name = "AnalyticsExportAction")
@Composable
private fun AnalyticsExportActionPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AnalyticsExportAction(isExporting = false, onExport = {})
    }
}

@Preview(name = "AnalyticsContent")
@Composable
private fun AnalyticsContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AnalyticsContent(
            state = previewState,
            snackbarHostState = SnackbarHostState(),
            onSelectDepartment = {},
            onExport = {},
            onImport = {},
            onRetry = {},
            onEmployeeClick = {},
        )
    }
}

private val previewState = AnalyticsUiState(
    isLoading = false,
    snapshot = AnalyticsSnapshot(
        total = 120,
        averageAge = 41.2,
        medianAge = 40.0,
        youngestAge = 22,
        oldestAge = 63,
        averageService = 9.4,
        medianService = 8.0,
        retirementSoon = 7,
        ageBuckets = listOf(
            BucketCount(AgeBucket.UNDER_25.name, 6, 0.05f),
            BucketCount(AgeBucket.B25_34.name, 34, 0.28f),
            BucketCount(AgeBucket.B35_44.name, 42, 0.35f),
            BucketCount(AgeBucket.B45_54.name, 28, 0.23f),
            BucketCount(AgeBucket.B55_PLUS.name, 10, 0.09f),
        ),
        serviceBuckets = emptyList(),
        departmentCounts = listOf(
            BucketCount("Finance", 32, 0.27f),
            BucketCount("Health", 27, 0.23f),
            BucketCount("Education", 21, 0.18f),
        ),
        gradeCounts = emptyList(),
        longestServing = emptyList(),
    ),
    departments = listOf("Finance", "Health", "Education"),
)
