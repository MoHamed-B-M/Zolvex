package com.gov.employeedb.presentation.employees.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.AppTopBar
import com.gov.employeedb.presentation.common.components.ConfirmDialog
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ErrorState
import com.gov.employeedb.presentation.common.components.LoadingShimmer

/**
 * The read-only employee screen (PLAN.md Phase-8 checklist): a large avatar header, the Personal,
 * Professional and Notes cards, and a top bar carrying back, edit and delete.
 *
 * Deleting is the one irreversible thing here, so it goes through [ConfirmDialog] first and the
 * screen only leaves once the repository has actually removed the row ([EmployeeDetailEffect]).
 * Edit is reachable twice on purpose - from the top bar and from the extended FAB - because this
 * screen exists to be read, and the FAB is the primary action once it has been.
 *
 * [avatarModifier] and [nameModifier] come from the navigation graph as this destination's
 * shared-element modifiers; they are passed straight through to the header.
 */
@Composable
fun EmployeeDetailScreen(
    onNavigateUp: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
    viewModel: EmployeeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val employee = state.employee
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmployeeDetailEffect.Deleted -> onNavigateUp()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                titleRes = R.string.employee_detail,
                onNavigateUp = onNavigateUp,
                actions = {
                    if (employee != null) {
                        IconButton(onClick = { onEdit(employee.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (employee != null) {
                ExtendedFloatingActionButton(
                    onClick = { onEdit(employee.id) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                        )
                    },
                    text = { Text(text = stringResource(R.string.edit_employee)) },
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.error -> ErrorState(
                    onRetry = { viewModel.onEvent(EmployeeDetailEvent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )

                state.isLoading -> LoadingShimmer(modifier = Modifier.fillMaxWidth())

                employee == null -> EmptyState(
                    icon = Icons.Outlined.People,
                    headlineRes = R.string.no_data,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> EmployeeDetailContent(
                    employee = employee,
                    modifier = Modifier.fillMaxSize(),
                    avatarModifier = avatarModifier,
                    nameModifier = nameModifier,
                )
            }
        }
    }

    if (confirmDelete && employee != null) {
        ConfirmDialog(
            titleRes = R.string.delete_confirm_title,
            messageRes = R.string.delete_confirm_message,
            onConfirm = {
                confirmDelete = false
                viewModel.onEvent(EmployeeDetailEvent.Delete)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
