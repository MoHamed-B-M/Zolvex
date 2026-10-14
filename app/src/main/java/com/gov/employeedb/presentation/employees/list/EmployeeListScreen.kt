package com.gov.employeedb.presentation.employees.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.ErrorState
import com.gov.employeedb.presentation.employees.list.components.EmployeeListFab
import com.gov.employeedb.presentation.employees.list.components.EmployeeListTopBar
import com.gov.employeedb.presentation.employees.list.components.FilterSheet
import kotlinx.coroutines.launch

/**
 * The searchable, sortable employee list (PLAN.md Phase 6).
 *
 * The screen owns only what is genuinely view state — the FAB, sheet and menu open flags, and the
 * Snackbar host — while the list itself lives in [EmployeeListViewModel]. Deletion is optimistic:
 * the swipe reports immediately and the Snackbar's Undo restores the full record the ViewModel kept.
 *
 * The top and horizontal insets belong to this screen's own Scaffold; the bottom system bar is
 * consumed by the navigation shell's bottom bar, so the content inset here excludes it.
 *
 * [avatarModifier] and [titleModifier] arrive from the navigation graph as the shared-element
 * modifiers for a given employee and are forwarded to the list content untouched.
 */
@Composable
fun EmployeeListScreen(
    onEmployeeClick: (Long) -> Unit,
    onAddEmployee: () -> Unit,
    onImportExcel: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: @Composable (Long) -> Modifier = { Modifier },
    titleModifier: @Composable (Long) -> Modifier = { Modifier },
    viewModel: EmployeeListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by rememberSaveable { mutableStateOf(false) }
    val undoLabel = stringResource(R.string.undo)
    val deletedLabel = stringResource(R.string.delete_employee)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmployeeListEffect.NavigateToAdd -> onAddEmployee()
                EmployeeListEffect.NavigateToImport -> onImportExcel()
            }
        }
    }

    val departments = remember(state.items) {
        state.items.mapNotNull { it.department }.distinct().sorted()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            EmployeeListTopBar(
                isSeeding = state.isSeeding,
                sort = state.sort,
                sortMenuOpen = sortMenuOpen,
                onSortMenuOpenChange = { sortMenuOpen = it },
                onSortSelected = { sort ->
                    viewModel.onEvent(EmployeeListEvent.SetSort(sort))
                    sortMenuOpen = false
                },
                onSeed = { viewModel.onEvent(EmployeeListEvent.SeedDebugData) },
            )
        },
        floatingActionButton = {
            EmployeeListFab(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                onAddEmployee = { viewModel.onEvent(EmployeeListEvent.AddClick) },
                onImportExcel = { viewModel.onEvent(EmployeeListEvent.ImportClick) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.error) {
                ErrorState(
                    onRetry = { viewModel.onEvent(EmployeeListEvent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                EmployeeListContent(
                    state = state,
                    departments = departments,
                    onQueryChange = { viewModel.onEvent(EmployeeListEvent.Search(it)) },
                    onDepartmentSelected = { department ->
                        viewModel.onEvent(
                            EmployeeListEvent.ApplyFilter(
                                state.filters.copy(
                                    departments = department?.let { setOf(it) } ?: emptySet(),
                                ),
                            ),
                        )
                    },
                    onOpenFilters = { showFilters = true },
                    onLoadMore = { viewModel.onEvent(EmployeeListEvent.LoadMore) },
                    onEmployeeClick = onEmployeeClick,
                    onDelete = { employee ->
                        viewModel.onEvent(EmployeeListEvent.Delete(employee.id))
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = deletedLabel,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onEvent(EmployeeListEvent.UndoDelete)
                            }
                        }
                    },
                    onImport = onImportExcel,
                    avatarModifier = avatarModifier,
                    titleModifier = titleModifier,
                )
            }
        }
        if (showFilters) {
            FilterSheet(
                departments = departments,
                filters = state.filters,
                onApply = { filters ->
                    viewModel.onEvent(EmployeeListEvent.ApplyFilter(filters))
                    showFilters = false
                },
                onDismiss = { showFilters = false },
            )
        }
    }
}
