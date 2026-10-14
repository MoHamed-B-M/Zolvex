package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.presentation.common.components.EmployeeCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * One list row wrapped in a bidirectional [SwipeToDismissBox] (PLAN.md Phase-6 checklist). A swipe
 * in either direction reveals a delete affordance and, once it settles, reports the dismiss; the
 * caller shows the Undo Snackbar so the removal is never final without a way back.
 *
 * [avatarModifier] and [titleModifier] are forwarded to the [EmployeeCard] so the Phase-8
 * shared-element transition starts on this row's avatar and name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeRow(
    employee: EmployeeSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        EmployeeCard(
            employee = employee,
            onClick = onClick,
            avatarModifier = avatarModifier,
            titleModifier = titleModifier,
        )
    }
}

@Preview(name = "EmployeeRow")
@Composable
private fun EmployeeRowPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeRow(
            employee = EmployeeSummary(
                id = 1L,
                fullName = "Amine Benali",
                department = "Finance",
                position = "Analyst",
                age = 34,
                serviceYears = 8,
            ),
            onClick = {},
            onDelete = {},
        )
    }
}
