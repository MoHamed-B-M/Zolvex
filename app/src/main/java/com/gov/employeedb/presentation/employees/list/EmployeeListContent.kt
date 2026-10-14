package com.gov.employeedb.presentation.employees.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.LoadingShimmer
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.list.components.EmployeeRow
import com.gov.employeedb.presentation.employees.list.components.FilterBar
import com.gov.employeedb.presentation.employees.list.components.SearchBar
import com.gov.employeedb.presentation.theme.AppMotion
import kotlinx.coroutines.delay

/**
 * The scrolling body of the employee list (PLAN.md Phase-6 checklist): a pinned search field, the
 * filter row, then either the shimmer, the honest empty state, or the rows.
 *
 * Rows cascade in through [AppMotion.staggeredDelay] (each index a step later, clamped inside the
 * motion helper so a long list never crawls) and every row uses `Modifier.animateItem()`, so sort
 * and filter changes slide rather than snap. The footer requests the next page as soon as it
 * composes, which is what makes the window feel endless.
 *
 * [avatarModifier] and [titleModifier] turn an employee id into the shared-element modifier that
 * marks that row's avatar and name as the near end of the Phase-8 transition; the default returns
 * plain `Modifier`, so the content still composes on its own in a preview.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmployeeListContent(
    state: EmployeeListUiState,
    departments: List<String>,
    onQueryChange: (String) -> Unit,
    onDepartmentSelected: (String?) -> Unit,
    onOpenFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onEmployeeClick: (Long) -> Unit,
    onDelete: (EmployeeSummary) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: @Composable (Long) -> Modifier = { Modifier },
    titleModifier: @Composable (Long) -> Modifier = { Modifier },
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        stickyHeader(key = "search") {
            Surface(color = MaterialTheme.colorScheme.surface) {
                SearchBar(query = state.query, onQueryChange = onQueryChange)
            }
        }
        item(key = "filters") {
            FilterBar(
                departments = departments,
                filters = state.filters,
                onDepartmentSelected = onDepartmentSelected,
                onOpenSheet = onOpenFilters,
            )
        }
        when {
            state.isLoading -> item(key = "shimmer") {
                Box(modifier = Modifier.padding(vertical = 16.dp)) {
                    LoadingShimmer()
                }
            }

            state.items.isEmpty() -> item(key = "empty") {
                EmptyList(
                    isFiltering = state.isFiltering,
                    onImport = onImport,
                )
            }

            else -> {
                itemsIndexed(
                    items = state.items,
                    key = { _, employee -> employee.id },
                ) { index, employee ->
                    val visibleState = remember { MutableTransitionState(false) }
                    LaunchedEffect(Unit) {
                        delay(AppMotion.staggeredDelay(index))
                        visibleState.targetState = true
                    }
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = AppMotion.effects()) +
                            scaleIn(animationSpec = AppMotion.spatial(), initialScale = 0.96f),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                    ) {
                        EmployeeRow(
                            employee = employee,
                            onClick = { onEmployeeClick(employee.id) },
                            onDelete = { onDelete(employee) },
                            avatarModifier = avatarModifier(employee.id),
                            titleModifier = titleModifier(employee.id),
                        )
                    }
                }
                if (state.canLoadMore) {
                    item(key = "load_more") {
                        LaunchedEffect(state.items.size) { onLoadMore() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyList(
    isFiltering: Boolean,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = if (isFiltering) Icons.Outlined.SearchOff else Icons.Outlined.People,
            headlineRes = if (isFiltering) {
                R.string.empty_state_search
            } else {
                R.string.empty_state_employees
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        )
        if (!isFiltering) {
            Button(onClick = onImport) {
                Text(text = stringResource(R.string.import_excel))
            }
        }
    }
}

@Preview(name = "EmployeeListContent")
@Composable
private fun EmployeeListContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeListContent(
            state = EmployeeListUiState(
                items = listOf(
                    EmployeeSummary(
                        id = 1L,
                        fullName = "Amina Haddad",
                        department = "Finance",
                        position = "Analyst",
                        age = 34,
                        serviceYears = 8,
                    ),
                ),
                total = 1,
                isLoading = false,
            ),
            departments = listOf("Finance"),
            onQueryChange = {},
            onDepartmentSelected = {},
            onOpenFilters = {},
            onLoadMore = {},
            onEmployeeClick = {},
            onDelete = {},
            onImport = {},
        )
    }
}
