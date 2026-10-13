package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.list.EmployeeSort
import com.gov.employeedb.presentation.employees.list.SortDirection
import com.gov.employeedb.presentation.employees.list.SortField

/**
 * The sort dropdown (PLAN.md Phase-6 checklist): the four sortable columns, a divider, then the
 * direction. The active choice on each line carries a check, so the menu always reads as a
 * selection rather than a command list.
 */
@Composable
fun SortMenu(
    expanded: Boolean,
    sort: EmployeeSort,
    onDismiss: () -> Unit,
    onSelected: (EmployeeSort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortField.entries.forEach { field ->
            DropdownMenuItem(
                text = { Text(text = stringResource(field.labelRes)) },
                onClick = { onSelected(sort.copy(field = field)) },
                trailingIcon = { SelectionMark(visible = field == sort.field) },
            )
        }
        HorizontalDivider()
        SortDirection.entries.forEach { direction ->
            DropdownMenuItem(
                text = { Text(text = stringResource(direction.labelRes)) },
                onClick = { onSelected(sort.copy(direction = direction)) },
                trailingIcon = { SelectionMark(visible = direction == sort.direction) },
            )
        }
    }
}

@Composable
private fun SelectionMark(visible: Boolean) {
    if (visible) {
        Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
    }
}

@Preview(name = "SortMenu")
@Composable
private fun SortMenuPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        SortMenu(expanded = true, sort = EmployeeSort(), onDismiss = {}, onSelected = {})
    }
}
